package com.haritalar.core.traffic

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

    /** Invalidates a running result and clears the refresh cooldown for a new route. */
    @Synchronized
    fun reset() {
        generation += 1L
        lastCompletedAtMs = null
        lastResult = emptyList()
    }

    companion object {
        const val DEFAULT_MINIMUM_INTERVAL_MS = 60_000L
    }
}
