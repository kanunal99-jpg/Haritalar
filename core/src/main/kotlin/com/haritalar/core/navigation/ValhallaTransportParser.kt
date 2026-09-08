package com.haritalar.core.navigation

import org.json.JSONArray
import org.json.JSONObject

/** Extracts provider-declared transport types without guessing when fields are absent. */
object ValhallaTransportParser {
    fun parseTransportTypes(response: JSONObject): List<RouteTransportType> {
        val types = mutableListOf<RouteTransportType>()
        val trip = response.optJSONObject("trip") ?: return listOf(RouteTransportType.UNKNOWN)
        val legs = trip.optJSONArray("legs") ?: return listOf(RouteTransportType.UNKNOWN)

        for (legIndex in 0 until legs.length()) {
            val leg = legs.optJSONObject(legIndex) ?: continue
            parseManeuvers(leg.optJSONArray("maneuvers"), types)
        }

        return types.distinct().ifEmpty { listOf(RouteTransportType.UNKNOWN) }
    }

    private fun parseManeuvers(maneuvers: JSONArray?, types: MutableList<RouteTransportType>) {
        if (maneuvers == null) return
        for (index in 0 until maneuvers.length()) {
            val maneuver = maneuvers.optJSONObject(index) ?: continue
            types += ValhallaTransportClassifier.classify(
                ferry = maneuver.optBoolean("ferry", false),
                travelType = maneuver.optString("travel_type", ""),
                travelMode = maneuver.optString("travel_mode", ""),
                maneuverType = if (maneuver.has("type")) maneuver.optInt("type") else null,
            )
        }
    }
}
