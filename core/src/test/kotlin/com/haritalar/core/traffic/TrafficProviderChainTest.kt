package com.haritalar.core.traffic

import com.haritalar.core.navigation.GeoCoordinate
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TrafficProviderChainTest {
    private val coordinate = GeoCoordinate(41.0, 29.0)
    private val bounds = TrafficBounds(40.9, 28.9, 41.1, 29.1)

    @Test
    fun selectsHighestPrioritySupportedLiveProvider() = runTest {
        val low = FakeProvider("low", 10, TrafficSnapshot("low", emptyList(), 1, 10_000, TrafficConfidence.MEDIUM))
        val high = FakeProvider("high", 20, TrafficSnapshot("high", emptyList(), 1, 10_000, TrafficConfidence.HIGH))
        val chain = TrafficProviderChain(listOf(low, high))

        assertEquals("high", chain.fetch(coordinate, bounds, null, 100)?.providerId)
    }

    @Test
    fun skipsExpiredLiveDataAndUsesCache() = runTest {
        val live = FakeProvider("live", 100, TrafficSnapshot("live", emptyList(), 1, 50, TrafficConfidence.HIGH))
        val cached = TrafficSnapshot("cache", emptyList(), 1, 10_000, TrafficConfidence.MEDIUM)
        val chain = TrafficProviderChain(listOf(live), object : TrafficCacheProvider {
            override suspend fun fetchCached(bounds: TrafficBounds, route: TrafficRoute?) = cached
        })

        assertEquals("cache", chain.fetch(coordinate, bounds, null, 100)?.providerId)
    }

    @Test
    fun returnsNullWhenNothingCanProvideData() = runTest {
        val chain = TrafficProviderChain(emptyList())
        assertNull(chain.fetch(coordinate, bounds, null, 100))
    }

    private class FakeProvider(
        override val id: String,
        override val priority: Int,
        private val snapshot: TrafficSnapshot,
    ) : TrafficProvider {
        override fun supports(coordinate: GeoCoordinate) = true
        override suspend fun fetchTraffic(bounds: TrafficBounds, route: TrafficRoute?) = snapshot
    }
}
