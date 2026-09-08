package com.haritalar.core.navigation

import org.json.JSONArray
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
            val name = tags.optString("name").trim().ifEmpty { continue }
            val coordinate = coordinateOf(element) ?: continue
            val category = categoryOf(tags) ?: NavigationPoiCategory.OTHER
            val id = "osm:${element.optString("type", "unknown")}:${element.optLong("id", -1L)}"
            if (id.endsWith(":-1")) continue

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
                imageUrl = tags.optString("image").trim().ifBlank { null },
                streetImageUrl = null,
            )
        }
        return results
    }

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
        tags.optString("amenity") in setOf("restaurant") -> NavigationPoiCategory.RESTAURANT
        tags.optString("amenity") in setOf("cafe") -> NavigationPoiCategory.CAFE
        tags.optString("amenity") in setOf("pharmacy") -> NavigationPoiCategory.PHARMACY
        tags.optString("amenity") in setOf("hospital") -> NavigationPoiCategory.HOSPITAL
        tags.optString("amenity") in setOf("school") -> NavigationPoiCategory.SCHOOL
        tags.optString("amenity") in setOf("parking") -> NavigationPoiCategory.PARKING
        tags.optString("amenity") in setOf("fuel") -> NavigationPoiCategory.FUEL
        tags.optString("amenity") in setOf("atm") -> NavigationPoiCategory.ATM
        tags.optString("amenity") in setOf("charging_station") -> NavigationPoiCategory.CHARGING_STATION
        tags.optString("shop") in setOf("supermarket", "convenience", "mall", "department_store") -> NavigationPoiCategory.MARKET
        tags.optString("leisure") == "park" -> NavigationPoiCategory.PARK
        tags.optString("leisure") == "rest_area" -> NavigationPoiCategory.REST_AREA
        tags.optString("tourism") == "hotel" -> NavigationPoiCategory.HOTEL
        tags.optString("tourism") in setOf("attraction", "museum", "viewpoint") -> NavigationPoiCategory.TOURISM
        tags.has("place_of_worship") -> NavigationPoiCategory.PLACE_OF_WORSHIP
        tags.has("public_transport") -> NavigationPoiCategory.TRANSIT
        else -> null
    }
}
