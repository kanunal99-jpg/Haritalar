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

        val validSegments = segments.filter { it.distanceMeters.isFinite() && it.distanceMeters > 0.0 }
        if (validSegments.isEmpty()) return baseDurationSeconds

        val baseDistance = validSegments.sumOf { it.distanceMeters }
        if (!baseDistance.isFinite() || baseDistance <= 0.0) return baseDurationSeconds

        val weightedFactor = validSegments.sumOf { segment ->
            segment.distanceMeters * durationFactor(segment.traffic)
        } / baseDistance
        if (!weightedFactor.isFinite()) return baseDurationSeconds

        val adjusted = kotlin.math.ceil(baseDurationSeconds.toDouble() * weightedFactor.coerceAtLeast(1.0))
        if (!adjusted.isFinite() || adjusted >= Long.MAX_VALUE.toDouble()) return Long.MAX_VALUE
        return adjusted.toLong().coerceAtLeast(baseDurationSeconds)
    }

    private fun durationFactor(segment: TrafficSegment): Double {
        if (segment.closure) return CLOSED_SEGMENT_FACTOR

        val live = segment.speedKmh
        val freeFlow = segment.freeFlowSpeedKmh
        if (live == null || freeFlow == null || !live.isFinite() || !freeFlow.isFinite() || live <= 0.0 || freeFlow <= 0.0) return 1.0
        return (freeFlow / live).coerceIn(1.0, MAX_CONGESTION_FACTOR)
    }

    private const val MAX_CONGESTION_FACTOR = 6.0
    private const val CLOSED_SEGMENT_FACTOR = 6.0
}

data class TrafficRouteSegment(
    val distanceMeters: Double,
    val traffic: TrafficSegment,
)
