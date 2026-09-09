package com.haritalar.core.traffic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TrafficRouteMapPresentationTest {
    @Test
    fun mapsProviderCongestionToExpectedSeverity() {
        assertEquals(TrafficRouteMapPresentation.Severity.FREE, TrafficRouteMapPresentation.severityOf(TrafficSegment("free", congestion = TrafficCongestion.FREE)))
        assertEquals(TrafficRouteMapPresentation.Severity.LIGHT, TrafficRouteMapPresentation.severityOf(TrafficSegment("light", congestion = TrafficCongestion.LIGHT)))
        assertEquals(TrafficRouteMapPresentation.Severity.MODERATE, TrafficRouteMapPresentation.severityOf(TrafficSegment("moderate", congestion = TrafficCongestion.MODERATE)))
        assertEquals(TrafficRouteMapPresentation.Severity.HEAVY, TrafficRouteMapPresentation.severityOf(TrafficSegment("heavy", congestion = TrafficCongestion.HEAVY)))
        assertEquals(TrafficRouteMapPresentation.Severity.SEVERE, TrafficRouteMapPresentation.severityOf(TrafficSegment("severe", congestion = TrafficCongestion.SEVERE)))
    }

    @Test
    fun unknownTrafficProducesNoColor() {
        assertNull(TrafficRouteMapPresentation.severityOf(TrafficSegment("unknown")))
    }

    @Test
    fun verifiedClosureIsSevere() {
        assertEquals(TrafficRouteMapPresentation.Severity.SEVERE, TrafficRouteMapPresentation.severityOf(TrafficSegment("closed", closure = true)))
    }
}
