package com.haritalar.core.navigation

/** Immutable presentation model for a selected map POI. */
data class NavigationPoiDetails(
    val poi: NavigationPoi,
) {
    val title: String get() = poi.name
    val categoryLabel: String
        get() = when (poi.category) {
            NavigationPoiCategory.TRAFFIC_SIGNAL -> "Trafik ışığı"
            NavigationPoiCategory.SPEED_CAMERA -> "Hız kamerası / radar"
            NavigationPoiCategory.RAILWAY -> "Tren / metro"
            NavigationPoiCategory.TRAM -> "Tramvay"
            NavigationPoiCategory.MARKET -> "Market / AVM"
            NavigationPoiCategory.FUEL -> "Benzin istasyonu"
            NavigationPoiCategory.PLACE_OF_WORSHIP -> "Cami / ibadethane"
            NavigationPoiCategory.PARKING -> "Otopark"
            NavigationPoiCategory.TRANSIT -> "Toplu taşıma"
            NavigationPoiCategory.CHARGING_STATION -> "Şarj istasyonu"
            NavigationPoiCategory.PUBLIC_INSTITUTION -> "Kamu kurumu"
            else -> poi.category.name.lowercase().replace('_', ' ').replaceFirstChar { it.titlecase() }
        }
    val addressLabel: String? get() = poi.address?.takeIf { it.isNotBlank() }
    val openingHoursLabel: String? get() = poi.openingHours?.takeIf { it.isNotBlank() }
    val imageUrl: String? get() = poi.imageUrl?.takeIf { it.isNotBlank() }
    val streetImageUrl: String? get() = poi.streetImageUrl?.takeIf { it.isNotBlank() }
}
