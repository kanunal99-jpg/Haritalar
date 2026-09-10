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

    /**
     * Each alternative gets its own bounded multi-point observation. TomTom Flow
     * Segment Data is point-based, so sparse route samples can miss the road
     * segment actually driven by the route. The provider receives the full route
     * and applies its own bounded evenly distributed sampling.
     */
    private suspend fun rankDetailedSuspend(
        routes: List<TrafficRouteRanking.RouteCandidate>,
        nowEpochMs: Long,
    ): DetailedResult {
        if (routes.isEmpty()) return DetailedResult(emptyList(), emptyMap())

        val observations = routes.mapNotNull { route ->
            val points = representativePoints(route.coordinates)
            if (points.isEmpty()) return@mapNotNull null
            val snapshot = runCatching {
                providerChain.fetch(
                    coordinate = points.first(),
                    bounds = boundsFor(route.coordinates),
                    route = TrafficRoute(route.coordinates),
                    nowEpochMs = nowEpochMs,
                )
            }.getOrNull()
            route.routeId to snapshot
        }.toMap()

        val matchedByRoute = buildMap {
            routes.forEach { route ->
                val snapshot = observations[route.routeId]
                val matched = if (snapshot != null && snapshot.isUsable(nowEpochMs) && snapshot.confidence != TrafficConfidence.LOW) {
                    TrafficRouteAdapter.matchSnapshotToRoute(
                        route = route.coordinates,
                        snapshot = snapshot,
                        nowEpochMs = nowEpochMs,
                    )
                } else {
                    emptyList()
                }
                put(route.routeId, matched)
                if (route.routeId.endsWith("|true")) {
                    put(route.routeId.removeSuffix("|true") + "|false", matched)
                }
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

    private fun representativePoints(coordinates: List<GeoCoordinate>, maxPoints: Int = 4): List<GeoCoordinate> {
        val valid = coordinates.filter(::validCoordinate).distinct()
        if (valid.isEmpty()) return emptyList()
        if (valid.size <= maxPoints) return valid
        return (0 until maxPoints).map { index ->
            valid[index * valid.lastIndex / (maxPoints - 1)]
        }.distinct()
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
