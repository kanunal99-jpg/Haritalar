package com.haritalar.core.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NavigationPoiModelTest {
    @Test
    fun validPoi_keepsCategoryAndCoordinates() {
        val poi = NavigationPoi(
            id = "fuel-1",
            category = NavigationPoiCategory.FUEL,
            name = "Örnek İstasyon",
            latitude = 41.0082,
            longitude = 28.9784,
        )

        assertEquals(NavigationPoiCategory.FUEL, poi.category)
        assertEquals(41.0082, poi.latitude)
        assertEquals(28.9784, poi.longitude)
    }

    @Test
    fun invalidCoordinates_areRejected() {
        assertFailsWith<IllegalArgumentException> {
            NavigationPoi("bad", NavigationPoiCategory.OTHER, "X", 91.0, 0.0)
        }
    }
}
