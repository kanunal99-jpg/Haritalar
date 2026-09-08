package com.haritalar.core.navigation

/** Deterministic MapLibre tracking state for browse vs active navigation. */
object NavigationTrackingPolicy {
    enum class Mode {
        BROWSE,
        NAVIGATION,
    }

    data class State(
        val cameraModeName: String,
        val renderModeName: String,
    )

    fun stateFor(mode: Mode): State = when (mode) {
        Mode.BROWSE -> State(
            cameraModeName = "NONE_GPS",
            renderModeName = "NORMAL",
        )
        Mode.NAVIGATION -> State(
            cameraModeName = "TRACKING_GPS",
            renderModeName = "GPS",
        )
    }
}
