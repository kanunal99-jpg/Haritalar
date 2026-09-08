package com.haritalar.core.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RoutePlanningModelsTest {
    private fun route(id: String, distanceMeters: Double, durationSeconds: Long) =
        NavigationRoute(
            id = id,
            points = listOf(GeoCoordinate(41.0, 29.0), GeoCoordinate(41.01, 29.01)),
            distanceMeters = distanceMeters,
            durationSeconds = durationSeconds,
        )

    @Test
    fun `route alternatives are deterministic by duration distance then id`() {
        val options = listOf(
            RouteAlternative("b", route("b", 1000.0, 600), RoutePreference.FASTEST, label = "B"),
            RouteAlternative("c", route("c", 900.0, 600), RoutePreference.SHORTEST, label = "C"),
            RouteAlternative("a", route("a", 900.0, 600), RoutePreference.TOLL_FREE, label = "A"),
            RouteAlternative("d", route("d", 1200.0, 540), RoutePreference.TOLL_FAST, label = "D"),
        )

        val sorted = RouteAlternativeRanker.sort(options)

        assertEquals(listOf("d", "a", "c", "b"), sorted.map { it.id })
    }

    @Test
    fun `unknown toll amount is never displayed as zero`() {
        val unknown = RouteToll(hasToll = true)

        assertEquals("Ücretli geçiş • tutar doğrulanamadı", RouteAlternativeRanker.tollLabel(unknown))
        assertTrue(!unknown.amountKnown)
    }

    @Test
    fun `non-finite toll amount is treated as unknown`() {
        val nan = RouteToll(hasToll = true, amountTry = Double.NaN)
        val infinite = RouteToll(hasToll = true, amountTry = Double.POSITIVE_INFINITY)

        assertFalse(nan.amountKnown)
        assertFalse(infinite.amountKnown)
        assertEquals("Ücretli geçiş • tutar doğrulanamadı", RouteAlternativeRanker.tollLabel(nan))
        assertEquals("Ücretli geçiş • tutar doğrulanamadı", RouteAlternativeRanker.tollLabel(infinite))
    }

    @Test
    fun `known toll amount is shown explicitly`() {
        val known = RouteToll(hasToll = true, amountTry = 995.0)

        assertEquals("Ücretli geçiş • 995,00 TL", RouteAlternativeRanker.tollLabel(known))
    }

    @Test
    fun `free route is not mislabeled as paid`() {
        val free = RouteToll(hasToll = false)

        assertEquals("Ücretsiz geçiş tespit edilmedi", RouteAlternativeRanker.tollLabel(free))
    }

    @Test
    fun `ferry cost remains unknown when provider gives no verified amount`() {
        val ferry = RouteFerry(used = true)
        val costs = RouteCostSummary(toll = RouteToll(hasToll = false), ferry = ferry)

        assertNull(costs.knownTotalTry)
        assertTrue(costs.hasUnknownCost)
        assertEquals("Feribot • ücret doğrulanamadı", RouteAlternativeRanker.ferryLabel(ferry))
    }

    @Test
    fun `combined known toll and ferry costs are summed`() {
        val costs = RouteCostSummary(
            toll = RouteToll(hasToll = true, amountTry = 60.0),
            ferry = RouteFerry(used = true, amountTry = 25.0),
        )

        assertEquals(85.0, costs.knownTotalTry!!, 0.0)
        assertFalse(costs.hasUnknownCost)
        assertEquals(
            "Ücretli geçiş • 60,00 TL • Feribot • 25,00 TL",
            RouteAlternativeRanker.costLabel(costs),
        )
    }
}
