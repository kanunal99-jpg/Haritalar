package com.haritalar.core.navigation

import org.json.JSONObject

/** Parses bounded Overpass JSON into safe navigation POI models. */
object OsmPoiParser {
    fun parse(payload: String, maxResults: Int = 300): List<NavigationPoi> {
        require(maxResults in 1..1000)
        val elements = JSONObject(payload).optJSONArray("elements") ?: return emptyList()
        val results = ArrayList<NavigationPoi>(minOf(elements.length(), maxResults))

        for (index in 0 until elements.length()) {
            if (results.size >= maxResults) break
            val element = elements.optJSONObject(index) ?: continue
            val tags = element.optJSONObject("tags") ?: continue
            val coordinate = coordinateOf(element) ?: continue
            val category = categoryOf(tags) ?: NavigationPoiCategory.OTHER
            val id = "osm:${element.optString("type", "unknown")}:${element.optLong("id", -1L)}"
            if (id.endsWith(":-1")) continue
            val name = tags.optString("name").trim()
            if (name.isBlank()) continue

            val street = tags.optString("addr:street").trim()
            val houseNumber = tags.optString("addr:housenumber").trim()
            val address = listOf(street, houseNumber)
                .filter { it.isNotBlank() }
                .joinToString(" ")
                .ifBlank { null }

            results += NavigationPoi(
                id = id,
                category = category,
                name = name,
                latitude = coordinate.first,
                longitude = coordinate.second,
                address = address,
                openingHours = tags.optString("opening_hours").trim().ifBlank { null },
                imageUrl = remoteUrl(tags.optString("image").trim()),
                streetImageUrl = remoteUrl(tags.optString("street_image_url").trim()),
            )
        }
        return results
    }

    private fun remoteUrl(value: String): String? =
        value.takeIf { it.startsWith("https://", ignoreCase = true) || it.startsWith("http://", ignoreCase = true) }

    private fun coordinateOf(element: JSONObject): Pair<Double, Double>? {
        val lat = element.optDouble("lat", Double.NaN)
        val lon = element.optDouble("lon", Double.NaN)
        if (lat.isFinite() && lon.isFinite()) return lat to lon

        val center = element.optJSONObject("center") ?: return null
        val centerLat = center.optDouble("lat", Double.NaN)
        val centerLon = center.optDouble("lon", Double.NaN)
        return if (centerLat.isFinite() && centerLon.isFinite()) centerLat to centerLon else null
    }

    private fun categoryOf(tags: JSONObject): NavigationPoiCategory? = when {
        tags.optString("highway") == "traffic_signals" -> NavigationPoiCategory.TRAFFIC_SIGNAL
        tags.optString("highway") == "speed_camera" || tags.optString("enforcement") in setOf("maxspeed", "average_speed") -> NavigationPoiCategory.SPEED_CAMERA
        tags.optString("railway") == "tram_stop" || tags.optString("route") == "tram" -> NavigationPoiCategory.TRAM
        tags.optString("railway") in setOf("station", "halt", "subway_entrance") -> NavigationPoiCategory.RAILWAY
        tags.optString("amenity") in setOf("restaurant", "fast_food") -> NavigationPoiCategory.RESTAURANT
        tags.optString("amenity") == "cafe" -> NavigationPoiCategory.CAFE
        tags.optString("amenity") == "pharmacy" -> NavigationPoiCategory.PHARMACY
        tags.optString("amenity") == "hospital" -> NavigationPoiCategory.HOSPITAL
        tags.optString("amenity") == "school" -> NavigationPoiCategory.SCHOOL
        tags.optString("amenity") == "parking" -> NavigationPoiCategory.PARKING
        tags.optString("amenity") == "fuel" -> NavigationPoiCategory.FUEL
        tags.optString("amenity") == "atm" -> NavigationPoiCategory.ATM
        tags.optString("amenity") == "charging_station" -> NavigationPoiCategory.CHARGING_STATION
        tags.optString("amenity") == "place_of_worship" -> NavigationPoiCategory.PLACE_OF_WORSHIP
        tags.optString("public_transport") in setOf("platform", "station", "stop_position") || tags.has("public_transport") -> NavigationPoiCategory.TRANSIT
        tags.optString("shop") in setOf("supermarket", "convenience", "mall", "department_store", "bakery", "butcher", "clothes", "electronics", "hardware", "furniture") -> NavigationPoiCategory.MARKET
        tags.optString("amenity") == "marketplace" -> NavigationPoiCategory.MARKET
        tags.optString("leisure") == "park" -> NavigationPoiCategory.PARK
        tags.optString("amenity") == "rest_area" || tags.optString("highway") == "rest_area" -> NavigationPoiCategory.REST_AREA
        tags.optString("tourism") == "hotel" -> NavigationPoiCategory.HOTEL
        tags.optString("tourism") in setOf("attraction", "museum", "viewpoint") -> NavigationPoiCategory.TOURISM
        tags.optString("amenity") in setOf("townhall", "courthouse", "police", "fire_station", "post_office", "library", "community_centre") -> NavigationPoiCategory.PUBLIC_INSTITUTION
        tags.optString("office") in setOf("government", "administrative") || tags.has("government") -> NavigationPoiCategory.PUBLIC_INSTITUTION
        else -> null
    }
}
