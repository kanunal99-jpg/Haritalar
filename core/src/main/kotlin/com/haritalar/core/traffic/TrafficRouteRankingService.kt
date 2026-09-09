package com.haritalar.core.traffic

import com.haritalar.core.navigation.GeoCoordinate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

/**
 * Fetches at most one verified traffic snapshot for a route set and applies it
 * to every candidate through the canonical geometry-aware ranking engine.
 *
 * This is deliberately UI-agnostic: callers can use the same orchestration
 * from route cards, navigation refresh, or rerouting without duplicating the
 * provider/cache/fallback rules.
 */
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
    ): List<TrafficRouteRanking.RankedCandidate> {
        return rankDetailed(routes, nowEpochMs).ranked
    }

    /**
     * Same verified snapshot as [rank], additionally exposing only the traffic
     * segments whose provider geometry was actually matched to each route.
     * The UI may render these segments; unmatched or unusable data never leaks
     * into the map.
     */
    suspend fun rankDetailed(
        routes: List<TrafficRouteRanking.RouteCandidate>,
        nowEpochMs: Long = System.currentTimeMillis(),
    ): DetailedResult {
        if (routes.isEmpty()) return DetailedResult(emptyList(), emptyMap())

        val allCoordinates = routes.flatMap { it.coordinates }.filter(::validCoordinate)
        if (allCoordinates.isEmpty()) {
            return DetailedResult(routes.map(::withoutTraffic), emptyMap())
        }

        val snapshot = runCatching {
            providerChain.fetch(
                coordinate = allCoordinates.first(),
                bounds = boundsFor(allCoordinates),
                route = TrafficRoute(allCoordinates),
                nowEpochMs = nowEpochMs,
            )
        }.getOrNull()

        if (snapshot == null || !snapshot.isUsable(nowEpochMs) || snapshot.confidence == TrafficConfidence.LOW) {
            return DetailedResult(routes.map(::withoutTraffic), emptyMap())
        }

        val matchedByRoute = routes.associate { route ->
            route.routeId to TrafficRouteMatcher.match(
                route = route.coordinates,
                segments = snapshot.segments,
                nowEpochMs = nowEpochMs,
            )
        }
        val ranked = TrafficRouteRanking.rank(
            routes = routes,
            snapshot = snapshot,
            nowEpochMs = nowEpochMs,
        )
        return DetailedResult(ranked, matchedByRoute)
    }

    fun rankBlocking(
        routes: List<TrafficRouteRanking.RouteCandidate>,
        nowEpochMs: Long = System.currentTimeMillis(),
        timeoutMs: Long = DEFAULT_BLOCKING_TIMEOUT_MS,
    ): List<TrafficRouteRanking.RankedCandidate> {
        require(timeoutMs > 0L) { "timeoutMs must be positive" }
        val completed = CountDownLatch(1)
        val result = AtomicReference<Result<List<TrafficRouteRanking.RankedCandidate>>?>()
        val rankingBlock: suspend () -> List<TrafficRouteRanking.RankedCandidate> = {
            rank(routes, nowEpochMs)
        }
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
        return result.get()?.getOrThrow()
            ?: throw IllegalStateException("Traffic ranking completed without a result")
    }

    private fun withoutTraffic(route: TrafficRouteRanking.RouteCandidate) =
        TrafficRouteRanking.RankedCandidate(
            routeId = route.routeId,
            baseDurationSeconds = route.baseDurationSeconds,
            adjustedDurationSeconds = route.baseDurationSeconds,
            trafficApplied = false,
        )

    private fun boundsFor(coordinates: List<GeoCoordinate>): TrafficBounds {
        val south = coordinates.minOf { it.latitude }
        val north = coordinates.maxOf { it.latitude }
        val west = coordinates.minOf { it.longitude }
        val east = coordinates.maxOf { it.longitude }
        return TrafficBounds(south = south, west = west, north = north, east = east)
    }

    private fun validCoordinate(coordinate: GeoCoordinate): Boolean =
        coordinate.latitude.isFinite() && coordinate.longitude.isFinite() &&
            coordinate.latitude in -90.0..90.0 && coordinate.longitude in -180.0..180.0

    private companion object {
        const val DEFAULT_BLOCKING_TIMEOUT_MS = 120_000L
    }
}
