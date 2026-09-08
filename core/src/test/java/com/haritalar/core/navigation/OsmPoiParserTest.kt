package com.haritalar.core.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OsmPoiParserTest {
    @Test
    fun parses_node_and_way_coordinates_and_categories() {
        val payload = """
            {
              "elements": [
                {"type":"node","id":1,"lat":41.01,"lon":29.02,"tags":{"name":"Örnek Market","shop":"supermarket","addr:street":"Örnek Sokak","addr:housenumber":"12","opening_hours":"Mo-Su 08:00-22:00"}},
                {"type":"way","id":2,"center":{"lat":41.011,"lon":29.021},"tags":{"name":"Örnek Hastane","amenity":"hospital"}}
              ]
            }
        """.trimIndent()

        val result = OsmPoiParser.parse(payload)

        assertEquals(2, result.size)
        assertEquals(NavigationPoiCategory.MARKET, result[0].category)
        assertEquals("Örnek Sokak 12", result[0].address)
        assertEquals("Mo-Su 08:00-22:00", result[0].openingHours)
        assertEquals(41.011, result[1].latitude)
        assertEquals(NavigationPoiCategory.HOSPITAL, result[1].category)
    }

    @Test
    fun keeps_only_real_http_image_urls_and_never_guesses_missing_street_images() {
        val payload = """
            {"elements":[
              {"type":"node","id":1,"lat":41.01,"lon":29.02,"tags":{"name":"Gerçek Görsel","amenity":"cafe","image":"https://example.com/cafe.jpg","street_image_url":"not-a-url"}},
              {"type":"node","id":2,"lat":41.02,"lon":29.03,"tags":{"name":"Dosya Adı","amenity":"restaurant","image":"photo.jpg"}}
            ]}
        """.trimIndent()

        val result = OsmPoiParser.parse(payload)

        assertEquals("https://example.com/cafe.jpg", result[0].imageUrl)
        assertNull(result[0].streetImageUrl)
        assertNull(result[1].imageUrl)
    }

    @Test
    fun parses_real_world_osm_category_tags() {
        val payload = """
            {"elements":[
              {"type":"node","id":1,"lat":41.01,"lon":29.02,"tags":{"name":"Dinlenme","amenity":"rest_area"}},
              {"type":"node","id":2,"lat":41.02,"lon":29.03,"tags":{"name":"İbadethane","amenity":"place_of_worship"}},
              {"type":"node","id":3,"lat":41.03,"lon":29.04,"tags":{"name":"Ulaşım","public_transport":"platform"}},
              {"type":"node","id":4,"lat":41.04,"lon":29.05,"tags":{"name":"Hızlı Yemek","amenity":"fast_food"}}
            ]}
        """.trimIndent()

        val result = OsmPoiParser.parse(payload)

        assertEquals(NavigationPoiCategory.REST_AREA, result[0].category)
        assertEquals(NavigationPoiCategory.PLACE_OF_WORSHIP, result[1].category)
        assertEquals(NavigationPoiCategory.TRANSIT, result[2].category)
        assertEquals(NavigationPoiCategory.RESTAURANT, result[3].category)
    }

    @Test
    fun skips_entries_without_name_or_coordinates() {
        val payload = """
            {
              "elements": [
                {"type":"node","id":1,"lat":41.01,"lon":29.02,"tags":{"amenity":"fuel"}},
                {"type":"node","id":2,"tags":{"name":"Koordinatsız"}},
                {"type":"node","id":3,"lat":41.03,"lon":29.04,"tags":{"name":"Akaryakıt","amenity":"fuel"}}
              ]
            }
        """.trimIndent()

        val result = OsmPoiParser.parse(payload)

        assertEquals(1, result.size)
        assertEquals("Akaryakıt", result.single().name)
        assertNull(result.single().streetImageUrl)
    }

    @Test
    fun applies_result_cap_after_parsing() {
        val payload = """
            {"elements":[
              {"type":"node","id":1,"lat":41.01,"lon":29.02,"tags":{"name":"Bir","amenity":"cafe"}},
              {"type":"node","id":2,"lat":41.02,"lon":29.03,"tags":{"name":"İki","amenity":"cafe"}}
            ]}
        """.trimIndent()

        assertEquals(1, OsmPoiParser.parse(payload, maxResults = 1).size)
    }
}
