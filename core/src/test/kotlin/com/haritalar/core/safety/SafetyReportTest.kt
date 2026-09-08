package com.haritalar.core.safety

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SafetyReportTest {
    @Test
    fun addReportBecomesLowConfidenceUserPoint() {
        val report = SafetyReport(
            id = "r1",
            type = SafetyReportType.ADD,
            latitude = 41.0,
            longitude = 29.0,
            createdAtEpochMs = 1L,
        )

        val point = report.toSafetyPoint()

        requireNotNull(point)
        assertEquals(SafetyPointType.USER_REPORTED_SAFETY_POINT, point.type)
        assertEquals(Confidence.LOW, point.confidence)
        assertEquals(DataSource.USER_REPORT, point.source)
    }

    @Test
    fun nonAddReportsDoNotCreateSafetyPoints() {
        val report = SafetyReport(
            id = "r2",
            type = SafetyReportType.REMOVED,
            latitude = 41.0,
            longitude = 29.0,
            createdAtEpochMs = 1L,
        )

        assertNull(report.toSafetyPoint())
    }
}
