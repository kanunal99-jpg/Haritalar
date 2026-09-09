package com.haritalar.core.traffic

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

/**
 * Coordinates periodic traffic ranking refreshes for navigation or route-card callers.
 *
 * The coordinator owns only refresh timing/concurrency. The supplied ranker remains the
 * canonical TrafficRouteRankingService path, so provider fallback/cache semantics are
 * not duplicated here.
 */
class TrafficRefreshCoordinator(
    private val minimumIntervalMs: Long = DEFAULT_MINIMUM_INTERVAL_MS,
) {
    init {
        require(minimumIntervalMs >= 0L) { "minimumIntervalMs must be >= 0" }
    }

    private var lastCompletedAtMs: Long? = null
    private var lastResult: List<TrafficRouteRanking.RankedCandidate> = emptyList()
    private var inFlight = false
    private var generation = 0L

    sealed class Result {
        data class Refreshed(val ranked: List<TrafficRouteRanking.RankedCandidate>) : Result()
        data class Skipped(val ranked: List<TrafficRouteRanking.RankedCandidate>) : Result()
        data class Stale(val ranked: List<TrafficRouteRanking.RankedCandidate>) : Result()
    }

    /**
     * Runs at most one ranking operation at a time and suppresses refreshes inside
     * the minimum interval. The caller should ignore [Result.Stale] because a newer
     * route generation invalidated the request while the provider was running.
     */
    suspend fun refresh(
        routes: List<TrafficRouteRanking.RouteCandidate>,
        nowEpochMs: Long,
        ranker: suspend (List<TrafficRouteRanking.RouteCandidate>, Long) -> List<TrafficRouteRanking.RankedCandidate>,
    ): Result {
        require(nowEpochMs >= 0L) { "nowEpochMs must be >= 0" }
        if (routes.isEmpty()) return Result.Skipped(emptyList())

        val requestGeneration: Long
        synchronized(this) {
            if (inFlight) return Result.Skipped(lastResult)
            val last = lastCompletedAtMs
            if (last != null && nowEpochMs - last < minimumIntervalMs) {
                return Result.Skipped(lastResult)
            }
            inFlight = true
            requestGeneration = generation
        }

        return try {
            val ranked = ranker(routes, nowEpochMs)
            synchronized(this) {
                if (requestGeneration != generation) {
                    Result.Stale(ranked)
                } else {
                    lastResult = ranked
                    lastCompletedAtMs = nowEpochMs
                    Result.Refreshed(ranked)
                }
            }
        } finally {
            synchronized(this) { inFlight = false }
        }
    }

    /**
     * Blocking bridge for Android callers that already own a background executor.
     * Never call this from the main thread. The suspend [refresh] function remains
     * the single source of truth for timing, concurrency and stale-generation logic.
     */
    fun refreshBlocking(
        routes: List<TrafficRouteRanking.RouteCandidate>,
        nowEpochMs: Long = System.currentTimeMillis(),
        timeoutMs: Long = DEFAULT_BLOCKING_TIMEOUT_MS,
        ranker: suspend (List<TrafficRouteRanking.RouteCandidate>, Long) -> List<TrafficRouteRanking.RankedCandidate>,
    ): Result {
        require(timeoutMs > 0L) { "timeoutMs must be positive" }

        val completed = CountDownLatch(1)
        val result = AtomicReference<kotlin.Result<TrafficRefreshCoordinator.Result>?>()
        val refreshBlock: suspend () -> TrafficRefreshCoordinator.Result = {
            refresh(routes, nowEpochMs, ranker)
        }

        refreshBlock.startCoroutine(object : Continuation<TrafficRefreshCoordinator.Result> {
            override val context = EmptyCoroutineContext

            override fun resumeWith(value: kotlin.Result<TrafficRefreshCoordinator.Result>) {
                result.set(value)
                completed.countDown()
            }
        })

        if (!completed.await(timeoutMs, TimeUnit.MILLISECONDS)) {
            throw IllegalStateException("Traffic refresh timed out after ${timeoutMs}ms")
        }
        return result.get()?.getOrThrow()
            ?: throw IllegalStateException("Traffic refresh completed without a result")
    }

    /** Invalidates a running result and clears the refresh cooldown for a new route. */
    @Synchronized
    fun reset() {
        generation += 1L
        lastCompletedAtMs = null
        lastResult = emptyList()
    }

    companion object {
        const val DEFAULT_MINIMUM_INTERVAL_MS = 60_000L
        private const val DEFAULT_BLOCKING_TIMEOUT_MS = 120_000L
    }
}
