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
    fun onlyAcknowledgedIdsAreRemovedFromQueue() {
        val reports = listOf(report("accepted-1"), report("accepted-2"), report("pending-1"))

        val remaining = SafetyReportQueue.acknowledge(
            reports,
            acknowledgedIds = setOf("accepted-1", "accepted-2"),
        )

        assertEquals(listOf("pending-1"), remaining.map { it.id })
    }

    @Test
    fun noOpProviderLeavesEveryReportQueued() {
        val reports = listOf(report("one"), report("two"))
        val result = NoOpSafetyReportSyncProvider.sync(reports)

        val remaining = SafetyReportQueue.acknowledge(reports, result.acknowledgedIds)

        assertEquals(emptySet(), result.acknowledgedIds)
        assertEquals(reports.map { it.id }, remaining.map { it.id })
    }

    @Test
    fun unknownIdsDoNotRemoveQueuedReports() {
        val reports = listOf(report("one"), report("two"))

        val remaining = SafetyReportQueue.acknowledge(
            reports,
            acknowledgedIds = setOf("remote-only"),
        )

        assertEquals(reports.map { it.id }, remaining.map { it.id })
    }

    @Test
    fun emptyAcknowledgementDoesNotMutateQueue() {
        val reports = listOf(report("one"), report("two"))

        val remaining = SafetyReportQueue.acknowledge(reports, emptySet())

        assertEquals(reports, remaining)
    }

    @Test
    fun duplicateAndBlankAcknowledgementsAreHarmless() {
        val reports = listOf(report("one"), report("two"), report("three"))

        val remaining = SafetyReportQueue.acknowledge(
            reports,
            acknowledgedIds = listOf(" one ", "one", "", "   ", "three"),
        )

        assertEquals(listOf("two"), remaining.map { it.id })
    }
}
