package com.haritalar.core.traffic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RouteTrafficPresentationTest {
    @Test
    fun rendersBaseDurationWhenTrafficWasNotApplied() {
        val model = RouteTrafficPresentation.present(
            RouteTrafficPresentation.Input(
                routeId = "route-1",
                baseDurationSeconds = 2_400L,
                adjustedDurationSeconds = 2_400L,
                trafficApplied = false,
            ),
        )

        assertEquals("40 dk", model.durationLabel)
        assertNull(model.trafficDelaySeconds)
    }

    @Test
    fun rendersVerifiedTrafficDelayAlongsideAdjustedDuration() {
        val model = RouteTrafficPresentation.present(
            RouteTrafficPresentation.Input(
                routeId = "route-1",
                baseDurationSeconds = 2_400L,
                adjustedDurationSeconds = 2_820L,
                trafficApplied = true,
            ),
        )

        assertEquals("47 dk • trafik +7 dk", model.durationLabel)
        assertEquals(420L, model.trafficDelaySeconds)
    }

    @Test
    fun neverShowsNegativeTrafficDelay() {
        val model = RouteTrafficPresentation.present(
            RouteTrafficPresentation.Input(
                routeId = "route-1",
                baseDurationSeconds = 2_820L,
                adjustedDurationSeconds = 2_400L,
                trafficApplied = true,
            ),
        )

        assertEquals("40 dk", model.durationLabel)
        assertEquals(0L, model.trafficDelaySeconds)
    }

    @Test
    fun formatsLongDurationsWithoutLosingMinutes() {
        val model = RouteTrafficPresentation.present(
            RouteTrafficPresentation.Input(
                routeId = "route-1",
                baseDurationSeconds = 4_200L,
                adjustedDurationSeconds = 4_500L,
                trafficApplied = true,
            ),
        )

        assertEquals("1 sa 15 dk • trafik +5 dk", model.durationLabel)
        assertEquals(300L, model.trafficDelaySeconds)
    }
}
