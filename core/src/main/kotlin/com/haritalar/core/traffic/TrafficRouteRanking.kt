package com.haritalar.core.traffic

import com.haritalar.core.navigation.GeoCoordinate

/**
 * Applies one verified traffic snapshot to multiple already calculated routes.
 *
 * Route geometry stays authoritative: every traffic observation is matched to the
 * route before it can affect ETA. Routes without verified traffic retain their
 * original routing-provider ETA.
 */
object TrafficRouteRanking {
    data class RouteCandidate(
        val routeId: String,
        val coordinates: List<GeoCoordinate>,
        val baseDurationSeconds: Long,
    )

    data class RankedCandidate(
        val routeId: String,
        val baseDurationSeconds: Long,
        val adjustedDurationSeconds: Long,
        val trafficApplied: Boolean,
    )

    fun rank(
        routes: List<RouteCandidate>,
        snapshot: TrafficSnapshot?,
        expectedProviderId: String? = null,
        nowEpochMs: Long,
    ): List<RankedCandidate> {
        val usableSnapshot = snapshot?.takeIf { it.isUsable(nowEpochMs) }
            ?.takeIf { expectedProviderId == null || it.providerId == expectedProviderId }
            ?.takeIf { it.confidence != TrafficConfidence.LOW }

        return RouteTrafficIntelligence.rank(
            routes.map { route ->
                val trafficSegments = if (usableSnapshot == null) {
                    emptyList()
                } else {
                    TrafficRouteAdapter.matchSnapshotToRoute(
                        route = route.coordinates,
                        snapshot = usableSnapshot,
                        expectedProviderId = expectedProviderId,
                        nowEpochMs = nowEpochMs,
                    )
                }
                RouteTrafficIntelligence.RouteInput(
                    routeId = route.routeId,
                    baseDurationSeconds = route.baseDurationSeconds,
                    trafficSegments = trafficSegments,
                )
            },
        ).map { ranked ->
            RankedCandidate(
                routeId = ranked.routeId,
                baseDurationSeconds = ranked.baseDurationSeconds,
                adjustedDurationSeconds = ranked.adjustedDurationSeconds,
                trafficApplied = ranked.trafficApplied,
            )
        }
    }
}
