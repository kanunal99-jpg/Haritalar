package com.haritalar.core.traffic

import com.haritalar.core.navigation.GeoCoordinate

/**
 * Applies an optional, already validated traffic snapshot to route candidates.
 *
 * A null or unusable snapshot is deliberately a no-op: callers retain the
 * routing engine's original ETA and ordering instead of receiving fabricated
 * traffic values.
 */
object TrafficRouteOrchestrator {
    data class RouteCandidate(
        val id: String,
        val geometry: List<GeoCoordinate>,
        val baseDurationSeconds: Long,
    )

    data class RankedRoute(
        val candidate: RouteCandidate,
        val trafficDurationSeconds: Long,
        val trafficApplied: Boolean,
    )

    fun rank(
        routes: List<RouteCandidate>,
        snapshot: TrafficSnapshot?,
        expectedProviderId: String? = null,
        nowEpochMs: Long,
    ): List<RankedRoute> {
        if (snapshot == null) {
            return routes.map { it.toUnchangedResult() }
        }

        val ranked = routes.map { route ->
            val adapted = TrafficRouteAdapter.matchSnapshotToRoute(
                route = route.geometry,
                snapshot = snapshot,
                expectedProviderId = expectedProviderId,
                nowEpochMs = nowEpochMs,
            )

            if (adapted.isEmpty()) {
                route.toUnchangedResult()
            } else {
                RankedRoute(
                    candidate = route,
                    trafficDurationSeconds = TrafficRouteCostModel.adjustedDurationSeconds(
                        baseDurationSeconds = route.baseDurationSeconds,
                        segments = adapted,
                    ),
                    trafficApplied = true,
                )
            }
        }

        return ranked.sortedWith(
            compareBy<RankedRoute> { it.trafficDurationSeconds }
                .thenBy { it.candidate.baseDurationSeconds }
                .thenBy { it.candidate.id },
        )
    }

    private fun RouteCandidate.toUnchangedResult(): RankedRoute = RankedRoute(
        candidate = this,
        trafficDurationSeconds = baseDurationSeconds,
        trafficApplied = false,
    )
}
