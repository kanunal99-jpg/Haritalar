package com.haritalar.core.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NavigationHudStateTest {
    @Test
    fun formatsRemainingDistance() {
        assertEquals("1.2 km", NavigationHudState(1234.0).remainingLabel)
        assertEquals("85 m", NavigationHudState(85.0).remainingLabel)
    }

    @Test
    fun formatsNextInstructionWithDistance() {
        val state = NavigationHudState(3200.0, "Sağa dön", 450.0)
        assertEquals("450 m • Sağa dön", state.nextInstructionLabel)
    }

    @Test
    fun reportsGpsQualityWithoutInventingAccuracy() {
        assertEquals("GPS bekleniyor", NavigationHudState(1000.0).gpsLabel)
        assertEquals("GPS güçlü", NavigationHudState(1000.0, gpsAccuracyMeters = 12f).gpsLabel)
        assertEquals("GPS orta", NavigationHudState(1000.0, gpsAccuracyMeters = 40f).gpsLabel)
        assertEquals("GPS zayıf", NavigationHudState(1000.0, gpsAccuracyMeters = 90f).gpsLabel)
    }

    @Test
    fun rejectsNonFiniteNavigationValues() {
        assertFailsWith<IllegalArgumentException> { NavigationHudState(Double.NaN) }
        assertFailsWith<IllegalArgumentException> { NavigationHudState(1000.0, speedKmh = Double.POSITIVE_INFINITY) }
        assertFailsWith<IllegalArgumentException> { NavigationHudState(1000.0, gpsAccuracyMeters = -1f) }
    }
}
