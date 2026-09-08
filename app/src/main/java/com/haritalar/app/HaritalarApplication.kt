package com.haritalar.app

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ScrollView
import android.widget.LinearLayout
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView

/**
 * Installs map-browse affordances without coupling them to the large MainActivity.
 * MapLibre's gesture settings are user-interaction settings and can be enabled
 * independently of programmatic camera movement.
 */
class HaritalarApplication : Application() {
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        SafetyAlertLifecycleBridge.install(this)
        SafetyReportUiBridge.install(this)
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                if (activity is MainActivity) {
                    activity.window.decorView.post { configureMapBrowse(activity) }
                }
            }

            override fun onActivityCreated(a: Activity, b: Bundle?) = Unit
            override fun onActivityStarted(a: Activity) = Unit
            override fun onActivityPaused(a: Activity) = Unit
            override fun onActivityStopped(a: Activity) = Unit
            override fun onActivitySaveInstanceState(a: Activity, b: Bundle) = Unit
            override fun onActivityDestroyed(a: Activity) = Unit
        })
    }

    private fun configureMapBrowse(activity: Activity) {
        val content = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
        val root = content.getChildAt(0) ?: return
        val mapView = findView(root, MapView::class.java) ?: return
        if (root is FrameLayout && root.childCount > 1) {
            val controls = root.getChildAt(1)
            val params = controls.layoutParams
            if (params != null && params.height != ViewGroup.LayoutParams.WRAP_CONTENT) {
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT
                controls.layoutParams = params
            }
        }
        installRoutePanelOverlay(root)
        mapView.getMapAsync { map ->
            map.uiSettings.setAllGesturesEnabled(true)
            map.uiSettings.isScrollGesturesEnabled = true
            map.uiSettings.isHorizontalScrollGesturesEnabled = true
            map.uiSettings.isZoomGesturesEnabled = true
            map.uiSettings.isDoubleTapGesturesEnabled = true
            map.uiSettings.isQuickZoomGesturesEnabled = true
            map.uiSettings.isRotateGesturesEnabled = true
            map.uiSettings.isTiltGesturesEnabled = true
            addRecenterButton(activity, root, map)
            centerInitialViewOnGps(activity, map)
        }
    }

    private fun centerInitialViewOnGps(activity: Activity, map: MapLibreMap) {
        if (activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && activity.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
        val initialCamera = map.cameraPosition
        val initialTarget = initialCamera.target
        val initialZoom = initialCamera.zoom
        val startedAt = System.currentTimeMillis()
        val poll = object : Runnable {
            override fun run() {
                val current = map.cameraPosition
                val currentTarget = current.target
                val moved = if (initialTarget != null && currentTarget != null) distanceMeters(currentTarget, initialTarget) > 50.0 else currentTarget == null
                if (moved || kotlin.math.abs(current.zoom - initialZoom) > 0.25) return
                val location = findLastDeviceLocation(activity) ?: map.locationComponent.getLastKnownLocation()
                if (location != null) {
                    map.locationComponent.setCameraMode(org.maplibre.android.location.modes.CameraMode.NONE_GPS)
                    map.cameraPosition = CameraPosition.Builder(current).target(LatLng(location.latitude, location.longitude)).zoom(maxOf(current.zoom, 14.5)).build()
                    return
                }
                if (System.currentTimeMillis() - startedAt < 12_000L) mainHandler.postDelayed(this, 500L)
            }
        }
        mainHandler.post(poll)
    }

    private fun findLastDeviceLocation(activity: Activity): Location? {
        val locationManager = activity.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        return listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).asSequence()
            .filter { try { locationManager.isProviderEnabled(it) } catch (_: Exception) { false } }
            .mapNotNull { try { locationManager.getLastKnownLocation(it) } catch (_: SecurityException) { null } }
            .maxByOrNull { it.time }
    }

    private fun distanceMeters(a: LatLng, b: LatLng): Double {
        val earth = 6_371_000.0
        val lat1 = Math.toRadians(a.latitude); val lat2 = Math.toRadians(b.latitude)
        val dLat = Math.toRadians(b.latitude - a.latitude); val dLon = Math.toRadians(b.longitude - a.longitude)
        val s1 = kotlin.math.sin(dLat / 2); val s2 = kotlin.math.sin(dLon / 2)
        val h = s1 * s1 + kotlin.math.cos(lat1) * kotlin.math.cos(lat2) * s2 * s2
        return 2 * earth * kotlin.math.atan2(kotlin.math.sqrt(h), kotlin.math.sqrt(1 - h))
    }

    private fun installRoutePanelOverlay(root: View) {
        val container = root as? FrameLayout ?: return
        if (container.findViewWithTag<View>(ROUTE_OVERLAY_TAG) != null) return
        val scroll = findRouteScrollView(root) ?: return
        val child = scroll.getChildAt(0) ?: return
        if (child !is ViewGroup) return
        val parent = scroll.parent as? ViewGroup ?: return
        parent.removeView(scroll)
        scroll.tag = ROUTE_OVERLAY_TAG
        scroll.isFillViewport = false
        scroll.clipToPadding = false
        scroll.setBackgroundColor(0xFFF7F7F7.toInt())
        val density = resources.displayMetrics.density
        container.addView(scroll, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (300 * density).toInt()).apply {
            gravity = Gravity.BOTTOM; leftMargin = (10 * density).toInt(); rightMargin = (10 * density).toInt(); bottomMargin = (10 * density).toInt()
        })
        scroll.visibility = if (child.visibility == View.VISIBLE) View.VISIBLE else View.GONE
        child.viewTreeObserver.addOnGlobalLayoutListener { scroll.visibility = if (child.visibility == View.VISIBLE) View.VISIBLE else View.GONE }
    }

    private fun findRouteScrollView(root: View): ScrollView? {
        val all = mutableListOf<ScrollView>(); collectViews(root, ScrollView::class.java, all)
        return all.firstOrNull { it.getChildAt(0) is LinearLayout && it.getChildAt(0).let { child -> (child as LinearLayout).childCount > 0 } }
    }

    private fun addRecenterButton(activity: Activity, root: View, map: MapLibreMap) {
        val container = root as? ViewGroup ?: return
        if (container.findViewWithTag<View>("haritalar-recenter") != null) return
        val density = resources.displayMetrics.density
        val button = Button(activity).apply {
            tag = "haritalar-recenter"; text = "⌖ Konumuma dön"; setAllCaps(false); textSize = 12f; minHeight = (50 * density).toInt(); setTextColor(0xFFFFFFFF.toInt())
            background = GradientDrawable().apply { setColor(0xFF1976D2.toInt()); cornerRadius = 18f * density }; elevation = 7f * density
            setOnClickListener { val location = findLastDeviceLocation(activity) ?: map.locationComponent.getLastKnownLocation(); if (location != null) map.cameraPosition = CameraPosition.Builder(map.cameraPosition).target(LatLng(location.latitude, location.longitude)).zoom(maxOf(map.cameraPosition.zoom, 15.5)).tilt(0.0).build() }
        }
        container.addView(button, FrameLayout.LayoutParams(-2, (50 * density).toInt()).apply { gravity = Gravity.TOP or Gravity.END; topMargin = (112 * density).toInt(); rightMargin = (12 * density).toInt() })
    }

    private fun <T : View> findView(root: View, type: Class<T>): T? {
        if (type.isInstance(root)) return type.cast(root)
        if (root !is ViewGroup) return null
        for (index in 0 until root.childCount) findView(root.getChildAt(index), type)?.let { return it }
        return null
    }
    private fun <T : View> collectViews(root: View, type: Class<T>, result: MutableList<T>) {
        if (type.isInstance(root)) result += type.cast(root)
        if (root !is ViewGroup) return
        for (index in 0 until root.childCount) collectViews(root.getChildAt(index), type, result)
    }
    companion object { private const val ROUTE_OVERLAY_TAG = "haritalar-route-overlay" }
}
