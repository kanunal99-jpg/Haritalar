package com.haritalar.core.safety

/**
 * Transport-neutral synchronization contract for queued driver reports.
 * Implementations must acknowledge only IDs the remote service accepted.
 */
interface SafetyReportSyncProvider {
    fun sync(reports: List<SafetyReport>): SafetyReportSyncResult
}

data class SafetyReportSyncResult(
    val acknowledgedIds: Set<String>,
)

/**
 * Safe default: no network side effects and nothing is acknowledged.
 */
object NoOpSafetyReportSyncProvider : SafetyReportSyncProvider {
    override fun sync(reports: List<SafetyReport>): SafetyReportSyncResult =
        SafetyReportSyncResult(emptySet())
}
