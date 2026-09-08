package com.haritalar.core.traffic

/**
 * Applies verified traffic observations to already calculated route alternatives.
 *
 * The router remains responsible for geometry; this class only changes the ETA
 * used for comparison. A route without usable traffic observations keeps its
 * provider ETA unchanged, so missing or low-confidence traffic never becomes
 * invented traffic.
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
            val usableSegments = route.trafficSegments.filter { hasUsableTraffic(it.traffic) }
            val adjusted = TrafficRouteCostModel.adjustedDurationSeconds(
                route.baseDurationSeconds,
                usableSegments,
            )
            RankedRoute(
                routeId = route.routeId,
                baseDurationSeconds = route.baseDurationSeconds,
                adjustedDurationSeconds = adjusted,
                trafficApplied = usableSegments.isNotEmpty(),
            )
        }
        .sortedWith(compareBy<RankedRoute> { it.adjustedDurationSeconds }.thenBy { it.baseDurationSeconds }.thenBy { it.routeId })

    private fun hasUsableTraffic(segment: TrafficSegment): Boolean {
        // A verified closure is actionable even when the provider has no speed
        // data and therefore uses the model's closure penalty.
        if (segment.closure) return true
        if (segment.confidence == TrafficConfidence.LOW) return false

        val live = segment.speedKmh
        val freeFlow = segment.freeFlowSpeedKmh
        return live != null && freeFlow != null && live > 0.0 && freeFlow > 0.0
    }
}
