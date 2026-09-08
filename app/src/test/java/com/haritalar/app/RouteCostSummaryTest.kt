package com.haritalar.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteCostSummaryTest {
    @Test
    fun sums_only_known_route_costs() {
        assertEquals(85.0, RouteCostSummary(tollCost = 60.0, ferryCost = 25.0).knownCost!!, 0.0)
    }

    @Test
    fun keeps_unknown_cost_unknown_instead_of_guessing() {
        val summary = RouteCostSummary(tollCost = null, ferryCost = 25.0)
        assertEquals(25.0, summary.knownCost!!, 0.0)
        assertTrue(summary.hasUnknownCost)
    }

    @Test
    fun empty_cost_summary_has_no_known_total() {
        val summary = RouteCostSummary()
        assertNull(summary.knownCost)
        assertTrue(summary.hasUnknownCost)
    }
}
