package com.haritalar.core.navigation

import org.json.JSONObject

/** Builds only the verified Valhalla auto costing knobs used by ferry-aware routing. */
object ValhallaAutoOptions {
    fun from(preference: RouteTransportPreference): JSONObject = JSONObject().apply {
        when (preference) {
            RouteTransportPreference.ALLOW_FERRY -> put("use_ferry", 1.0)
            RouteTransportPreference.AVOID_FERRY -> put("use_ferry", 0.0)
            RouteTransportPreference.PREFER_FERRY -> put("use_ferry", 1.0).put("ferry_cost", 0.0)
        }
    }
}
