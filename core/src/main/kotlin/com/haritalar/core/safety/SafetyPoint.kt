package com.haritalar.core.safety

/** A mapped point that can produce a driving-safety alert. */
data class SafetyPoint(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val type: SafetyPointType,
    val confidence: Confidence,
    val source: DataSource,
    val directionBearingDegrees: Double? = null,
    val speedLimitKmh: Int? = null,
    val active: Boolean = true,
)

enum class SafetyPointType {
    FIXED_SPEED_CAMERA,
    AVERAGE_SPEED_ZONE,
    TRAFFIC_LIGHT_CAMERA,
    VERIFIED_TRAFFIC_CONTROL,
    USER_REPORTED_SAFETY_POINT,
}

enum class Confidence { HIGH, MEDIUM, LOW }

enum class DataSource { LIVE, CACHE, OFFLINE, USER_REPORT }

data class RoutePosition(
    val latitude: Double,
    val longitude: Double,
    val bearingDegrees: Double?,
    val routeDistanceRemainingMeters: Double,
)
