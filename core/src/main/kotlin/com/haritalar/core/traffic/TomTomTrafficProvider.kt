package com.haritalar.core.traffic

import com.haritalar.core.navigation.GeoCoordinate
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Real TomTom Traffic Flow Segment Data adapter.
 *
 * The provider is deliberately inert when no API key is supplied. It never
 * fabricates traffic and it never scrapes the TomTom web site. Route sampling
 * is bounded so a GPS update cannot turn into an unbounded request burst.
 */
class TomTomTrafficProvider(
    private val apiKey: String,
    private val httpClient: TomTomTrafficHttpClient = UrlConnectionTomTomTrafficHttpClient(),
    private val nowEpochMs: () -> Long = { System.currentTimeMillis() },
    private val maxSamples: Int = DEFAULT_MAX_SAMPLES,
) : TrafficProvider {
    companion object {
        const val ID = "tomtom-flow"
        const val PRIORITY = 100
        const val DEFAULT_MAX_SAMPLES = 8
        private const val DEFAULT_TTL_MS = 60_000L
        private const val FLOW_ENDPOINT = "https://api.tomtom.com/traffic/services/4/flowSegmentData/absolute/10/json"

        private fun validCoordinate(coordinate: GeoCoordinate): Boolean =
            coordinate.latitude.isFinite() && coordinate.longitude.isFinite() &&
                coordinate.latitude in -90.0..90.0 && coordinate.longitude in -180.0..180.0
    }

    override val id: String = ID
    override val priority: Int = PRIORITY

    override fun supports(coordinate: GeoCoordinate): Boolean =
        apiKey.isNotBlank() && validCoordinate(coordinate)

    override suspend fun fetchTraffic(bounds: TrafficBounds, route: TrafficRoute?): TrafficSnapshot {
        require(apiKey.isNotBlank()) { "TomTom API key is required" }

        val points = (route?.coordinates.orEmpty().ifEmpty {
            listOf(
                GeoCoordinate(
                    latitude = (bounds.south + bounds.north) / 2.0,
                    longitude = (bounds.west + bounds.east) / 2.0,
                ),
            )
        }).filter(::validCoordinate).let(::samplePoints)

        require(points.isNotEmpty()) { "No valid traffic sample coordinates" }

        val fetchedAt = nowEpochMs()
        require(fetchedAt > 0L) { "Invalid observation timestamp" }
        val segments = points.mapNotNull { point ->
            runCatching { httpClient.fetch(point, apiKey, FLOW_ENDPOINT) }
                .getOrNull()
                ?.let { parseSegment(it, point) }
        }

        return TrafficSnapshot(
            providerId = id,
            segments = segments,
            fetchedAtEpochMs = fetchedAt,
            expiresAtEpochMs = fetchedAt + DEFAULT_TTL_MS,
            confidence = if (segments.isNotEmpty()) TrafficConfidence.HIGH else TrafficConfidence.LOW,
        )
    }

    private fun samplePoints(points: List<GeoCoordinate>): List<GeoCoordinate> {
        if (points.size <= maxSamples) return points.distinct()
        val count = maxSamples.coerceAtLeast(1)
        return (0 until count).map { index ->
            val sourceIndex = index * points.lastIndex / (count - 1).coerceAtLeast(1)
            points[sourceIndex]
        }.distinct()
    }

    private fun parseSegment(body: String, requestedPoint: GeoCoordinate): TrafficSegment? {
        val flow = JSONObject(body).optJSONObject("flowSegmentData") ?: return null
        val currentSpeed = flow.optDouble("currentSpeed", Double.NaN).takeIf { it.isFinite() && it >= 0.0 }
        val freeFlow = flow.optDouble("freeFlowSpeed", Double.NaN).takeIf { it.isFinite() && it > 0.0 }
        val geometry = flow.optJSONObject("coordinates")?.optJSONArray("coordinate")?.let { coordinates ->
            buildList {
                for (index in 0 until coordinates.length()) {
                    val coordinate = coordinates.optJSONObject(index) ?: continue
                    val lat = coordinate.optDouble("latitude", Double.NaN)
                    val lon = coordinate.optDouble("longitude", Double.NaN)
                    if (lat.isFinite() && lon.isFinite() && lat in -90.0..90.0 && lon in -180.0..180.0) {
                        add(GeoCoordinate(lat, lon))
                    }
                }
            }
        }.orEmpty()

        if (currentSpeed == null || freeFlow == null || geometry.size < 2) return null
        val id = flow.optString("frc").ifBlank {
            "${requestedPoint.latitude},${requestedPoint.longitude}"
        }
        val congestion = when {
            currentSpeed >= freeFlow * 0.9 -> TrafficCongestion.FREE
            currentSpeed >= freeFlow * 0.7 -> TrafficCongestion.LIGHT
            currentSpeed >= freeFlow * 0.5 -> TrafficCongestion.MODERATE
            currentSpeed >= freeFlow * 0.3 -> TrafficCongestion.HEAVY
            else -> TrafficCongestion.SEVERE
        }
        return TrafficSegment(
            id = id,
            speedKmh = currentSpeed,
            freeFlowSpeedKmh = freeFlow,
            congestion = congestion,
            confidence = TrafficConfidence.HIGH,
            geometry = geometry,
        )
    }
}

interface TomTomTrafficHttpClient {
    fun fetch(point: GeoCoordinate, apiKey: String, endpoint: String): String
}

class UrlConnectionTomTomTrafficHttpClient : TomTomTrafficHttpClient {
    override fun fetch(point: GeoCoordinate, apiKey: String, endpoint: String): String {
        val query = "point=${point.latitude},${point.longitude}&unit=KMPH&key=${URLEncoder.encode(apiKey, Charsets.UTF_8.name())}"
        val connection = (URL("$endpoint?$query").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8_000
            readTimeout = 12_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "Haritalar-Android-Traffic/1.0")
        }
        return try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) error("TomTom Flow HTTP $code")
            body
        } finally {
            connection.disconnect()
        }
    }
}
