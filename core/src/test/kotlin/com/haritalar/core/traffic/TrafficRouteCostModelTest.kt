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

    @Test
    fun penalizesClosedSegmentEvenWithoutSpeedData() {
        val closed = TrafficSegment("closed", closure = true)
        assertEquals(3_600L, TrafficRouteCostModel.adjustedDurationSeconds(
            600L,
            listOf(TrafficRouteSegment(1_000.0, closed)),
        ))
    }

    @Test
    fun ignoresNonFiniteTrafficDistanceAndKeepsBaseDuration() {
        val traffic = TrafficSegment("invalid", speedKmh = 20.0, freeFlowSpeedKmh = 60.0)
        assertEquals(
            600L,
            TrafficRouteCostModel.adjustedDurationSeconds(
                600L,
                listOf(
                    TrafficRouteSegment(Double.NaN, traffic),
                    TrafficRouteSegment(Double.POSITIVE_INFINITY, traffic),
                ),
            ),
        )
    }

    @Test
    fun ignoresNegativeTrafficDistance() {
        val traffic = TrafficSegment("negative", speedKmh = 20.0, freeFlowSpeedKmh = 60.0)
        assertEquals(
            600L,
            TrafficRouteCostModel.adjustedDurationSeconds(
                600L,
                listOf(TrafficRouteSegment(-500.0, traffic)),
            ),
        )
    }

    @Test
    fun capsExtremeAdjustedDurationWithoutOverflow() {
        val traffic = TrafficSegment("extreme", speedKmh = 1.0, freeFlowSpeedKmh = 6.0)
        assertEquals(
            Long.MAX_VALUE,
            TrafficRouteCostModel.adjustedDurationSeconds(
                Long.MAX_VALUE,
                listOf(TrafficRouteSegment(1_000.0, traffic)),
            ),
        )
    }
}
