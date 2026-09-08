package com.haritalar.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SafetyReportStoreTest {
    @Test fun `report types cover correction and addition flows`() {
        assertEquals(4, SafetyReportStore.Type.values().size)
        assertEquals(SafetyReportStore.Type.ADD, SafetyReportStore.Type.valueOf("ADD"))
        assertEquals(SafetyReportStore.Type.REMOVED, SafetyReportStore.Type.valueOf("REMOVED"))
        assertEquals(SafetyReportStore.Type.WRONG_LOCATION, SafetyReportStore.Type.valueOf("WRONG_LOCATION"))
        assertEquals(SafetyReportStore.Type.INCORRECT_TYPE, SafetyReportStore.Type.valueOf("INCORRECT_TYPE"))
    }

    @Test fun `invalid coordinates are rejected`() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        assertNull(SafetyReportStore.enqueue(context, SafetyReportStore.Type.ADD, 91.0, 29.0))
        assertNull(SafetyReportStore.enqueue(context, SafetyReportStore.Type.ADD, 41.0, 181.0))
    }
}
