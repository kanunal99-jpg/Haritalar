package com.haritalar.core.traffic

/**
 * Applies verified traffic observations to already calculated route alternatives.
 *
 * The router remains responsible for geometry; this class only changes the ETA
 * used for comparison. A route without usable traffic observations keeps its
 * provider ETA unchanged, so missing traffic never becomes invented traffic.
 */
object RouteTrafficIntelligence {
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

    fun rank(routes: List<RouteInput>): List<RankedRoute> = routes
        .map { route ->
            val adjusted = TrafficRouteCostModel.adjustedDurationSeconds(
                route.baseDurationSeconds,
                route.trafficSegments,
            )
            RankedRoute(
                routeId = route.routeId,
                baseDurationSeconds = route.baseDurationSeconds,
                adjustedDurationSeconds = adjusted,
                trafficApplied = route.trafficSegments.any { hasUsableSpeed(it.traffic) },
            )
        }
        .sortedWith(compareBy<RankedRoute> { it.adjustedDurationSeconds }.thenBy { it.baseDurationSeconds }.thenBy { it.routeId })

    private fun hasUsableSpeed(segment: TrafficSegment): Boolean {
        val live = segment.speedKmh
        val freeFlow = segment.freeFlowSpeedKmh
        return live != null && freeFlow != null && live > 0.0 && freeFlow > 0.0
    }
}
