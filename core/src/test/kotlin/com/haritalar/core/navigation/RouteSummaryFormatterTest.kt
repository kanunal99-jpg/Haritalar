package com.haritalar.core.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RouteSummaryFormatterTest {
    @Test
    fun `formats metric distance and duration deterministically`() {
        val option = RouteAlternative(
            id = "fast",
            route = NavigationRoute(
                id = "route-1",
                points = emptyList(),
                distanceMeters = 12_450.0,
                durationSeconds = 5_100L,
            ),
            preference = RoutePreference.FASTEST,
            label = "En hızlı",
        )

        val summary = RouteSummaryFormatter.format(option, recommended = true)

        assertEquals("12.5 km", summary.distanceText)
        assertEquals("1 sa 25 dk", summary.durationText)
        assertEquals("Ücretsiz geçiş tespit edilmedi", summary.tollText)
        assertTrue(summary.isRecommended)
    }

    @Test
    fun `preserves unknown toll amount`() {
        val option = RouteAlternative(
            id = "toll",
            route = NavigationRoute("route-2", emptyList(), 900.0, 1_200L),
            preference = RoutePreference.TOLL_FAST,
            toll = RouteToll(hasToll = true),
            label = "Ücretli hızlı",
        )

        val summary = RouteSummaryFormatter.format(option)

        assertEquals("900 m", summary.distanceText)
        assertEquals("20 dk", summary.durationText)
        assertEquals("Ücretli geçiş • tutar doğrulanamadı", summary.tollText)
    }
}
