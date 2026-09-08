package com.haritalar.core.navigation

import org.json.JSONObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ValhallaTransportParserTest {
    @Test
    fun detectsFerryFromExplicitFerryFlagAndKeepsRoad() {
        val response = JSONObject(
            """
            {
              "trip": {
                "legs": [{
                  "maneuvers": [
                    {"travel_type":"car"},
                    {"ferry":true}
                  ]
                }]
              }
            }
            """.trimIndent(),
        )

        val types = ValhallaTransportParser.parseTransportTypes(response)

        assertTrue(RouteTransportType.ROAD in types)
        assertTrue(RouteTransportType.FERRY in types)
    }

    @Test
    fun detectsFerryFromTravelTypeAndKnownFerryManeuverTypes() {
        val response = JSONObject(
            """
            {
              "trip": {
                "legs": [{
                  "maneuvers": [
                    {"travel_type":"ferry"},
                    {"type":28},
                    {"type":29}
                  ]
                }]
              }
            }
            """.trimIndent(),
        )

        val types = ValhallaTransportParser.parseTransportTypes(response)

        assertEquals(listOf(RouteTransportType.FERRY), types)
    }

    @Test
    fun doesNotGuessRoadWhenProviderOmitsTransportFields() {
        val response = JSONObject(
            """
            {
              "trip": {
                "legs": [{
                  "maneuvers": [
                    {"type":1},
                    {"instruction":"Continue"}
                  ]
                }]
              }
            }
            """.trimIndent(),
        )

        assertEquals(
            listOf(RouteTransportType.UNKNOWN),
            ValhallaTransportParser.parseTransportTypes(response),
        )
    }
}
