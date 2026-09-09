package com.haritalar.core.traffic

import com.haritalar.core.navigation.GeoCoordinate
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TrafficRouteRankingServiceDetailedTest {
    @Test
    fun detailedResultExposesOnlyMatchedVerifiedSegments() {
        val route = TrafficRouteRanking.RouteCandidate(
            routeId = "route-a",
            coordinates = listOf(GeoCoordinate(41.0, 29.0), GeoCoordinate(41.01, 29.01)),
            baseDurationSeconds = 2_400L,
        )
        val matched = TrafficSegment(
            id = "matched",
            speedKmh = 20.0,
            freeFlowSpeedKmh = 60.0,
            congestion = TrafficCongestion.HEAVY,
            confidence = TrafficConfidence.HIGH,
            geometry = route.coordinates,
        )
        val unrelated = TrafficSegment(
            id = "unrelated",
            speedKmh = 10.0,
            freeFlowSpeedKmh = 60.0,
            congestion = TrafficCongestion.SEVERE,
            confidence = TrafficConfidence.HIGH,
            geometry = listOf(GeoCoordinate(41.2, 29.2), GeoCoordinate(41.21, 29.21)),
        )
        val service = TrafficRouteRankingService(
            TrafficProviderChain(listOf(object : TrafficProvider {
                override val id = "test-live"
                override val priority = 10
                override fun supports(coordinate: GeoCoordinate) = true
                override suspend fun fetchTraffic(bounds: TrafficBounds, route: TrafficRoute?) = TrafficSnapshot(
                    providerId = id,
                    segments = listOf(matched, unrelated),
                    fetchedAtEpochMs = 1_000L,
                    expiresAtEpochMs = 10_000L,
                    confidence = TrafficConfidence.HIGH,
                )
            })),
        )

        val result = runSuspend { service.rankDetailed(listOf(route), nowEpochMs = 2_000L) }
        val segments = result.matchedSegmentsByRoute.getValue("route-a")

        assertEquals(1, segments.size)
        assertEquals("matched", segments.single().traffic.id)
        assertEquals(2_400L * 3L, result.ranked.single().adjustedDurationSeconds)
        assertTrue(result.ranked.single().trafficApplied)
    }

    private fun <T> runSuspend(block: suspend () -> T): T {
        var value: Result<T>? = null
        block.startCoroutine(object : Continuation<T> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(result: Result<T>) { value = result }
        })
        return value!!.getOrThrow()
    }
}
