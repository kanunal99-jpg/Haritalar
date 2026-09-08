package com.haritalar.core.safety

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SafetyPointDeduplicatorTest {
    private fun point(
        id: String,
        lat: Double = 41.0,
        lon: Double = 29.0,
        type: SafetyPointType = SafetyPointType.FIXED_SPEED_CAMERA,
        confidence: Confidence = Confidence.HIGH,
        source: DataSource = DataSource.LIVE,
        direction: Double? = null,
    ) = SafetyPoint(id, lat, lon, type, confidence, source, direction)

    @Test
    fun exactDuplicateIdsCollapseToOne() {
        val result = SafetyPointDeduplicator.deduplicate(listOf(
            point("same", source = DataSource.CACHE, confidence = Confidence.MEDIUM),
            point("same", source = DataSource.LIVE, confidence = Confidence.HIGH),
        ))
        assertEquals(1, result.size)
        assertEquals(DataSource.LIVE, result.single().source)
    }

    @Test
    fun nearbySameTypeRecordsCollapse() {
        val result = SafetyPointDeduplicator.deduplicate(listOf(
            point("live", source = DataSource.LIVE),
            point("cache", lon = 29.0003, source = DataSource.CACHE),
        ))
        assertEquals(1, result.size)
        assertEquals("live", result.single().id)
    }

    @Test
    fun differentTypesRemainSeparate() {
        val result = SafetyPointDeduplicator.deduplicate(listOf(
            point("speed", type = SafetyPointType.FIXED_SPEED_CAMERA),
            point("signal", type = SafetyPointType.TRAFFIC_LIGHT_CAMERA),
        ))
        assertEquals(2, result.size)
    }

    @Test
    fun oppositeDirectionsRemainSeparate() {
        val result = SafetyPointDeduplicator.deduplicate(listOf(
            point("east", direction = 90.0),
            point("west", direction = 270.0),
        ))
        assertEquals(2, result.size)
    }

    @Test
    fun userReportsAreNeverMergedIntoEnforcementPoints() {
        val result = SafetyPointDeduplicator.deduplicate(listOf(
            point("camera", source = DataSource.LIVE),
            point("report", source = DataSource.USER_REPORT, type = SafetyPointType.USER_REPORTED_SAFETY_POINT, confidence = Confidence.LOW),
        ))
        assertEquals(2, result.size)
        assertTrue(result.any { it.type == SafetyPointType.USER_REPORTED_SAFETY_POINT })
    }

    @Test
    fun farApartPointsRemainSeparate() {
        val result = SafetyPointDeduplicator.deduplicate(listOf(
            point("a"),
            point("b", lon = 29.002),
        ))
        assertEquals(2, result.size)
    }

    @Test
    fun resultIsDeterministicallySorted() {
        val result = SafetyPointDeduplicator.deduplicate(listOf(point("z"), point("a")))
        assertEquals(listOf("a", "z"), result.map { it.id })
    }
}
