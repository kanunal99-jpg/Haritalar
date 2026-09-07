package com.haritalar.core.navigation

import kotlin.math.*

/**
 * Offline-safe KGM tariff estimator for named bridge crossings.
 * A route is charged only when its geometry passes close to a known crossing.
 * Unknown motorway entry/exit pairs remain unknown rather than being guessed.
 */
object KgmTollEstimator {
    private const val MATCH_RADIUS_METERS = 350.0

    private data class Crossing(val bridge: TollBridge, val latitude: Double, val longitude: Double)

    private val crossings = listOf(
        Crossing(TollBridge.FSM_15_TEMMUZ, 41.0456, 29.0338),
        Crossing(TollBridge.OSMANGAZI, 40.7580, 29.5140),
        Crossing(TollBridge.YAVUZ_SULTAN_SELIM, 41.2045, 29.1130),
    )

    fun estimate(points: List<GeoCoordinate>, vehicleClass: Int = 1): RouteToll {
        if (points.isEmpty()) return RouteToll(hasToll = false)
        val matched = crossings.firstOrNull { crossing ->
            points.any { distanceMeters(it.latitude, it.longitude, crossing.latitude, crossing.longitude) <= MATCH_RADIUS_METERS }
        } ?: return RouteToll(hasToll = false)
        val amount = KgmTollCatalog.bridgePrice(matched.bridge, vehicleClass)
        return RouteToll(hasToll = true, amountTry = amount)
    }

    fun detectBridge(points: List<GeoCoordinate>): TollBridge? = crossings.firstOrNull { crossing ->
        points.any { distanceMeters(it.latitude, it.longitude, crossing.latitude, crossing.longitude) <= MATCH_RADIUS_METERS }
    }?.bridge

    private fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earth = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return earth * 2.0 * atan2(sqrt(a), sqrt(1.0 - a))
    }
}
