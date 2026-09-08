package com.haritalar.core.safety

/**
 * Versioned offline safety-data container. Validation is deliberately strict:
 * malformed or untrusted records are ignored rather than promoted to alerts.
 */
data class SafetyOfflinePackage(
    val version: Int,
    val regions: List<SafetyOfflineRegion>,
)

data class SafetyOfflineRegion(
    val id: String,
    val points: List<SafetyPoint>,
)

object SafetyOfflinePackageValidator {
    private const val SUPPORTED_VERSION = 1
    private const val MAX_REGIONS = 1000
    private const val MAX_POINTS_PER_REGION = 10_000

    fun validate(pkg: SafetyOfflinePackage): Boolean {
        if (pkg.version != SUPPORTED_VERSION) return false
        if (pkg.regions.size > MAX_REGIONS) return false
        if (pkg.regions.any { !validRegion(it) }) return false
        return true
    }

    /** Public point-level guard for adapters that parse the package incrementally. */
    fun isValidOfflinePoint(point: SafetyPoint): Boolean = validPoint(point)

    private fun validRegion(region: SafetyOfflineRegion): Boolean {
        if (region.id.isBlank()) return false
        if (region.points.size > MAX_POINTS_PER_REGION) return false
        return region.points.all(::validPoint)
    }

    private fun validPoint(point: SafetyPoint): Boolean {
        if (point.id.isBlank()) return false
        if (!point.latitude.isFinite() || !point.longitude.isFinite()) return false
        if (point.latitude !in -90.0..90.0 || point.longitude !in -180.0..180.0) return false
        if (point.source != DataSource.OFFLINE) return false
        if (point.type == SafetyPointType.USER_REPORTED_SAFETY_POINT) return false
        if (point.type == SafetyPointType.VERIFIED_TRAFFIC_CONTROL && point.confidence != Confidence.HIGH) return false
        val bearing = point.directionBearingDegrees
        if (bearing != null && (!bearing.isFinite() || bearing < 0.0 || bearing >= 360.0)) return false
        return true
    }
}
