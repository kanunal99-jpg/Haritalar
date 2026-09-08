package com.haritalar.core.traffic

import com.haritalar.core.navigation.GeoCoordinate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TrafficRouteRankingTest {
    private val route = listOf(
        GeoCoordinate(41.0000, 29.0000),
        GeoCoordinate(41.0100, 29.0000),
    )

    private fun snapshot(
        speedKmh: Double,
        confidence: TrafficConfidence = TrafficConfidence.HIGH,
    ) = TrafficSnapshot(
        providerId = "test-live",
        segments = listOf(
            TrafficSegment(
                id = "segment-1",
                speedKmh = speedKmh,
                freeFlowSpeedKmh = 60.0,
                confidence = confidence,
                geometry = route,
            ),
        ),
        fetchedAtEpochMs = 1_000L,
        expiresAtEpochMs = 10_000L,
        confidence = confidence,
    )

    @Test
    fun `verified traffic can change route ordering`() {
        val routes = listOf(
            TrafficRouteRanking.RouteCandidate("slow-route", route, 600L),
            TrafficRouteRanking.RouteCandidate("fast-route", route, 700L),
        )

        val ranked = TrafficRouteRanking.rank(
            routes = routes,
            snapshot = snapshot(speedKmh = 20.0),
            expectedProviderId = "test-live",
            nowEpochMs = 2_000L,
        )

        assertEquals("fast-route", ranked.first().routeId)
        assertTrue(ranked.first().adjustedDurationSeconds < ranked.last().adjustedDurationSeconds)
        assertTrue(ranked.any { it.trafficApplied })
    }

    @Test
    fun `missing snapshot preserves provider eta`() {
        val routes = listOf(
            TrafficRouteRanking.RouteCandidate("a", route, 600L),
            TrafficRouteRanking.RouteCandidate("b", route, 700L),
        )

        val ranked = TrafficRouteRanking.rank(routes, null, nowEpochMs = 2_000L)

        assertEquals(listOf(600L, 700L), ranked.map { it.adjustedDurationSeconds })
        assertTrue(ranked.none { it.trafficApplied })
    }

    @Test
    fun `expired snapshot is ignored`() {
        val ranked = TrafficRouteRanking.rank(
            routes = listOf(TrafficRouteRanking.RouteCandidate("a", route, 600L)),
            snapshot = snapshot(20.0),
            nowEpochMs = 10_000L,
        )

        assertEquals(600L, ranked.single().adjustedDurationSeconds)
        assertFalse(ranked.single().trafficApplied)
    }

    @Test
    fun `low confidence snapshot is ignored`() {
        val ranked = TrafficRouteRanking.rank(
            routes = listOf(TrafficRouteRanking.RouteCandidate("a", route, 600L)),
            snapshot = snapshot(20.0, TrafficConfidence.LOW),
            nowEpochMs = 2_000L,
        )

        assertEquals(600L, ranked.single().adjustedDurationSeconds)
        assertFalse(ranked.single().trafficApplied)
    }
}
