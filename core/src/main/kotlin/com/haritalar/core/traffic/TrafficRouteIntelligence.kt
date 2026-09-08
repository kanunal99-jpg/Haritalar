package com.haritalar.core.traffic

/**
 * Canonical traffic-intelligence entry point kept for callers that use the
 * TrafficRouteIntelligence name. The existing RouteTrafficIntelligence engine
 * remains the single implementation so ranking behavior is not duplicated.
 */
object TrafficRouteIntelligence {
    data class RouteInput(
        val routeId: String,
        val baseDurationSeconds: Long,
        val trafficSegments: List<TrafficRouteSegment> = emptyList(),
    )

    data class RankedRoute(
        val routeId: String,
        val baseDurationSeconds: Long,
        val adjustedDurationSeconds: Long,
        val trafficApplied: Boolean,
    )

    fun rank(routes: List<RouteInput>): List<RankedRoute> =
        RouteTrafficIntelligence.rank(
            routes.map { route ->
                RouteTrafficIntelligence.RouteInput(
                    routeId = route.routeId,
                    baseDurationSeconds = route.baseDurationSeconds,
                    trafficSegments = route.trafficSegments,
                )
            },
        ).map { ranked ->
            RankedRoute(
                routeId = ranked.routeId,
                baseDurationSeconds = ranked.baseDurationSeconds,
                adjustedDurationSeconds = ranked.adjustedDurationSeconds,
                trafficApplied = ranked.trafficApplied,
            )
        }
}
