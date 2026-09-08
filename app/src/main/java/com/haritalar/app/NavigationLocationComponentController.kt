package com.haritalar.app

import android.os.Handler
import android.os.Looper
import com.haritalar.core.navigation.NavigationTrackingPolicy
import org.maplibre.android.location.LocationComponent
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode

/**
 * Applies the shared navigation tracking policy to MapLibre's LocationComponent.
 *
 * Browse mode deliberately releases camera tracking after the initial GPS fix so the
 * user can freely pan, zoom, rotate and inspect other cities. Navigation mode keeps
 * GPS camera tracking active.
 */
object NavigationLocationComponentController {
    private val NAVIGATION_VIEWPORT_PADDING = doubleArrayOf(0.0, 0.0, 0.0, 260.0)
    private val BROWSE_VIEWPORT_PADDING = doubleArrayOf(0.0, 0.0, 0.0, 0.0)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var initialBrowseCenterRequested = false

    fun apply(component: LocationComponent, navigationActive: Boolean) {
        apply(component, NavigationTrackingPolicy.modeFor(navigationActive))
    }

    fun apply(component: LocationComponent, mode: NavigationTrackingPolicy.Mode) {
        val state = NavigationTrackingPolicy.stateFor(mode)
        component.cameraMode = when (state.cameraMode) {
            NavigationTrackingPolicy.CameraMode.TRACKING_GPS -> CameraMode.TRACKING_GPS
            NavigationTrackingPolicy.CameraMode.NONE_GPS -> CameraMode.NONE_GPS
        }
        component.setRenderMode(
            when (state.renderMode) {
                NavigationTrackingPolicy.RenderMode.GPS -> RenderMode.GPS
                NavigationTrackingPolicy.RenderMode.NORMAL -> RenderMode.NORMAL
            },
        )
        if (mode == NavigationTrackingPolicy.Mode.NAVIGATION) {
            component.paddingWhileTracking(NAVIGATION_VIEWPORT_PADDING, 450L)
        } else {
            component.paddingWhileTracking(BROWSE_VIEWPORT_PADDING, 350L)
            centerBrowseOnInitialGpsFix(component)
        }
    }

    /**
     * The old hard-coded Istanbul camera made the first screen misleading for users
     * outside Istanbul. Temporarily track GPS once, then release tracking so browse
     * mode remains a normal freely movable map.
     */
    private fun centerBrowseOnInitialGpsFix(component: LocationComponent) {
        if (initialBrowseCenterRequested) return
        initialBrowseCenterRequested = true

        component.cameraMode = CameraMode.TRACKING_GPS
        mainHandler.postDelayed({
            if (component.isLocationComponentActivated && component.isLocationComponentEnabled) {
                component.cameraMode = CameraMode.NONE_GPS
            }
        }, 2500L)
    }
}
