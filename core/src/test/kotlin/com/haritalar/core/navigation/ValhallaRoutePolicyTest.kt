package com.haritalar.core.navigation

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValhallaRoutePolicyTest {
    @Test
    fun noFerryModeRequestsZeroFerryWillingness() {
        val options = ValhallaRoutePolicy.autoCostingOptions("no_ferry")
        assertEquals(0.0, options.getDouble("use_ferry"), 0.0)
    }

    @Test
    fun ferryFastModeRequestsMaximumFerryWillingness() {
        val options = ValhallaRoutePolicy.autoCostingOptions("ferry_fast")
        assertEquals(1.0, options.getDouble("use_ferry"), 0.0)
    }

    @Test
    fun tollAndFerryPoliciesCanBeComposedByCaller() {
        val options = ValhallaRoutePolicy.autoCostingOptions("no_toll")
        assertEquals(0.0, options.getDouble("use_tolls"), 0.0)
        assertEquals(3600.0, options.getDouble("toll_booth_penalty"), 0.0)
    }

    @Test
    fun combinedNoTollNoFerryPolicySetsBothConstraints() {
        val options = ValhallaRoutePolicy.autoCostingOptions("no_toll_no_ferry")
        assertEquals(0.0, options.getDouble("use_tolls"), 0.0)
        assertEquals(3600.0, options.getDouble("toll_booth_penalty"), 0.0)
        assertEquals(0.0, options.getDouble("use_ferry"), 0.0)
    }

    @Test
    fun combinedTollFastFerryPolicySetsBothPreferences() {
        val options = ValhallaRoutePolicy.autoCostingOptions("toll_fast_ferry")
        assertEquals(1.0, options.getDouble("use_tolls"), 0.0)
        assertEquals(1.0, options.getDouble("use_ferry"), 0.0)
    }

    @Test
    fun combinedNoTollFerryPolicyKeepsFerryAvailable() {
        val options = ValhallaRoutePolicy.autoCostingOptions("no_toll_ferry")
        assertEquals(0.0, options.getDouble("use_tolls"), 0.0)
        assertEquals(3600.0, options.getDouble("toll_booth_penalty"), 0.0)
        assertEquals(1.0, options.getDouble("use_ferry"), 0.0)
    }

    @Test
    fun explicitFerryManeuverIsDetected() {
        assertTrue(ValhallaRoutePolicy.isFerryManeuver(JSONObject().put("ferry", true)))
        assertTrue(ValhallaRoutePolicy.isFerryManeuver(JSONObject().put("travel_type", "ferry")))
        assertTrue(ValhallaRoutePolicy.isFerryManeuver(JSONObject().put("travel_type", "rail-ferry")))
    }

    @Test
    fun ordinaryDriveManeuverIsNotMarkedAsFerry() {
        assertFalse(ValhallaRoutePolicy.isFerryManeuver(JSONObject().put("travel_mode", "drive").put("travel_type", "car")))
    }
}
