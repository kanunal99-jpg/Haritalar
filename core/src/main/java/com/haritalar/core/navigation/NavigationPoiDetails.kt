package com.haritalar.core.navigation

/** Immutable presentation model for a selected map POI. */
data class NavigationPoiDetails(
    val poi: NavigationPoi,
) {
    val title: String get() = poi.name
    val categoryLabel: String
        get() = poi.category.name
            .lowercase()
            .replace('_', ' ')
            .replaceFirstChar { it.titlecase() }
    val addressLabel: String? get() = poi.address?.takeIf { it.isNotBlank() }
    val openingHoursLabel: String? get() = poi.openingHours?.takeIf { it.isNotBlank() }
    val imageUrl: String? get() = poi.imageUrl?.takeIf { it.isNotBlank() }
    val streetImageUrl: String? get() = poi.streetImageUrl?.takeIf { it.isNotBlank() }
}
