package com.haritalar.core.navigation

/** Categories shown by the future 3D navigation/POI scene. */
enum class NavigationPoiCategory {
    BUILDING,
    MARKET,
    FUEL,
    REST_AREA,
    PARKING,
    RESTAURANT,
    CAFE,
    PHARMACY,
    HOSPITAL,
    HOTEL,
    SCHOOL,
    ATM,
    CHARGING_STATION,
    PARK,
    PLACE_OF_WORSHIP,
    TRANSIT,
    TOURISM,
    OTHER,
}

data class NavigationPoi(
    val id: String,
    val category: NavigationPoiCategory,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val address: String? = null,
    val openingHours: String? = null,
    val imageUrl: String? = null,
    val streetImageUrl: String? = null,
) {
    init {
        require(id.isNotBlank())
        require(name.isNotBlank())
        require(latitude in -90.0..90.0)
        require(longitude in -180.0..180.0)
    }
}

/** Image source abstraction so no paid provider is hard-coded into navigation core. */
interface NavigationImageryProvider {
    suspend fun streetImageUrl(latitude: Double, longitude: Double): String?
}
