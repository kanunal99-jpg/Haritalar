package com.haritalar.app

import com.haritalar.core.traffic.TrafficRouteRanking

/**
 * Converts the core traffic ranking result into presentation data.
 *
 * Keeping this boundary in the app layer prevents UI code from interpreting
 * raw traffic values and guarantees that the verified ranking result remains
 * the source of truth for ETA labels.
 */
object RouteTrafficPresentation {
    fun from(ranked: TrafficRouteRanking.RankedCandidate): RouteTrafficUiModel =
        RouteTrafficUiModel.from(
            baseDurationSeconds = ranked.baseDurationSeconds.toDouble(),
            adjustedDurationSeconds = ranked.adjustedDurationSeconds.toDouble(),
            trafficApplied = ranked.trafficApplied,
        )

    /** Returns models in the exact order produced by the verified ranking. */
    fun fromRanked(
        ranked: List<TrafficRouteRanking.RankedCandidate>,
    ): List<RouteTrafficUiModel> = ranked.map(::from)
}
