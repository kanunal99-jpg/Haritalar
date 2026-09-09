package com.haritalar.core.traffic

import com.haritalar.core.navigation.GeoCoordinate

/**
 * Provider boundary for live traffic. Providers must return only data they are
 * authorized to expose; the core never scrapes web pages as a traffic source.
 */
interface TrafficProvider {
    val id: String
    val priority: Int
    fun supports(coordinate: GeoCoordinate): Boolean
    suspend fun fetchTraffic(bounds: TrafficBounds, route: TrafficRoute?): TrafficSnapshot
}

data class TrafficBounds(
    val south: Double,
    val west: Double,
    val north: Double,
    val east: Double,
) {
    init {
        require(south in -90.0..90.0 && north in -90.0..90.0)
        require(west in -180.0..180.0 && east in -180.0..180.0)
        require(south <= north)
    }
}

data class TrafficRoute(val coordinates: List<GeoCoordinate>)

data class TrafficSnapshot(
    val providerId: String,
    val segments: List<TrafficSegment>,
    val fetchedAtEpochMs: Long,
    val expiresAtEpochMs: Long,
    val confidence: TrafficConfidence,
) {
    fun isUsable(nowEpochMs: Long): Boolean =
        fetchedAtEpochMs > 0L &&
            expiresAtEpochMs > nowEpochMs &&
            providerId.isNotBlank() &&
            segments.all { it.isValid() }
}

data class TrafficSegment(
    val id: String,
    val speedKmh: Double? = null,
    val freeFlowSpeedKmh: Double? = null,
    val congestion: TrafficCongestion = TrafficCongestion.UNKNOWN,
    val closure: Boolean = false,
    val roadwork: Boolean = false,
    val accident: Boolean = false,
    val directionBearingDegrees: Double? = null,
    val confidence: TrafficConfidence = TrafficConfidence.LOW,
    /** Provider geometry used to verify that an observation belongs to a route. */
    val geometry: List<GeoCoordinate> = emptyList(),
) {
    fun isValid(): Boolean =
        id.isNotBlank() &&
            speedKmh.isFiniteOrNullNonNegative() &&
            freeFlowSpeedKmh.isFiniteOrNullPositive() &&
            directionBearingDegrees.isValidBearing() &&
            geometry.all { it.latitude.isFinite() && it.longitude.isFinite() && it.latitude in -90.0..90.0 && it.longitude in -180.0..180.0 }
}

private fun Double?.isFiniteOrNullNonNegative(): Boolean = this == null || (isFinite() && this >= 0.0)

private fun Double?.isFiniteOrNullPositive(): Boolean = this == null || (isFinite() && this > 0.0)

private fun Double?.isValidBearing(): Boolean = this == null || (isFinite() && this in 0.0..360.0)

enum class TrafficCongestion { UNKNOWN, FREE, LIGHT, MODERATE, HEAVY, SEVERE }
enum class TrafficConfidence { LOW, MEDIUM, HIGH }
