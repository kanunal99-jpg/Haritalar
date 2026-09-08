package com.haritalar.app

/** User-facing routing preferences kept provider-agnostic. */
data class RoutePreference(
    val avoidTolls: Boolean = false,
    val avoidFerries: Boolean = false,
    val preferFastest: Boolean = true,
)
