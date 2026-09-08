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
    /** Karayolları Genel Müdürlüğü (KGM) official domain. */
    val KGM = VerifiedSafetySource(
        name = "Karayolları Genel Müdürlüğü",
        baseUrl = "https://www.kgm.gov.tr",
    )

    fun isAllowed(source: VerifiedSafetySource): Boolean =
        source.name == KGM.name && source.baseUrl.equals(KGM.baseUrl, ignoreCase = true)

    fun isAllowedUrl(url: String): Boolean =
        url.startsWith("${KGM.baseUrl}/", ignoreCase = true)
}
