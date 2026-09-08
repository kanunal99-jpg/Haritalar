package com.haritalar.core.traffic

import kotlin.test.Test
import kotlin.test.assertEquals

class TrafficRouteClosureTest {
    @Test
    fun closedSegmentPenalizesEtaEvenWithoutSpeedData() {
        val traffic = TrafficSegment("closed", closure = true)

        assertEquals(
            3_600L,
            TrafficRouteCostModel.adjustedDurationSeconds(
                600L,
                listOf(TrafficRouteSegment(1_000.0, traffic)),
            ),
        )
    }
}
