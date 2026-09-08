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
