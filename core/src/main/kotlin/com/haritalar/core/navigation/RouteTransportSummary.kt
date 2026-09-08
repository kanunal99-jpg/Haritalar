package com.haritalar.core.navigation

/** Transport modes that can occur inside a single route returned by a routing provider. */
enum class RouteTransportType {
    ROAD,
    FERRY,
    UNKNOWN,
}

enum class RouteTransportPreference {
    ALLOW_FERRY,
    AVOID_FERRY,
    PREFER_FERRY,
}

data class RouteTransportSummary(
    val types: List<RouteTransportType>,
    val ferryCount: Int,
) {
    val usesFerry: Boolean
        get() = ferryCount > 0

    val isRoadOnly: Boolean
        get() = !usesFerry && types.all { it == RouteTransportType.ROAD }

    fun label(): String = when {
        usesFerry && types.any { it == RouteTransportType.ROAD } -> "Kara + feribot + kara"
        usesFerry -> "Feribot"
        isRoadOnly -> "Karayolu"
        else -> "Ulaşım türü doğrulanamadı"
    }

    companion object {
        fun from(types: List<RouteTransportType>): RouteTransportSummary {
            val normalized = types.ifEmpty { listOf(RouteTransportType.UNKNOWN) }
            return RouteTransportSummary(
                types = normalized,
                ferryCount = normalized.count { it == RouteTransportType.FERRY },
            )
        }
    }
}

/** Provider-agnostic ferry policy. The provider remains responsible for honoring the preference. */
object RouteTransportPolicy {
    fun allowsFerry(preference: RouteTransportPreference): Boolean =
        preference != RouteTransportPreference.AVOID_FERRY

    fun prefersFerry(preference: RouteTransportPreference): Boolean =
        preference == RouteTransportPreference.PREFER_FERRY
}
