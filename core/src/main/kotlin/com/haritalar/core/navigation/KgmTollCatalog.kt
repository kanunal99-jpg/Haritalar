package com.haritalar.core.navigation

/** Official 2026 KGM tariffs bundled for deterministic, offline-safe toll display. */
object KgmTollCatalog {
    const val TARIFF_YEAR = 2026
    const val SOURCE = "Karayolları Genel Müdürlüğü"

    /** 15 Temmuz Şehitler / FSM bridge, effective 2026-01-01. */
    val fsmAnd15TemmuzTry: Map<Int, Double> = mapOf(
        1 to 59.0,
        2 to 75.0,
        3 to 168.0,
        4 to 333.0,
        5 to 440.0,
        6 to 25.0,
    )

    /** Osmangazi bridge, effective 2026-01-01. */
    val osmangaziTry: Map<Int, Double> = mapOf(
        1 to 995.0,
        2 to 1590.0,
        3 to 1890.0,
        4 to 2505.0,
        5 to 3165.0,
        6 to 695.0,
    )

    /** Yavuz Sultan Selim bridge, effective 2026-01-01. */
    val yavuzSultanSelimTry: Map<Int, Double> = mapOf(
        1 to 95.0,
        2 to 125.0,
        3 to 235.0,
        4 to 595.0,
        5 to 740.0,
        6 to 65.0,
    )

    fun bridgePrice(bridge: TollBridge, vehicleClass: Int): Double? = when (bridge) {
        TollBridge.FSM_15_TEMMUZ -> fsmAnd15TemmuzTry[vehicleClass]
        TollBridge.OSMANGAZI -> osmangaziTry[vehicleClass]
        TollBridge.YAVUZ_SULTAN_SELIM -> yavuzSultanSelimTry[vehicleClass]
    }
}

enum class TollBridge {
    FSM_15_TEMMUZ,
    OSMANGAZI,
    YAVUZ_SULTAN_SELIM,
}

/** Vehicle class is intentionally explicit because KGM tariffs vary by class. */
data class TollVehicleProfile(
    val kgmClass: Int = 1,
)
