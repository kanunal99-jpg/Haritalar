package com.haritalar.core.safety

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VerifiedSafetySourceTest {
    @Test
    fun allowsOnlyOfficialKgmSource() {
        assertTrue(VerifiedSafetySources.isAllowed(VerifiedSafetySources.KGM))
        assertFalse(
            VerifiedSafetySources.isAllowed(
                VerifiedSafetySource("Example", "https://example.com"),
            ),
        )
    }

    @Test
    fun allowsOnlyKgmUrls() {
        assertTrue(VerifiedSafetySources.isAllowedUrl("https://www.kgm.gov.tr/Sayfalar/KGM/SiteTr/Trafik/HizSinirlari.aspx"))
        assertFalse(VerifiedSafetySources.isAllowedUrl("https://example.com/kgm"))
        assertFalse(VerifiedSafetySources.isAllowedUrl("https://www.kgm.gov.tr.evil.example/"))
        assertFalse(VerifiedSafetySources.isAllowedUrl("http://www.kgm.gov.tr/data"))
    }
}
