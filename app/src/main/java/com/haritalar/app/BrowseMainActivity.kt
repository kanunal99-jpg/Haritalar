package com.haritalar.app

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
 * Keeps the existing navigation activity intact while making the map surface a true
 * browse surface. The old route ScrollView occupied the whole remaining screen even
 * when its panel was hidden, so it could consume map gestures.
 */
class BrowseMainActivity : MainActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.post { configureBrowseSurface() }
    }

    private fun configureBrowseSurface() {
        val content = findViewById<ViewGroup>(android.R.id.content) ?: return
        val root = content.getChildAt(0) as? FrameLayout ?: return
        val mapView = findView(root, MapView::class.java) ?: return
        val routeScroll = findView(root, ScrollView::class.java) ?: return
        val routePanel = routeScroll.getChildAt(0)

        val parent = routeScroll.parent as? ViewGroup
        if (parent !== root) {
            parent?.removeView(routeScroll)
            root.addView(routeScroll, FrameLayout.LayoutParams(-1, 330).apply {
                gravity = Gravity.BOTTOM
                leftMargin = 12
                rightMargin = 12
                bottomMargin = 12
            })
        }
        routeScroll.visibility = View.GONE

        mapView.getMapAsync { map ->
            enableMapGestures(map)
            addRecenterButton(root, map)
        }

        val observer = root.viewTreeObserver
        observer.addOnGlobalLayoutListener {
            routeScroll.visibility = if (routePanel.visibility == View.VISIBLE) View.VISIBLE else View.GONE
        }
    }

    private fun enableMapGestures(map: MapLibreMap) {
        map.uiSettings.isScrollGesturesEnabled = true
        map.uiSettings.isZoomGesturesEnabled = true
        map.uiSettings.isRotateGesturesEnabled = true
        map.uiSettings.isTiltGesturesEnabled = true
        map.uiSettings.isDoubleTapGesturesEnabled = true
        map.uiSettings.isQuickZoomGesturesEnabled = true
        map.uiSettings.isHorizontalScrollGesturesEnabled = true
    }

    private fun addRecenterButton(root: FrameLayout, map: MapLibreMap) {
        val button = Button(this).apply {
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
        root.addView(button, FrameLayout.LayoutParams(-2, 50).apply {
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
}
