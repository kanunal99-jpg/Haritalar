package com.haritalar.app

/** UI-only traffic metadata. A route never displays traffic unless verified data was applied. */
data class RouteTrafficUiModel(
    val baseDurationSeconds: Double,
    val adjustedDurationSeconds: Double = baseDurationSeconds,
    val trafficApplied: Boolean = false,
) {
    val delaySeconds: Double
        get() = (adjustedDurationSeconds - baseDurationSeconds).coerceAtLeast(0.0)

    fun durationLabel(): String = if (!trafficApplied) {
        "%.0f dk".format(baseDurationSeconds / 60.0)
    } else {
        "%.0f dk • trafik +%.0f dk".format(adjustedDurationSeconds / 60.0, delaySeconds / 60.0)
    }
}
