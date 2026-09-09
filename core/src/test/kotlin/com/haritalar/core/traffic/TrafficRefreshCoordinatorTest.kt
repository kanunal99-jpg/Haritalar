package com.haritalar.core.traffic

import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class TrafficRefreshCoordinatorTest {
    private val route = TrafficRouteRanking.RouteCandidate(
        routeId = "route-a",
        coordinates = emptyList(),
        baseDurationSeconds = 600L,
    )

    private val ranked = listOf(
        TrafficRouteRanking.RankedCandidate(
            routeId = "route-a",
            baseDurationSeconds = 600L,
            adjustedDurationSeconds = 540L,
            trafficApplied = true,
        ),
    )

    @Test
    fun `cooldown prevents repeated ranking`() {
        val coordinator = TrafficRefreshCoordinator(minimumIntervalMs = 60_000L)
        val calls = AtomicInteger(0)
        val ranker: suspend (List<TrafficRouteRanking.RouteCandidate>, Long) -> List<TrafficRouteRanking.RankedCandidate> = { _, _ ->
            calls.incrementAndGet()
            ranked
        }

        val first = await { coordinator.refresh(listOf(route), 1_000L, ranker) }
        val second = await { coordinator.refresh(listOf(route), 30_000L, ranker) }
        val third = await { coordinator.refresh(listOf(route), 61_000L, ranker) }

        assertIs<TrafficRefreshCoordinator.Result.Refreshed>(first)
        assertIs<TrafficRefreshCoordinator.Result.Skipped>(second)
        assertIs<TrafficRefreshCoordinator.Result.Refreshed>(third)
        assertEquals(2, calls.get())
    }

    @Test
    fun `reset allows immediate refresh`() {
        val coordinator = TrafficRefreshCoordinator(minimumIntervalMs = 60_000L)
        val calls = AtomicInteger(0)
        val ranker: suspend (List<TrafficRouteRanking.RouteCandidate>, Long) -> List<TrafficRouteRanking.RankedCandidate> = { _, _ ->
            calls.incrementAndGet()
            ranked
        }

        await { coordinator.refresh(listOf(route), 1_000L, ranker) }
        coordinator.reset()
        val result = await { coordinator.refresh(listOf(route), 2_000L, ranker) }

        assertIs<TrafficRefreshCoordinator.Result.Refreshed>(result)
        assertEquals(2, calls.get())
    }

    @Test
    fun `running refresh becomes stale after reset`() {
        val coordinator = TrafficRefreshCoordinator(minimumIntervalMs = 0L)
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val resultRef = AtomicReference<TrafficRefreshCoordinator.Result>()

        val ranker: suspend (List<TrafficRouteRanking.RouteCandidate>, Long) -> List<TrafficRouteRanking.RankedCandidate> = { _, _ ->
            started.countDown()
            release.await()
            ranked
        }

        val worker = Thread {
            resultRef.set(await { coordinator.refresh(listOf(route), 1_000L, ranker) })
        }
        worker.start()
        assertTrue(started.await(2, java.util.concurrent.TimeUnit.SECONDS))
        coordinator.reset()
        release.countDown()
        worker.join(2_000L)

        assertIs<TrafficRefreshCoordinator.Result.Stale>(resultRef.get())
    }

    @Test
    fun `blocking refresh delegates to canonical ranker`() {
        val coordinator = TrafficRefreshCoordinator(minimumIntervalMs = 0L)
        val calls = AtomicInteger(0)

        val result = coordinator.refreshBlocking(
            routes = listOf(route),
            nowEpochMs = 10_000L,
        ) { routes, nowEpochMs ->
            assertEquals(listOf(route), routes)
            assertEquals(10_000L, nowEpochMs)
            calls.incrementAndGet()
            ranked
        }

        assertIs<TrafficRefreshCoordinator.Result.Refreshed>(result)
        assertEquals(1, calls.get())
        assertEquals(ranked, (result as TrafficRefreshCoordinator.Result.Refreshed).ranked)
    }

    private fun <T> await(block: suspend () -> T): T {
        val completed = CountDownLatch(1)
        val result = AtomicReference<Result<T>>()
        block.startCoroutine(object : Continuation<T> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(value: Result<T>) {
                result.set(value)
                completed.countDown()
            }
        })
        assertTrue(completed.await(2, java.util.concurrent.TimeUnit.SECONDS))
        return result.get().getOrThrow()
    }
}
