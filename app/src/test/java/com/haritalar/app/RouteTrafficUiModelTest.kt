package com.haritalar.app

import kotlin.test.Test
import kotlin.test.assertEquals

class RouteTrafficUiModelTest {
    @Test
    fun `without traffic shows only base duration`() {
        val model = RouteTrafficUiModel(baseDurationSeconds = 2_400.0)

        assertEquals(0.0, model.delaySeconds)
        assertEquals("40 dk", model.durationLabel())
    }

    @Test
    fun `verified traffic shows positive delay`() {
        val model = RouteTrafficUiModel(
            baseDurationSeconds = 2_400.0,
            adjustedDurationSeconds = 2_820.0,
            trafficApplied = true,
        )

        assertEquals(420.0, model.delaySeconds)
        assertEquals("47 dk • trafik +7 dk", model.durationLabel())
    }

    @Test
    fun `traffic model never reports negative delay`() {
        val model = RouteTrafficUiModel(
            baseDurationSeconds = 2_400.0,
            adjustedDurationSeconds = 2_000.0,
            trafficApplied = true,
        )

        assertEquals(0.0, model.delaySeconds)
        assertEquals("33 dk • trafik +0 dk", model.durationLabel())
    }
}
