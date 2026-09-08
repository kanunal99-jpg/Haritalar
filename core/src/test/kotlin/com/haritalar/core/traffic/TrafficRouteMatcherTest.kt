package com.haritalar.core.traffic

import com.haritalar.core.navigation.GeoCoordinate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TrafficRouteMatcherTest {
    private val route = listOf(
        GeoCoordinate(41.0000, 29.0000),
        GeoCoordinate(41.0010, 29.0000),
        GeoCoordinate(41.0020, 29.0000),
    )

    @Test
    fun `nearby high confidence segment is matched`() {
        val matched = TrafficRouteMatcher.match(
            route,
            listOf(
                segment(
                    id = "near",
                    geometry = listOf(
                        GeoCoordinate(41.0004, 29.0001),
                        GeoCoordinate(41.0008, 29.0001),
                    ),
                ),
            ),
        )

        assertEquals(listOf("near"), matched.map { it.traffic.id })
        assertTrue(matched.single().distanceMeters > 1.0)
    }

    @Test
    fun `far segment is ignored`() {
        val matched = TrafficRouteMatcher.match(
            route,
            listOf(
                segment(
                    id = "far",
                    geometry = listOf(
                        GeoCoordinate(41.0100, 29.0100),
                        GeoCoordinate(41.0110, 29.0100),
                    ),
                ),
            ),
        )

        assertTrue(matched.isEmpty())
    }

    @Test
    fun `opposite direction is ignored when bearing is available`() {
        val matched = TrafficRouteMatcher.match(
            route,
            listOf(
                segment(
                    id = "wrong-way",
                    geometry = listOf(
                        GeoCoordinate(41.0004, 29.0001),
                        GeoCoordinate(41.0008, 29.0001),
                    ),
                    bearing = 180.0,
                ),
            ),
        )

        assertTrue(matched.isEmpty())
    }

    @Test
    fun `low confidence observation is ignored`() {
        val matched = TrafficRouteMatcher.match(
            route,
            listOf(
                segment(
                    id = "low",
                    geometry = listOf(
                        GeoCoordinate(41.0004, 29.0001),
                        GeoCoordinate(41.0008, 29.0001),
                    ),
                    confidence = TrafficConfidence.LOW,
                ),
            ),
        )

        assertTrue(matched.isEmpty())
    }

    @Test
    fun `missing geometry is ignored`() {
        val matched = TrafficRouteMatcher.match(
            route,
            listOf(segment(id = "no-geometry", geometry = emptyList())),
        )

        assertTrue(matched.isEmpty())
    }

    private fun segment(
        id: String,
        geometry: List<GeoCoordinate>,
        bearing: Double? = null,
        confidence: TrafficConfidence = TrafficConfidence.HIGH,
    ) = TrafficSegment(
        id = id,
        speedKmh = 30.0,
        freeFlowSpeedKmh = 60.0,
        directionBearingDegrees = bearing,
        confidence = confidence,
        geometry = geometry,
    )
}
