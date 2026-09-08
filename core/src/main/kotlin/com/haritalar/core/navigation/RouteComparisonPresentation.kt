package com.haritalar.core.navigation

/**
 * Stable, UI-agnostic labels for route comparison.
 *
 * The presentation layer intentionally distinguishes a route with no detected toll from a
 * paid route whose amount is unknown. It never converts unknown toll data into a price.
 */
object RouteComparisonPresentation {
    fun routeStatus(toll: RouteToll): String = when {
        !toll.hasToll -> "Ücretli geçiş tespit edilmedi"
        toll.amountKnown -> RouteAlternativeRanker.tollLabel(toll)
        else -> "Ücretli geçiş • tutar doğrulanamadı"
    }

    fun summary(summary: RouteComparisonSummary): String = buildString {
        append(summary.routeCount)
        append(" rota")
        if (summary.paidRouteCount > 0) {
            append(" • ")
            append(summary.paidRouteCount)
            append(" ücretli")
        }
        if (summary.noTollDetectedCount > 0) {
            append(" • ")
            append(summary.noTollDetectedCount)
            append(" ücretli geçiş tespit edilmedi")
        }
        if (summary.unknownTollCount > 0) {
            append(" • ")
            append(summary.unknownTollCount)
            append(" ücret tutarı doğrulanamadı")
        }
    }
}
