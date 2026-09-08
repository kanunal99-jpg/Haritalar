package com.haritalar.app

import com.haritalar.core.traffic.TomTomTrafficProvider
import com.haritalar.core.traffic.TrafficProviderChain
import com.haritalar.core.traffic.TrafficRouteRankingService

/**
 * Creates the app's traffic ranking engine without embedding credentials in source.
 *
 * An empty API key intentionally produces an inert TomTom provider. The core
 * provider chain then returns no live snapshot, so route ETAs remain the
 * routing-provider values rather than being replaced by fabricated traffic.
 */
object TrafficEngineFactory {
    fun createRankingService(): TrafficRouteRankingService {
        val provider = TomTomTrafficProvider(BuildConfig.TOMTOM_API_KEY)
        return TrafficRouteRankingService(
            providerChain = TrafficProviderChain(providers = listOf(provider)),
        )
    }
}
