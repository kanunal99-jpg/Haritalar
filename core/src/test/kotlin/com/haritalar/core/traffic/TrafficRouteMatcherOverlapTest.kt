package com.haritalar.core.traffic

import com.haritalar.core.navigation.GeoCoordinate
import kotlin.test.Test
import kotlin.test.assertEquals

class TrafficRouteMatcherOverlapTest {
    private val route = listOf(
        GeoCoordinate(41.0000, 29.0000),
        GeoCoordinate(41.0000, 29.0100),
    )

    @Test
    fun `rejects observation when only one of many geometry points touches route`() {
        val observation = TrafficSegment(
            id = "weak",
            speedKmh = 20.0,
            freeFlowSpeedKmh = 60.0,
            confidence = TrafficConfidence.HIGH,
            geometry = listOf(
                GeoCoordinate(41.0000, 29.0050),
                GeoCoordinate(41.0200, 29.0050),
                GeoCoordinate(41.0400, 29.0050),
            ),
        )

        val matched = TrafficRouteMatcher.match(route, listOf(observation))
        assertEquals(0, matched.size)
    }

    @Test
    fun `accepts observation when meaningful geometry overlaps route`() {
        val observation = TrafficSegment(
            id = "strong",
            speedKmh = 20.0,
            freeFlowSpeedKmh = 60.0,
            confidence = TrafficConfidence.HIGH,
            geometry = listOf(
                GeoCoordinate(41.0000, 29.0040),
                GeoCoordinate(41.0000, 29.0060),
                GeoCoordinate(41.0000, 29.0080),
            ),
        )

        val matched = TrafficRouteMatcher.match(route, listOf(observation))
        assertEquals(1, matched.size)
        assertEquals("strong", matched.single().traffic.id)
    }
}
