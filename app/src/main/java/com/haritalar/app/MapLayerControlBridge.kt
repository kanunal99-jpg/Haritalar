package com.haritalar.app

import android.app.Activity
import android.app.Application
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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

/** Visible map controls whose state is always derived from the current MapLibre style/camera. */
object MapLayerControlBridge {
    private const val TRAFFIC_LAYER_PREFIX = "haritalar-global-traffic-layer-"
    private val TRAFFIC_SEVERITY_LAYER_IDS = listOf("free", "light", "moderate", "heavy", "severe")
    private const val TOMTOM_RASTER_TRAFFIC_LAYER_ID = "haritalar-tomtom-traffic-layer"
    private const val POI_LAYER_ID = "haritalar-live-poi-circles"
    private const val POI_LABEL_LAYER_ID = "haritalar-live-poi-labels"
    private const val POI_SELECTED_LAYER_ID = "haritalar-live-poi-selected"
    private const val CONTROL_TAG = "haritalar-layer-controls"
    private const val PANEL_TAG = "haritalar-layer-panel"
    private const val STYLE_WAIT_INTERVAL_MS = 100L
    private const val STYLE_WAIT_TIMEOUT_MS = 15_000L
    private val mainHandler = Handler(Looper.getMainLooper())

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

    /** Re-checks the current activity after MainActivity changes the MapLibre style. */
    fun refresh(activity: Activity) {
        if (activity is MainActivity) installWhenReady(activity)
    }

    private fun installWhenReady(activity: Activity) {
        val root = activity.findViewById<ViewGroup>(android.R.id.content)?.getChildAt(0) as? ViewGroup ?: return
        val mapView = findView(root, MapView::class.java) ?: return
        if (root.findViewWithTag<View>(CONTROL_TAG) != null) return
        mapView.getMapAsync { map -> waitForStyleAndAddControls(activity, root, map, System.currentTimeMillis()) }
    }

    private fun waitForStyleAndAddControls(activity: Activity, root: ViewGroup, map: MapLibreMap, startedAt: Long) {
        if (root.findViewWithTag<View>(CONTROL_TAG) != null) return
        if (!activity.isFinishing && !activity.isDestroyed && map.style != null) {
            addControls(activity, root, map)
            return
        }
        if (activity.isFinishing || activity.isDestroyed) return
        if (System.currentTimeMillis() - startedAt >= STYLE_WAIT_TIMEOUT_MS) return
        mainHandler.postDelayed({ waitForStyleAndAddControls(activity, root, map, startedAt) }, STYLE_WAIT_INTERVAL_MS)
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
        val traffic = makeLayerToggle(activity, "Trafik", map) { trafficLayerIds(map) }
        val poi = makeLayerToggle(activity, "Güvenlik / POI", map) {
            listOf(POI_LAYER_ID, POI_LABEL_LAYER_ID, POI_SELECTED_LAYER_ID)
        }
        val camera = Button(activity).apply {
            setAllCaps(false)
            textSize = 12f
            refreshCameraLabel(this, map)
            setOnClickListener {
                val current = map.cameraPosition
                val tilt = if (current.tilt >= 30.0) 0.0 else 45.0
                map.cameraPosition = CameraPosition.Builder(current).tilt(tilt).build()
                refreshCameraLabel(this, map)
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

    private fun makeLayerToggle(
        activity: Activity,
        title: String,
        map: MapLibreMap,
        idsProvider: () -> List<String>,
    ): Button = Button(activity).apply {
        isAllCaps = false
        textSize = 12f

        fun refresh() {
            val state = groupVisibility(map, idsProvider())
            isEnabled = state != null
            text = when (state) {
                true -> "$title: Açık"
                false -> "$title: Kapalı"
                null -> "$title: Kullanılamıyor"
            }
        }

        refresh()
        setOnClickListener {
            val ids = idsProvider()
            val current = groupVisibility(map, ids)
            if (current == null) {
                refresh()
                return@setOnClickListener
            }
            setVisible(map, ids, !current)
            refresh()
        }
    }

    private fun refreshCameraLabel(button: Button, map: MapLibreMap) {
        button.text = if (map.cameraPosition.tilt >= 30.0) "2D navigasyon" else "3D navigasyon"
    }

    private fun trafficLayerIds(map: MapLibreMap): List<String> = buildList {
        add(TOMTOM_RASTER_TRAFFIC_LAYER_ID)
        TRAFFIC_SEVERITY_LAYER_IDS.forEach { add(TRAFFIC_LAYER_PREFIX + it) }
    }.filter { map.style?.getLayer(it) != null }

    private fun groupVisibility(map: MapLibreMap, ids: List<String>): Boolean? {
        val style = map.style ?: return null
        if (ids.isEmpty()) return null
        val values = ids.mapNotNull { id -> style.getLayer(id)?.getVisibility()?.value }
        if (values.size != ids.size) return null
        val visible = values.count { it != Property.NONE }
        return when {
            visible == values.size -> true
            visible == 0 -> false
            else -> null
        }
    }

    private fun setVisible(map: MapLibreMap, ids: List<String>, visible: Boolean): Boolean {
        val style = map.style ?: return false
        val existingIds = ids.filter { style.getLayer(it) != null }
        if (existingIds.isEmpty()) return false
        existingIds.forEach { id ->
            style.getLayer(id)?.setProperties(visibility(if (visible) Property.VISIBLE else Property.NONE))
        }
        return groupVisibility(map, existingIds) == visible
    }

    private fun rounded(color: Int, radius: Float) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius
    }

    private fun <T : View> findView(root: View, type: Class<T>): T? {
        if (type.isInstance(root)) return type.cast(root)
        if (root !is ViewGroup) return null
        for (i in 0 until root.childCount) {
            findView(root.getChildAt(i), type)?.let { return it }
        }
        return null
    }
}
