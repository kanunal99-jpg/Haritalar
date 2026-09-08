package com.haritalar.core.safety

/** Pure-JVM queue operations shared by persistence adapters and tests. */
object SafetyReportQueue {
    /**
     * Removes only IDs explicitly acknowledged by the sync result.
     * Unknown IDs are ignored and unacknowledged reports remain queued.
     */
    fun <T> acknowledge(
        reports: List<T>,
        acknowledgedIds: Collection<String>,
        idSelector: (T) -> String,
    ): List<T> {
        val ids = acknowledgedIds
            .mapNotNull { it.trim().takeIf(String::isNotEmpty) }
            .toSet()
        if (ids.isEmpty()) return reports
        return reports.filterNot { idSelector(it) in ids }
    }

    fun acknowledge(
        reports: List<SafetyReport>,
        acknowledgedIds: Collection<String>,
    ): List<SafetyReport> = acknowledge(reports, acknowledgedIds) { it.id }
}
