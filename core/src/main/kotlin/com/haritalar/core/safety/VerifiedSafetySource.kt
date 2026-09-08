package com.haritalar.core.safety

/**
 * Allow-list for sources that may publish verified traffic-control data.
 * A source being allow-listed does not create any safety point by itself;
 * records still have to pass VerifiedTrafficControl.toSafetyPoint().
 */
data class VerifiedSafetySource(
    val name: String,
    val baseUrl: String,
)

object VerifiedSafetySources {
    val KGM = VerifiedSafetySource("Karayolları Genel Müdürlüğü", "https://www.kgm.gov.tr")
    val EGM = VerifiedSafetySource("Emniyet Genel Müdürlüğü", "https://onlineislemler.egm.gov.tr")
    val INTERIOR_MINISTRY = VerifiedSafetySource("T.C. İçişleri Bakanlığı", "https://www.icisleri.gov.tr")

    private val allowedSources = listOf(KGM, EGM, INTERIOR_MINISTRY)

    fun isAllowed(source: VerifiedSafetySource): Boolean =
        allowedSources.any { it.name == source.name && it.baseUrl.equals(source.baseUrl, ignoreCase = true) }

    fun isAllowedUrl(url: String): Boolean =
        allowedSources.any { url.startsWith("${it.baseUrl}/", ignoreCase = true) }
}
