package com.haritalar.core.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

class RouteComparisonPresentationTest {
    @Test
    fun routeStatus_neverTurnsUnknownTollIntoZero() {
        assertEquals(
            "Ücretli geçiş • tutar doğrulanamadı",
            RouteComparisonPresentation.routeStatus(
                RouteToll(hasToll = true, amountTry = null),
            ),
        )
    }

    @Test
    fun routeStatus_keepsVerifiedTollAmount() {
        assertEquals(
            "Ücretli geçiş • 42,50 TL",
            RouteComparisonPresentation.routeStatus(
                RouteToll(hasToll = true, amountTry = 42.5),
            ),
        )
    }

    @Test
    fun summary_exposesUnknownAndPaidCounts() {
        val summary = RouteComparisonSummary(
            routeCount = 4,
            fastestRouteId = "fast",
            shortestRouteId = "short",
            lowestKnownTollRouteId = "paid",
            lowestKnownTollTry = 42.5,
            paidRouteCount = 2,
            unknownTollCount = 1,
            noTollDetectedCount = 2,
        )

        assertEquals(
            "4 rota • 2 ücretli • 2 ücretli geçiş tespit edilmedi • 1 ücret tutarı doğrulanamadı",
            RouteComparisonPresentation.summary(summary),
        )
    }
}
