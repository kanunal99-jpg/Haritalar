package com.haritalar.navigation

import com.haritalar.core.traffic.TrafficProviderChain
import com.haritalar.core.traffic.TrafficRouteRanking
import com.haritalar.core.traffic.TrafficRouteRankingService
import com.haritalar.core.traffic.TrafficRefreshCoordinator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class TrafficRefreshNavigationBridgeTest {
    @Test
    fun `location update runs refresh on supplied executor and returns ranked snapshot`() {
        val coordinator = TrafficRefreshCoordinator(minimumIntervalMs = 60_000L)
        val rankingService = TrafficRouteRankingService(TrafficProviderChain(emptyList()))
        val callback = CountDownLatch(1)
        var callbackThread = ""
        var rankedSize = -1

        val bridge = TrafficRefreshNavigationBridge(
            coordinator = coordinator,
            rankingService = rankingService,
            backgroundExecutor = java.util.concurrent.Executor { task ->
                Thread(task, "traffic-refresh-test").start()
            },
            onRefreshed = { ranked ->
                callbackThread = Thread.currentThread().name
                rankedSize = ranked.size
                callback.countDown()
            },
        )

        bridge.onLocationUpdate(
            routes = listOf(
                TrafficRouteRanking.RouteCandidate(
                    routeId = "route-a",
                    coordinates = emptyList(),
                    baseDurationSeconds = 600L,
                ),
            ),
            nowEpochMs = 1_000L,
        )

        assertTrue(callback.await(2, TimeUnit.SECONDS))
        assertEquals("traffic-refresh-test", callbackThread)
        assertEquals(1, rankedSize)
    }

    @Test
    fun `route change resets coordinator cooldown`() {
        val coordinator = TrafficRefreshCoordinator(minimumIntervalMs = 60_000L)
        val rankingService = TrafficRouteRankingService(TrafficProviderChain(emptyList()))
        val first = CountDownLatch(1)
        val second = CountDownLatch(1)
        var refreshCount = 0

        val bridge = TrafficRefreshNavigationBridge(
            coordinator = coordinator,
            rankingService = rankingService,
            backgroundExecutor = java.util.concurrent.Executor { it.run() },
            onRefreshed = {
                refreshCount++
                if (refreshCount == 1) first.countDown() else second.countDown()
            },
        )
        val route = TrafficRouteRanking.RouteCandidate("route-a", emptyList(), 600L)

        bridge.onLocationUpdate(listOf(route), 1_000L)
        assertTrue(first.await(2, TimeUnit.SECONDS))
        bridge.onRouteChanged()
        bridge.onLocationUpdate(listOf(route), 2_000L)
        assertTrue(second.await(2, TimeUnit.SECONDS))
        assertEquals(2, refreshCount)
    }
}
