package com.haritalar.core.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ValhallaRouteRequestTest {
    @Test
    fun `request defaults to allowing ferry and carries transport preference`() {
        val request = ValhallaRouteRequest.build(41.0, 29.0, 40.0, 29.0, "auto")
        val auto = request.getJSONObject("costing_options").getJSONObject("auto")

        assertEquals(0.5, auto.getDouble("use_ferry"))
        assertEquals("auto", request.getString("costing"))
        assertNotNull(request.getJSONArray("locations"))
    }

    @Test
    fun `avoid ferry and no toll are combined without dropping either policy`() {
        val request = ValhallaRouteRequest.build(
            41.0,
            29.0,
            40.0,
            29.0,
            "no_toll",
            RouteTransportPreference.AVOID_FERRY,
        )
        val auto = request.getJSONObject("costing_options").getJSONObject("auto")

        assertEquals(0.0, auto.getDouble("use_ferry"))
        assertEquals(0.0, auto.getDouble("use_tolls"))
        assertEquals(3600.0, auto.getDouble("toll_booth_penalty"))
    }

    @Test
    fun `prefer ferry and toll fast are combined without inventing ferry fare`() {
        val request = ValhallaRouteRequest.build(
            41.0,
            29.0,
            40.0,
            29.0,
            "toll_fast",
            RouteTransportPreference.PREFER_FERRY,
        )
        val auto = request.getJSONObject("costing_options").getJSONObject("auto")

        assertEquals(1.0, auto.getDouble("use_ferry"))
        assertEquals(1.0, auto.getDouble("use_tolls"))
        assertEquals(false, auto.has("ferry_cost"))
    }
}
