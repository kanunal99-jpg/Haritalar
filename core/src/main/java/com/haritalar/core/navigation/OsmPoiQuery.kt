package com.haritalar.core.navigation

/** Builds bounded Overpass queries for live OSM POI discovery around the current map view. */
object OsmPoiQuery {
    private const val OVERPASS_ENDPOINT = "https://overpass-api.de/api/interpreter"

    fun endpoint(): String = OVERPASS_ENDPOINT

    fun build(
        south: Double,
        west: Double,
        north: Double,
        east: Double,
        limit: Int = 300,
    ): String {
        require(south in -90.0..90.0)
        require(north in -90.0..90.0)
        require(west in -180.0..180.0)
        require(east in -180.0..180.0)
        require(south <= north)
        require(limit in 1..1000)

        val box = "$south,$west,$north,$east"
        return """
            [out:json][timeout:20];
            (
              nwr[amenity~"^(restaurant|cafe|pharmacy|hospital|school|parking|fuel|bank|atm|charging_station|fast_food|marketplace|rest_area|townhall|courthouse|police|fire_station|post_office|library|community_centre)$"]($box);
              nwr[shop~"^(supermarket|convenience|mall|department_store|bakery|butcher|clothes|electronics|hardware|furniture)$"]($box);
              nwr[highway="rest_area"]($box);
              nwr[office~"^(government|administrative)$"]($box);
              nwr[government]($box);
              nwr[leisure~"^(park)$"]($box);
              nwr[tourism~"^(hotel|attraction|museum|viewpoint)$"]($box);
              nwr[amenity="place_of_worship"]($box);
              nwr[public_transport]($box);
            );
            out center tags;
        """.trimIndent()
    }

    /** Applies the requested result cap after a valid Overpass response is parsed. */
    fun <T> capResults(results: List<T>, limit: Int): List<T> {
        require(limit in 1..1000)
        return results.take(limit)
    }
}
