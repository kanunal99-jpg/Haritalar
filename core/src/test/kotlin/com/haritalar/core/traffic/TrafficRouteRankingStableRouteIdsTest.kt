package com.haritalar.core.traffic

import com.haritalar.core.navigation.GeoCoordinate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TrafficRouteRankingStableRouteIdsTest {
    private val trafficGeometry = listOf(
        GeoCoordinate(41.0000, 29.0000),
        GeoCoordinate(41.0100, 29.0000),
    )

    @Test
    fun `traffic reordering preserves every stable route id exactly once`() {
        val routeIds = listOf(
            "fastest",
            "shortest",
            "free-road",
            "fast-toll",
            "fast-ferry",
            "no-ferry",
            "free-no-ferry",
        )
        val routes = routeIds.mapIndexed { index, id ->
            TrafficRouteRanking.RouteCandidate(
                routeId = id,
                coordinates = trafficGeometry,
                baseDurationSeconds = 900L + (index * 60L),
            )
        }
        val snapshot = TrafficSnapshot(
            providerId = "test-live",
            segments = listOf(
                TrafficSegment(
                    id = "segment-1",
                    speedKmh = 15.0,
                    freeFlowSpeedKmh = 60.0,
                    confidence = TrafficConfidence.HIGH,
                    geometry = trafficGeometry,
                ),
            ),
            fetchedAtEpochMs = 1_000L,
            expiresAtEpochMs = 10_000L,
            confidence = TrafficConfidence.HIGH,
        )

        val ranked = TrafficRouteRanking.rank(
            routes = routes,
            snapshot = snapshot,
            expectedProviderId = "test-live",
            nowEpochMs = 2_000L,
        )

        assertEquals(routeIds.toSet(), ranked.map { it.routeId }.toSet())
        assertEquals(routeIds.size, ranked.map { it.routeId }.distinct().size)
        assertEquals(routeIds.size, ranked.size)
        assertTrue(ranked.any { it.routeId == "fastest" && it.trafficApplied })
    }
}
