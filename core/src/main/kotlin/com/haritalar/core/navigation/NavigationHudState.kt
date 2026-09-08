package com.haritalar.core.navigation

/** Safe, UI-ready state for the live navigation HUD. */
data class NavigationHudState(
    val remainingMeters: Double,
    val nextInstruction: String? = null,
    val nextInstructionMeters: Double? = null,
    val speedKmh: Double? = null,
    val gpsAccuracyMeters: Float? = null,
    val offRoute: Boolean = false,
    val arrived: Boolean = false,
) {
    init {
        require(remainingMeters.isFinite() && remainingMeters >= 0.0)
        require(nextInstructionMeters == null || (nextInstructionMeters.isFinite() && nextInstructionMeters >= 0.0))
        require(speedKmh == null || (speedKmh.isFinite() && speedKmh >= 0.0))
        require(gpsAccuracyMeters == null || (gpsAccuracyMeters.isFinite() && gpsAccuracyMeters >= 0.0f))
    }

    val remainingLabel: String
        get() = if (remainingMeters >= 1000.0) {
            "%.1f km".format(java.util.Locale.US, remainingMeters / 1000.0)
        } else {
            "%.0f m".format(java.util.Locale.US, remainingMeters)
        }

    val nextInstructionLabel: String?
        get() = nextInstruction?.takeIf { it.isNotBlank() }?.let { text ->
            val meters = nextInstructionMeters
            if (meters == null) text
            else if (meters >= 1000.0) "%.1f km • %s".format(java.util.Locale.US, meters / 1000.0, text)
            else "%.0f m • %s".format(java.util.Locale.US, meters, text)
        }

    val gpsLabel: String
        get() = when {
            gpsAccuracyMeters == null -> "GPS bekleniyor"
            gpsAccuracyMeters <= 20f -> "GPS güçlü"
            gpsAccuracyMeters <= 60f -> "GPS orta"
            else -> "GPS zayıf"
        }
}
