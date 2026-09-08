package com.haritalar.app

import android.os.Handler
import android.os.Looper
import com.google.gson.JsonObject
import com.haritalar.core.navigation.NavigationPoi
import com.haritalar.core.navigation.OsmPoiParser
import com.haritalar.core.navigation.OsmPoiQuery
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleOpacity
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.iconAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.textAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.textField
import org.maplibre.android.style.layers.PropertyFactory.textIgnorePlacement
import org.maplibre.android.style.layers.PropertyFactory.textSize
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.Executors

/** Bounded, throttled live OSM POI overlay. No paid provider or API key is required. */
class LiveNavigationPoiLayer {
    companion object {
        const val SOURCE_ID = "haritalar-live-poi-source"
        const val CIRCLE_LAYER_ID = "haritalar-live-poi-circles"
        const val LABEL_LAYER_ID = "haritalar-live-poi-labels"
        private const val DEBOUNCE_MS = 900L
        private const val MIN_REFRESH_MS = 5_000L
        private const val MAX_RESULTS = 300
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()
    private var map: MapLibreMap? = null
    private var refreshRunnable: Runnable? = null
    private var lastRefreshAt = 0L
    private var lastBoundsKey: String? = null

    fun install(map: MapLibreMap, style: Style) {
        this.map = map
        if (style.getSource(SOURCE_ID) == null) {
            style.addSource(GeoJsonSource(SOURCE_ID, FeatureCollection.fromFeatures(emptyArray())))
        }
        if (style.getLayer(CIRCLE_LAYER_ID) == null) {
            style.addLayer(
                CircleLayer(CIRCLE_LAYER_ID, SOURCE_ID).withProperties(
                    circleRadius(6f),
                    circleColor("#1976D2"),
                    circleOpacity(0.9f),
                    circleStrokeColor("#FFFFFF"),
                    circleStrokeWidth(2f),
                ),
            )
        }
        if (style.getLayer(LABEL_LAYER_ID) == null) {
            style.addLayer(
                SymbolLayer(LABEL_LAYER_ID, SOURCE_ID).withProperties(
                    textField(Expression.get("name")),
                    textSize(11f),
                    textAllowOverlap(false),
                    textIgnorePlacement(false),
                    iconAllowOverlap(false),
                ),
            )
        }
    }

    fun scheduleRefresh(bounds: LatLngBounds?) {
        if (bounds == null) return
        val south = bounds.latSouth
        val north = bounds.latNorth
        val west = bounds.lonWest
        val east = bounds.lonEast
        val key = "%.3f,%.3f,%.3f,%.3f".format(south, west, north, east)
        if (key == lastBoundsKey) return
        lastBoundsKey = key
        refreshRunnable?.let(mainHandler::removeCallbacks)
        val runnable = Runnable { refresh(south, west, north, east) }
        refreshRunnable = runnable
        mainHandler.postDelayed(runnable, DEBOUNCE_MS)
    }

    private fun refresh(south: Double, west: Double, north: Double, east: Double) {
        val now = System.currentTimeMillis()
        if (now - lastRefreshAt < MIN_REFRESH_MS) return
        lastRefreshAt = now
        executor.execute {
            try {
                val query = OsmPoiQuery.build(south, west, north, east, MAX_RESULTS)
                val connection = (URL(OsmPoiQuery.endpoint()).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 8_000
                    readTimeout = 15_000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                    setRequestProperty("User-Agent", "Haritalar/1.0 (open-source navigation app)")
                }
                connection.outputStream.use {
                    it.write(("data=" + URLEncoder.encode(query, "UTF-8")).toByteArray(Charsets.UTF_8))
                }
                val code = connection.responseCode
                if (code !in 200..299) error("Overpass HTTP $code")
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                val pois = OsmPoiParser.parse(body, MAX_RESULTS)
                mainHandler.post { updateSource(pois) }
            } catch (_: Exception) {
                // Keep the last successful POI set on transient network/provider errors.
            }
        }
    }

    private fun updateSource(pois: List<NavigationPoi>) {
        val style = map?.style ?: return
        val source = style.getSource(SOURCE_ID) as? GeoJsonSource ?: return
        val features = pois.map { poi ->
            val properties = JsonObject().apply {
                addProperty("name", poi.name)
                addProperty("category", poi.category.name)
                addProperty("address", poi.address ?: "")
            }
            Feature.fromGeometry(Point.fromLngLat(poi.longitude, poi.latitude), properties)
        }
        source.setGeoJson(FeatureCollection.fromFeatures(features.toTypedArray()))
    }

    fun destroy() {
        refreshRunnable?.let(mainHandler::removeCallbacks)
        executor.shutdownNow()
        map = null
    }
}
