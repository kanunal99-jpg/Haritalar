package com.haritalar.core.safety

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VerifiedSafetySourceTest {
    @Test
    fun allowsOfficialSources() {
        assertTrue(VerifiedSafetySources.isAllowed(VerifiedSafetySources.KGM))
        assertTrue(VerifiedSafetySources.isAllowed(VerifiedSafetySources.EGM))
        assertTrue(VerifiedSafetySources.isAllowed(VerifiedSafetySources.INTERIOR_MINISTRY))
    }

    @Test
    fun rejectsUnknownSources() {
        assertFalse(VerifiedSafetySources.isAllowed(VerifiedSafetySource("Example", "https://example.com")))
    }

    @Test
    fun allowsOnlyExactOfficialHttpsPrefixes() {
        assertTrue(VerifiedSafetySources.isAllowedUrl("https://onlineislemler.egm.gov.tr/trafik/sayfalar/edsharita.aspx"))
        assertTrue(VerifiedSafetySources.isAllowedUrl("https://www.icisleri.gov.tr/iller-arasi-radar-ve-kontrol-noktasi-uygulama-sayilari"))
        assertTrue(VerifiedSafetySources.isAllowedUrl("https://www.kgm.gov.tr/Sayfalar/KGM/SiteTr/Trafik/HizSinirlari.aspx"))
        assertFalse(VerifiedSafetySources.isAllowedUrl("http://onlineislemler.egm.gov.tr/data"))
        assertFalse(VerifiedSafetySources.isAllowedUrl("https://onlineislemler.egm.gov.tr.evil.example/"))
        assertFalse(VerifiedSafetySources.isAllowedUrl("https://example.com/egm"))
    }
}
