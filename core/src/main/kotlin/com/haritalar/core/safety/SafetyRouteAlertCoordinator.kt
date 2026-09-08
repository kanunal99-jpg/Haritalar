package com.haritalar.core.safety

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Connects route geometry + live position to the deterministic SafetyAlertEngine.
 *
 * The coordinator never invents safety points. If the provider supplies no points,
 * evaluation is silent. Points are evaluated only when they project onto the active
 * route and are still ahead of the current route progress.
 * Offline points are additionally checked against the offline trust boundary before
 * they can become tracked alert candidates.
 */
class SafetyRouteAlertCoordinator(
    private val engine: SafetyAlertEngine = SafetyAlertEngine(),
    private val routeMatchToleranceMeters: Double = 80.0,
    private val duplicatePointToleranceMeters: Double = 50.0,
) {
    data class RoutePoint(val latitude: Double, val longitude: Double)

    data class TrackedPoint(
        val point: SafetyPoint,
        val routeProgressMeters: Double,
    )

    fun trackPoints(
        route: List<RoutePoint>,
        cumulativeMeters: List<Double>,
        points: List<SafetyPoint>,
    ): List<TrackedPoint> {
        if (route.size < 2 || route.size != cumulativeMeters.size) return emptyList()
        return deduplicate(points).mapNotNull { point ->
            if (point.source == DataSource.OFFLINE && !SafetyOfflinePackageValidator.isValidOfflinePoint(point)) {
                return@mapNotNull null
            }
            val projection = project(route, point.latitude, point.longitude) ?: return@mapNotNull null
            if (projection.distanceMeters > routeMatchToleranceMeters) return@mapNotNull null
            TrackedPoint(point, projection.routeProgressMeters)
        }.sortedBy { it.routeProgressMeters }
    }

    /**
     * Collapses records that describe the same safety point from different providers.
     * The most authoritative record wins; different safety types remain distinct.
     * Records with explicit, materially different travel directions remain distinct
     * because they can represent separate enforcement for opposite traffic flows.
     */
    fun deduplicate(points: List<SafetyPoint>): List<SafetyPoint> {
        if (points.size < 2) return points
        val result = ArrayList<SafetyPoint>(points.size)
        for (candidate in points) {
            val duplicateIndex = result.indexOfFirst { existing ->
                existing.type == candidate.type &&
                    directionsCompatible(existing.directionBearingDegrees, candidate.directionBearingDegrees) &&
                    distanceMeters(existing.latitude, existing.longitude, candidate.latitude, candidate.longitude) <= duplicatePointToleranceMeters
            }
            if (duplicateIndex < 0) {
                result += candidate
            } else if (authority(candidate) > authority(result[duplicateIndex])) {
                result[duplicateIndex] = candidate
            }
        }
        return result
    }

    fun evaluate(
        trackedPoints: List<TrackedPoint>,
        currentRouteProgressMeters: Double,
        bearingDegrees: Double?,
    ): List<SafetyAlert> {
        if (trackedPoints.isEmpty()) return emptyList()
        val alerts = ArrayList<SafetyAlert>()
        for (tracked in trackedPoints) {
            val remaining = tracked.routeProgressMeters - currentRouteProgressMeters
            if (remaining < -25.0) continue
            val position = RoutePosition(
                latitude = tracked.point.latitude,
                longitude = tracked.point.longitude,
                bearingDegrees = bearingDegrees,
                routeDistanceRemainingMeters = max(0.0, remaining),
            )
            engine.evaluate(tracked.point, position)?.let(alerts::add)
        }
        return alerts
    }

    fun reset(pointId: String) = engine.reset(pointId)

    private fun directionsCompatible(a: Double?, b: Double?): Boolean {
        if (a == null || b == null) return true
        val delta = abs(((a - b + 540.0) % 360.0) - 180.0)
        return delta <= 55.0
    }

    private fun authority(point: SafetyPoint): Int {
        val source = when (point.source) {
            DataSource.LIVE -> 40
            DataSource.CACHE -> 30
            DataSource.OFFLINE -> 20
            DataSource.USER_REPORT -> 10
        }
        val confidence = when (point.confidence) {
            Confidence.HIGH -> 3
            Confidence.MEDIUM -> 2
            Confidence.LOW -> 1
        }
        return source * 10 + confidence
    }

    private data class Projection(
        val routeProgressMeters: Double,
        val distanceMeters: Double,
    )

    private fun project(
        route: List<RoutePoint>,
        latitude: Double,
        longitude: Double,
    ): Projection? {
        var best: Projection? = null
        for (i in 0 until route.lastIndex) {
            val a = route[i]
            val b = route[i + 1]
            val segment = haversineMeters(a.latitude, a.longitude, b.latitude, b.longitude)
            val projection = projectSegment(latitude, longitude, a, b)
            val progress = if (segment == 0.0) 0.0 else projection.fraction * segment
            val candidate = Projection(progress + segmentPrefix(route, i), projection.distanceMeters)
            if (best == null || candidate.distanceMeters < best!!.distanceMeters) best = candidate
        }
        return best
    }

    private fun segmentPrefix(route: List<RoutePoint>, segmentIndex: Int): Double {
        var total = 0.0
        for (i in 0 until segmentIndex) {
            total += haversineMeters(route[i].latitude, route[i].longitude, route[i + 1].latitude, route[i + 1].longitude)
        }
        return total
    }

    private data class SegmentProjection(val fraction: Double, val distanceMeters: Double)

    private fun projectSegment(
        lat: Double,
        lon: Double,
        a: RoutePoint,
        b: RoutePoint,
    ): SegmentProjection {
        val meanLat = Math.toRadians((a.latitude + b.latitude + lat) / 3.0)
        val sx = 111320.0 * cos(meanLat)
        val sy = 110540.0
        val ax = (a.longitude - lon) * sx
        val ay = (a.latitude - lat) * sy
        val bx = (b.longitude - lon) * sx
        val by = (b.latitude - lat) * sy
        val dx = bx - ax
        val dy = by - ay
        val denominator = dx * dx + dy * dy
        val fraction = (if (denominator == 0.0) 0.0 else (-ax * dx - ay * dy) / denominator).coerceIn(0.0, 1.0)
        val cx = ax + fraction * dx
        val cy = ay + fraction * dy
        return SegmentProjection(fraction, sqrt(cx * cx + cy * cy))
    }

    private fun distanceMeters(aLat: Double, aLon: Double, bLat: Double, bLon: Double): Double {
        val earth = 6_371_000.0
        val dLat = Math.toRadians(bLat - aLat)
        val dLon = Math.toRadians(bLon - aLon)
        val lat1 = Math.toRadians(aLat)
        val lat2 = Math.toRadians(bLat)
        val h = sinSquared(dLat / 2) + cos(lat1) * cos(lat2) * sinSquared(dLon / 2)
        return earth * 2 * kotlin.math.atan2(sqrt(h), sqrt(max(0.0, 1.0 - h)))
    }

    private fun haversineMeters(aLat: Double, aLon: Double, bLat: Double, bLon: Double): Double =
        distanceMeters(aLat, aLon, bLat, bLon)

    private fun sinSquared(value: Double) = kotlin.math.sin(value).pow(2)
}
