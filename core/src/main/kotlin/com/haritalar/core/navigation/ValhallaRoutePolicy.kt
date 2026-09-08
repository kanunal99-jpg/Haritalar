package com.haritalar.core.navigation

import org.json.JSONObject

/**
 * Provider-specific policy kept outside the UI so route alternatives remain testable.
 * Valhalla exposes ferry willingness as use_ferry (0..1) and marks ferry maneuvers explicitly.
 */
object ValhallaRoutePolicy {
    fun autoCostingOptions(mode: String): JSONObject = JSONObject().apply {
        when (mode) {
            "no_toll" -> {
                put("use_tolls", 0.0)
                put("toll_booth_penalty", 3600.0)
            }
            "toll_fast" -> put("use_tolls", 1.0)
            "no_ferry" -> put("use_ferry", 0.0)
            "ferry_fast" -> put("use_ferry", 1.0)
            "ferry_balanced" -> put("use_ferry", 0.5)
        }
    }

    fun isFerryManeuver(maneuver: JSONObject): Boolean =
        maneuver.optBoolean("ferry", false) ||
            maneuver.optString("travel_type").equals("ferry", ignoreCase = true) ||
            maneuver.optString("travel_type").equals("rail-ferry", ignoreCase = true)
}
