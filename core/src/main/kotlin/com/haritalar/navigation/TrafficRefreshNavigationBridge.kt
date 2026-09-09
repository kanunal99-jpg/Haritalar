package com.haritalar.navigation

import com.haritalar.core.traffic.TrafficRefreshCoordinator
import com.haritalar.core.traffic.TrafficRouteRanking
import com.haritalar.core.traffic.TrafficRouteRankingService
import java.util.concurrent.Executor

/**
 * Keeps navigation/GPS callbacks lightweight while delegating traffic ranking
 * to the caller-provided background executor.
 *
 * The bridge owns no provider/fallback logic. TrafficRouteRankingService remains
 * the canonical provider/cache/ranking path and TrafficRefreshCoordinator owns
 * cooldown, single-flight and stale-generation protection.
 */
class TrafficRefreshNavigationBridge(
    private val coordinator: TrafficRefreshCoordinator,
    private val rankingService: TrafficRouteRankingService,
    private val backgroundExecutor: Executor,
    private val onRefreshed: (List<TrafficRouteRanking.RankedCandidate>) -> Unit,
    private val onStale: (List<TrafficRouteRanking.RankedCandidate>) -> Unit = {},
) {
    fun onLocationUpdate(
        routes: List<TrafficRouteRanking.RouteCandidate>,
        nowEpochMs: Long,
    ) {
        if (routes.isEmpty()) return
        backgroundExecutor.execute {
            val result = runCatching {
                coordinator.refresh(routes, nowEpochMs) { candidates, timestamp ->
                    rankingService.rank(candidates, timestamp)
                }
            }.getOrNull() ?: return@execute

            when (result) {
                is TrafficRefreshCoordinator.Result.Refreshed -> onRefreshed(result.ranked)
                is TrafficRefreshCoordinator.Result.Stale -> onStale(result.ranked)
                is TrafficRefreshCoordinator.Result.Skipped -> Unit
            }
        }
    }

    /** Starts a new route generation and invalidates any running old result. */
    fun onRouteChanged() {
        coordinator.reset()
    }
}
