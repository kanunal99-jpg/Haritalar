package com.haritalar.core.traffic

/** Maps only provider-declared congestion to visible route colors. */
object TrafficRouteMapPresentation {
    enum class Severity { FREE, LIGHT, MODERATE, HEAVY, SEVERE }

    fun severityOf(segment: TrafficSegment): Severity? = when {
        segment.closure -> Severity.SEVERE
        else -> when (segment.congestion) {
            TrafficCongestion.FREE -> Severity.FREE
            TrafficCongestion.LIGHT -> Severity.LIGHT
            TrafficCongestion.MODERATE -> Severity.MODERATE
            TrafficCongestion.HEAVY -> Severity.HEAVY
            TrafficCongestion.SEVERE -> Severity.SEVERE
            TrafficCongestion.UNKNOWN -> null
        }
    }

    fun colorHex(severity: Severity): String = when (severity) {
        Severity.FREE -> "#2E7D32"
        Severity.LIGHT -> "#F9A825"
        Severity.MODERATE -> "#FB8C00"
        Severity.HEAVY -> "#E53935"
        Severity.SEVERE -> "#B71C1C"
    }
}
