package com.haritalar.core.navigation

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class RouteTransportSummaryTest {
    @Test
    fun `road and ferry are presented as combined journey`() {
        val summary = RouteTransportSummary.from(
            listOf(RouteTransportType.ROAD, RouteTransportType.FERRY, RouteTransportType.ROAD),
        )

        assertTrue(summary.usesFerry)
        assertFalse(summary.isRoadOnly)
        assertEquals(1, summary.ferryCount)
        assertEquals("Kara + feribot + kara", summary.label())
    }

    @Test
    fun `road only remains road only`() {
        val summary = RouteTransportSummary.from(
            listOf(RouteTransportType.ROAD),
        )

        assertFalse(summary.usesFerry)
        assertTrue(summary.isRoadOnly)
        assertEquals("Karayolu", summary.label())
    }

    @Test
    fun `missing provider transport data is not called road`() {
        val summary = RouteTransportSummary.from(emptyList())

        assertFalse(summary.usesFerry)
        assertFalse(summary.isRoadOnly)
        assertEquals("Ulaşım türü doğrulanamadı", summary.label())
    }

    @Test
    fun `avoid ferry policy disables ferry while prefer policy requests it`() {
        assertFalse(RouteTransportPolicy.allowsFerry(RouteTransportPreference.AVOID_FERRY))
        assertTrue(RouteTransportPolicy.allowsFerry(RouteTransportPreference.ALLOW_FERRY))
        assertTrue(RouteTransportPolicy.prefersFerry(RouteTransportPreference.PREFER_FERRY))
        assertFalse(RouteTransportPolicy.prefersFerry(RouteTransportPreference.ALLOW_FERRY))
    }
}
