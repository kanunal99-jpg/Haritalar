package com.haritalar.app

import com.haritalar.core.navigation.GeoCoordinate
import com.haritalar.core.traffic.TomTomTrafficProvider
import com.haritalar.core.traffic.TrafficBounds
import com.haritalar.core.traffic.TrafficProviderChain
import com.haritalar.core.traffic.TrafficRoute
import com.haritalar.core.traffic.TrafficRouteRankingService
import com.haritalar.core.traffic.TrafficSegment
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

object TrafficEngineFactory {
    private val tomTomProvider = TomTomTrafficProvider(BuildConfig.TOMTOM_API_KEY)

    fun createRankingService(): TrafficRouteRankingService = TrafficRouteRankingService(
        providerChain = TrafficProviderChain(providers = listOf(tomTomProvider)),
    )

    fun fetchMapTrafficAtPointBlocking(point: GeoCoordinate, timeoutMs: Long = 15_000L): List<TrafficSegment> {
        if (!tomTomProvider.supports(point)) return emptyList()
        val bounds = TrafficBounds(point.latitude, point.longitude, point.latitude, point.longitude)
        val completed = CountDownLatch(1)
        val result = AtomicReference<Result<List<TrafficSegment>>?>()
        val block: suspend () -> List<TrafficSegment> = {
            tomTomProvider.fetchTraffic(bounds, TrafficRoute(listOf(point))).segments
        }
        block.startCoroutine(object : Continuation<List<TrafficSegment>> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(value: Result<List<TrafficSegment>>) {
                result.set(value)
                completed.countDown()
            }
        })
        if (!completed.await(timeoutMs, TimeUnit.MILLISECONDS)) return emptyList()
        return result.get()?.getOrNull().orEmpty()
    }
}
