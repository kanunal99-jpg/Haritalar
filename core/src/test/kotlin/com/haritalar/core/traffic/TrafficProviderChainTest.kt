package com.haritalar.core.traffic

import com.haritalar.core.navigation.GeoCoordinate
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TrafficProviderChainTest {
    private val coordinate = GeoCoordinate(41.0, 29.0)
    private val bounds = TrafficBounds(40.9, 28.9, 41.1, 29.1)

    @Test
    fun selectsHighestPrioritySupportedLiveProvider() {
        val low = FakeProvider("low", 10, TrafficSnapshot("low", emptyList(), 1, 10_000, TrafficConfidence.MEDIUM))
        val high = FakeProvider("high", 20, TrafficSnapshot("high", emptyList(), 1, 10_000, TrafficConfidence.HIGH))
        val chain = TrafficProviderChain(listOf(low, high))
        assertEquals("high", runSuspend { chain.fetch(coordinate, bounds, null, 100) }?.providerId)
    }

    @Test
    fun skipsFailingHigherPriorityProviderAndUsesNextLiveProvider() {
        val failing = object : TrafficProvider {
            override val id = "failing"
            override val priority = 100
            override fun supports(coordinate: GeoCoordinate) = true
            override suspend fun fetchTraffic(bounds: TrafficBounds, route: TrafficRoute?): TrafficSnapshot {
                error("provider unavailable")
            }
        }
        val backup = FakeProvider("backup", 50, TrafficSnapshot("backup", emptyList(), 1, 10_000, TrafficConfidence.MEDIUM))
        val chain = TrafficProviderChain(listOf(failing, backup))
        assertEquals("backup", runSuspend { chain.fetch(coordinate, bounds, null, 100) }?.providerId)
    }

    @Test
    fun rejectsMismatchedProviderIdAndUsesNextLiveProvider() {
        val wrongIdentity = FakeProvider("declared", 100, TrafficSnapshot("different", emptyList(), 1, 10_000, TrafficConfidence.HIGH))
        val backup = FakeProvider("backup", 50, TrafficSnapshot("backup", emptyList(), 1, 10_000, TrafficConfidence.MEDIUM))
        val chain = TrafficProviderChain(listOf(wrongIdentity, backup))
        assertEquals("backup", runSuspend { chain.fetch(coordinate, bounds, null, 100) }?.providerId)
    }

    @Test
    fun rejectsMalformedSpeedAndUsesNextLiveProvider() {
        val malformed = FakeProvider("bad", 100, TrafficSnapshot("bad", listOf(segment(speedKmh = -1.0)), 1, 10_000, TrafficConfidence.HIGH))
        val backup = FakeProvider("backup", 50, TrafficSnapshot("backup", emptyList(), 1, 10_000, TrafficConfidence.MEDIUM))
        val chain = TrafficProviderChain(listOf(malformed, backup))
        assertEquals("backup", runSuspend { chain.fetch(coordinate, bounds, null, 100) }?.providerId)
    }

    @Test
    fun rejectsNonFiniteBearingAndUsesNextLiveProvider() {
        val malformed = FakeProvider("bad", 100, TrafficSnapshot("bad", listOf(segment(bearing = Double.NaN)), 1, 10_000, TrafficConfidence.HIGH))
        val backup = FakeProvider("backup", 50, TrafficSnapshot("backup", emptyList(), 1, 10_000, TrafficConfidence.MEDIUM))
        val chain = TrafficProviderChain(listOf(malformed, backup))
        assertEquals("backup", runSuspend { chain.fetch(coordinate, bounds, null, 100) }?.providerId)
    }

    @Test
    fun rejectsInvalidGeometryCoordinateAndUsesNextLiveProvider() {
        val malformed = FakeProvider(
            "bad",
            100,
            TrafficSnapshot(
                "bad",
                listOf(segment(geometry = listOf(GeoCoordinate(91.0, 29.0), GeoCoordinate(41.001, 29.0)))),
                1,
                10_000,
                TrafficConfidence.HIGH,
            ),
        )
        val backup = FakeProvider("backup", 50, TrafficSnapshot("backup", emptyList(), 1, 10_000, TrafficConfidence.MEDIUM))
        val chain = TrafficProviderChain(listOf(malformed, backup))
        assertEquals("backup", runSuspend { chain.fetch(coordinate, bounds, null, 100) }?.providerId)
    }

    @Test
    fun skipsExpiredLiveDataAndUsesCache() {
        val live = FakeProvider("live", 100, TrafficSnapshot("live", emptyList(), 1, 50, TrafficConfidence.HIGH))
        val cached = TrafficSnapshot("cache", emptyList(), 1, 10_000, TrafficConfidence.MEDIUM)
        val chain = TrafficProviderChain(listOf(live), object : TrafficCacheProvider {
            override suspend fun fetchCached(bounds: TrafficBounds, route: TrafficRoute?) = cached
        })
        assertEquals("cache", runSuspend { chain.fetch(coordinate, bounds, null, 100) }?.providerId)
    }

    @Test
    fun usesFreshFallbackWhenLiveAndCacheAreUnavailable() {
        val fallback = TrafficSnapshot("fallback", emptyList(), 1, 10_000, TrafficConfidence.MEDIUM)
        val chain = TrafficProviderChain(
            providers = emptyList(),
            fallbackProvider = object : TrafficFallbackProvider {
                override suspend fun fetchFallback(bounds: TrafficBounds, route: TrafficRoute?) = fallback
            },
        )
        assertEquals("fallback", runSuspend { chain.fetch(coordinate, bounds, null, 100) }?.providerId)
    }

    @Test
    fun rejectsExpiredFallbackData() {
        val expired = TrafficSnapshot("fallback", emptyList(), 1, 50, TrafficConfidence.MEDIUM)
        val chain = TrafficProviderChain(
            providers = emptyList(),
            fallbackProvider = object : TrafficFallbackProvider {
                override suspend fun fetchFallback(bounds: TrafficBounds, route: TrafficRoute?) = expired
            },
        )
        assertNull(runSuspend { chain.fetch(coordinate, bounds, null, 100) })
    }

    @Test
    fun ignoresFailingFallbackProvider() {
        val chain = TrafficProviderChain(
            providers = emptyList(),
            fallbackProvider = object : TrafficFallbackProvider {
                override suspend fun fetchFallback(bounds: TrafficBounds, route: TrafficRoute?): TrafficSnapshot? {
                    error("fallback unavailable")
                }
            },
        )
        assertNull(runSuspend { chain.fetch(coordinate, bounds, null, 100) })
    }

    @Test
    fun returnsNullWhenNothingCanProvideData() {
        val chain = TrafficProviderChain(emptyList())
        assertNull(runSuspend { chain.fetch(coordinate, bounds, null, 100) })
    }

    private fun segment(
        speedKmh: Double? = 30.0,
        bearing: Double? = null,
        geometry: List<GeoCoordinate> = listOf(GeoCoordinate(41.0004, 29.0001), GeoCoordinate(41.0008, 29.0001)),
    ) = TrafficSegment(
        id = "segment",
        speedKmh = speedKmh,
        freeFlowSpeedKmh = 60.0,
        directionBearingDegrees = bearing,
        confidence = TrafficConfidence.HIGH,
        geometry = geometry,
    )

    private class FakeProvider(
        override val id: String,
        override val priority: Int,
        private val snapshot: TrafficSnapshot,
    ) : TrafficProvider {
        override fun supports(coordinate: GeoCoordinate) = true
        override suspend fun fetchTraffic(bounds: TrafficBounds, route: TrafficRoute?) = snapshot
    }

    private fun <T> runSuspend(block: suspend () -> T): T {
        var result: Result<T>? = null
        block.startCoroutine(object : Continuation<T> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(value: Result<T>) { result = value }
        })
        return result!!.getOrThrow()
    }
}
