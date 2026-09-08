package com.haritalar.app

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
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

    /**
     * MainActivity historically supplied Istanbul as its initial camera target.
     * Do not let that placeholder survive when a real device location is already
     * available. We only replace the known Istanbul fallback, so a user-selected
     * address or a manually browsed city is never overwritten.
     */
    private fun centerInitialViewOnGps(activity: Activity, map: MapLibreMap) {
        if (activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            activity.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) return

        val initialTarget = LatLng(41.0082, 28.9784)
        val startedAt = System.currentTimeMillis()
        val timeoutMs = 12_000L
        val poll = object : Runnable {
            override fun run() {
                val cameraTarget = map.cameraPosition.target
                val stillOnIstanbulFallback = distanceMeters(cameraTarget, initialTarget) <= 2_500.0
                if (!stillOnIstanbulFallback) return

                val location = map.locationComponent.getLastKnownLocation()
                if (location != null) {
                    map.locationComponent.setCameraMode(org.maplibre.android.location.modes.CameraMode.NONE_GPS)
                    map.cameraPosition = CameraPosition.Builder(map.cameraPosition)
                        .target(LatLng(location.latitude, location.longitude))
                        .zoom(maxOf(map.cameraPosition.zoom, 14.5))
                        .build()
                    return
                }

                if (System.currentTimeMillis() - startedAt < timeoutMs) {
                    mainHandler.postDelayed(this, 500L)
                }
            }
        }
        mainHandler.post(poll)
    }

    private fun distanceMeters(a: LatLng, b: LatLng): Double {
        val earthRadius = 6_371_000.0
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val sinLat = kotlin.math.sin(dLat / 2.0)
        val sinLon = kotlin.math.sin(dLon / 2.0)
        val h = sinLat * sinLat + kotlin.math.cos(lat1) * kotlin.math.cos(lat2) * sinLon * sinLon
        return 2.0 * earthRadius * kotlin.math.atan2(kotlin.math.sqrt(h), kotlin.math.sqrt(1.0 - h))
    }

    /**
     * MainActivity's route ScrollView is created as a weighted child of the
     * controls column. That makes it cover the map even while the route panel
     * is only a small card. Move that ScrollView to the root as a bottom overlay
     * so the rest of the map remains directly draggable, zoomable and rotatable.
     */
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
        val maxHeight = (300 * density).toInt()
        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            maxHeight,
        ).apply {
            gravity = Gravity.BOTTOM
            leftMargin = (10 * density).toInt()
            rightMargin = (10 * density).toInt()
            bottomMargin = (10 * density).toInt()
        }
        container.addView(scroll, params)

        scroll.visibility = if (child.visibility == View.VISIBLE) View.VISIBLE else View.GONE
        child.viewTreeObserver.addOnGlobalLayoutListener {
            scroll.visibility = if (child.visibility == View.VISIBLE) View.VISIBLE else View.GONE
        }
    }

    private fun findRouteScrollView(root: View): ScrollView? {
        val scrollViews = mutableListOf<ScrollView>()
        collectViews(root, ScrollView::class.java, scrollViews)
        return scrollViews.firstOrNull { scroll ->
            val child = scroll.getChildAt(0)
            child is LinearLayout && child.childCount > 0
        }
    }

    private fun addRecenterButton(activity: Activity, root: View, map: MapLibreMap) {
        val container = root as? ViewGroup ?: return
        if (container.findViewWithTag<View>("haritalar-recenter") != null) return

        val density = resources.displayMetrics.density
        val button = Button(activity).apply {
            tag = "haritalar-recenter"
            text = "⌖ Konumuma dön"
            setAllCaps(false)
            textSize = 12f
            minHeight = (50 * density).toInt()
            setTextColor(0xFFFFFFFF.toInt())
            background = GradientDrawable().apply {
                setColor(0xFF1976D2.toInt())
                cornerRadius = 18f * density
            }
            elevation = 7f * density
            setOnClickListener {
                val location = map.locationComponent.getLastKnownLocation()
                if (location != null) {
                    map.locationComponent.setCameraMode(org.maplibre.android.location.modes.CameraMode.NONE_GPS)
                    map.cameraPosition = CameraPosition.Builder(map.cameraPosition)
                        .target(LatLng(location.latitude, location.longitude))
                        .zoom(maxOf(map.cameraPosition.zoom, 15.5))
                        .tilt(0.0)
                        .build()
                }
            }
        }
        container.addView(button, FrameLayout.LayoutParams(-2, (50 * density).toInt()).apply {
            gravity = Gravity.TOP or Gravity.END
            topMargin = (112 * density).toInt()
            rightMargin = (12 * density).toInt()
        })
    }

    private fun <T : View> findView(root: View, type: Class<T>): T? {
        if (type.isInstance(root)) return type.cast(root)
        if (root !is ViewGroup) return null
        for (index in 0 until root.childCount) {
            findView(root.getChildAt(index), type)?.let { return it }
        }
        return null
    }

    private fun <T : View> collectViews(root: View, type: Class<T>, result: MutableList<T>) {
        if (type.isInstance(root)) result += type.cast(root)
        if (root !is ViewGroup) return
        for (index in 0 until root.childCount) {
            collectViews(root.getChildAt(index), type, result)
        }
    }

    companion object {
        private const val ROUTE_OVERLAY_TAG = "haritalar-route-overlay"
    }
}
