package com.haritalar.core.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

class ValhallaTransportClassifierTest {
    @Test
    fun ferryFlagWins() {
        assertEquals(
            RouteTransportType.FERRY,
            ValhallaTransportClassifier.classify(true, "car", "drive", 1),
        )
    }

    @Test
    fun ferryTravelTypeIsDetected() {
        assertEquals(
            RouteTransportType.FERRY,
            ValhallaTransportClassifier.classify(false, "ferry", "transit", 30),
        )
    }

    @Test
    fun ferryManeuverTypesAreDetected() {
        assertEquals(RouteTransportType.FERRY, ValhallaTransportClassifier.classify(false, null, "drive", 28))
        assertEquals(RouteTransportType.FERRY, ValhallaTransportClassifier.classify(false, null, "drive", 29))
    }

    @Test
    fun driveIsRoad() {
        assertEquals(
            RouteTransportType.ROAD,
            ValhallaTransportClassifier.classify(false, "car", "drive", 8),
        )
    }

    @Test
    fun missingProviderMetadataIsUnknown() {
        assertEquals(
            RouteTransportType.UNKNOWN,
            ValhallaTransportClassifier.classify(false, null, null, null),
        )
    }
}
