package com.haritalar.core.traffic

import com.haritalar.core.navigation.GeoCoordinate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class TrafficRouteOrchestratorTest {
    private val route = listOf(
        GeoCoordinate(41.0000, 29.0000),
        GeoCoordinate(41.0010, 29.0000),
    )

    @Test
    fun nullSnapshotPreservesBaseEtaAndOrder() {
        val routes = listOf(
            TrafficRouteOrchestrator.RouteCandidate("fast", route, 100L),
            TrafficRouteOrchestrator.RouteCandidate("short", route, 120L),
        )

        val result = TrafficRouteOrchestrator.rank(routes, null, "provider", 1L)

        assertEquals(listOf("fast", "short"), result.map { it.candidate.id })
        assertEquals(listOf(100L, 120L), result.map { it.trafficDurationSeconds })
        assertFalse(result.any { it.trafficApplied })
    }
}
