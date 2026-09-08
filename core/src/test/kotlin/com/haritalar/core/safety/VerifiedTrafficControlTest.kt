package com.haritalar.core.safety

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class VerifiedTrafficControlTest {
    private val now = 1_800_000_000_000L

    private fun record(
        verified: Boolean = true,
        sourceName: String = "Resmi kaynak",
        sourceUrl: String = "https://example.gov.tr/traffic",
        publishedAt: Long = now - 1_000,
        expiresAt: Long? = null,
        lat: Double = 41.0,
        lon: Double = 29.0,
    ) = VerifiedTrafficControl(
        id = "control-1",
        latitude = lat,
        longitude = lon,
        sourceName = sourceName,
        sourceUrl = sourceUrl,
        verified = verified,
        publishedAtEpochMs = publishedAt,
        expiresAtEpochMs = expiresAt,
    )

    @Test
    fun `valid verified record becomes high confidence safety point`() {
        val point = record().toSafetyPoint(now)
        requireNotNull(point)
        assertEquals(SafetyPointType.VERIFIED_TRAFFIC_CONTROL, point.type)
        assertEquals(Confidence.HIGH, point.confidence)
        assertEquals(DataSource.LIVE, point.source)
    }

    @Test
    fun `unverified record is rejected`() {
        assertNull(record(verified = false).toSafetyPoint(now))
    }

    @Test
    fun `non https source is rejected`() {
        assertNull(record(sourceUrl = "http://example.gov.tr/traffic").toSafetyPoint(now))
    }

    @Test
    fun `missing publisher is rejected`() {
        assertNull(record(sourceName = "").toSafetyPoint(now))
    }

    @Test
    fun `future publication is rejected`() {
        assertNull(record(publishedAt = now + 1).toSafetyPoint(now))
    }

    @Test
    fun `expired record is rejected`() {
        assertNull(record(expiresAt = now - 1).toSafetyPoint(now))
    }

    @Test
    fun `invalid coordinates are rejected`() {
        assertNull(record(lat = 91.0).toSafetyPoint(now))
        assertNull(record(lon = 181.0).toSafetyPoint(now))
    }
}
