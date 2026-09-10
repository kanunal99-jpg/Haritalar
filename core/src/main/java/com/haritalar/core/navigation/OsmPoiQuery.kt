package com.haritalar.core.navigation

/** Builds bounded Overpass queries for live OSM POI discovery around the current map view. */
object OsmPoiQuery {
    private const val OVERPASS_ENDPOINT = "https://overpass-api.de/api/interpreter"

    fun endpoint(): String = OVERPASS_ENDPOINT

    fun build(south: Double, west: Double, north: Double, east: Double, limit: Int = 300): String =
        buildInternal(south, west, north, east, limit, null)

    fun buildForCategory(south: Double, west: Double, north: Double, east: Double, category: NavigationPoiCategory, limit: Int = 80): String =
        buildInternal(south, west, north, east, limit, category)

    private fun buildInternal(
        south: Double, west: Double, north: Double, east: Double, limit: Int, category: NavigationPoiCategory?,
    ): String {
        require(south in -90.0..90.0); require(north in -90.0..90.0)
        require(west in -180.0..180.0); require(east in -180.0..180.0)
        require(south <= north); require(limit in 1..1000)
        val box = "$south,$west,$north,$east"
        val scoped = category?.let { categoryClauses(it, box) }
        val query = scoped ?: """
              nwr[amenity~"^(restaurant|cafe|pharmacy|hospital|school|parking|fuel|bank|atm|charging_station|fast_food|marketplace|rest_area|townhall|courthouse|police|fire_station|post_office|library|community_centre)$"]($box);
              nwr[shop~"^(supermarket|convenience|mall|department_store|bakery|butcher|clothes|electronics|hardware|furniture)$"]($box);
              nwr[highway~"^(traffic_signals|speed_camera|crossing)$"]($box);
              nwr[highway="rest_area"]($box);
              nwr[enforcement~"^(maxspeed|average_speed)$"]($box);
              nwr[railway~"^(station|halt|tram_stop|subway_entrance)$"]($box);
              nwr[route="tram"]($box);
              nwr[office~"^(government|administrative)$"]($box);
              nwr[government]($box);
              nwr[leisure~"^(park|nature_reserve)$"]($box);
              nwr[landuse="forest"]($box);
              nwr[natural="wood"]($box);
              nwr[tourism~"^(hotel|attraction|museum|viewpoint)$"]($box);
              nwr[amenity="place_of_worship"]($box);
              nwr[public_transport]($box);
        """.trimIndent()
        return """
            [out:json][timeout:20];
            ($query);
            out center tags;
        """.trimIndent()
    }

    private fun categoryClauses(category: NavigationPoiCategory, box: String): String = when (category) {
        NavigationPoiCategory.MARKET -> """
            nwr[shop~"^(supermarket|convenience|mall|department_store)$"]($box);
            nwr[amenity="marketplace"]($box);
        """.trimIndent()
        NavigationPoiCategory.FUEL -> "nwr[amenity=\"fuel\"]($box);"
        NavigationPoiCategory.PLACE_OF_WORSHIP -> "nwr[amenity=\"place_of_worship\"]($box);"
        NavigationPoiCategory.PARKING -> "nwr[amenity=\"parking\"]($box);"
        NavigationPoiCategory.RESTAURANT -> "nwr[amenity~\"^(restaurant|fast_food)$\"]($box);"
        NavigationPoiCategory.CAFE -> "nwr[amenity=\"cafe\"]($box);"
        NavigationPoiCategory.PHARMACY -> "nwr[amenity=\"pharmacy\"]($box);"
        NavigationPoiCategory.HOSPITAL -> "nwr[amenity=\"hospital\"]($box);"
        NavigationPoiCategory.SCHOOL -> "nwr[amenity=\"school\"]($box);"
        NavigationPoiCategory.CHARGING_STATION -> "nwr[amenity=\"charging_station\"]($box);"
        NavigationPoiCategory.TRAFFIC_SIGNAL -> "nwr[highway=\"traffic_signals\"]($box);"
        NavigationPoiCategory.SPEED_CAMERA -> """
            nwr[highway="speed_camera"]($box);
            nwr[enforcement~"^(maxspeed|average_speed)$"]($box);
        """.trimIndent()
        NavigationPoiCategory.PEDESTRIAN_CROSSING -> "nwr[highway=\"crossing\"]($box);"
        NavigationPoiCategory.TRAM -> "nwr[railway=\"tram_stop\"]($box); nwr[route=\"tram\"]($box);"
        NavigationPoiCategory.RAILWAY -> "nwr[railway~\"^(station|halt|subway_entrance)$\"]($box);"
        NavigationPoiCategory.TRANSIT -> "nwr[public_transport]($box);"
        NavigationPoiCategory.PARK -> "nwr[leisure=\"park\"]($box);"
        NavigationPoiCategory.FOREST -> "nwr[landuse=\"forest\"]($box); nwr[natural=\"wood\"]($box); nwr[leisure=\"nature_reserve\"]($box);"
        NavigationPoiCategory.HOTEL -> "nwr[tourism=\"hotel\"]($box);"
        NavigationPoiCategory.TOURISM -> "nwr[tourism~\"^(attraction|museum|viewpoint)$\"]($box);"
        NavigationPoiCategory.PUBLIC_INSTITUTION -> """
            nwr[amenity~"^(townhall|courthouse|police|fire_station|post_office|library|community_centre)$"]($box);
            nwr[office~"^(government|administrative)$"]($box);
        """.trimIndent()
        else -> "nwr[amenity]($box);"
    }

    fun <T> capResults(results: List<T>, limit: Int): List<T> {
        require(limit in 1..1000)
        return results.take(limit)
    }
}
