package com.haritalar.core.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RouteOptionPolicyTest {
    private fun option(id: String, duration: Long, distance: Double) = RouteAlternative(
        id = id,
        route = NavigationRoute(id, emptyList(), distance, duration),
        preference = RoutePreference.FASTEST,
        label = id,
    )

    @Test
    fun `recommended route is deterministic`() {
        val options = listOf(
            option("long", 900, 5_000.0),
            option("fast", 600, 8_000.0),
            option("tie", 600, 7_000.0),
        )

        assertEquals("tie", RouteOptionPolicy.recommended(options)?.id)
    }

    @Test
    fun `ordered route options use duration then distance then id`() {
        val options = listOf(
            option("z", 600, 7_000.0),
            option("a", 600, 7_000.0),
            option("b", 500, 9_000.0),
        )

        assertEquals(listOf("b", "a", "z"), RouteOptionPolicy.ordered(options).map { it.id })
    }

    @Test
    fun `empty options have no recommendation`() {
        assertNull(RouteOptionPolicy.recommended(emptyList()))
    }
}
