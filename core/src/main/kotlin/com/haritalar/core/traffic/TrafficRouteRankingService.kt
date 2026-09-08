package com.haritalar.core.traffic

import com.haritalar.core.navigation.GeoCoordinate

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
    suspend fun rank(
        routes: List<TrafficRouteRanking.RouteCandidate>,
        nowEpochMs: Long = System.currentTimeMillis(),
    ): List<TrafficRouteRanking.RankedCandidate> {
        if (routes.isEmpty()) return emptyList()

        val allCoordinates = routes.flatMap { it.coordinates }.filter(::validCoordinate)
        if (allCoordinates.isEmpty()) return routes.map(::withoutTraffic)

        val snapshot = runCatching {
            providerChain.fetch(
                coordinate = allCoordinates.first(),
                bounds = boundsFor(allCoordinates),
                route = TrafficRoute(allCoordinates),
                nowEpochMs = nowEpochMs,
            )
        }.getOrNull()

        return TrafficRouteRanking.rank(
            routes = routes,
            snapshot = snapshot,
            nowEpochMs = nowEpochMs,
        )
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
}
