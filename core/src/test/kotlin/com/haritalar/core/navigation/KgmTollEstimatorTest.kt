package com.haritalar.core.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class KgmTollEstimatorTest {
    @Test
    fun `osmangazi class one uses official 2026 tariff`() {
        val toll = KgmTollEstimator.estimate(listOf(GeoCoordinate(40.7580, 29.5140)), vehicleClass = 1)
        assertEquals(TollBridge.OSMANGAZI, KgmTollEstimator.detectBridge(listOf(GeoCoordinate(40.7580, 29.5140))))
        assertEquals(995.0, toll.amountTry)
    }

    @Test
    fun `yavuz sultan selim class two uses official 2026 tariff`() {
        val toll = KgmTollEstimator.estimate(listOf(GeoCoordinate(41.2045, 29.1130)), vehicleClass = 2)
        assertEquals(125.0, toll.amountTry)
    }

    @Test
    fun `ordinary route stays toll free when no known crossing is matched`() {
        val toll = KgmTollEstimator.estimate(listOf(GeoCoordinate(39.925, 32.836)))
        assertFalse(toll.hasToll)
        assertNull(toll.amountTry)
    }
}
