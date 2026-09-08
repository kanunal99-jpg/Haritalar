package com.haritalar.app

/**
 * Transparent cost information for a route option.
 * Unknown charges remain unknown instead of being guessed.
 */
data class RouteCostSummary(
    val tollCost: Double? = null,
    val ferryCost: Double? = null,
) {
    val knownCost: Double?
        get() = listOfNotNull(tollCost, ferryCost).takeIf { it.isNotEmpty() }?.sum()

    val hasUnknownCost: Boolean
        get() = tollCost == null || ferryCost == null
}
