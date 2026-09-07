package com.haritalar.core.navigation

import kotlin.math.*

object NavigationMath {
    private const val EarthRadiusMeters = 6_371_000.0

    fun distanceMeters(a: GeoCoordinate, b: GeoCoordinate): Double {
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val dLat = lat2 - lat1
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val h = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
        return 2 * EarthRadiusMeters * asin(sqrt(h.coerceIn(0.0, 1.0)))
    }

    fun initialBearingDegrees(a: GeoCoordinate, b: GeoCoordinate): Double {
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    }
}
