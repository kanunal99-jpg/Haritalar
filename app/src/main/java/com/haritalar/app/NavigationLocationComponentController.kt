package com.haritalar.app

import com.haritalar.core.navigation.NavigationTrackingPolicy
import org.maplibre.android.location.LocationComponent
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode

/**
 * Applies the shared navigation tracking policy to MapLibre's LocationComponent.
 *
 * Keeping the SDK-specific mapping here lets the core module remain Android/MapLibre-free
 * while MainActivity can switch between browse and active-navigation tracking deterministically.
 */
object NavigationLocationComponentController {
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
    }
}
