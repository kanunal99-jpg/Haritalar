package com.haritalar.navigation

import com.haritalar.traffic.TrafficRefreshCoordinator
import java.util.concurrent.Executor

/**
 * Keeps navigation/GPS callbacks lightweight while delegating traffic ranking
 * to the caller-provided background executor.
 *
 * The bridge deliberately owns no provider/fallback logic; TrafficRefreshCoordinator
 * remains the canonical refresh/concurrency/generation guard.
 */
class TrafficRefreshNavigationBridge(
    private val coordinator: TrafficRefreshCoordinator,
    private val backgroundExecutor: Executor,
    private val rankSnapshot: () -> Unit,
    private val onStaleResult: () -> Unit = {},
) {
    fun onLocationUpdate(routeGeneration: Long) {
        backgroundExecutor.execute {
            coordinator.requestRefresh(routeGeneration) { result ->
                when (result) {
                    is TrafficRefreshCoordinator.RefreshResult.Success -> rankSnapshot()
                    is TrafficRefreshCoordinator.RefreshResult.Stale -> onStaleResult()
                    TrafficRefreshCoordinator.RefreshResult.Skipped -> Unit
                }
            }
        }
    }

    fun onRouteChanged(routeGeneration: Long) {
        coordinator.reset(routeGeneration)
    }
}
