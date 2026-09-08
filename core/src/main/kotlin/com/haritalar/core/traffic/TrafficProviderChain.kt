package com.haritalar.core.traffic

import com.haritalar.core.navigation.GeoCoordinate

/**
 * Resilient traffic selection: use supported live providers first, then cache,
 * then historical/default fallback. No synthetic traffic values are generated.
 */
class TrafficProviderChain(
    private val providers: List<TrafficProvider>,
    private val cacheProvider: TrafficCacheProvider? = null,
    private val fallbackProvider: TrafficFallbackProvider? = null,
) {
    suspend fun fetch(
        coordinate: GeoCoordinate,
        bounds: TrafficBounds,
        route: TrafficRoute?,
        nowEpochMs: Long = System.currentTimeMillis(),
    ): TrafficSnapshot? {
        val ordered = providers
            .filter { it.supports(coordinate) }
            .sortedByDescending { it.priority }

        for (provider in ordered) {
            runCatching { provider.fetchTraffic(bounds, route) }
                .getOrNull()
                ?.takeIf { it.providerId == provider.id && it.isUsable(nowEpochMs) }
                ?.let { return it }
        }

        cacheProvider?.let {
            runCatching { it.fetchCached(bounds, route) }
                .getOrNull()
                ?.takeIf { it.isUsable(nowEpochMs) }
                ?.let { return it }
        }

        return fallbackProvider
            ?.let { provider ->
                runCatching { provider.fetchFallback(bounds, route) }
                    .getOrNull()
                    ?.takeIf { it.isUsable(nowEpochMs) }
            }
    }
}

interface TrafficCacheProvider {
    suspend fun fetchCached(bounds: TrafficBounds, route: TrafficRoute?): TrafficSnapshot?
}

interface TrafficFallbackProvider {
    /** Fallback may report no-data or historical data; it must not invent live traffic. */
    suspend fun fetchFallback(bounds: TrafficBounds, route: TrafficRoute?): TrafficSnapshot?
}
