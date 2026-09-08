package com.haritalar.core.traffic

/**
 * Converts provider traffic observations into a route ETA adjustment without
 * inventing traffic values. A segment is adjusted only when both live and
 * free-flow speeds are available; otherwise its original duration is retained.
 */
object TrafficRouteCostModel {
    fun adjustedDurationSeconds(
        baseDurationSeconds: Long,
        segments: List<TrafficRouteSegment>,
    ): Long {
        if (baseDurationSeconds <= 0L || segments.isEmpty()) return baseDurationSeconds

        val baseDistance = segments.sumOf { it.distanceMeters.coerceAtLeast(0.0) }
        if (baseDistance <= 0.0) return baseDurationSeconds

        val weightedFactor = segments.sumOf { segment ->
            segment.distanceMeters.coerceAtLeast(0.0) * durationFactor(segment.traffic)
        } / baseDistance

        return kotlin.math.ceil(baseDurationSeconds * weightedFactor.coerceAtLeast(1.0)).toLong()
    }

    private fun durationFactor(segment: TrafficSegment): Double {
        if (segment.closure) return 1.0
        val live = segment.speedKmh
        val freeFlow = segment.freeFlowSpeedKmh
        if (live == null || freeFlow == null || live <= 0.0 || freeFlow <= 0.0) return 1.0
        return (freeFlow / live).coerceIn(1.0, 6.0)
    }
}

data class TrafficRouteSegment(
    val distanceMeters: Double,
    val traffic: TrafficSegment,
)
