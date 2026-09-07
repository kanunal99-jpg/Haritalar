package com.haritalar.core.navigation

import java.util.Locale

/** Presentation-ready, provider-independent summary for a route option. */
data class RouteSummary(
    val title: String,
    val distanceText: String,
    val durationText: String,
    val tollText: String,
    val isRecommended: Boolean,
)

object RouteSummaryFormatter {
    fun format(option: RouteAlternative, recommended: Boolean = false): RouteSummary {
        val distance = option.route.distanceMeters.coerceAtLeast(0.0)
        val duration = option.route.durationSeconds.coerceAtLeast(0L)
        return RouteSummary(
            title = option.label,
            distanceText = formatDistance(distance),
            durationText = formatDuration(duration),
            tollText = RouteAlternativeRanker.tollLabel(option.toll),
            isRecommended = recommended,
        )
    }

    private fun formatDistance(meters: Double): String {
        return if (meters < 1000.0) {
            "%.0f m".format(Locale.US, meters)
        } else {
            "%.1f km".format(Locale.US, meters / 1000.0)
        }
    }

    private fun formatDuration(seconds: Long): String {
        val minutes = (seconds / 60L).coerceAtLeast(0L)
        val hours = minutes / 60L
        val remainingMinutes = minutes % 60L
        return when {
            hours > 0L && remainingMinutes > 0L -> "$hours sa $remainingMinutes dk"
            hours > 0L -> "$hours sa"
            else -> "$minutes dk"
        }
    }
}
