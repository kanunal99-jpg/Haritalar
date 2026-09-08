package com.haritalar.core.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

class Navigation3dCameraPolicyTest {
    @Test
    fun navigation_camera_uses_forward_bias() {
        val state = Navigation3dCameraPolicy.forNavigation()
        assertEquals(45.0, state.pitchDegrees)
        assertEquals(0.72, state.centerBias)
        assertEquals(16.0, state.minimumZoom)
    }

    @Test
    fun browse_camera_stays_neutral() {
        val state = Navigation3dCameraPolicy.forBrowse()
        assertEquals(0.0, state.pitchDegrees)
        assertEquals(0.5, state.centerBias)
    }
}
