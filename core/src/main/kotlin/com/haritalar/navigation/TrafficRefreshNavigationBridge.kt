package com.haritalar.navigation

import com.haritalar.core.traffic.TrafficRefreshCoordinator
import com.haritalar.core.traffic.TrafficRouteRanking
import com.haritalar.core.traffic.TrafficRouteRankingService
import java.util.concurrent.Executor
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

/**
 * Keeps navigation/GPS callbacks lightweight while running the coordinator's
 * suspend refresh on a caller-provided background executor.
 *
 * Provider/cache/fallback logic stays in TrafficRouteRankingService. Refresh
 * cooldown, single-flight and stale-generation protection stay in the coordinator.
 * The navigation generation token additionally protects work that is queued on
 * the executor but has not entered the coordinator yet.
 */
class TrafficRefreshNavigationBridge(
    private val coordinator: TrafficRefreshCoordinator,
    private val rankingService: TrafficRouteRankingService,
    private val backgroundExecutor: Executor,
    private val currentGeneration: () -> Long = { 0L },
    private val onRefreshed: (List<TrafficRouteRanking.RankedCandidate>) -> Unit,
    private val onStale: (List<TrafficRouteRanking.RankedCandidate>) -> Unit = {},
    private val onFailure: (Throwable) -> Unit = {},
) {
    fun onLocationUpdate(
        routes: List<TrafficRouteRanking.RouteCandidate>,
        nowEpochMs: Long = System.currentTimeMillis(),
    ) {
        if (routes.isEmpty()) return
        val requestGeneration = currentGeneration()
        backgroundExecutor.execute {
            if (currentGeneration() != requestGeneration) return@execute
            val refreshBlock: suspend () -> TrafficRefreshCoordinator.Result = {
                coordinator.refresh(routes, nowEpochMs) { candidates, timestamp ->
                    rankingService.rank(candidates, timestamp)
                }
            }
            refreshBlock.startCoroutine(object : Continuation<TrafficRefreshCoordinator.Result> {
                override val context = EmptyCoroutineContext

                override fun resumeWith(result: Result<TrafficRefreshCoordinator.Result>) {
                    result.fold(
                        onSuccess = { refreshResult ->
                            when (refreshResult) {
                                is TrafficRefreshCoordinator.Result.Refreshed -> {
                                    if (currentGeneration() == requestGeneration) {
                                        onRefreshed(refreshResult.ranked)
                                    } else {
                                        onStale(refreshResult.ranked)
                                    }
                                }
                                is TrafficRefreshCoordinator.Result.Stale -> onStale(refreshResult.ranked)
                                is TrafficRefreshCoordinator.Result.Skipped -> Unit
                            }
                        },
                        onFailure = onFailure,
                    )
                }
            })
        }
    }

    /** Starts a new route generation and invalidates any running old result. */
    fun onRouteChanged() {
        coordinator.reset()
    }
}
