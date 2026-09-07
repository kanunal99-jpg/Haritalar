package com.haritalar.core.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RouteComparisonSummaryTest {
    private fun route(id: String, distanceMeters: Double, durationSeconds: Long) =
        NavigationRoute(
            id = id,
            points = listOf(GeoCoordinate(41.0, 29.0), GeoCoordinate(41.01, 29.01)),
            distanceMeters = distanceMeters,
            durationSeconds = durationSeconds,
        )

    @Test
    fun `summary identifies fastest shortest and cheapest known toll route`() {
        val options = listOf(
            RouteAlternative(
                "fast",
                route("fast", 18000.0, 900),
                RoutePreference.FASTEST,
                RouteToll(hasToll = true, amountTry = 995.0),
                "En hızlı",
            ),
            RouteAlternative(
                "short",
                route("short", 12000.0, 1100),
                RoutePreference.SHORTEST,
                RouteToll(hasToll = false),
                "En kısa",
            ),
            RouteAlternative(
                "cheap",
                route("cheap", 20000.0, 1000),
                RoutePreference.TOLL_FAST,
                RouteToll(hasToll = true, amountTry = 120.5),
                "Ücretli hızlı",
            ),
        )

        val summary = RouteComparisonSummary.from(options)

        assertEquals(3, summary.routeCount)
        assertEquals("fast", summary.fastestRouteId)
        assertEquals("short", summary.shortestRouteId)
        assertEquals("cheap", summary.lowestKnownTollRouteId)
        assertEquals(120.5, summary.lowestKnownTollTry)
        assertEquals(2, summary.paidRouteCount)
        assertEquals(0, summary.unknownTollCount)
        assertEquals(1, summary.noTollDetectedCount)
        assertTrue(summary.hasComparableTollCost)
        assertEquals("En düşük doğrulanmış ücret: 120,50 TL", summary.lowestKnownTollLabel())
    }

    @Test
    fun `unknown toll is excluded from cost comparison and never becomes zero`() {
        val options = listOf(
            RouteAlternative(
                "unknown",
                route("unknown", 10000.0, 800),
                RoutePreference.FASTEST,
                RouteToll(hasToll = true),
                "En hızlı",
            ),
            RouteAlternative(
                "known",
                route("known", 11000.0, 900),
                RoutePreference.TOLL_FAST,
                RouteToll(hasToll = true, amountTry = 250.0),
                "Ücretli hızlı",
            ),
        )

        val summary = RouteComparisonSummary.from(options)

        assertEquals("known", summary.lowestKnownTollRouteId)
        assertEquals(250.0, summary.lowestKnownTollTry)
        assertEquals(1, summary.unknownTollCount)
        assertTrue(summary.hasComparableTollCost)
        assertFalse(summary.lowestKnownTollTry == 0.0)
    }

    @Test
    fun `all unknown tolls produce an explicit unavailable cost summary`() {
        val options = listOf(
            RouteAlternative(
                "a",
                route("a", 10000.0, 800),
                RoutePreference.FASTEST,
                RouteToll(hasToll = true, amountTry = Double.NaN),
                "A",
            ),
            RouteAlternative(
                "b",
                route("b", 12000.0, 900),
                RoutePreference.TOLL_FAST,
                RouteToll(hasToll = true, amountTry = Double.POSITIVE_INFINITY),
                "B",
            ),
        )

        val summary = RouteComparisonSummary.from(options)

        assertEquals(2, summary.unknownTollCount)
        assertEquals(null, summary.lowestKnownTollRouteId)
        assertEquals(null, summary.lowestKnownTollTry)
        assertFalse(summary.hasComparableTollCost)
        assertEquals("Doğrulanmış ücret bilgisi yok", summary.lowestKnownTollLabel())
    }
}
