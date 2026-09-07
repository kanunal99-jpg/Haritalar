package com.haritalar.core.navigation

/**
 * Cost information for a route. A missing amount is deliberately represented as unknown;
 * the app must never invent a toll price from a boolean toll flag.
 */
data class RouteToll(
    val hasToll: Boolean,
    val amountTry: Double? = null,
    val amountKnown: Boolean = amountTry != null,
)

enum class RoutePreference {
    FASTEST,
    SHORTEST,
    TOLL_FREE,
    TOLL_FAST,
}

data class RouteAlternative(
    val id: String,
    val route: NavigationRoute,
    val preference: RoutePreference,
    val toll: RouteToll = RouteToll(hasToll = false),
    val label: String,
)

/**
 * Keeps route selection deterministic and provider-agnostic.
 * Unknown toll prices are never converted to zero.
 */
object RouteAlternativeRanker {
    fun sort(options: List<RouteAlternative>): List<RouteAlternative> =
        options.sortedWith(
            compareBy<RouteAlternative> { it.route.durationSeconds }
                .thenBy { it.route.distanceMeters }
                .thenBy { it.id },
        )

    fun tollLabel(toll: RouteToll): String = when {
        !toll.hasToll -> "Ücretsiz geçiş tespit edilmedi"
        toll.amountKnown -> "Ücretli geçiş • %.2f TL".format(toll.amountTry)
        else -> "Ücretli geçiş • tutar doğrulanamadı"
    }
}
