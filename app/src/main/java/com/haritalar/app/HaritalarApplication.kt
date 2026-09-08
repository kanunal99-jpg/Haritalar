package com.haritalar.app

import android.app.Activity
import android.app.Application
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ScrollView
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

        releaseHiddenRouteOverlay(root)

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
        }
    }

    /**
     * MainActivity keeps the route panel inside a weighted ScrollView. When the
     * panel itself is GONE, that ScrollView can still occupy the whole upper UI
     * and consume map touch events. Keep the parent hidden whenever its route
     * panel is hidden so the map receives drag/zoom gestures everywhere else.
     */
    private fun releaseHiddenRouteOverlay(root: View) {
        val routePanel = findViewByClassName(root, "com.haritalar.app.MainActivity")
        if (routePanel != null) return

        val scrollViews = mutableListOf<ScrollView>()
        collectViews(root, ScrollView::class.java, scrollViews)
        scrollViews.forEach { scroll ->
            val child = scroll.getChildAt(0) ?: return@forEach
            if (child is ViewGroup && child.visibility == View.GONE) {
                scroll.visibility = View.GONE
                child.viewTreeObserver.addOnGlobalLayoutListener {
                    scroll.visibility = if (child.visibility == View.VISIBLE) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun addRecenterButton(activity: Activity, root: View, map: MapLibreMap) {
        val container = root as? ViewGroup ?: return
        if (container.findViewWithTag<View>("haritalar-recenter") != null) return

        val button = Button(activity).apply {
            tag = "haritalar-recenter"
            text = "⌖ Konumuma dön"
            setAllCaps(false)
            textSize = 12f
            minHeight = 50
            setTextColor(0xFFFFFFFF.toInt())
            background = GradientDrawable().apply {
                setColor(0xFF1976D2.toInt())
                cornerRadius = 18f
            }
            elevation = 7f
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
        container.addView(button, FrameLayout.LayoutParams(-2, 50).apply {
            gravity = Gravity.TOP or Gravity.END
            topMargin = 112
            rightMargin = 12
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

    private fun findViewByClassName(root: View, className: String): View? {
        if (root.javaClass.name == className) return root
        if (root !is ViewGroup) return null
        for (index in 0 until root.childCount) {
            findViewByClassName(root.getChildAt(index), className)?.let { return it }
        }
        return null
    }
}
