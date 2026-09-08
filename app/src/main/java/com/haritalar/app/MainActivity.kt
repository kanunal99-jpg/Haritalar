package com.haritalar.app

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.haritalar.core.navigation.GeoCoordinate
import com.haritalar.core.navigation.KgmTollEstimator
import com.haritalar.core.navigation.Navigation3dCameraPolicy
import com.haritalar.core.navigation.NavigationPoiDetails
import com.haritalar.core.navigation.NavigationProgressEngine
import com.haritalar.core.navigation.TollBridge
import org.json.JSONArray
import org.json.JSONObject
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponent
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.LocationComponentOptions
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.LineString
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Collections
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.concurrent.thread
import kotlin.math.*

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    companion object {
        private const val LOCATION_REQUEST = 1001
        private const val MAP_STYLE = "https://tiles.openfreemap.org/styles/liberty"
        private const val ROUTE_ENDPOINT = "https://valhalla1.openstreetmap.de/route"
        private const val GEOCODE_ENDPOINT = "https://nominatim.openstreetmap.org/search"
        private const val ROUTE_SOURCE = "haritalar-route-source"
        private const val ROUTE_LAYER = "haritalar-route-layer"
        private const val OFF_ROUTE_METERS = 60.0
        private const val REROUTE_COOLDOWN_MS = 15_000L
        private const val START_TTS = "LANU iyi yolculuklar diler. Emniyet kemerinizi, aynalarınızı ve lastiklerinizi kontrol ediniz. Güvenli yolculuklar."
    }

    private lateinit var mapView: MapView
    private lateinit var locationManager: LocationManager
    private lateinit var status: TextView
    private lateinit var tts: TextToSpeech
    private lateinit var searchInput: EditText
    private lateinit var searchResults: LinearLayout
    private lateinit var routePanel: LinearLayout
    private lateinit var poiPanel: LinearLayout
    private lateinit var navigationButton: Button
    private var locationComponent: LocationComponent? = null
    private var livePoiLayer: LiveNavigationPoiLayer? = null
    private val routeExecutor = Executors.newFixedThreadPool(4)
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
    private var currentManeuvers: List<NavigationProgressEngine.Maneuver> = emptyList()
    private var routeGeneration = 0

    private data class SearchPlace(val name: String, val lat: Double, val lon: Double)
    private data class RouteOption(
        val title: String,
        val subtitle: String,
        val points: List<LatLng>,
        val cumulativeMeters: List<Double>,
        val totalMeters: Double,
        val distanceKm: Double,
        val durationSeconds: Double,
        val toll: Boolean,
        val tollAmountTry: Double?,
        val tollName: String?,
        val maneuvers: List<NavigationProgressEngine.Maneuver>,
    )

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            lastLocation = location
            locationComponent?.forceLocationUpdate(location)
            val distance = routeDistanceFromLocation(location)
            if (navigationActive && destination != null && routePoints.size >= 2) {
                if (distance == null) status.text = "GPS zayıf • rota konumu bulunamadı"
                else processNavigation(distance)
            } else {
                status.text = "GPS aktif • %.5f, %.5f".format(location.latitude, location.longitude)
            }
            if (navigationActive) followLocation(location)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        MapLibre.getInstance(this)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        tts = TextToSpeech(this, this)

        val root = FrameLayout(this)
        mapView = MapView(this)
        mapView.onCreate(savedInstanceState)
        root.addView(mapView, FrameLayout.LayoutParams(-1, -1))

        val controls = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val searchPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 14, 16, 12)
            background = rounded(0xF7FFFFFF.toInt(), 24f)
            elevation = 8f
        }
        val searchRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        searchInput = EditText(this).apply {
            hint = "Nereye gitmek istiyorsun?"
            singleLine = true
            textSize = 16f
            minHeight = 62
            setPadding(18, 0, 14, 0)
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            background = rounded(0xFFF2F4F7.toInt(), 18f)
        }
        searchRow.addView(searchInput, LinearLayout.LayoutParams(0, 62, 1f))
        searchRow.addView(makeActionButton("Ara") { searchAddress(searchInput.text.toString()) }, LinearLayout.LayoutParams(82, 62).apply { leftMargin = 8 })
        searchPanel.addView(searchRow)
        searchResults = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 8, 0, 0) }
        searchPanel.addView(searchResults)
        controls.addView(searchPanel, LinearLayout.LayoutParams(-1, -2))

        val routeScroll = ScrollView(this).apply { isFillViewport = true; overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS }
        routePanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14, 14, 14, 16)
            background = rounded(0xFAFFFFFF.toInt(), 24f)
            elevation = 10f
        }
        routeScroll.addView(routePanel)
        controls.addView(routeScroll, LinearLayout.LayoutParams(-1, 0, 1f).apply { topMargin = 10 })
        root.addView(controls, FrameLayout.LayoutParams(-1, -1).apply {
            gravity = Gravity.TOP
            topMargin = 34
            leftMargin = 12
            rightMargin = 12
            bottomMargin = 12
        })

        status = TextView(this).apply {
            text = "Haritalar • GPS bekleniyor"
            textSize = 13f
            setPadding(18, 10, 18, 10)
            background = rounded(0xEEFFFFFF.toInt(), 18f)
            elevation = 6f
        }
        root.addView(status, FrameLayout.LayoutParams(-2, -2).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            topMargin = 106
        })

        poiPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 14, 18, 14)
            background = rounded(0xFCFFFFFF.toInt(), 24f)
            elevation = 14f
            visibility = View.GONE
        }
        root.addView(poiPanel, FrameLayout.LayoutParams(-1, -2).apply {
            gravity = Gravity.BOTTOM
            leftMargin = 12
            rightMargin = 12
            bottomMargin = 218
        })

        navigationButton = makeActionButton("Rota Bitir") { stopNavigation() }.apply { visibility = View.GONE }
        root.addView(navigationButton, FrameLayout.LayoutParams(-2, 60).apply {
            gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
            bottomMargin = 150
        })
        root.addView(TextView(this).apply {
            text = "© OpenStreetMap • OpenFreeMap • Valhalla"
            textSize = 10f
            setPadding(8, 4, 8, 4)
            background = rounded(0xCCFFFFFF.toInt(), 10f)
        }, FrameLayout.LayoutParams(-2, -2).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            bottomMargin = 8
            marginEnd = 12
        })
        setContentView(root)
        routePanel.visibility = View.GONE

        searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchAddress(searchInput.text.toString())
                true
            } else false
        }

        mapView.getMapAsync { map ->
            map.setStyle(Style.Builder().fromUri(MAP_STYLE)) { style ->
                map.cameraPosition = CameraPosition.Builder()
                    .target(LatLng(41.0082, 28.9784))
                    .zoom(11.5)
                    .build()
                ThreeDNavigationLayer.install(style)
                livePoiLayer = LiveNavigationPoiLayer { details -> showPoiDetails(details) }
                    .also { it.install(map, style) }
                activateLocationComponent(map, style)
                map.addOnCameraIdleListener {
                    livePoiLayer?.scheduleRefresh(map.projection.visibleRegion.latLngBounds)
                }
                map.addOnMapClickListener { point ->
                    if (lastLocation == null) status.text = "Önce GPS konumu bekleniyor"
                    else selectDestination(point)
                    true
                }
                status.text = "Harita hazır • adres ara veya haritaya dokun"
                livePoiLayer?.scheduleRefresh(map.projection.visibleRegion.latLngBounds)
            }
        }
        requestLocationPermission()
    }

    private fun activateLocationComponent(map: MapLibreMap, style: Style) {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) return
        if (locationComponent?.isLocationComponentActivated == true) return
        val options = LocationComponentOptions.builder(this).pulseEnabled(true).trackingGesturesManagement(true).build()
        val activation = LocationComponentActivationOptions.builder(this, style)
            .locationComponentOptions(options)
            .useDefaultLocationEngine(false)
            .build()
        locationComponent = map.locationComponent
        locationComponent!!.activateLocationComponent(activation)
        locationComponent!!.isLocationComponentEnabled = true
        NavigationLocationComponentController.apply(locationComponent!!, false)
        lastLocation?.let { locationComponent!!.forceLocationUpdate(it) }
    }

    private fun makeActionButton(label: String, onClick: () -> Unit) = Button(this).apply {
        text = label
        setAllCaps(false)
        textSize = 14f
        minHeight = 56
        gravity = Gravity.CENTER
        setPadding(8, 0, 8, 0)
        background = rounded(0xFF1976D2.toInt(), 18f)
        setTextColor(0xFFFFFFFF.toInt())
        elevation = 2f
        setOnClickListener { onClick() }
    }

    private fun rounded(color: Int, radius: Float) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius
    }

    private fun showPoiDetails(details: NavigationPoiDetails) {
        if (!::poiPanel.isInitialized) return
        poiPanel.removeAllViews()
        poiPanel.visibility = View.VISIBLE

        poiPanel.addView(TextView(this).apply {
            text = details.title
            textSize = 19f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 4)
        })
        poiPanel.addView(TextView(this).apply {
            text = details.categoryLabel
            textSize = 12f
            setPadding(0, 0, 0, 6)
        })
        details.addressLabel?.let { address ->
            poiPanel.addView(TextView(this).apply {
                text = address
                textSize = 13f
                setPadding(0, 0, 0, 4)
            })
        }
        details.openingHoursLabel?.let { hours ->
            poiPanel.addView(TextView(this).apply {
                text = "Saatler: $hours"
                textSize = 12f
                setPadding(0, 0, 0, 8)
            })
        }

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        actions.addView(makeActionButton("Buraya git") {
            val target = LatLng(details.poi.latitude, details.poi.longitude)
            searchInput.setText(details.title)
            poiPanel.visibility = View.GONE
            livePoiLayer?.clearSelection()
            mapView.getMapAsync { map ->
                map.cameraPosition = CameraPosition.Builder(map.cameraPosition)
                    .target(target)
                    .zoom(max(map.cameraPosition.zoom, 15.5))
                    .build()
            }
            selectDestination(target)
        }, LinearLayout.LayoutParams(0, 58, 1f))
        actions.addView(makeActionButton("Kapat") {
            poiPanel.visibility = View.GONE
            livePoiLayer?.clearSelection()
        }, LinearLayout.LayoutParams(0, 58, 1f).apply { leftMargin = 8 })
        poiPanel.addView(actions)

        Log.i("MainActivity", "POI hedef kartı açıldı: ${details.title} (${details.poi.latitude}, ${details.poi.longitude})")
    }

    private fun selectDestination(point: LatLng) {
        val origin = lastLocation
        if (origin == null) {
            status.text = "GPS konumu bekleniyor"
            return
        }
        poiPanel.visibility = View.GONE
        destination = point
        navigationActive = false
        locationComponent?.let { NavigationLocationComponentController.apply(it, false) }
        navigationEngine.reset()
        routePanel.visibility = View.VISIBLE
        routePanel.removeAllViews()
        routePanel.addView(TextView(this).apply { text = "Rota seçenekleri"; textSize = 21f; setPadding(4, 2, 4, 8) })
        routePanel.addView(TextView(this).apply { text = "Hız, mesafe ve ücret durumu karşılaştırılıyor…"; textSize = 13f })
        status.text = "Çoklu rota hesaplanıyor…"
        requestRouteOptions(origin, point)
    }

    private fun searchAddress(query: String) {
        val clean = query.trim()
        if (clean.length < 3) { status.text = "En az 3 karakter girin"; return }
        status.text = "Adres aranıyor…"
        searchResults.removeAllViews()
        thread(name = "geocoder") {
            try {
                val encoded = URLEncoder.encode(clean, "UTF-8")
                val connection = (URL("$GEOCODE_ENDPOINT?format=jsonv2&addressdetails=1&limit=5&countrycodes=tr&accept-language=tr&q=$encoded").openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 8_000
                    readTimeout = 12_000
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "Haritalar/1.0 (open-source navigation app)")
                }
                val code = connection.responseCode
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                if (code !in 200..299) error("Arama HTTP $code")
                val array = JSONArray(body)
                val places = (0 until array.length()).map { i ->
                    val item = array.getJSONObject(i)
                    SearchPlace(item.optString("display_name"), item.getDouble("lat"), item.getDouble("lon"))
                }
                runOnUiThread {
                    searchResults.removeAllViews()
                    if (places.isEmpty()) {
                        searchResults.addView(TextView(this).apply { text = "Sonuç bulunamadı"; setPadding(8, 8, 8, 8) })
                    } else places.forEach { place ->
                        searchResults.addView(Button(this).apply {
                            text = place.name
                            textSize = 12f
                            setAllCaps(false)
                            gravity = Gravity.START or Gravity.CENTER_VERTICAL
                            minHeight = 56
                            setPadding(16, 6, 16, 6)
                            background = rounded(0xFFF6F8FA.toInt(), 16f)
                            setOnClickListener {
                                val target = LatLng(place.lat, place.lon)
                                searchInput.setText(place.name)
                                searchResults.removeAllViews()
                                mapView.getMapAsync { map ->
                                    map.cameraPosition = CameraPosition.Builder(map.cameraPosition).target(target).zoom(16.0).build()
                                }
                                selectDestination(target)
                            }
                        })
                    }
                    status.text = "${places.size} adres/yer bulundu"
                }
            } catch (e: Exception) {
                runOnUiThread { status.text = "Adres araması başarısız • ${e.message ?: "ağ hatası"}" }
            }
        }
    }

    override fun onInit(statusCode: Int) {
        if (statusCode == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale("tr", "TR"))
            ttsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
        }
    }

    private fun speak(text: String, urgent: Boolean = false) {
        if (ttsReady && text.isNotBlank()) {
            tts.speak(text, if (urgent) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD, null, "haritalar-${System.nanoTime()}")
        }
    }

    private fun processNavigation(distance: RouteDistance) {
        if (distance.distanceToRouteMeters > OFF_ROUTE_METERS) {
            status.text = "Rotadan sapıldı • yeniden rota aranıyor"
            lastLocation?.let { maybeReroute(it) }
            return
        }
        when (val event = navigationEngine.update(distance.remainingMeters, routeTotalMeters, currentManeuvers)) {
            is NavigationProgressEngine.Event.Instruction -> {
                val meters = event.distanceMeters
                val prefix = if (meters >= 1000) "%.1f kilometre sonra".format(meters / 1000.0) else "%.0f metre sonra".format(meters)
                val message = if (event.immediate) "Şimdi ${event.maneuver.text}" else "$prefix ${event.maneuver.text}"
                speak(message, event.immediate)
                status.text = "Navigasyon • $message"
            }
            NavigationProgressEngine.Event.Arrived -> {
                navigationActive = false
                locationComponent?.let { NavigationLocationComponentController.apply(it, false) }
                navigationButton.visibility = View.GONE
                speak("Hedefinize ulaştınız.", true)
                status.text = "Hedefe ulaştınız"
            }
            null -> status.text = "Navigasyon • %.1f km kaldı".format(distance.remainingMeters / 1000.0)
        }
    }

    private fun maybeReroute(location: Location) {
        val target = destination ?: return
        val now = System.currentTimeMillis()
        if (rerouteInFlight || now - lastRerouteAt < REROUTE_COOLDOWN_MS) return
        rerouteInFlight = true
        lastRerouteAt = now
        requestSingleRoute(location.latitude, location.longitude, target.latitude, target.longitude, "Yeniden rota", "auto") { option ->
            rerouteInFlight = false
            applyRoute(option, true)
        }
    }

    private fun requestRouteOptions(origin: Location, target: LatLng) {
        val generation = ++routeGeneration
        val specs = listOf(
            Triple("En hızlı", "Hızlı rota • ücretli yollar kullanılabilir", "auto"),
            Triple("En kısa", "Mesafeyi azaltır", "auto_shorter"),
            Triple("Ücretsiz öncelikli", "Ücretli yollardan kaçınmayı dener", "no_toll"),
            Triple("Ücretli hızlı", "Ücretli yolları tercih eder", "toll_fast"),
        )
        val results = Collections.synchronizedList(mutableListOf<RouteOption>())
        specs.forEach { spec ->
            routeExecutor.execute {
                try {
                    val option = fetchRoute(origin.latitude, origin.longitude, target.latitude, target.longitude, spec.first, spec.second, spec.third)
                    results.add(option)
                    runOnUiThread { if (generation == routeGeneration) renderRouteOptions(results.toList()) }
                } catch (e: Exception) {
                    runOnUiThread {
                        if (generation == routeGeneration && results.isEmpty()) status.text = "Rota hazırlanamadı • ${e.message ?: "ağ hatası"}"
                    }
                }
            }
        }
    }

    private fun requestSingleRoute(fromLat: Double, fromLon: Double, toLat: Double, toLon: Double, title: String, mode: String, onSuccess: (RouteOption) -> Unit) {
        thread(name = "route-request") {
            try {
                val option = fetchRoute(fromLat, fromLon, toLat, toLon, title, "", mode)
                runOnUiThread { onSuccess(option) }
            } catch (e: Exception) {
                runOnUiThread { status.text = "Rota alınamadı • ${e.message ?: "ağ hatası"}"; rerouteInFlight = false }
            }
        }
    }

    private fun fetchRoute(fromLat: Double, fromLon: Double, toLat: Double, toLon: Double, title: String, subtitle: String, mode: String): RouteOption {
        val autoOptions = JSONObject()
        when (mode) {
            "no_toll" -> autoOptions.put("use_tolls", 0.0).put("toll_booth_penalty", 3600.0)
            "toll_fast" -> autoOptions.put("use_tolls", 1.0)
        }
        val payload = JSONObject().apply {
            put("locations", JSONArray().apply {
                put(JSONObject().apply { put("lat", fromLat); put("lon", fromLon); put("type", "break") })
                put(JSONObject().apply { put("lat", toLat); put("lon", toLon); put("type", "break") })
            })
            put("costing", if (mode == "auto_shorter") "auto_shorter" else "auto")
            if (autoOptions.length() > 0) put("costing_options", JSONObject().put("auto", autoOptions))
            put("units", "kilometers")
            put("directions_options", JSONObject().put("language", "tr-TR"))
        }
        val connection = (URL(ROUTE_ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout = 20_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
        }
        connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
        val code = connection.responseCode
        val body = (if (code in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (code !in 200..299) error("Routing HTTP $code")
        return parseRoute(JSONObject(body), title, subtitle)
    }

    private fun renderRouteOptions(options: List<RouteOption>) {
        routePanel.removeAllViews()
        routePanel.addView(TextView(this).apply { text = "Rota seçenekleri"; textSize = 21f; setPadding(4, 0, 4, 8) })
        options.sortedWith(compareBy<RouteOption> { it.durationSeconds }.thenBy { it.distanceKm }).forEachIndexed { index, option ->
            val tollText = when {
                !option.toll -> "Ücretsiz • ücretli geçiş tespit edilmedi"
                option.tollAmountTry != null -> "Ücretli • %.0f TL".format(option.tollAmountTry) + (option.tollName?.let { " • $it" } ?: "")
                else -> "Ücretli • tutar doğrulanamadı"
            }
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(18, 12, 18, 12)
                background = rounded(if (index == 0) 0xFFEAF3FF.toInt() else 0xFFF6F8FA.toInt(), 18f)
                setOnClickListener { applyRoute(option, false) }
            }
            card.addView(TextView(this).apply { text = if (index == 0) "ÖNERİLEN • ${option.title}" else option.title; textSize = 16f; setTypeface(typeface, android.graphics.Typeface.BOLD) })
            card.addView(TextView(this).apply { text = "%.1f km • %.0f dk".format(option.distanceKm, option.durationSeconds / 60.0); textSize = 14f; setPadding(0, 4, 0, 2) })
            card.addView(TextView(this).apply { text = tollText; textSize = 12f })
            routePanel.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 8 })
        }
        status.text = "${options.size} rota seçeneği hazır"
    }

    private fun applyRoute(option: RouteOption, isReroute: Boolean) {
        routePoints = option.points
        routeCumulativeMeters = option.cumulativeMeters
        routeTotalMeters = option.totalMeters
        currentManeuvers = option.maneuvers
        navigationEngine.reset()
        drawRoute(option.points)
        if (isReroute) {
            navigationActive = true
            locationComponent?.let { NavigationLocationComponentController.apply(it, true) }
            navigationButton.visibility = View.VISIBLE
            routePanel.visibility = View.GONE
            applyNavigationCamera()
            status.text = "Yeni rota aktif • %.1f km • %.0f dk".format(option.distanceKm, option.durationSeconds / 60.0)
            speak("Yeni rota hesaplandı.", true)
        } else {
            navigationActive = false
            locationComponent?.let { NavigationLocationComponentController.apply(it, false) }
            navigationButton.visibility = View.GONE
            routePanel.visibility = View.VISIBLE
            routePanel.removeAllViews()
            routePanel.addView(TextView(this).apply { text = "Rota hazır"; textSize = 21f; setPadding(4, 2, 4, 6) })
            routePanel.addView(TextView(this).apply { text = "${option.title} • %.1f km • %.0f dk".format(option.distanceKm, option.durationSeconds / 60.0); textSize = 15f; setPadding(4, 0, 4, 12) })
            routePanel.addView(TextView(this).apply { text = option.subtitle.ifBlank { "Seçilen rota haritada gösteriliyor." }; textSize = 12f; setPadding(4, 0, 4, 12) })
            routePanel.addView(makeActionButton("Rotaya Başla") { startNavigation() }, LinearLayout.LayoutParams(-1, 60))
            routePanel.addView(makeActionButton("Başka rota seç") {
                navigationActive = false
                locationComponent?.let { NavigationLocationComponentController.apply(it, false) }
                routePanel.visibility = View.VISIBLE
                status.text = "Rota seçeneklerinden birini seçin"
                val origin = lastLocation
                val target = destination
                if (origin != null && target != null) requestRouteOptions(origin, target)
            }, LinearLayout.LayoutParams(-1, 60).apply { topMargin = 8 })
            status.text = "Rota hazır • Başla ile navigasyonu başlat"
        }
    }

    private fun startNavigation() {
        if (routePoints.size < 2 || destination == null) { status.text = "Önce geçerli bir rota seçin"; return }
        if (lastLocation == null) { status.text = "GPS konumu bekleniyor"; return }
        navigationActive = true
        navigationEngine.reset()
        locationComponent?.let { NavigationLocationComponentController.apply(it, true) }
        navigationButton.visibility = View.VISIBLE
        routePanel.visibility = View.GONE
        applyNavigationCamera()
        followLocation(lastLocation!!)
        speak(START_TTS, true)
        status.text = "Navigasyon başladı • rota takip ediliyor"
    }

    private fun applyNavigationCamera() {
        val state = Navigation3dCameraPolicy.forNavigation()
        mapView.getMapAsync { map ->
            val current = map.cameraPosition
            map.cameraPosition = CameraPosition.Builder(current)
                .tilt(state.pitchDegrees)
                .zoom(max(current.zoom, state.minimumZoom))
                .build()
        }
    }

    private fun stopNavigation() {
        navigationActive = false
        rerouteInFlight = false
        locationComponent?.let { NavigationLocationComponentController.apply(it, false) }
        navigationButton.visibility = View.GONE
        mapView.getMapAsync { map ->
            val state = Navigation3dCameraPolicy.forBrowse()
            map.cameraPosition = CameraPosition.Builder(map.cameraPosition).tilt(state.pitchDegrees).build()
        }
        status.text = "Navigasyon durduruldu • harita serbest"
        speak("Navigasyon durduruldu.", true)
    }

    private data class RouteDistance(val distanceToRouteMeters: Double, val remainingMeters: Double)

    private fun routeDistanceFromLocation(location: Location): RouteDistance? {
        if (routePoints.size < 2 || routeCumulativeMeters.size != routePoints.size) return null
        val user = LatLng(location.latitude, location.longitude)
        var best = Double.MAX_VALUE
        var progress = 0.0
        for (i in 0 until routePoints.lastIndex) {
            val a = routePoints[i]
            val b = routePoints[i + 1]
            val projection = projectOntoSegment(user, a, b)
            if (projection.distanceMeters < best) {
                best = projection.distanceMeters
                progress = routeCumulativeMeters[i] + projection.fraction * haversineMeters(a, b)
            }
        }
        return RouteDistance(best, max(0.0, routeTotalMeters - progress))
    }

    private data class Projection(val fraction: Double, val distanceMeters: Double)

    private fun projectOntoSegment(p: LatLng, a: LatLng, b: LatLng): Projection {
        val meanLat = Math.toRadians((a.latitude + b.latitude + p.latitude) / 3.0)
        val sx = 111320.0 * cos(meanLat)
        val sy = 110540.0
        val ax = (a.longitude - p.longitude) * sx
        val ay = (a.latitude - p.latitude) * sy
        val bx = (b.longitude - p.longitude) * sx
        val by = (b.latitude - p.latitude) * sy
        val dx = bx - ax
        val dy = by - ay
        val denominator = dx * dx + dy * dy
        val t = (if (denominator == 0.0) 0.0 else (-ax * dx - ay * dy) / denominator).coerceIn(0.0, 1.0)
        val cx = ax + t * dx
        val cy = ay + t * dy
        return Projection(t, sqrt(cx * cx + cy * cy))
    }

    private fun haversineMeters(a: LatLng, b: LatLng): Double {
        val earth = 6371000.0
        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val h = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
        return earth * 2 * atan2(sqrt(h), sqrt(max(0.0, 1 - h)))
    }

    private fun parseRoute(root: JSONObject, title: String, subtitle: String): RouteOption {
        val trip = root.getJSONObject("trip")
        val summary = trip.getJSONObject("summary")
        val legs = trip.getJSONArray("legs")
        val points = ArrayList<LatLng>()
        val cumulative = ArrayList<Double>()
        val maneuvers = ArrayList<NavigationProgressEngine.Maneuver>()
        var distance = 0.0
        var globalIndex = 0
        var toll = false
        for (legIndex in 0 until legs.length()) {
            val leg = legs.getJSONObject(legIndex)
            val legPoints = decodePolyline6(leg.getString("shape"))
            val offset = points.size
            for ((local, point) in legPoints.withIndex()) {
                if (points.isNotEmpty() && local == 0) continue
                if (points.isNotEmpty()) distance += haversineMeters(points.last(), point)
                points.add(point)
                cumulative.add(distance)
            }
            val maneuverArray = leg.optJSONArray("maneuvers") ?: continue
            for (i in 0 until maneuverArray.length()) {
                val maneuver = maneuverArray.getJSONObject(i)
                toll = toll || maneuver.optBoolean("toll", false)
                val localIndex = maneuver.optInt("begin_shape_index", -1)
                if (localIndex < 0 || legPoints.isEmpty()) continue
                val globalPoint = offset + min(localIndex, legPoints.lastIndex) - if (offset > 0) 1 else 0
                if (globalPoint !in cumulative.indices) continue
                val text = maneuver.optString("verbal_pre_transition_instruction")
                    .ifBlank { maneuver.optString("verbal_transition_alert_instruction") }
                    .ifBlank { maneuver.optString("instruction") }
                    .trim()
                if (text.isNotBlank()) {
                    maneuvers += NavigationProgressEngine.Maneuver(globalIndex++, text, cumulative[globalPoint])
                }
            }
        }
        val coords = points.map { GeoCoordinate(it.latitude, it.longitude) }
        val bridge = KgmTollEstimator.detectBridge(coords)
        val estimate = KgmTollEstimator.estimate(coords, 1)
        val bridgeName = when (bridge) {
            TollBridge.FSM_15_TEMMUZ -> "15 Temmuz Şehitler / FSM"
            TollBridge.OSMANGAZI -> "Osmangazi"
            TollBridge.YAVUZ_SULTAN_SELIM -> "Yavuz Sultan Selim"
            null -> null
        }
        return RouteOption(
            title = title,
            subtitle = subtitle,
            points = points,
            cumulativeMeters = cumulative,
            totalMeters = distance,
            distanceKm = summary.optDouble("length", distance / 1000.0),
            durationSeconds = summary.optDouble("time", 0.0),
            toll = toll || estimate.hasToll,
            tollAmountTry = estimate.amountTry,
            tollName = bridgeName,
            maneuvers = maneuvers.sortedBy { it.distanceFromRouteStartMeters },
        )
    }

    private fun followLocation(location: Location) {
        if (!navigationActive) return
        mapView.getMapAsync { map ->
            val current = map.cameraPosition
            val bearing = if (location.hasBearing()) location.bearing.toDouble() else current.bearing
            val state = Navigation3dCameraPolicy.forNavigation()
            map.cameraPosition = CameraPosition.Builder(current)
                .target(LatLng(location.latitude, location.longitude))
                .zoom(max(current.zoom, state.minimumZoom))
                .bearing(bearing)
                .tilt(state.pitchDegrees)
                .build()
        }
    }

    private fun drawRoute(points: List<LatLng>) {
        mapView.getMapAsync { map ->
            val style = map.style
            if (style != null) {
                style.removeLayer(ROUTE_LAYER)
                style.removeSource(ROUTE_SOURCE)
                if (points.size >= 2) {
                    val coords = points.map { org.maplibre.geojson.Point.fromLngLat(it.longitude, it.latitude) }
                    style.addSource(GeoJsonSource(ROUTE_SOURCE, Feature.fromGeometry(LineString.fromLngLats(coords))))
                    style.addLayer(LineLayer(ROUTE_LAYER, ROUTE_SOURCE).withProperties(lineWidth(6f), lineColor("#1976D2")))
                }
            }
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
        ) {
            startLocationUpdates()
            mapView.getMapAsync { map -> map.style?.let { activateLocationComponent(map, it) } }
        } else {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), LOCATION_REQUEST)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_REQUEST && grantResults.any { it == PackageManager.PERMISSION_GRANTED }) {
            startLocationUpdates()
            mapView.getMapAsync { map -> map.style?.let { activateLocationComponent(map, it) } }
        } else status.text = "GPS izni verilmedi • Harita çevrim içi çalışıyor"
    }

    private fun startLocationUpdates() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) return
        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 5f, locationListener)
            }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000L, 10f, locationListener)
            }
            status.text = "GPS etkin • Konum bekleniyor"
        } catch (_: SecurityException) {
            status.text = "GPS izni alınamadı"
        }
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
        livePoiLayer?.destroy()
        routeExecutor.shutdownNow()
        mapView.onDestroy()
        if (::tts.isInitialized) { tts.stop(); tts.shutdown() }
        super.onDestroy()
    }
}
