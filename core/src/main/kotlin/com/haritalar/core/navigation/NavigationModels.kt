package com.haritalar.core.navigation

/** Basic navigation domain models; routing providers can implement these without coupling the UI. */
data class GeoCoordinate(val latitude: Double, val longitude: Double)

data class NavigationRoute(
    val id: String,
    val points: List<GeoCoordinate>,
    val distanceMeters: Double,
    val durationSeconds: Long,
)

data class NavigationInstruction(
    val index: Int,
    val text: String,
    val distanceMeters: Double,
    val bearingDegrees: Double? = null,
)

interface RoutingEngine {
    suspend fun calculateRoute(from: GeoCoordinate, to: GeoCoordinate): Result<NavigationRoute>
    suspend fun instructions(route: NavigationRoute): Result<List<NavigationInstruction>>
}
