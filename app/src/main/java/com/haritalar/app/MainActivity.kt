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
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

class MainActivity : Activity() {
    companion object {
        private const val LOCATION_REQUEST = 1001
        private const val MAP_STYLE = "https://tiles.openfreemap.org/styles/liberty"
    }

    private lateinit var mapView: MapView
    private lateinit var locationManager: LocationManager
    private lateinit var status: TextView

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            status.text = "GPS aktif • %.5f, %.5f".format(location.latitude, location.longitude)
            mapView.getMapAsync { map ->
                map.locationComponent?.cameraMode = org.maplibre.android.location.modes.CameraMode.TRACKING
                map.cameraPosition = CameraPosition.Builder()
                    .target(LatLng(location.latitude, location.longitude))
                    .zoom(16.0)
                    .bearing(location.bearing.toDouble())
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
        val statusParams = FrameLayout.LayoutParams(-2, -2).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            topMargin = 32
        }
        root.addView(status, statusParams)

        val attribution = TextView(this).apply {
            text = "© OpenStreetMap contributors • OpenFreeMap"
            textSize = 11f
            setPadding(8, 4, 8, 4)
            setBackgroundColor(0xCCFFFFFF.toInt())
        }
        val attributionParams = FrameLayout.LayoutParams(-2, -2).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            bottomMargin = 12
            marginEnd = 12
        }
        root.addView(attribution, attributionParams)

        setContentView(root)

        mapView.getMapAsync { map ->
            map.setStyle(Style.Builder().fromUri(MAP_STYLE)) {
                map.cameraPosition = CameraPosition.Builder()
                    .target(LatLng(41.0082, 28.9784))
                    .zoom(11.5)
                    .build()
                status.text = "Haritalar • Harita hazır"
            }
        }

        requestLocationPermission()
    }

    private fun requestLocationPermission() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) {
            startLocationUpdates()
        } else {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_REQUEST
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_REQUEST && grantResults.any { it == PackageManager.PERMISSION_GRANTED }) {
            startLocationUpdates()
        } else {
            status.text = "GPS izni verilmedi • Harita çevrim içi çalışıyor"
        }
    }

    private fun startLocationUpdates() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) return

        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000L,
                    5f,
                    locationListener
                )
            }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    2000L,
                    10f,
                    locationListener
                )
            }
            status.text = "GPS etkin • Konum bekleniyor"
        } catch (_: SecurityException) {
            status.text = "GPS izni alınamadı"
        }
    }

    private fun stopLocationUpdates() {
        try {
            locationManager.removeUpdates(locationListener)
        } catch (_: SecurityException) {
            // Permission may have been revoked while the activity was alive.
        }
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        mapView.onPause()
        super.onPause()
    }

    override fun onStop() {
        stopLocationUpdates()
        mapView.onStop()
        super.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        stopLocationUpdates()
        mapView.onDestroy()
        super.onDestroy()
    }
}
