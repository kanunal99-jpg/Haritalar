package com.haritalar.core.navigation

/** Deterministic camera posture for browse mode versus active 3D navigation. */
object Navigation3dCameraPolicy {
    data class State(
        val pitchDegrees: Double,
        val centerBias: Double,
        val minimumZoom: Double = 0.0,
    ) {
        init {
            require(pitchDegrees in 0.0..75.0)
            require(centerBias in 0.0..1.0)
            require(minimumZoom >= 0.0)
        }
    }

    fun forBrowse(): State = State(
        pitchDegrees = 0.0,
        centerBias = 0.5,
    )

    fun forNavigation(): State = State(
        pitchDegrees = 45.0,
        centerBias = 0.72,
        minimumZoom = 16.0,
    )
}
