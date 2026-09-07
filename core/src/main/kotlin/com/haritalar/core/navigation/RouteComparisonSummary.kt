package com.haritalar.core.navigation

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Product-ready summary for comparing route alternatives without inventing toll costs.
 *
 * A toll amount is eligible for cost comparison only when it is finite and explicitly known.
 * Unknown tolls are never treated as zero.
 */
data class RouteComparisonSummary(
    val routeCount: Int,
    val fastestRouteId: String?,
    val shortestRouteId: String?,
    val lowestKnownTollRouteId: String?,
    val lowestKnownTollTry: Double?,
    val paidRouteCount: Int,
    val unknownTollCount: Int,
    val noTollDetectedCount: Int,
) {
    val hasComparableTollCost: Boolean
        get() = lowestKnownTollRouteId != null && lowestKnownTollTry != null

    fun lowestKnownTollLabel(): String =
        if (hasComparableTollCost) {
            "En düşük doğrulanmış ücret: ${formatTry(lowestKnownTollTry)} TL"
        } else {
            "Doğrulanmış ücret bilgisi yok"
        }

    private fun formatTry(amount: Double?): String {
        if (amount == null || !amount.isFinite()) return "tutar doğrulanamadı"
        return BigDecimal.valueOf(amount)
            .setScale(2, RoundingMode.HALF_UP)
            .toPlainString()
            .replace('.', ',')
    }

    companion object {
        fun from(options: List<RouteAlternative>): RouteComparisonSummary {
            val sorted = RouteAlternativeRanker.sort(options)
            val knownPaid = options.filter { it.toll.hasToll && it.toll.amountKnown }
            val lowestKnown = knownPaid.minWithOrNull(
                compareBy<RouteAlternative> { it.toll.amountTry ?: Double.POSITIVE_INFINITY }
                    .thenBy { it.route.durationSeconds }
                    .thenBy { it.id },
            )

            return RouteComparisonSummary(
                routeCount = options.size,
                fastestRouteId = sorted.firstOrNull()?.id,
                shortestRouteId = options.minWithOrNull(
                    compareBy<RouteAlternative> { it.route.distanceMeters }
                        .thenBy { it.route.durationSeconds }
                        .thenBy { it.id },
                )?.id,
                lowestKnownTollRouteId = lowestKnown?.id,
                lowestKnownTollTry = lowestKnown?.toll?.amountTry,
                paidRouteCount = options.count { it.toll.hasToll },
                unknownTollCount = options.count { it.toll.hasToll && !it.toll.amountKnown },
                noTollDetectedCount = options.count { !it.toll.hasToll },
            )
        }
    }
}
