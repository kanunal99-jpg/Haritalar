package com.haritalar.core.traffic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RouteTrafficIntelligenceTest {
    @Test
    fun `ranks by adjusted traffic eta`() {
        val slow = TrafficRouteIntelligence.RouteInput(
            routeId = "slow",
            baseDurationSeconds = 1_000,
            trafficSegments = listOf(segment(20.0, 60.0)),
        )
        val clear = TrafficRouteIntelligence.RouteInput(
            routeId = "clear",
            baseDurationSeconds = 1_100,
            trafficSegments = listOf(segment(60.0, 60.0)),
        )

        val ranked = TrafficRouteIntelligence.rank(listOf(slow, clear))

        assertEquals(listOf("clear", "slow"), ranked.map { it.routeId })
        assertTrue(ranked.first().trafficApplied)
        assertTrue(ranked.last().adjustedDurationSeconds > ranked.last().baseDurationSeconds)
    }

    @Test
    fun `missing traffic does not change eta`() {
        val route = TrafficRouteIntelligence.RouteInput("r1", 900)
        val ranked = TrafficRouteIntelligence.rank(listOf(route))

        assertEquals(900, ranked.single().adjustedDurationSeconds)
        assertFalse(ranked.single().trafficApplied)
    }

    @Test
    fun `low confidence traffic does not change eta`() {
        val route = TrafficRouteIntelligence.RouteInput(
            "r1",
            900,
            listOf(
                TrafficRouteSegment(
                    distanceMeters = 1_000.0,
                    traffic = TrafficSegment(
                        id = "low-confidence",
                        speedKmh = 10.0,
                        freeFlowSpeedKmh = 60.0,
                        confidence = TrafficConfidence.LOW,
                    ),
                ),
            ),
        )

        val ranked = TrafficRouteIntelligence.rank(listOf(route))

        assertEquals(900, ranked.single().adjustedDurationSeconds)
        assertFalse(ranked.single().trafficApplied)
    }

    @Test
    fun `speed above free flow never makes route faster`() {
        val route = TrafficRouteIntelligence.RouteInput(
            "r1",
            900,
            listOf(segment(100.0, 80.0)),
        )

        val ranked = TrafficRouteIntelligence.rank(listOf(route))

        assertEquals(900, ranked.single().adjustedDurationSeconds)
        assertTrue(ranked.single().trafficApplied)
    }

    private fun segment(live: Double, freeFlow: Double) = TrafficRouteSegment(
        distanceMeters = 1_000.0,
        traffic = TrafficSegment(
            id = "segment",
            speedKmh = live,
            freeFlowSpeedKmh = freeFlow,
            confidence = TrafficConfidence.HIGH,
        ),
    )
}
