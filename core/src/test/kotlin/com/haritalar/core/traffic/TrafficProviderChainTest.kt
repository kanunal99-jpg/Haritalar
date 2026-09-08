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
