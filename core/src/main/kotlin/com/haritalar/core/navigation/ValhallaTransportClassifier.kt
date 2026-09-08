package com.haritalar.core.navigation

/** Classifies one Valhalla maneuver using only provider-declared fields. */
object ValhallaTransportClassifier {
    fun classify(
        ferry: Boolean,
        travelType: String?,
        travelMode: String?,
        maneuverType: Int?,
    ): RouteTransportType = when {
        ferry -> RouteTransportType.FERRY
        travelType.equals("ferry", ignoreCase = true) -> RouteTransportType.FERRY
        maneuverType == 28 || maneuverType == 29 -> RouteTransportType.FERRY
        travelType.equals("car", ignoreCase = true) -> RouteTransportType.ROAD
        travelMode.equals("drive", ignoreCase = true) -> RouteTransportType.ROAD
        else -> RouteTransportType.UNKNOWN
    }
}
