package com.haritalar.core.navigation

import org.json.JSONObject

/** Builds verified Valhalla auto costing knobs used by ferry-aware routing. */
object ValhallaAutoOptions {
    fun from(preference: RouteTransportPreference): JSONObject = JSONObject().apply {
        put(
            "use_ferry",
            when (preference) {
                RouteTransportPreference.ALLOW_FERRY -> 0.5
                RouteTransportPreference.AVOID_FERRY -> 0.0
                RouteTransportPreference.PREFER_FERRY -> 1.0
            },
        )
    }
}
