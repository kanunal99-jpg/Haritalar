package com.haritalar.core.safety

import kotlin.test.Test
import kotlin.test.assertEquals

class SafetyReportSyncTest {
    private fun report(id: String) = SafetyReport(
        id = id,
        type = SafetyReportType.ADD,
        latitude = 41.0,
        longitude = 29.0,
        createdAtEpochMs = 1_000L,
    )

    @Test
    fun onlyAcknowledgedIdsRemainAcknowledged() {
        val reports = listOf(report("accepted-1"), report("accepted-2"), report("pending-1"))
        val result = SafetyReportSyncResult(setOf("accepted-1", "accepted-2"))
        val remaining = reports.filterNot { it.id in result.acknowledgedIds }

        assertEquals(setOf("accepted-1", "accepted-2"), result.acknowledgedIds)
        assertEquals(listOf("pending-1"), remaining.map { it.id })
    }

    @Test
    fun noOpProviderAcknowledgesNothing() {
        val reports = listOf(report("one"), report("two"))
        val result = NoOpSafetyReportSyncProvider.sync(reports)

        assertEquals(emptySet(), result.acknowledgedIds)
        assertEquals(reports.map { it.id }, reports.filterNot { it.id in result.acknowledgedIds }.map { it.id })
    }

    @Test
    fun unknownIdsDoNotRemoveQueuedReports() {
        val reports = listOf(report("one"), report("two"))
        val result = SafetyReportSyncResult(setOf("remote-only"))
        val remaining = reports.filterNot { it.id in result.acknowledgedIds }

        assertEquals(reports.map { it.id }, remaining.map { it.id })
    }
}
