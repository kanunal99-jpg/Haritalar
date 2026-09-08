package com.haritalar.app

/**
 * Keeps traffic refresh timing deterministic and prevents the UI from polling
 * a provider more often than the configured minimum interval.
 *
 * A refresh policy deliberately knows nothing about a provider or traffic
 * values. It only answers whether a new fetch is allowed.
 */
class TrafficRefreshPolicy(
    private val minimumIntervalMs: Long = DEFAULT_MINIMUM_INTERVAL_MS,
) {
    init {
        require(minimumIntervalMs >= 0L) { "minimumIntervalMs must be >= 0" }
    }

    private var lastRefreshAtMs: Long? = null

    fun shouldRefresh(nowMs: Long): Boolean {
        require(nowMs >= 0L) { "nowMs must be >= 0" }
        val last = lastRefreshAtMs ?: return true
        return nowMs - last >= minimumIntervalMs
    }

    fun markRefreshed(nowMs: Long) {
        require(nowMs >= 0L) { "nowMs must be >= 0" }
        lastRefreshAtMs = nowMs
    }

    fun reset() {
        lastRefreshAtMs = null
    }

    companion object {
        const val DEFAULT_MINIMUM_INTERVAL_MS = 60_000L
    }
}
