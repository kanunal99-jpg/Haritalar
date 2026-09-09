package com.haritalar.core.traffic

import com.haritalar.core.navigation.GeoCoordinate
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class TrafficRouteRankingServiceFreshnessTest {
    private val route = TrafficRouteRanking.RouteCandidate(
        routeId = "route-a",
        coordinates = listOf(
            GeoCoordinate(41.0, 29.0),
            GeoCoordinate(41.01, 29.01),
        ),
        baseDurationSeconds = 2_400L,
    )

    @Test
    fun expiredSnapshotFallsBackToBaseEta() {
        val service = serviceWithSnapshot(
            TrafficSnapshot(
                providerId = "test-live",
                segments = listOf(
                    TrafficSegment(
                        id = "segment-a",
                        speedKmh = 20.0,
                        freeFlowSpeedKmh = 60.0,
                        confidence = TrafficConfidence.HIGH,
                        geometry = route.coordinates,
                    ),
                ),
                fetchedAtEpochMs = 0L,
                expiresAtEpochMs = 1_000L,
                confidence = TrafficConfidence.HIGH,
            ),
        )

        val result = runSuspend { service.rank(listOf(route), nowEpochMs = 1_001L) }

        assertEquals(2_400L, result.single().adjustedDurationSeconds)
        assertFalse(result.single().trafficApplied)
    }

    @Test
    fun lowConfidenceSnapshotFallsBackToBaseEta() {
        val service = serviceWithSnapshot(
            TrafficSnapshot(
                providerId = "test-live",
                segments = listOf(
                    TrafficSegment(
                        id = "segment-a",
                        speedKmh = 20.0,
                        freeFlowSpeedKmh = 60.0,
                        confidence = TrafficConfidence.HIGH,
                        geometry = route.coordinates,
                    ),
                ),
                fetchedAtEpochMs = 900L,
                expiresAtEpochMs = 2_000L,
                confidence = TrafficConfidence.LOW,
            ),
        )

        val result = runSuspend { service.rank(listOf(route), nowEpochMs = 1_000L) }

        assertEquals(2_400L, result.single().adjustedDurationSeconds)
        assertFalse(result.single().trafficApplied)
    }

    private fun serviceWithSnapshot(snapshot: TrafficSnapshot): TrafficRouteRankingService =
        TrafficRouteRankingService(
            TrafficProviderChain(
                listOf(object : TrafficProvider {
                    override val id = "test-live"
                    override val priority = 10
                    override fun supports(coordinate: GeoCoordinate) = true
                    override suspend fun fetchTraffic(
                        bounds: TrafficBounds,
                        route: TrafficRoute?,
                    ) = snapshot
                }),
            ),
        )

    private fun <T> runSuspend(block: suspend () -> T): T {
        var value: Result<T>? = null
        block.startCoroutine(object : Continuation<T> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(result: Result<T>) {
                value = result
            }
        })
        return value!!.getOrThrow()
    }
}
