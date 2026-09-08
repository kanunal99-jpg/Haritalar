package com.haritalar.core.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

class NavigationTrackingPolicyTest {
    @Test
    fun inactiveNavigationUsesBrowseMode() {
        assertEquals(
            NavigationTrackingPolicy.Mode.BROWSE,
            NavigationTrackingPolicy.modeFor(navigationActive = false),
        )
    }

    @Test
    fun activeNavigationUsesNavigationMode() {
        assertEquals(
            NavigationTrackingPolicy.Mode.NAVIGATION,
            NavigationTrackingPolicy.modeFor(navigationActive = true),
        )
    }

    @Test
    fun browseKeepsMapFreeAndUsesGpsBearingWithoutFollowing() {
        assertEquals(
            NavigationTrackingPolicy.State(
                cameraMode = NavigationTrackingPolicy.CameraMode.NONE_GPS,
                renderMode = NavigationTrackingPolicy.RenderMode.NORMAL,
            ),
            NavigationTrackingPolicy.stateFor(NavigationTrackingPolicy.Mode.BROWSE),
        )
    }

    @Test
    fun navigationFollowsGpsBearing() {
        assertEquals(
            NavigationTrackingPolicy.State(
                cameraMode = NavigationTrackingPolicy.CameraMode.TRACKING_GPS,
                renderMode = NavigationTrackingPolicy.RenderMode.GPS,
            ),
            NavigationTrackingPolicy.stateFor(NavigationTrackingPolicy.Mode.NAVIGATION),
        )
    }
}
