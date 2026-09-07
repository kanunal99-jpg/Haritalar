package com.haritalar.core.navigation

import kotlin.test.Test
import kotlin.test.assertTrue

class NavigationMathTest {
    @Test fun samePointHasZeroDistance() {
        val p = GeoCoordinate(41.0082, 28.9784)
        assertTrue(NavigationMath.distanceMeters(p, p) < 0.001)
    }

    @Test fun nearbyPointsHavePositiveDistance() {
        val a = GeoCoordinate(41.0082, 28.9784)
        val b = GeoCoordinate(41.0092, 28.9784)
        val distance = NavigationMath.distanceMeters(a, b)
        assertTrue(distance in 100.0..120.0)
    }

    @Test fun northBearingIsApproximatelyZero() {
        val a = GeoCoordinate(41.0, 29.0)
        val b = GeoCoordinate(42.0, 29.0)
        assertTrue(NavigationMath.initialBearingDegrees(a, b) < 1.0 || NavigationMath.initialBearingDegrees(a, b) > 359.0)
    }
}
