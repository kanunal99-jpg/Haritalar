package com.haritalar.app

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView
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
import kotlin.concurrent.thread

class MainActivity : Activity() {
    companion object {
        private const val LOCATION_REQUEST = 1001
        private const val MAP_STYLE = "https://tiles.openfreemap.org/styles/liberty"
        private const val ROUTE_ENDPOINT = "https://valhalla1.openstreetmap.de/route"
        private const val ROUTE_SOURCE = "haritalar-route-source"
        private const val ROUTE_LAYER = "haritalar-route-layer"
    }

    private lateinit var mapView: MapView
    private lateinit var locationManager: LocationManager
    private lateinit var status: TextView
    private var lastLocation: Location? = null

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            lastLocation = location
            status.text = "GPS aktif • %.5f, %.5f • Hedef için haritaya dokun".format(location.latitude, location.longitude)
            mapView.getMapAsync { map ->
                val bearing = if (location.hasBearing()) location.bearing.toDouble() else map.cameraPosition.bearing
                map.cameraPosition = CameraPosition.Builder(map.cameraPosition)
                    .target(LatLng(location.latitude, location.longitude))
                    .zoom(16.0)
                    .bearing(bearing)
                    .build()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(this)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

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
            map.setStyle(Style.Builder().fromUri(MAP_STYLE)) { style ->
                map.cameraPosition = CameraPosition.Builder()
                    .target(LatLng(41.0082, 28.9784))
                    .zoom(11.5)
                    .build()
                map.addOnMapClickListener { point ->
                    val origin = lastLocation
                    if (origin == null) {
                        status.text = "Önce GPS konumu bekleniyor"
                    } else {
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

    private fun requestRoute(fromLat: Double, fromLon: Double, toLat: Double, toLon: Double) {
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
                val body = (if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (connection.responseCode !in 200..299) throw IllegalStateException("Routing HTTP ${connection.responseCode}")
                val trip = JSONObject(body).getJSONObject("trip")
                val summary = trip.getJSONObject("summary")
                val distanceKm = summary.optDouble("length", 0.0)
                val durationSeconds = summary.optDouble("time", 0.0)
                val shape = trip.getJSONArray("legs").getJSONObject(0).getString("shape")
                val points = decodePolyline6(shape)
                runOnUiThread {
                    drawRoute(points)
                    status.text = "Rota hazır • %.1f km • %.0f dk".format(distanceKm, durationSeconds / 60.0)
                }
            } catch (error: Exception) {
                runOnUiThread { status.text = "Rota alınamadı • ${error.message ?: "ağ hatası"}" }
            }
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
                val b = encoded[index++].code - 63
                value = value or ((b and 0x1f) shl shift)
                shift += 5
                if (b < 0x20) break
            }
            lat += if ((value and 1) != 0) -(value shr 1) - 1 else value shr 1
            shift = 0
            value = 0
            while (true) {
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
    override fun onDestroy() { stopLocationUpdates(); mapView.onDestroy(); super.onDestroy() }
}
