package com.haritalar.core.safety

/**
 * A traffic-control record may enter the verified layer only when its publisher,
 * verification state, timestamp and coordinates are explicit. Generic POIs are
 * intentionally not accepted as enforcement data.
 */
data class VerifiedTrafficControl(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val type: SafetyPointType = SafetyPointType.VERIFIED_TRAFFIC_CONTROL,
    val sourceName: String,
    val sourceUrl: String,
    val verified: Boolean,
    val publishedAtEpochMs: Long,
    val expiresAtEpochMs: Long? = null,
    val directionBearingDegrees: Double? = null,
)

fun VerifiedTrafficControl.toSafetyPoint(nowEpochMs: Long = System.currentTimeMillis()): SafetyPoint? {
    if (type != SafetyPointType.VERIFIED_TRAFFIC_CONTROL) return null
    if (!verified || sourceName.isBlank() || !isHttps(sourceUrl)) return null
    if (!latitude.isFinite() || !longitude.isFinite() || latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return null
    if (publishedAtEpochMs <= 0L || publishedAtEpochMs > nowEpochMs) return null
    if (expiresAtEpochMs != null && expiresAtEpochMs <= nowEpochMs) return null
    val bearing = directionBearingDegrees?.let { ((it % 360.0) + 360.0) % 360.0 }
    return SafetyPoint(
        id = id,
        latitude = latitude,
        longitude = longitude,
        type = SafetyPointType.VERIFIED_TRAFFIC_CONTROL,
        confidence = Confidence.HIGH,
        source = DataSource.LIVE,
        directionBearingDegrees = bearing,
    )
}

private fun isHttps(value: String): Boolean = value.startsWith("https://", ignoreCase = true) && value.length > 8
