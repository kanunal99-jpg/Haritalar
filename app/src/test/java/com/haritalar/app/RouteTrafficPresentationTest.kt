package com.haritalar.app

import com.haritalar.core.traffic.TrafficRouteRanking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RouteTrafficPresentationTest {
    @Test
    fun `maps verified traffic delay to ui model`() {
        val ranked = TrafficRouteRanking.RankedCandidate(
            routeId = "route-1",
            baseDurationSeconds = 2400,
            adjustedDurationSeconds = 2820,
            trafficApplied = true,
        )

        val model = RouteTrafficPresentation.from(ranked)

        assertEquals(2400.0, model.baseDurationSeconds)
        assertEquals(2820.0, model.adjustedDurationSeconds)
        assertEquals(420.0, model.delaySeconds)
        assertTrue(model.trafficApplied)
        assertEquals("47 dk • trafik +7 dk", model.durationLabel())
    }

    @Test
    fun `does not manufacture traffic delay when ranking did not apply traffic`() {
        val ranked = TrafficRouteRanking.RankedCandidate(
            routeId = "route-2",
            baseDurationSeconds = 2400,
            adjustedDurationSeconds = 2400,
            trafficApplied = false,
        )

        val model = RouteTrafficPresentation.from(ranked)

        assertEquals("40 dk", model.durationLabel())
        assertEquals(0.0, model.delaySeconds)
    }
}
