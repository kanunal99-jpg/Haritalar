package com.haritalar.core.navigation

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFailsWith

class OsmPoiQueryTest {
    @Test
    fun query_is_bounded_and_contains_supported_poi_groups() {
        val query = OsmPoiQuery.build(40.99, 28.95, 41.02, 29.01)
        assertContains(query, "40.99,28.95,41.02,29.01")
        assertContains(query, "supermarket")
        assertContains(query, "fuel")
        assertContains(query, "parking")
        assertContains(query, "rest_area")
        assertContains(query, "place_of_worship")
        assertContains(query, "public_transport")
    }

    @Test
    fun invalid_bbox_is_rejected() {
        assertFailsWith<IllegalArgumentException> {
            OsmPoiQuery.build(41.1, 29.0, 41.0, 29.1)
        }
    }

    @Test
    fun limit_is_bounded() {
        assertFailsWith<IllegalArgumentException> {
            OsmPoiQuery.build(40.9, 28.9, 41.1, 29.1, 1001)
        }
    }
}
