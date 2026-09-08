package com.haritalar.core.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NavigationPoiDetailsTest {
    @Test
    fun exposesSelectedPoiPresentationFields() {
        val poi = NavigationPoi(
            id = "node-1",
            category = NavigationPoiCategory.RESTAURANT,
            name = "Deneme Restoran",
            latitude = 41.0082,
            longitude = 28.9784,
            address = "Taksim, İstanbul",
            openingHours = "Mo-Su 10:00-23:00",
            imageUrl = "https://example.com/image.jpg",
            streetImageUrl = null,
        )

        val details = NavigationPoiDetails(poi)

        assertEquals("Deneme Restoran", details.title)
        assertEquals("Restaurant", details.categoryLabel)
        assertEquals("Taksim, İstanbul", details.addressLabel)
        assertEquals("Mo-Su 10:00-23:00", details.openingHoursLabel)
        assertEquals("https://example.com/image.jpg", details.imageUrl)
        assertNull(details.streetImageUrl)
    }

    @Test
    fun blanksAreHiddenFromPresentation() {
        val poi = NavigationPoi(
            id = "node-2",
            category = NavigationPoiCategory.OTHER,
            name = "Nokta",
            latitude = 41.0,
            longitude = 29.0,
            address = "",
            openingHours = "   ",
            imageUrl = "",
            streetImageUrl = "",
        )

        val details = NavigationPoiDetails(poi)

        assertNull(details.addressLabel)
        assertNull(details.openingHoursLabel)
        assertNull(details.imageUrl)
        assertNull(details.streetImageUrl)
    }
}
