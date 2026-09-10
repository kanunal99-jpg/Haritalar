package com.haritalar.core.traffic

import com.haritalar.core.navigation.GeoCoordinate
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TrafficRouteRankingServiceTest {
    private val routeA = TrafficRouteRanking.RouteCandidate(
        routeId = "a",
        coordinates = listOf(GeoCoordinate(41.0, 29.0), GeoCoordinate(41.01, 29.01)),
        baseDurationSeconds = 2_400L,
    )

    private val routeB = TrafficRouteRanking.RouteCandidate(
        routeId = "b",
        coordinates = listOf(GeoCoordinate(41.02, 29.0), GeoCoordinate(41.02, 29.02)),
        baseDurationSeconds = 2_700L,
    )

    @Test
    fun emptyRoutesDoNotCallProvider() {
        var calls = 0
        val service = TrafficRouteRankingService(
            TrafficProviderChain(listOf(object : TrafficProvider {
                override val id = "fake"
                override val priority = 1
                override fun supports(coordinate: GeoCoordinate) = true
                override suspend fun fetchTraffic(bounds: TrafficBounds, route: TrafficRoute?): TrafficSnapshot {
                    calls++
                    error("must not be called")
                }
            })),
        )

        val result = runSuspend { service.rank(emptyList(), nowEpochMs = 1_000L) }
        assertTrue(result.isEmpty())
        assertEquals(0, calls)
    }

    @Test
    fun providerFailureKeepsBaseEtas() {
        val service = TrafficRouteRankingService(
            TrafficProviderChain(listOf(object : TrafficProvider {
                override val id = "failing"
                override val priority = 1
                override fun supports(coordinate: GeoCoordinate) = true
                override suspend fun fetchTraffic(bounds: TrafficBounds, route: TrafficRoute?) =
                    error("network failure")
            })),
        )

        val result = runSuspend { service.rank(listOf(routeA, routeB), nowEpochMs = 1_000L) }
        assertEquals(listOf(2_400L, 2_700L), result.map { it.adjustedDurationSeconds })
        assertTrue(result.none { it.trafficApplied })
    }

    @Test
    fun rankBlockingUsesSameRankingPath() {
        val service = TrafficRouteRankingService(
            TrafficProviderChain(emptyList()),
        )

        val result = service.rankBlocking(listOf(routeA, routeB), nowEpochMs = 1_000L, timeoutMs = 1_000L)

        assertEquals(listOf("a", "b"), result.map { it.routeId })
        assertEquals(listOf(2_400L, 2_700L), result.map { it.adjustedDurationSeconds })
        assertTrue(result.none { it.trafficApplied })
    }

    @Test
    fun verifiedSnapshotCanChangeRouteOrdering() {
        val service = TrafficRouteRankingService(
            TrafficProviderChain(listOf(object : TrafficProvider {
                override val id = "test-live"
                override val priority = 10
                override fun supports(coordinate: GeoCoordinate) = true
                override suspend fun fetchTraffic(bounds: TrafficBounds, route: TrafficRoute?) =
                    TrafficSnapshot(
                        providerId = id,
                        segments = listOf(
                            TrafficSegment(
                                id = "segment-a",
                                speedKmh = 20.0,
                                freeFlowSpeedKmh = 60.0,
                                confidence = TrafficConfidence.HIGH,
                                geometry = listOf(GeoCoordinate(41.0, 29.0), GeoCoordinate(41.01, 29.01)),
                            ),
                        ),
                        fetchedAtEpochMs = 900L,
                        expiresAtEpochMs = 2_000L,
                        confidence = TrafficConfidence.HIGH,
                    )
            })),
        )

        val result = runSuspend { service.rank(listOf(routeA, routeB), nowEpochMs = 1_000L) }
        assertEquals("b", result.first().routeId)
        assertTrue(result.any { it.routeId == "a" && it.trafficApplied })
        assertFalse(result.any { it.adjustedDurationSeconds < it.baseDurationSeconds })
    }

    @Test
    fun eachRouteGetsItsOwnTrafficObservation() {
        val calls = mutableListOf<String>()
        val service = TrafficRouteRankingService(
            TrafficProviderChain(listOf(object : TrafficProvider {
                override val id = "route-local-live"
                override val priority = 10
                override fun supports(coordinate: GeoCoordinate) = true
                override suspend fun fetchTraffic(bounds: TrafficBounds, route: TrafficRoute?): TrafficSnapshot {
                    val point = route?.coordinates?.singleOrNull()
                    calls += "${point?.latitude},${point?.longitude}"
                    val geometry = when {
                        point?.latitude == 41.01 -> listOf(GeoCoordinate(41.0, 29.0), GeoCoordinate(41.01, 29.01))
                        point?.latitude == 41.02 -> listOf(GeoCoordinate(41.02, 29.0), GeoCoordinate(41.02, 29.02))
                        else -> emptyList()
                    }
                    return TrafficSnapshot(
                        providerId = id,
                        segments = geometry.takeIf { it.size >= 2 }?.let {
                            listOf(
                                TrafficSegment(
                                    id = "segment-${point?.latitude}",
                                    speedKmh = 20.0,
                                    freeFlowSpeedKmh = 60.0,
                                    confidence = TrafficConfidence.HIGH,
                                    geometry = it,
                                ),
                            )
                        }.orEmpty(),
                        fetchedAtEpochMs = 900L,
                        expiresAtEpochMs = 2_000L,
                        confidence = TrafficConfidence.HIGH,
                    )
                }
            })),
        )

        val result = runSuspend { service.rank(listOf(routeA, routeB), nowEpochMs = 1_000L) }

        assertEquals(2, calls.size)
        assertTrue(result.all { it.trafficApplied })
        assertTrue(result.all { it.adjustedDurationSeconds > it.baseDurationSeconds })
    }

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
