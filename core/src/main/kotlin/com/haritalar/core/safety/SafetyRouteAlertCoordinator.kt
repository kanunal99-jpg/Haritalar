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
 */
class SafetyRouteAlertCoordinator(
    private val engine: SafetyAlertEngine = SafetyAlertEngine(),
    private val routeMatchToleranceMeters: Double = 80.0,
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
        return points.mapNotNull { point ->
            val projection = project(route, point.latitude, point.longitude) ?: return@mapNotNull null
            if (projection.distanceMeters > routeMatchToleranceMeters) return@mapNotNull null
            TrackedPoint(point, projection.routeProgressMeters)
        }.sortedBy { it.routeProgressMeters }
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
            val progress = i.toDouble().let { if (segment == 0.0) 0.0 else projection.fraction * segment } 
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

    private fun haversineMeters(aLat: Double, aLon: Double, bLat: Double, bLon: Double): Double {
        val earth = 6_371_000.0
        val dLat = Math.toRadians(bLat - aLat)
        val dLon = Math.toRadians(bLon - aLon)
        val lat1 = Math.toRadians(aLat)
        val lat2 = Math.toRadians(bLat)
        val h = sinSquared(dLat / 2) + cos(lat1) * cos(lat2) * sinSquared(dLon / 2)
        return earth * 2 * kotlin.math.atan2(sqrt(h), sqrt(max(0.0, 1.0 - h)))
    }

    private fun sinSquared(value: Double) = kotlin.math.sin(value).pow(2)
}
