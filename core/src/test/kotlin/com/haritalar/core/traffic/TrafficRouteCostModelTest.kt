package com.haritalar.core.traffic

import kotlin.test.Test
import kotlin.test.assertEquals

class TrafficRouteCostModelTest {
    @Test
    fun keepsBaseDurationWhenTrafficSpeedsAreMissing() {
        val traffic = TrafficSegment("a")
        assertEquals(600L, TrafficRouteCostModel.adjustedDurationSeconds(600L, listOf(TrafficRouteSegment(1_000.0, traffic))))
    }

    @Test
    fun increasesEtaFromObservedSpeedRatio() {
        val traffic = TrafficSegment("a", speedKmh = 30.0, freeFlowSpeedKmh = 60.0)
        assertEquals(1_200L, TrafficRouteCostModel.adjustedDurationSeconds(600L, listOf(TrafficRouteSegment(1_000.0, traffic))))
    }

    @Test
    fun weightsMultipleSegmentsByDistance() {
        val slow = TrafficSegment("slow", speedKmh = 30.0, freeFlowSpeedKmh = 60.0)
        val clear = TrafficSegment("clear", speedKmh = 60.0, freeFlowSpeedKmh = 60.0)
        assertEquals(900L, TrafficRouteCostModel.adjustedDurationSeconds(
            600L,
            listOf(
                TrafficRouteSegment(1_000.0, slow),
                TrafficRouteSegment(1_000.0, clear),
            ),
        ))
    }

    @Test
    fun doesNotReduceEtaWhenTrafficIsFasterThanFreeFlow() {
        val traffic = TrafficSegment("a", speedKmh = 80.0, freeFlowSpeedKmh = 60.0)
        assertEquals(600L, TrafficRouteCostModel.adjustedDurationSeconds(600L, listOf(TrafficRouteSegment(1_000.0, traffic))))
    }
}
