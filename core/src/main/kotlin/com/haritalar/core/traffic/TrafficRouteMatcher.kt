package com.haritalar.core.traffic

import com.haritalar.core.navigation.GeoCoordinate
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Matches provider traffic observations to an already calculated route.
 *
 * Only observations carrying provider geometry and sufficient confidence are
 * returned. No traffic value is synthesized when an observation cannot be
 * verified against the route geometry.
 */
object TrafficRouteMatcher {
    const val DEFAULT_TOLERANCE_METERS = 80.0
    const val DEFAULT_DIRECTION_TOLERANCE_DEGREES = 55.0

    fun match(
        route: List<GeoCoordinate>,
        segments: List<TrafficSegment>,
        toleranceMeters: Double = DEFAULT_TOLERANCE_METERS,
        directionToleranceDegrees: Double = DEFAULT_DIRECTION_TOLERANCE_DEGREES,
    ): List<TrafficRouteSegment> {
        require(toleranceMeters >= 0.0)
        require(directionToleranceDegrees in 0.0..180.0)
        if (route.size < 2) return emptyList()

        return segments.mapNotNull { segment ->
            if (segment.confidence == TrafficConfidence.LOW || segment.geometry.size < 2) return@mapNotNull null
            if (!geometryTouchesRoute(route, segment.geometry, toleranceMeters)) return@mapNotNull null

            val trafficBearing = segment.directionBearingDegrees
            if (trafficBearing != null) {
                val routeBearing = nearestRouteBearing(route, segment.geometry)
                    ?: return@mapNotNull null
                if (angularDifference(routeBearing, trafficBearing) > directionToleranceDegrees) {
                    return@mapNotNull null
                }
            }

            TrafficRouteSegment(
                distanceMeters = matchedDistanceMeters(segment.geometry),
                traffic = segment,
            )
        }
    }

    private fun geometryTouchesRoute(
        route: List<GeoCoordinate>,
        geometry: List<GeoCoordinate>,
        toleranceMeters: Double,
    ): Boolean = geometry.any { observationPoint ->
        route.zipWithNext().any { (a, b) -> pointToSegmentDistanceMeters(observationPoint, a, b) <= toleranceMeters }
    }

    private fun nearestRouteBearing(
        route: List<GeoCoordinate>,
        geometry: List<GeoCoordinate>,
    ): Double? {
        var bestDistance = Double.POSITIVE_INFINITY
        var bestBearing: Double? = null
        for (point in geometry) {
            for ((a, b) in route.zipWithNext()) {
                val distance = pointToSegmentDistanceMeters(point, a, b)
                if (distance < bestDistance) {
                    bestDistance = distance
                    bestBearing = bearingDegrees(a, b)
                }
            }
        }
        return bestBearing
    }

    private fun matchedDistanceMeters(geometry: List<GeoCoordinate>): Double =
        geometry.zipWithNext().sumOf { (a, b) -> haversineMeters(a, b) }.coerceAtLeast(1.0)

    private fun pointToSegmentDistanceMeters(
        point: GeoCoordinate,
        start: GeoCoordinate,
        end: GeoCoordinate,
    ): Double {
        // Local equirectangular projection is sufficiently accurate for the
        // short road segments used by traffic providers.
        val latScale = 111_320.0
        val lonScale = 111_320.0 * cos(Math.toRadians(point.latitude))
        val px = (point.longitude - start.longitude) * lonScale
        val py = (point.latitude - start.latitude) * latScale
        val ex = (end.longitude - start.longitude) * lonScale
        val ey = (end.latitude - start.latitude) * latScale
        val lengthSquared = ex * ex + ey * ey
        if (lengthSquared == 0.0) return sqrt(px * px + py * py)
        val t = ((px * ex) + (py * ey)) / lengthSquared
        val clamped = t.coerceIn(0.0, 1.0)
        val dx = px - (clamped * ex)
        val dy = py - (clamped * ey)
        return sqrt(dx * dx + dy * dy)
    }

    private fun bearingDegrees(a: GeoCoordinate, b: GeoCoordinate): Double {
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    }

    private fun angularDifference(a: Double, b: Double): Double {
        val normalized = abs(((a - b) % 360.0 + 360.0) % 360.0)
        return minOf(normalized, 360.0 - normalized)
    }

    private fun haversineMeters(a: GeoCoordinate, b: GeoCoordinate): Double {
        val earthRadius = 6_371_000.0
        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val h = sin(dLat / 2.0) * sin(dLat / 2.0) +
            cos(lat1) * cos(lat2) * sin(dLon / 2.0) * sin(dLon / 2.0)
        return earthRadius * 2.0 * atan2(sqrt(h), sqrt(1.0 - h))
    }
}
