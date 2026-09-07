package com.haritalar.core.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RouteAlternativeRankerTest {
    private fun route(id: String, distance: Double, duration: Long) =
        NavigationRoute(id, emptyList(), distance, duration)

    @Test
    fun `sort is deterministic by duration then distance then id`() {
        val options = listOf(
            RouteAlternative("b", route("b", 10_000.0, 600), RoutePreference.FASTEST, label = "B"),
            RouteAlternative("c", route("c", 8_000.0, 600), RoutePreference.SHORTEST, label = "C"),
            RouteAlternative("a", route("a", 12_000.0, 600), RoutePreference.TOLL_FREE, label = "A"),
        )

        assertEquals(listOf("c", "b", "a"), RouteAlternativeRanker.sort(options).map { it.id })
    }

    @Test
    fun `unknown toll amount stays unknown`() {
        val toll = RouteToll(hasToll = true)
        assertTrue(toll.hasToll)
        assertNull(toll.amountTry)
        assertEquals("Ücretli geçiş • tutar doğrulanamadı", RouteAlternativeRanker.tollLabel(toll))
    }

    @Test
    fun `known toll amount is rendered in Turkish try format`() {
        assertEquals(
            "Ücretli geçiş • 125,50 TL",
            RouteAlternativeRanker.tollLabel(RouteToll(true, 125.5)),
        )
    }
}
