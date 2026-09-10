package com.haritalar.core.traffic

import com.haritalar.core.navigation.GeoCoordinate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

/** Fetches verified traffic and applies the canonical geometry-aware ranking engine. */
class TrafficRouteRankingService(
    private val providerChain: TrafficProviderChain,
) {
    data class DetailedResult(
        val ranked: List<TrafficRouteRanking.RankedCandidate>,
        val matchedSegmentsByRoute: Map<String, List<TrafficRouteSegment>>,
    )

    suspend fun rank(
        routes: List<TrafficRouteRanking.RouteCandidate>,
        nowEpochMs: Long = System.currentTimeMillis(),
    ): List<TrafficRouteRanking.RankedCandidate> = rankDetailedSuspend(routes, nowEpochMs).ranked

    /**
     * Blocking bridge for Android UI/executor callers that are not suspend functions.
     * The actual provider/ranking pipeline remains suspend-based and is executed once.
     */
    fun rankDetailed(
        routes: List<TrafficRouteRanking.RouteCandidate>,
        nowEpochMs: Long = System.currentTimeMillis(),
        timeoutMs: Long = DEFAULT_BLOCKING_TIMEOUT_MS,
    ): DetailedResult {
        require(timeoutMs > 0L) { "timeoutMs must be positive" }
        val completed = CountDownLatch(1)
        val result = AtomicReference<Result<DetailedResult>?>()
        val rankingBlock: suspend () -> DetailedResult = { rankDetailedSuspend(routes, nowEpochMs) }
        rankingBlock.startCoroutine(object : Continuation<DetailedResult> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(value: Result<DetailedResult>) {
                result.set(value)
                completed.countDown()
            }
        })
        if (!completed.await(timeoutMs, TimeUnit.MILLISECONDS)) {
            throw IllegalStateException("Detailed traffic ranking timed out after ${timeoutMs}ms")
        }
        return result.get()?.getOrThrow()
            ?: throw IllegalStateException("Detailed traffic ranking completed without a result")
    }

    /** Suspend implementation shared by the public suspend and blocking bridges. */
    private suspend fun rankDetailedSuspend(
        routes: List<TrafficRouteRanking.RouteCandidate>,
        nowEpochMs: Long,
    ): DetailedResult {
        if (routes.isEmpty()) return DetailedResult(emptyList(), emptyMap())

        /*
         * A single snapshot built from all alternative geometries can undersample
         * individual routes: TomTom Flow Segment Data is point-based. One request
         * is therefore made for one representative point per route, bounded by the
         * number of route alternatives. This keeps traffic attribution route-local
         * and avoids applying one route's segment to another route.
         */
        val observations = routes.mapNotNull { route ->
            val point = representativePoint(route.coordinates) ?: return@mapNotNull null
            val snapshot = runCatching {
                providerChain.fetch(
                    coordinate = point,
                    bounds = boundsFor(route.coordinates),
                    route = TrafficRoute(listOf(point)),
                    nowEpochMs = nowEpochMs,
                )
            }.getOrNull()
            route.routeId to snapshot
        }.toMap()

        val matchedByRoute = routes.associate { route ->
            val snapshot = observations[route.routeId]
            route.routeId to if (snapshot != null && snapshot.isUsable(nowEpochMs) && snapshot.confidence != TrafficConfidence.LOW) {
                TrafficRouteAdapter.matchSnapshotToRoute(
                    route = route.coordinates,
                    snapshot = snapshot,
                    nowEpochMs = nowEpochMs,
                )
            } else {
                emptyList()
            }
        }

        val ranked = routes.map { route ->
            val snapshot = observations[route.routeId]
            if (snapshot == null || !snapshot.isUsable(nowEpochMs) || snapshot.confidence == TrafficConfidence.LOW) {
                withoutTraffic(route)
            } else {
                TrafficRouteRanking.rank(
                    routes = listOf(route),
                    snapshot = snapshot,
                    nowEpochMs = nowEpochMs,
                ).single()
            }
        }.sortedWith(
            compareBy<TrafficRouteRanking.RankedCandidate> { it.adjustedDurationSeconds }
                .thenBy { it.baseDurationSeconds }
                .thenBy { it.routeId },
        )

        return DetailedResult(ranked = ranked, matchedSegmentsByRoute = matchedByRoute)
    }

    fun rankBlocking(
        routes: List<TrafficRouteRanking.RouteCandidate>,
        nowEpochMs: Long = System.currentTimeMillis(),
        timeoutMs: Long = DEFAULT_BLOCKING_TIMEOUT_MS,
    ): List<TrafficRouteRanking.RankedCandidate> {
        require(timeoutMs > 0L) { "timeoutMs must be positive" }
        val completed = CountDownLatch(1)
        val result = AtomicReference<Result<List<TrafficRouteRanking.RankedCandidate>>?>()
        val rankingBlock: suspend () -> List<TrafficRouteRanking.RankedCandidate> = { rank(routes, nowEpochMs) }
        rankingBlock.startCoroutine(object : Continuation<List<TrafficRouteRanking.RankedCandidate>> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(value: Result<List<TrafficRouteRanking.RankedCandidate>>) {
                result.set(value)
                completed.countDown()
            }
        })
        if (!completed.await(timeoutMs, TimeUnit.MILLISECONDS)) {
            throw IllegalStateException("Traffic ranking timed out after ${timeoutMs}ms")
        }
        return result.get()?.getOrThrow() ?: throw IllegalStateException("Traffic ranking completed without a result")
    }

    private fun withoutTraffic(route: TrafficRouteRanking.RouteCandidate) = TrafficRouteRanking.RankedCandidate(
        routeId = route.routeId,
        baseDurationSeconds = route.baseDurationSeconds,
        adjustedDurationSeconds = route.baseDurationSeconds,
        trafficApplied = false,
    )

    private fun representativePoint(coordinates: List<GeoCoordinate>): GeoCoordinate? {
        val valid = coordinates.filter(::validCoordinate)
        if (valid.isEmpty()) return null
        return valid[valid.lastIndex / 2]
    }

    private fun boundsFor(coordinates: List<GeoCoordinate>): TrafficBounds {
        val valid = coordinates.filter(::validCoordinate)
        require(valid.isNotEmpty()) { "No valid coordinates for traffic bounds" }
        return TrafficBounds(
            south = valid.minOf { it.latitude },
            west = valid.minOf { it.longitude },
            north = valid.maxOf { it.latitude },
            east = valid.maxOf { it.longitude },
        )
    }

    private fun validCoordinate(coordinate: GeoCoordinate): Boolean =
        coordinate.latitude.isFinite() && coordinate.longitude.isFinite() &&
            coordinate.latitude in -90.0..90.0 && coordinate.longitude in -180.0..180.0

    private companion object { const val DEFAULT_BLOCKING_TIMEOUT_MS = 120_000L }
}
