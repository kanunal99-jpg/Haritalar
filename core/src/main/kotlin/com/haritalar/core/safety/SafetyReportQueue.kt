package com.haritalar.core.safety

/** Pure-JVM queue operations shared by persistence adapters and tests. */
object SafetyReportQueue {
    /**
     * Removes only IDs explicitly acknowledged by the sync result.
     * Unknown IDs are ignored and unacknowledged reports remain queued.
     */
    fun acknowledge(
        reports: List<SafetyReport>,
        acknowledgedIds: Collection<String>,
    ): List<SafetyReport> {
        val ids = acknowledgedIds
            .mapNotNull { it.trim().takeIf(String::isNotEmpty) }
            .toSet()
        if (ids.isEmpty()) return reports
        return reports.filterNot { it.id in ids }
    }
}
