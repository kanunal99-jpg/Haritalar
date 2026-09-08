package com.haritalar.core.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

class ValhallaAutoOptionsTest {
    @Test
    fun mapsFerryPreferencesToValhallaWillingness() {
        assertEquals(0.5, ValhallaAutoOptions.from(RouteTransportPreference.ALLOW_FERRY).getDouble("use_ferry"))
        assertEquals(0.0, ValhallaAutoOptions.from(RouteTransportPreference.AVOID_FERRY).getDouble("use_ferry"))
        assertEquals(1.0, ValhallaAutoOptions.from(RouteTransportPreference.PREFER_FERRY).getDouble("use_ferry"))
    }
}
