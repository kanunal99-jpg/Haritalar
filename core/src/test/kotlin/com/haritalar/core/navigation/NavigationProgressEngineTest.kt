package com.haritalar.core.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class NavigationProgressEngineTest {
    @Test
    fun doesNotAnnounceTooEarly() {
        val engine = NavigationProgressEngine()
        val maneuver = NavigationProgressEngine.Maneuver(1, "Sağa dön", 1500.0)
        assertNull(engine.update(800.0, 2000.0, listOf(maneuver)))
    }

    @Test
    fun announcesWithinFiveHundredMeters() {
        val engine = NavigationProgressEngine()
        val maneuver = NavigationProgressEngine.Maneuver(1, "Sağa dön", 900.0)
        val event = engine.update(1400.0, 2000.0, listOf(maneuver))
        val instruction = assertIs<NavigationProgressEngine.Event.Instruction>(event)
        assertEquals(300.0, instruction.distanceMeters)
        assertEquals(false, instruction.immediate)
    }

    @Test
    fun announcesImmediateAtFortyMeters() {
        val engine = NavigationProgressEngine()
        val maneuver = NavigationProgressEngine.Maneuver(1, "Sağa dön", 980.0)
        val event = engine.update(1040.0, 2000.0, listOf(maneuver))
        val instruction = assertIs<NavigationProgressEngine.Event.Instruction>(event)
        assertEquals(20.0, instruction.distanceMeters)
        assertEquals(true, instruction.immediate)
    }

    @Test
    fun neverRepeatsTheSameManeuver() {
        val engine = NavigationProgressEngine()
        val maneuver = NavigationProgressEngine.Maneuver(1, "Sağa dön", 900.0)
        assertIs<NavigationProgressEngine.Event.Instruction>(engine.update(1400.0, 2000.0, listOf(maneuver)))
        assertNull(engine.update(1300.0, 2000.0, listOf(maneuver)))
    }

    @Test
    fun arrivalIsAnnouncedOnlyOnce() {
        val engine = NavigationProgressEngine()
        assertIs<NavigationProgressEngine.Event.Arrived>(engine.update(20.0, 2000.0, emptyList()))
        assertNull(engine.update(10.0, 2000.0, emptyList()))
    }
}
