package com.haritalar.app

import android.app.Activity
import android.app.Application
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory.visibility

/** Visible map controls for traffic, safety/POI overlays and navigation camera mode. */
object MapLayerControlBridge {
    private const val TRAFFIC_LAYER_ID = "haritalar-tomtom-traffic-layer"
    private const val POI_LAYER_ID = "haritalar-live-poi-circles"
    private const val POI_LABEL_LAYER_ID = "haritalar-live-poi-labels"
    private const val POI_SELECTED_LAYER_ID = "haritalar-live-poi-selected"
    private const val CONTROL_TAG = "haritalar-layer-controls"
    private const val PANEL_TAG = "haritalar-layer-panel"

    fun install(app: Application) {
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                if (activity is MainActivity) installWhenReady(activity)
            }
            override fun onActivityCreated(a: Activity, b: Bundle?) = Unit
            override fun onActivityStarted(a: Activity) = Unit
            override fun onActivityPaused(a: Activity) = Unit
            override fun onActivityStopped(a: Activity) = Unit
            override fun onActivitySaveInstanceState(a: Activity, b: Bundle) = Unit
            override fun onActivityDestroyed(a: Activity) = Unit
        })
    }

    private fun installWhenReady(activity: Activity) {
        val root = activity.findViewById<ViewGroup>(android.R.id.content)?.getChildAt(0) as? ViewGroup ?: return
        val mapView = findView(root, MapView::class.java) ?: return
        if (root.findViewWithTag<View>(CONTROL_TAG) != null) return
        mapView.getMapAsync { map ->
            if (root.findViewWithTag<View>(CONTROL_TAG) == null) addControls(activity, root, map)
        }
    }

    private fun addControls(activity: Activity, root: ViewGroup, map: MapLibreMap) {
        val density = activity.resources.displayMetrics.density
        val container = LinearLayout(activity).apply {
            tag = CONTROL_TAG
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding((6 * density).toInt(), (6 * density).toInt(), (6 * density).toInt(), (6 * density).toInt())
            background = rounded(0xF5FFFFFF.toInt(), 18f * density)
            elevation = 8f * density
        }
        val button = Button(activity).apply {
            text = "Katmanlar"
            setAllCaps(false)
            textSize = 12f
            minHeight = (46 * density).toInt()
            setOnClickListener { togglePanel(activity, root, map) }
        }
        container.addView(button, LinearLayout.LayoutParams((108 * density).toInt(), (46 * density).toInt()))
        root.addView(container, android.widget.FrameLayout.LayoutParams(-2, -2).apply {
            gravity = Gravity.TOP or Gravity.END
            topMargin = (170 * density).toInt()
            rightMargin = (12 * density).toInt()
        })
    }

    private fun togglePanel(activity: Activity, root: ViewGroup, map: MapLibreMap) {
        root.findViewWithTag<View>(PANEL_TAG)?.let { root.removeView(it); return }
        val density = activity.resources.displayMetrics.density
        val panel = LinearLayout(activity).apply {
            tag = PANEL_TAG
            orientation = LinearLayout.VERTICAL
            setPadding((10 * density).toInt(), (10 * density).toInt(), (10 * density).toInt(), (10 * density).toInt())
            background = rounded(0xF8FFFFFF.toInt(), 20f * density)
            elevation = 10f * density
        }
        val traffic = makeToggle(activity, "Trafik", isVisible(map, TRAFFIC_LAYER_ID)) { visible ->
            setVisible(map, TRAFFIC_LAYER_ID, visible)
        }
        val poi = makeToggle(activity, "Güvenlik / POI", isVisible(map, POI_LAYER_ID)) { visible ->
            listOf(POI_LAYER_ID, POI_LABEL_LAYER_ID, POI_SELECTED_LAYER_ID).forEach { setVisible(map, it, visible) }
        }
        val camera = Button(activity).apply {
            text = if (map.cameraPosition.tilt >= 30.0) "2D navigasyon" else "3D navigasyon"
            setAllCaps(false)
            textSize = 12f
            setOnClickListener {
                val current = map.cameraPosition
                val tilt = if (current.tilt >= 30.0) 0.0 else 45.0
                map.cameraPosition = CameraPosition.Builder(current).tilt(tilt).build()
                text = if (tilt > 0) "2D navigasyon" else "3D navigasyon"
            }
        }
        panel.addView(traffic)
        panel.addView(poi)
        panel.addView(camera)
        root.addView(panel, android.widget.FrameLayout.LayoutParams((190 * density).toInt(), -2).apply {
            gravity = Gravity.TOP or Gravity.END
            topMargin = (220 * density).toInt()
            rightMargin = (12 * density).toInt()
        })
    }

    private fun makeToggle(activity: Activity, title: String, initial: Boolean, onChanged: (Boolean) -> Unit): Button =
        Button(activity).apply {
            isAllCaps = false
            textSize = 12f
            var enabled = initial
            fun refresh() { text = "$title: ${if (enabled) "Açık" else "Kapalı"}" }
            refresh()
            setOnClickListener { enabled = !enabled; refresh(); onChanged(enabled) }
        }

    private fun isVisible(map: MapLibreMap, id: String): Boolean =
        map.style?.getLayer(id)?.getVisibility()?.value != Property.NONE

    private fun setVisible(map: MapLibreMap, id: String, visible: Boolean) {
        map.style?.getLayer(id)?.setProperties(visibility(if (visible) Property.VISIBLE else Property.NONE))
    }

    private fun rounded(color: Int, radius: Float) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius
    }

    private fun <T : View> findView(root: View, type: Class<T>): T? {
        if (type.isInstance(root)) return type.cast(root)
        if (root !is ViewGroup) return null
        for (i in 0 until root.childCount) findView(root.getChildAt(i), type)?.let { return it }
        return null
    }
}
