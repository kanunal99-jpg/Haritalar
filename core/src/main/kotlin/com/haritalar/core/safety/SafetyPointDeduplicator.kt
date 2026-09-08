package com.haritalar.core.safety

import kotlin.math.*

/** Deterministically merges overlapping safety records without promoting user reports. */
object SafetyPointDeduplicator {
    private const val DEFAULT_DISTANCE_METERS = 50.0
    private const val DIRECTION_TOLERANCE_DEGREES = 55.0

    fun deduplicate(points: List<SafetyPoint>, distanceMeters: Double = DEFAULT_DISTANCE_METERS): List<SafetyPoint> {
        if (points.isEmpty()) return emptyList()
        require(distanceMeters >= 0.0 && distanceMeters.isFinite())
        val result = mutableListOf<SafetyPoint>()
        for (point in points) {
            val existingIndex = result.indexOfFirst { equivalent(it, point, distanceMeters) }
            if (existingIndex < 0) result += point
            else result[existingIndex] = chooseWinner(result[existingIndex], point)
        }
        return result.sortedBy { it.id }
    }

    private fun equivalent(a: SafetyPoint, b: SafetyPoint, tolerance: Double): Boolean {
        if (a.id.isNotBlank() && a.id == b.id) return true
        if (a.type != b.type) return false
        if (a.type == SafetyPointType.USER_REPORTED_SAFETY_POINT || b.type == SafetyPointType.USER_REPORTED_SAFETY_POINT) return false
        if (distanceMeters(a.latitude, a.longitude, b.latitude, b.longitude) > tolerance) return false
        val da = a.directionBearingDegrees
        val db = b.directionBearingDegrees
        return da == null || db == null || angularDifference(da, db) <= DIRECTION_TOLERANCE_DEGREES
    }

    private fun chooseWinner(a: SafetyPoint, b: SafetyPoint): SafetyPoint {
        val aRank = trustRank(a)
        val bRank = trustRank(b)
        return when {
            bRank > aRank -> merge(b, a)
            aRank > bRank -> merge(a, b)
            confidenceRank(b.confidence) > confidenceRank(a.confidence) -> merge(b, a)
            else -> merge(a, b)
        }
    }

    private fun merge(winner: SafetyPoint, other: SafetyPoint): SafetyPoint = winner.copy(
        directionBearingDegrees = winner.directionBearingDegrees ?: other.directionBearingDegrees,
        speedLimitKmh = winner.speedLimitKmh ?: other.speedLimitKmh,
        active = winner.active || other.active,
    )

    private fun trustRank(point: SafetyPoint): Int = when (point.source) {
        DataSource.USER_REPORT -> 0
        DataSource.OFFLINE -> 2
        DataSource.CACHE -> 3
        DataSource.LIVE -> 4
    }

    private fun confidenceRank(confidence: Confidence): Int = when (confidence) {
        Confidence.LOW -> 0
        Confidence.MEDIUM -> 1
        Confidence.HIGH -> 2
    }

    private fun angularDifference(a: Double, b: Double): Double =
        abs(((a - b) % 360.0 + 540.0) % 360.0 - 180.0)

    private fun distanceMeters(aLat: Double, aLon: Double, bLat: Double, bLon: Double): Double {
        val earth = 6_371_000.0
        val dLat = Math.toRadians(bLat - aLat)
        val dLon = Math.toRadians(bLon - aLon)
        val lat1 = Math.toRadians(aLat)
        val lat2 = Math.toRadians(bLat)
        val h = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
        return earth * 2 * atan2(sqrt(h), sqrt(max(0.0, 1.0 - h)))
    }
}
