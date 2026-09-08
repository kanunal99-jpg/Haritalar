package com.haritalar.core.traffic

/**
 * Converts verified route-ranking output into UI-safe traffic text.
 *
 * No traffic delay is invented: when traffic was not applied, the presentation
 * contains only the base duration supplied by the routing engine.
 */
object RouteTrafficPresentation {
    data class Input(
        val routeId: String,
        val baseDurationSeconds: Long,
        val adjustedDurationSeconds: Long,
        val trafficApplied: Boolean,
    )

    data class Model(
        val routeId: String,
        val durationLabel: String,
        val trafficDelaySeconds: Long?,
    )

    fun present(input: Input): Model {
        val durationSeconds = input.adjustedDurationSeconds.coerceAtLeast(0L)
        val delaySeconds = if (input.trafficApplied) {
            (input.adjustedDurationSeconds - input.baseDurationSeconds).coerceAtLeast(0L)
        } else {
            null
        }

        val baseLabel = formatDuration(durationSeconds)
        val durationLabel = when {
            delaySeconds != null && delaySeconds > 0L -> {
                "$baseLabel • trafik +${formatDuration(delaySeconds)}"
            }
            else -> baseLabel
        }

        return Model(
            routeId = input.routeId,
            durationLabel = durationLabel,
            trafficDelaySeconds = delaySeconds,
        )
    }

    private fun formatDuration(seconds: Long): String {
        val safeSeconds = seconds.coerceAtLeast(0L)
        val minutes = (safeSeconds + 30L) / 60L
        return when {
            minutes < 60L -> "${minutes} dk"
            else -> {
                val hours = minutes / 60L
                val remainingMinutes = minutes % 60L
                if (remainingMinutes == 0L) "${hours} sa" else "${hours} sa ${remainingMinutes} dk"
            }
        }
    }
}
