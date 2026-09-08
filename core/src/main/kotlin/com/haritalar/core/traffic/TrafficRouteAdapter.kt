package com.haritalar.core.traffic

import com.haritalar.core.navigation.GeoCoordinate

/**
 * Converts a provider snapshot into traffic observations that are safe to use
 * for a calculated route. Expired snapshots, provider mismatches and unusable
 * snapshots are ignored; route matching remains geometry- and direction-aware.
 */
object TrafficRouteAdapter {
    fun matchSnapshotToRoute(
        route: List<GeoCoordinate>,
        snapshot: TrafficSnapshot,
        expectedProviderId: String? = null,
        nowEpochMs: Long,
    ): List<TrafficRouteSegment> {
        if (!snapshot.isUsable(nowEpochMs)) return emptyList()
        if (expectedProviderId != null && snapshot.providerId != expectedProviderId) return emptyList()
        if (snapshot.confidence == TrafficConfidence.LOW) return emptyList()

        return TrafficRouteMatcher.match(
            route = route,
            segments = snapshot.segments,
        )
    }
}
