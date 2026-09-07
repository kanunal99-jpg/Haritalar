package com.haritalar.core.navigation

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Cost information for a route. A missing or non-finite amount is deliberately represented as
 * unknown; the app must never invent a toll price from a boolean toll flag.
 */
data class RouteToll(
    val hasToll: Boolean,
    val amountTry: Double? = null,
    val amountKnown: Boolean = amountTry?.isFinite() == true,
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
        toll.amountKnown -> "Ücretli geçiş • ${formatTry(toll.amountTry)} TL"
        else -> "Ücretli geçiş • tutar doğrulanamadı"
    }

    private fun formatTry(amount: Double?): String {
        if (amount == null || !amount.isFinite()) return "tutar doğrulanamadı"
        val value = BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP)
        return value.toPlainString().replace('.', ',')
    }
}
