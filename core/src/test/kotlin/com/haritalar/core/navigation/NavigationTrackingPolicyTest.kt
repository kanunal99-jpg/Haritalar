package com.haritalar.core.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

class NavigationTrackingPolicyTest {
    @Test
    fun browseKeepsMapFreeAndUsesGpsBearingWithoutFollowing() {
        assertEquals(
            NavigationTrackingPolicy.State("NONE_GPS", "NORMAL"),
            NavigationTrackingPolicy.stateFor(NavigationTrackingPolicy.Mode.BROWSE),
        )
    }

    @Test
    fun navigationFollowsGpsBearing() {
        assertEquals(
            NavigationTrackingPolicy.State("TRACKING_GPS", "GPS"),
            NavigationTrackingPolicy.stateFor(NavigationTrackingPolicy.Mode.NAVIGATION),
        )
    }
}
