package com.haritalar.core.navigation

/** Deterministic MapLibre tracking state for browse vs active navigation. */
object NavigationTrackingPolicy {
    enum class Mode {
        BROWSE,
        NAVIGATION,
    }

    enum class CameraMode {
        NONE_GPS,
        TRACKING_GPS,
    }

    enum class RenderMode {
        NORMAL,
        GPS,
    }

    data class State(
        val cameraMode: CameraMode,
        val renderMode: RenderMode,
    )

    fun stateFor(mode: Mode): State = when (mode) {
        Mode.BROWSE -> State(
            cameraMode = CameraMode.NONE_GPS,
            renderMode = RenderMode.NORMAL,
        )
        Mode.NAVIGATION -> State(
            cameraMode = CameraMode.TRACKING_GPS,
            renderMode = RenderMode.GPS,
        )
    }
}
