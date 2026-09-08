package com.haritalar.core.navigation

import org.json.JSONArray
import org.json.JSONObject

/** Builds the complete auto-routing request so UI code does not duplicate provider rules. */
object ValhallaRouteRequest {
    fun build(
        fromLat: Double,
        fromLon: Double,
        toLat: Double,
        toLon: Double,
        mode: String,
        transportPreference: RouteTransportPreference = RouteTransportPreference.ALLOW_FERRY,
    ): JSONObject = JSONObject().apply {
        put("locations", JSONArray().apply {
            put(JSONObject().apply {
                put("lat", fromLat)
                put("lon", fromLon)
                put("type", "break")
            })
            put(JSONObject().apply {
                put("lat", toLat)
                put("lon", toLon)
                put("type", "break")
            })
        })
        put("costing", if (mode == "auto_shorter") "auto_shorter" else "auto")

        val auto = ValhallaAutoOptions.from(transportPreference)
        when (mode) {
            "no_toll" -> auto.put("use_tolls", 0.0).put("toll_booth_penalty", 3600.0)
            "toll_fast" -> auto.put("use_tolls", 1.0)
        }
        put("costing_options", JSONObject().put("auto", auto))
        put("units", "kilometers")
        put("directions_options", JSONObject().put("language", "tr-TR"))
    }
}
