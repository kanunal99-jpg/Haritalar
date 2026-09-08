package com.haritalar.core.traffic

import com.haritalar.core.navigation.GeoCoordinate
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TomTomTrafficProviderTest {
    private val point = GeoCoordinate(41.0082, 28.9784)
    private val bounds = TrafficBounds(41.0, 28.9, 41.1, 29.1)

    @Test
    fun blankApiKeyDisablesProvider() {
        val provider = TomTomTrafficProvider(apiKey = "")

        assertFalse(provider.supports(point))
    }

    @Test
    fun validTomTomResponseIsParsedWithoutInventingGeometry() {
        val response = """
            {
              "flowSegmentData": {
                "frc": "FRC2",
                "currentSpeed": 36,
                "freeFlowSpeed": 60,
                "coordinates": {
                  "coordinate": [
                    {"latitude": 41.0080, "longitude": 28.9780},
                    {"latitude": 41.0090, "longitude": 28.9790}
                  ]
                }
              }
            }
        """.trimIndent()
        val client = FakeClient(response)
        val provider = TomTomTrafficProvider(
            apiKey = "test-key",
            httpClient = client,
            nowEpochMs = { 1_000L },
            maxSamples = 2,
        )

        val snapshot = runSuspend { provider.fetchTraffic(bounds, TrafficRoute(listOf(point))) }

        assertEquals("tomtom-flow", snapshot.providerId)
        assertEquals(1_000L, snapshot.fetchedAtEpochMs)
        assertEquals(61_000L, snapshot.expiresAtEpochMs)
        assertEquals(TrafficConfidence.HIGH, snapshot.confidence)
        val segment = snapshot.segments.single()
        assertEquals(36.0, segment.speedKmh)
        assertEquals(60.0, segment.freeFlowSpeedKmh)
        assertEquals(TrafficCongestion.MODERATE, segment.congestion)
        assertEquals(2, segment.geometry.size)
        assertNotNull(client.lastPoint)
        assertEquals("test-key", client.lastApiKey)
    }

    @Test
    fun repeatedSampleWithinCacheTtlUsesOnlyOneHttpRequest() {
        val response = """
            {"flowSegmentData":{"frc":"FRC2","currentSpeed":36,"freeFlowSpeed":60,
            "coordinates":{"coordinate":[{"latitude":41.0080,"longitude":28.9780},
            {"latitude":41.0090,"longitude":28.9790}]}}}
        """.trimIndent()
        var now = 1_000L
        val client = FakeClient(response)
        val provider = TomTomTrafficProvider(
            apiKey = "test-key",
            httpClient = client,
            nowEpochMs = { now },
            maxSamples = 2,
            cacheTtlMs = 30_000L,
        )

        runSuspend { provider.fetchTraffic(bounds, TrafficRoute(listOf(point))) }
        now += 10_000L
        runSuspend { provider.fetchTraffic(bounds, TrafficRoute(listOf(point))) }

        assertEquals(1, client.requestCount)
    }

    @Test
    fun sampleCacheExpiresAndRefreshesAfterTtl() {
        val response = """
            {"flowSegmentData":{"frc":"FRC2","currentSpeed":36,"freeFlowSpeed":60,
            "coordinates":{"coordinate":[{"latitude":41.0080,"longitude":28.9780},
            {"latitude":41.0090,"longitude":28.9790}]}}}
        """.trimIndent()
        var now = 1_000L
        val client = FakeClient(response)
        val provider = TomTomTrafficProvider(
            apiKey = "test-key",
            httpClient = client,
            nowEpochMs = { now },
            cacheTtlMs = 30_000L,
        )

        runSuspend { provider.fetchTraffic(bounds, TrafficRoute(listOf(point))) }
        now += 30_001L
        runSuspend { provider.fetchTraffic(bounds, TrafficRoute(listOf(point))) }

        assertEquals(2, client.requestCount)
    }

    @Test
    fun malformedProviderPayloadDoesNotCreateTrafficSegment() {
        val provider = TomTomTrafficProvider(
            apiKey = "test-key",
            httpClient = FakeClient("{\"flowSegmentData\":{\"currentSpeed\":40}}"),
            nowEpochMs = { 1_000L },
        )

        val snapshot = runSuspend { provider.fetchTraffic(bounds, TrafficRoute(listOf(point))) }

        assertTrue(snapshot.segments.isEmpty())
        assertEquals(TrafficConfidence.LOW, snapshot.confidence)
    }

    private class FakeClient(private val body: String) : TomTomTrafficHttpClient {
        var lastPoint: GeoCoordinate? = null
        var lastApiKey: String? = null
        var requestCount: Int = 0

        override fun fetch(point: GeoCoordinate, apiKey: String, endpoint: String): String {
            requestCount++
            lastPoint = point
            lastApiKey = apiKey
            return body
        }
    }

    private fun <T> runSuspend(block: suspend () -> T): T {
        var result: Result<T>? = null
        block.startCoroutine(object : Continuation<T> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(value: Result<T>) { result = value }
        })
        return result!!.getOrThrow()
    }
}
