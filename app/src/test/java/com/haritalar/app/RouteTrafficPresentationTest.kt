package com.haritalar.app

import com.haritalar.core.traffic.TrafficRouteRanking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RouteTrafficPresentationTest {
    @Test
    fun `maps verified traffic delay to ui model`() {
        val ranked = TrafficRouteRanking.RankedCandidate("route-1", 2400, 2820, true)
        val model = RouteTrafficPresentation.from(ranked)
        assertEquals(2400.0, model.baseDurationSeconds)
        assertEquals(2820.0, model.adjustedDurationSeconds)
        assertEquals(420.0, model.delaySeconds)
        assertTrue(model.trafficApplied)
        assertEquals("47 dk • trafik +7 dk", model.durationLabel())
    }

    @Test
    fun `does not manufacture traffic delay when ranking did not apply traffic`() {
        val ranked = TrafficRouteRanking.RankedCandidate("route-2", 2400, 2400, false)
        val model = RouteTrafficPresentation.from(ranked)
        assertEquals("40 dk", model.durationLabel())
        assertEquals(0.0, model.delaySeconds)
    }

    @Test
    fun `ranked list preserves core ordering`() {
        val ranked = listOf(
            TrafficRouteRanking.RankedCandidate("fast", 2400, 2820, true),
            TrafficRouteRanking.RankedCandidate("slow", 3000, 3000, false),
        )
        val models = RouteTrafficPresentation.fromRanked(ranked)
        assertEquals(2, models.size)
        assertEquals("47 dk • trafik +7 dk", models[0].durationLabel())
        assertEquals("50 dk", models[1].durationLabel())
    }

    @Test
    fun `ranked list normalizes faster adjusted duration`() {
        val ranked = listOf(
            TrafficRouteRanking.RankedCandidate("route", 2400, 2000, true),
        )
        val model = RouteTrafficPresentation.fromRanked(ranked).single()
        assertEquals("40 dk", model.durationLabel())
        assertEquals(false, model.trafficApplied)
    }
}
