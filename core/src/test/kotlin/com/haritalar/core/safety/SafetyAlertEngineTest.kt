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

    private fun position(distance: Double, bearing: Double? = 90.0) =
        RoutePosition(41.0, 29.0, bearing, distance)

    @Test fun startsAtFiveThousandKm() {
        assertEquals(5_000_000.0, assertIs<SafetyAlert.Approach>(engine.evaluate(point, position(5_000_000.0))).remainingMeters)
        assertEquals(null, engine.evaluate(point, position(5_000_001.0)))
    }

    @Test fun repeatsEveryFiveHundredKm() {
        assertIs<SafetyAlert.Approach>(engine.evaluate(point, position(5_000_000.0)))
        assertIs<SafetyAlert.Approach>(engine.evaluate(point, position(4_500_000.0)))
        assertIs<SafetyAlert.Approach>(engine.evaluate(point, position(4_000_000.0)))
        assertIs<SafetyAlert.Approach>(engine.evaluate(point, position(3_500_000.0)))
        assertIs<SafetyAlert.Approach>(engine.evaluate(point, position(3_000_000.0)))
        assertIs<SafetyAlert.Approach>(engine.evaluate(point, position(2_500_000.0)))
        assertIs<SafetyAlert.Approach>(engine.evaluate(point, position(2_000_000.0)))
        assertIs<SafetyAlert.Approach>(engine.evaluate(point, position(1_500_000.0)))
        assertIs<SafetyAlert.Approach>(engine.evaluate(point, position(1_000_000.0)))
        assertEquals(AlertLevel.FINAL, assertIs<SafetyAlert.Approach>(engine.evaluate(point, position(500_000.0))).level)
    }

    @Test fun doesNotSpamWithinSameBucket() {
        assertIs<SafetyAlert.Approach>(engine.evaluate(point, position(5_000_000.0)))
        assertEquals(null, engine.evaluate(point, position(4_900_000.0)))
    }

    @Test fun announcesPassedPointOnlyOnce() {
        assertIs<SafetyAlert.Approach>(engine.evaluate(point, position(500_000.0)))
        assertIs<SafetyAlert.Passed>(engine.evaluate(point, position(10.0)))
        assertEquals(null, engine.evaluate(point, position(5.0)))
    }

    @Test fun ignoresOppositeDirectionWhenBearingIsKnown() {
        val directionalPoint = point.copy(directionBearingDegrees = 90.0)
        assertEquals(null, engine.evaluate(directionalPoint, position(5_000_000.0, bearing = 270.0)))
    }
}
