package com.haritalar.app

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TrafficRefreshPolicyTest {
    @Test
    fun `first refresh is always allowed`() {
        val policy = TrafficRefreshPolicy(minimumIntervalMs = 60_000L)

        assertTrue(policy.shouldRefresh(1_000L))
    }

    @Test
    fun `refresh is blocked before minimum interval`() {
        val policy = TrafficRefreshPolicy(minimumIntervalMs = 60_000L)
        policy.markRefreshed(100_000L)

        assertFalse(policy.shouldRefresh(159_999L))
        assertTrue(policy.shouldRefresh(160_000L))
    }

    @Test
    fun `reset allows a fresh refresh`() {
        val policy = TrafficRefreshPolicy(minimumIntervalMs = 60_000L)
        policy.markRefreshed(100_000L)
        policy.reset()

        assertTrue(policy.shouldRefresh(100_001L))
    }
}
