package com.haritalar.core.safety

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SafetyAlertEngineTest {
    private val point = SafetyPoint(
        id = "radar-1", latitude = 41.0, longitude = 29.0,
        type = SafetyPointType.FIXED_SPEED_CAMERA,
        confidence = Confidence.HIGH, source = DataSource.OFFLINE,
    )

    private val engine = SafetyAlertEngine()

    private fun position(distance: Double) = RoutePosition(41.0, 29.0, 90.0, distance)

    @Test fun startsAtFourKm() {
        assertEquals(4000.0, assertIs<SafetyAlert.Approach>(engine.evaluate(point, position(4000.0))).remainingMeters)
    }

    @Test fun repeatsEveryFiveHundredMeters() {
        assertIs<SafetyAlert.Approach>(engine.evaluate(point, position(4000.0)))
        assertIs<SafetyAlert.Approach>(engine.evaluate(point, position(3500.0)))
        assertIs<SafetyAlert.Approach>(engine.evaluate(point, position(3000.0)))
        assertIs<SafetyAlert.Approach>(engine.evaluate(point, position(2500.0)))
        assertIs<SafetyAlert.Approach>(engine.evaluate(point, position(2000.0)))
        assertIs<SafetyAlert.Approach>(engine.evaluate(point, position(1500.0)))
        assertIs<SafetyAlert.Approach>(engine.evaluate(point, position(1000.0)))
        assertEquals(AlertLevel.FINAL, assertIs<SafetyAlert.Approach>(engine.evaluate(point, position(500.0))).level)
    }

    @Test fun doesNotSpamWithinSameBucket() {
        assertIs<SafetyAlert.Approach>(engine.evaluate(point, position(4000.0)))
        assertEquals(null, engine.evaluate(point, position(3900.0)))
    }

    @Test fun announcesPassedPoint() {
        assertIs<SafetyAlert.Approach>(engine.evaluate(point, position(500.0)))
        assertIs<SafetyAlert.Passed>(engine.evaluate(point, position(10.0)))
    }
}
