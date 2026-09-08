package com.haritalar.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SafetyReportStoreTest {
    @Test fun `report types cover correction and addition flows`() {
        assertEquals(4, SafetyReportStore.Type.values().size)
        assertEquals(SafetyReportStore.Type.ADD, SafetyReportStore.Type.valueOf("ADD"))
        assertEquals(SafetyReportStore.Type.REMOVED, SafetyReportStore.Type.valueOf("REMOVED"))
        assertEquals(SafetyReportStore.Type.WRONG_LOCATION, SafetyReportStore.Type.valueOf("WRONG_LOCATION"))
        assertEquals(SafetyReportStore.Type.INCORRECT_TYPE, SafetyReportStore.Type.valueOf("INCORRECT_TYPE"))
    }

    @Test fun `valid coordinates are accepted`() {
        assertTrue(SafetyReportStore.isValidCoordinate(41.0, 29.0))
        assertTrue(SafetyReportStore.isValidCoordinate(-90.0, -180.0))
        assertTrue(SafetyReportStore.isValidCoordinate(90.0, 180.0))
    }

    @Test fun `invalid coordinates are rejected`() {
        assertFalse(SafetyReportStore.isValidCoordinate(91.0, 29.0))
        assertFalse(SafetyReportStore.isValidCoordinate(41.0, 181.0))
        assertFalse(SafetyReportStore.isValidCoordinate(Double.NaN, 29.0))
        assertFalse(SafetyReportStore.isValidCoordinate(41.0, Double.POSITIVE_INFINITY))
    }
}
