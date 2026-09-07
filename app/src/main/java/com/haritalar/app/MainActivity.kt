package com.haritalar.app

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView
import com.haritalar.core.navigation.NavigationProgressEngine
import org.json.JSONObject
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.LineString
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.concurrent.thread
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    companion object {
        private const val LOCATION_REQUEST = 1001
        private const val MAP_STYLE = "https://tiles.openfreemap.org/styles/liberty"
        private const val ROUTE_ENDPOINT = "https://valhalla1.openstreetmap.de/route"
        private const val ROUTE_SOURCE = "haritalar-route-source"
        private const val ROUTE_LAYER = "haritalar-route-layer"
        private const val OFF_ROUTE_METERS = 60.0
        private const val REROUTE_COOLDOWN_MS = 15_000L
    }

    private lateinit var mapView: MapView
    private lateinit var locationManager: LocationManager
    private lateinit var status: TextView
    private lateinit var tts: TextToSpeech

    private var lastLocation: Location? = null
    private var destination: LatLng? = null
    private var routePoints: List<LatLng> = emptyList()
    private var routeCumulativeMeters: List<Double> = emptyList()
    private var routeTotalMeters = 0.0
    private var navigationActive = false
    private var rerouteInFlight = false
    private var lastRerouteAt = 0L
    private val navigationEngine = NavigationProgressEngine()
    private var ttsReady = false

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            lastLocation = location
            val routeDistance = routeDistanceFromLocation(location)
            if (navigationActive && destination != null && routePoints.size >= 2) {
                if (routeDistance == null) {
                    status.text = "GPS zayıf • rota konumu bulunamadı"
                } else {
                    processNavigation(location, routeDistance)
                }
            } else {
                status.text = "GPS aktif • %.5f, %.5f • Hedef için haritaya dokun".format(location.latitude, location.longitude)
            }
            followLocation(location)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(this)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        tts = TextToSpeech(this, this)

        val root = FrameLayout(this)
        mapView = MapView(this)
        mapView.onCreate(savedInstanceState)
        root.addView(mapView, FrameLayout.LayoutParams(-1, -1))

        status = TextView(this).apply {
            text = "Haritalar • GPS bekleniyor"
            textSize = 14f
            setPadding(24, 14, 24, 14)
            setBackgroundColor(0xEEFFFFFF.toInt())
        }
        root.addView(status, FrameLayout.LayoutParams(-2, -2).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            topMargin = 32
        })

        val attribution = TextView(this).apply {
            text = "© OpenStreetMap contributors • OpenFreeMap • Valhalla"
            textSize = 11f
            setPadding(8, 4, 8, 4)
            setBackgroundColor(0xCCFFFFFF.toInt())
        }
        root.addView(attribution, FrameLayout.LayoutParams(-2, -2).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            bottomMargin = 12
            marginEnd = 12
        })

        setContentView(root)

        mapView.getMapAsync { map ->
            map.setStyle(Style.Builder().fromUri(MAP_STYLE)) {
                map.cameraPosition = CameraPosition.Builder()
                    .target(LatLng(41.0082, 28.9784))
                    .zoom(11.5)
                    .build()
                map.addOnMapClickListener { point ->
                    val origin = lastLocation
                    if (origin == null) {
                        status.text = "Önce GPS konumu bekleniyor"
                    } else {
                        destination = point
                        navigationActive = false
                        navigationEngine.reset()
                        status.text = "Rota hesaplanıyor…"
                        requestRoute(origin.latitude, origin.longitude, point.latitude, point.longitude)
                    }
                    true
                }
                status.text = "Haritalar • Harita hazır • Hedef için haritaya dokun"
            }
        }
        requestLocationPermission()
    }

    override fun onInit(statusCode: Int) {
        if (statusCode == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale("tr", "TR"))
            ttsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
        }
    }

    private fun speak(text: String, urgent: Boolean = false) {
        if (!ttsReady || text.isBlank()) return
        tts.speak(text, if (urgent) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD, null, "haritalar-${System.nanoTime()}")
    }

    private fun processNavigation(location: Location, routeDistance: RouteDistance) {
        val offRoute = routeDistance.distanceToRouteMeters > OFF_ROUTE_METERS
        if (offRoute) {
            status.text = "Rotadan sapıldı • yeniden rota aranıyor"
            maybeReroute(location)
            return
        }

        val event = navigationEngine.update(routeDistance.remainingMeters, routeTotalMeters, currentManeuvers)
        when (event) {
            is NavigationProgressEngine.Event.Instruction -> {
                val meters = event.distanceMeters
                val distanceText = if (meters >= 1000) "%.1f kilometre sonra".format(meters / 1000.0) else "%.0f metre sonra".format(meters)
                val message = if (event.immediate) "Şimdi ${event.maneuver.text}" else "$distanceText ${event.maneuver.text}"
                speak(message, event.immediate)
                status.text = "Navigasyon • $message"
            }
            NavigationProgressEngine.Event.Arrived -> {
                navigationActive = false
                speak("Hedefinize ulaştınız.", true)
                status.text = "Hedefe ulaştınız"
            }
            null -> {
                val remaining = routeDistance.remainingMeters
                status.text = "Navigasyon • %.1f km kaldı".format(remaining / 1000.0)
            }
        }
    }

    private var currentManeuvers: List<NavigationProgressEngine.Maneuver> = emptyList()

    private fun maybeReroute(location: Location) {
        val target = destination ?: return
        val now = System.currentTimeMillis()
        if (rerouteInFlight || now - lastRerouteAt < REROUTE_COOLDOWN_MS) return
        rerouteInFlight = true
        lastRerouteAt = now
        requestRoute(
            location.latitude,
            location.longitude,
            target.latitude,
            target.longitude,
            isReroute = true
        )
    }

    private fun requestRoute(fromLat: Double, fromLon: Double, toLat: Double, toLon: Double, isReroute: Boolean = false) {
        thread(name = "route-request") {
            try {
                val payload = JSONObject().apply {
                    put("locations", org.json.JSONArray().apply {
                        put(JSONObject().apply { put("lat", fromLat); put("lon", fromLon) })
                        put(JSONObject().apply { put("lat", toLat); put("lon", toLon) })
                    })
                    put("costing", "auto")
                    put("units", "kilometers")
                    put("directions_options", JSONObject().apply { put("language", "tr-TR") })
                }
                val connection = (URL(ROUTE_ENDPOINT).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 10_000
                    readTimeout = 20_000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("X-Client-Id", "haritalar-open-source-dev")
                }
                connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                val responseCode = connection.responseCode
                val body = (if (responseCode in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (responseCode !in 200..299) throw IllegalStateException("Routing HTTP $responseCode")

                val parsed = parseRoute(JSONObject(body))
                if (parsed.points.size < 2) throw IllegalStateException("Rota geometrisi boş")

                runOnUiThread {
                    rerouteInFlight = false
                    routePoints = parsed.points
                    routeCumulativeMeters = parsed.cumulativeMeters
                    routeTotalMeters = parsed.totalMeters
                    currentManeuvers = parsed.maneuvers
                    navigationEngine.reset()
                    navigationActive = true
                    drawRoute(parsed.points)
                    val prefix = if (isReroute) "Yeni rota" else "Rota hazır"
                    status.text = "$prefix • %.1f km • %.0f dk".format(parsed.distanceKm, parsed.durationSeconds / 60.0)
                    if (isReroute) speak("Yeni rota hesaplandı.", true)
                }
            } catch (error: Exception) {
                runOnUiThread {
                    rerouteInFlight = false
                    status.text = "Rota alınamadı • ${error.message ?: "ağ hatası"}"
                }
            }
        }
    }

    private data class ParsedRoute(
        val points: List<LatLng>,
        val cumulativeMeters: List<Double>,
        val totalMeters: Double,
        val distanceKm: Double,
        val durationSeconds: Double,
        val maneuvers: List<NavigationProgressEngine.Maneuver>
    )

    private fun parseRoute(root: JSONObject): ParsedRoute {
        val trip = root.getJSONObject("trip")
        val summary = trip.getJSONObject("summary")
        val distanceKm = summary.optDouble("length", 0.0)
        val durationSeconds = summary.optDouble("time", 0.0)
        val legs = trip.getJSONArray("legs")
        val allPoints = ArrayList<LatLng>()
        val allCumulative = ArrayList<Double>()
        val maneuvers = ArrayList<NavigationProgressEngine.Maneuver>()
        var cumulative = 0.0
        var globalManeuverIndex = 0

        for (legIndex in 0 until legs.length()) {
            val leg = legs.getJSONObject(legIndex)
            val legPoints = decodePolyline6(leg.getString("shape"))
            if (legPoints.isEmpty()) continue
            val pointOffset = allPoints.size
            for ((localIndex, point) in legPoints.withIndex()) {
                if (allPoints.isNotEmpty() && localIndex == 0) continue
                if (allPoints.isNotEmpty()) cumulative += haversineMeters(allPoints.last(), point)
                allPoints.add(point)
                allCumulative.add(cumulative)
            }

            val legManeuvers = leg.optJSONArray("maneuvers") ?: continue
            for (i in 0 until legManeuvers.length()) {
                val maneuver = legManeuvers.getJSONObject(i)
                val localShapeIndex = maneuver.optInt("begin_shape_index", -1)
                if (localShapeIndex < 0 || legPoints.isEmpty()) continue
                val clampedLocal = min(localShapeIndex, legPoints.lastIndex)
                val globalPointIndex = pointOffset + clampedLocal - if (pointOffset > 0) 1 else 0
                if (globalPointIndex !in allCumulative.indices) continue
                val text = maneuver.optString("verbal_pre_transition_instruction")
                    .ifBlank { maneuver.optString("verbal_transition_alert") }
                    .ifBlank { maneuver.optString("instruction") }
                    .trim()
                if (text.isBlank()) continue
                maneuvers += NavigationProgressEngine.Maneuver(
                    index = globalManeuverIndex++,
                    text = text,
                    distanceFromRouteStartMeters = allCumulative[globalPointIndex]
                )
            }
        }

        return ParsedRoute(
            points = allPoints,
            cumulativeMeters = allCumulative,
            totalMeters = cumulative,
            distanceKm = distanceKm,
            durationSeconds = durationSeconds,
            maneuvers = maneuvers.sortedBy { it.distanceFromRouteStartMeters }
        )
    }

    private data class RouteDistance(val distanceToRouteMeters: Double, val remainingMeters: Double)

    private fun routeDistanceFromLocation(location: Location): RouteDistance? {
        if (routePoints.size < 2 || routeCumulativeMeters.size != routePoints.size) return null
        val user = LatLng(location.latitude, location.longitude)
        var bestDistance = Double.MAX_VALUE
        var bestProgress = 0.0
        for (i in 0 until routePoints.lastIndex) {
            val a = routePoints[i]
            val b = routePoints[i + 1]
            val projection = projectOntoSegment(user, a, b)
            if (projection.distanceMeters < bestDistance) {
                bestDistance = projection.distanceMeters
                bestProgress = routeCumulativeMeters[i] + projection.fraction * haversineMeters(a, b)
            }
        }
        return RouteDistance(bestDistance, max(0.0, routeTotalMeters - bestProgress))
    }

    private data class Projection(val fraction: Double, val distanceMeters: Double)

    private fun projectOntoSegment(p: LatLng, a: LatLng, b: LatLng): Projection {
        val meanLat = Math.toRadians((a.latitude + b.latitude + p.latitude) / 3.0)
        val scaleX = 111_320.0 * cos(meanLat)
        val scaleY = 110_540.0
        val ax = (a.longitude - p.longitude) * scaleX
        val ay = (a.latitude - p.latitude) * scaleY
        val bx = (b.longitude - p.longitude) * scaleX
        val by = (b.latitude - p.latitude) * scaleY
        val dx = bx - ax
        val dy = by - ay
        val denom = dx * dx + dy * dy
        val fraction = if (denom == 0.0) 0.0 else (-ax * dx - ay * dy) / denom
        val t = fraction.coerceIn(0.0, 1.0)
        val cx = ax + t * dx
        val cy = ay + t * dy
        return Projection(t, sqrt(cx * cx + cy * cy))
    }

    private fun haversineMeters(a: LatLng, b: LatLng): Double {
        val earth = 6_371_000.0
        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val h = sin(dLat / 2) * sin(dLat / 2) + cos(lat1) * cos(lat2) * sin(dLon / 2) * sin(dLon / 2)
        return earth * 2 * atan2(sqrt(h), sqrt(max(0.0, 1 - h)))
    }

    private fun followLocation(location: Location) {
        mapView.getMapAsync { map ->
            val bearing = if (location.hasBearing()) location.bearing.toDouble() else map.cameraPosition.bearing
            map.cameraPosition = CameraPosition.Builder(map.cameraPosition)
                .target(LatLng(location.latitude, location.longitude))
                .zoom(if (navigationActive) 17.0 else 16.0)
                .bearing(bearing)
                .build()
        }
    }

    private fun drawRoute(points: List<LatLng>) {
        mapView.getMapAsync { map ->
            val style = map.style ?: return@getMapAsync
            style.removeLayer(ROUTE_LAYER)
            style.removeSource(ROUTE_SOURCE)
            if (points.size < 2) return@getMapAsync
            val coordinates = points.map { org.maplibre.geojson.Point.fromLngLat(it.longitude, it.latitude) }
            val feature = Feature.fromGeometry(LineString.fromLngLats(coordinates))
            style.addSource(GeoJsonSource(ROUTE_SOURCE, feature))
            style.addLayer(LineLayer(ROUTE_LAYER, ROUTE_SOURCE).withProperties(lineWidth(6f)))
        }
    }

    private fun decodePolyline6(encoded: String): List<LatLng> {
        val result = ArrayList<LatLng>()
        var index = 0
        var lat = 0
        var lon = 0
        while (index < encoded.length) {
            var shift = 0
            var value = 0
            while (true) {
                if (index >= encoded.length) return result
                val b = encoded[index++].code - 63
                value = value or ((b and 0x1f) shl shift)
                shift += 5
                if (b < 0x20) break
            }
            lat += if ((value and 1) != 0) -(value shr 1) - 1 else value shr 1
            shift = 0
            value = 0
            while (true) {
                if (index >= encoded.length) return result
                val b = encoded[index++].code - 63
                value = value or ((b and 0x1f) shl shift)
                shift += 5
                if (b < 0x20) break
            }
            lon += if ((value and 1) != 0) -(value shr 1) - 1 else value shr 1
            result.add(LatLng(lat / 1e6, lon / 1e6))
        }
        return result
    }

    private fun requestLocationPermission() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) startLocationUpdates() else requestPermissions(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), LOCATION_REQUEST
        )
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_REQUEST && grantResults.any { it == PackageManager.PERMISSION_GRANTED }) startLocationUpdates()
        else status.text = "GPS izni verilmedi • Harita çevrim içi çalışıyor"
    }

    private fun startLocationUpdates() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 5f, locationListener)
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000L, 10f, locationListener)
            status.text = "GPS etkin • Konum bekleniyor"
        } catch (_: SecurityException) { status.text = "GPS izni alınamadı" }
    }

    private fun stopLocationUpdates() {
        try { locationManager.removeUpdates(locationListener) } catch (_: SecurityException) { }
    }

    override fun onStart() { super.onStart(); mapView.onStart() }
    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { mapView.onPause(); super.onPause() }
    override fun onStop() { stopLocationUpdates(); mapView.onStop(); super.onStop() }
    override fun onLowMemory() { super.onLowMemory(); mapView.onLowMemory() }
    override fun onDestroy() {
        stopLocationUpdates()
        mapView.onDestroy()
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }
}
