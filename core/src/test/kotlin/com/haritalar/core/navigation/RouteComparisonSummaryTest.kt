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
                id = "fast",
                route = route("fast", 18000.0, 900),
                preference = RoutePreference.FASTEST,
                toll = RouteToll(hasToll = true, amountTry = 995.0),
                label = "En hızlı",
            ),
            RouteAlternative(
                id = "short",
                route = route("short", 12000.0, 1100),
                preference = RoutePreference.SHORTEST,
                toll = RouteToll(hasToll = false),
                label = "En kısa",
            ),
            RouteAlternative(
                id = "cheap",
                route = route("cheap", 20000.0, 1000),
                preference = RoutePreference.TOLL_FAST,
                toll = RouteToll(hasToll = true, amountTry = 120.5),
                label = "Ücretli hızlı",
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
                id = "unknown",
                route = route("unknown", 10000.0, 800),
                preference = RoutePreference.FASTEST,
                toll = RouteToll(hasToll = true),
                label = "En hızlı",
            ),
            RouteAlternative(
                id = "known",
                route = route("known", 11000.0, 900),
                preference = RoutePreference.TOLL_FAST,
                toll = RouteToll(hasToll = true, amountTry = 250.0),
                label = "Ücretli hızlı",
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
                id = "a",
                route = route("a", 10000.0, 800),
                preference = RoutePreference.FASTEST,
                toll = RouteToll(hasToll = true, amountTry = Double.NaN),
                label = "A",
            ),
            RouteAlternative(
                id = "b",
                route = route("b", 12000.0, 900),
                preference = RoutePreference.TOLL_FAST,
                toll = RouteToll(hasToll = true, amountTry = Double.POSITIVE_INFINITY),
                label = "B",
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
