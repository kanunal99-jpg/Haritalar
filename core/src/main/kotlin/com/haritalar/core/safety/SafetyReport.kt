package com.haritalar.core.safety

/** A privacy-minimal driver report queued for validation/synchronization. */
data class SafetyReport(
    val id: String,
    val type: SafetyReportType,
    val latitude: Double,
    val longitude: Double,
    val pointId: String? = null,
    val createdAtEpochMs: Long,
)

enum class SafetyReportType {
    ADD,
    REMOVED,
    WRONG_LOCATION,
    INCORRECT_TYPE,
}

/** User reports are never treated as verified enforcement data automatically. */
fun SafetyReport.toSafetyPoint(): SafetyPoint? = when (type) {
    SafetyReportType.ADD -> SafetyPoint(
        id = "user-$id",
        latitude = latitude,
        longitude = longitude,
        type = SafetyPointType.USER_REPORTED_SAFETY_POINT,
        confidence = Confidence.LOW,
        source = DataSource.USER_REPORT,
    )
    SafetyReportType.REMOVED,
    SafetyReportType.WRONG_LOCATION,
    SafetyReportType.INCORRECT_TYPE -> null
}
