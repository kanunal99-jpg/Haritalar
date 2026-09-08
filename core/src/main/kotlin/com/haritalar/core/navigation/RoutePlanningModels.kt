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

/** Ferry usage is separate from toll state because a ferry can be free or paid. */
data class RouteFerry(
    val used: Boolean,
    val amountTry: Double? = null,
    val amountKnown: Boolean = amountTry?.isFinite() == true,
)

data class RouteCostSummary(
    val toll: RouteToll = RouteToll(hasToll = false),
    val ferry: RouteFerry = RouteFerry(used = false),
) {
    val knownTotalTry: Double?
        get() = listOfNotNull(
            toll.amountTry?.takeIf { toll.amountKnown },
            ferry.amountTry?.takeIf { ferry.amountKnown },
        ).takeIf { it.isNotEmpty() }?.sum()

    val hasUnknownCost: Boolean
        get() = (toll.hasToll && !toll.amountKnown) || (ferry.used && !ferry.amountKnown)
}

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
    val ferry: RouteFerry = RouteFerry(used = false),
    val label: String,
) {
    val costs: RouteCostSummary
        get() = RouteCostSummary(toll = toll, ferry = ferry)
}

/**
 * Keeps route selection deterministic and provider-agnostic.
 * Unknown toll/ferry prices are never converted to zero.
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

    fun ferryLabel(ferry: RouteFerry): String = when {
        !ferry.used -> "Feribot yok"
        ferry.amountKnown -> "Feribot • ${formatTry(ferry.amountTry)} TL"
        else -> "Feribot • ücret doğrulanamadı"
    }

    fun costLabel(costs: RouteCostSummary): String {
        val parts = mutableListOf<String>()
        if (costs.toll.hasToll) parts += tollLabel(costs.toll)
        if (costs.ferry.used) parts += ferryLabel(costs.ferry)
        if (parts.isEmpty()) return "Ücretli geçiş/feribot tespit edilmedi"
        return parts.joinToString(" • ")
    }

    private fun formatTry(amount: Double?): String {
        if (amount == null || !amount.isFinite()) return "tutar doğrulanamadı"
        val value = BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP)
        return value.toPlainString().replace('.', ',')
    }
}
