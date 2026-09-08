package com.haritalar.app

import android.app.Activity
import android.app.Application
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.haritalar.core.safety.Confidence
import com.haritalar.core.safety.DataSource
import com.haritalar.core.safety.SafetyAlert
import com.haritalar.core.safety.SafetyPoint
import com.haritalar.core.safety.SafetyPointType
import com.haritalar.core.safety.SafetyRouteAlertCoordinator
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.*

/** Route safety integration: live OSM/Overpass -> cache -> silent safe default. */
object SafetyAlertLifecycleBridge {
    private const val ENDPOINT = "https://overpass-api.de/api/interpreter"
    private const val PREFS = "haritalar_safety_cache"
    private const val CACHE_TTL = 30 * 60 * 1000L
    private const val POLL_MS = 1000L
    private const val ALERT_VISIBLE_MS = 7000L
    private val handler = Handler(Looper.getMainLooper())
    private val io = Executors.newSingleThreadExecutor()
    private val coordinator = SafetyRouteAlertCoordinator()
    private var activity: Activity? = null
    private var tracked = emptyList<SafetyRouteAlertCoordinator.TrackedPoint>()
    private var signature: String? = null
    private var generation = 0
    private var card: TextView? = null
    private var lastAlertKey: String? = null

    fun install(app: Application) {
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(a: Activity) { if (a is MainActivity) { activity = a; schedule() } }
            override fun onActivityPaused(a: Activity) { if (activity === a) activity = null }
            override fun onActivityDestroyed(a: Activity) { if (activity === a) activity = null }
            override fun onActivityCreated(a: Activity, b: Bundle?) = Unit
            override fun onActivityStarted(a: Activity) = Unit
            override fun onActivityStopped(a: Activity) = Unit
            override fun onActivitySaveInstanceState(a: Activity, b: Bundle) = Unit
        })
    }

    private fun schedule() {
        handler.removeCallbacksAndMessages(null)
        handler.post(object : Runnable {
            override fun run() { tick(); handler.postDelayed(this, POLL_MS) }
        })
    }

    private fun tick() {
        val a = activity ?: return
        if (!(field(a, "navigationActive") as? Boolean ?: false)) {
            tracked = emptyList(); signature = null; lastAlertKey = null; hideCard(); return
        }
        val route = latLngs(a)
        val cumulative = (field(a, "routeCumulativeMeters") as? List<Double>).orEmpty()
        val location = field(a, "lastLocation") as? Location ?: return
        if (route.size < 2 || route.size != cumulative.size) return
        val sig = route.joinToString(";") { "%.5f,%.5f".format(Locale.US, it.first, it.second) }
        if (sig != signature) {
            signature = sig; tracked = emptyList(); lastAlertKey = null; hideCard(); val g = ++generation
            io.execute {
                val points = loadPoints(a, route)
                if (g != generation) return@execute
                handler.post {
                    if (g != generation) return@post
                    tracked = coordinator.trackPoints(route.map { SafetyRouteAlertCoordinator.RoutePoint(it.first, it.second) }, cumulative, points)
                }
            }
        }
        if (tracked.isEmpty()) return
        val progress = progress(route, cumulative, location)
        val bearing = if (location.hasBearing()) location.bearing.toDouble() else null
        coordinator.evaluate(tracked, progress, bearing).forEach { alert ->
            when (alert) {
                is SafetyAlert.Approach -> show(a, alert)
                is SafetyAlert.Passed -> if (lastAlertKey?.startsWith("${alert.pointId}:") == true) { lastAlertKey = null; hideCard() }
            }
        }
    }

    private fun loadPoints(context: Context, route: List<Pair<Double, Double>>): List<SafetyPoint> {
        val cached = readCache(context)
        return try { fetch(route).also { if (it.isNotEmpty()) writeCache(context, it) }.ifEmpty { cached } }
        catch (_: Exception) { cached }
    }

    private fun fetch(route: List<Pair<Double, Double>>): List<SafetyPoint> {
        val south = route.minOf { it.first } - .002; val north = route.maxOf { it.first } + .002
        val west = route.minOf { it.second } - .002; val east = route.maxOf { it.second } + .002
        val q = "[out:json][timeout:20];node[\"highway\"=\"speed_camera\"]($south,$west,$north,$east);out body;"
        val c = URL(ENDPOINT).openConnection() as HttpURLConnection
        c.requestMethod = "POST"; c.connectTimeout = 8000; c.readTimeout = 25000; c.doOutput = true
        c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
        c.setRequestProperty("User-Agent", "Haritalar-Android/1.0 (open-source navigation app)")
        c.outputStream.use { it.write(("data=" + URLEncoder.encode(q, "UTF-8")).toByteArray()) }
        if (c.responseCode !in 200..299) error("Overpass HTTP ${c.responseCode}")
        val elements = JSONObject(c.inputStream.bufferedReader().use { it.readText() }).optJSONArray("elements") ?: JSONArray()
        return buildList {
            for (i in 0 until elements.length()) {
                val e = elements.getJSONObject(i); val tags = e.optJSONObject("tags") ?: JSONObject()
                val lat = e.optDouble("lat", Double.NaN); val lon = e.optDouble("lon", Double.NaN)
                if (!lat.isFinite() || !lon.isFinite()) continue
                add(SafetyPoint("osm-speed-camera-${e.optLong("id", i.toLong())}", lat, lon, SafetyPointType.FIXED_SPEED_CAMERA, Confidence.HIGH, DataSource.LIVE, directionBearingDegrees = direction(tags.optString("direction")), speedLimitKmh = tags.optString("maxspeed").takeWhile(Char::isDigit).toIntOrNull()))
            }
        }
    }

    private fun direction(s: String): Double? = s.toDoubleOrNull()?.let { (it % 360 + 360) % 360 } ?: when (s.lowercase()) {
        "n" -> 0.0; "ne" -> 45.0; "e" -> 90.0; "se" -> 135.0; "s" -> 180.0; "sw" -> 225.0; "w" -> 270.0; "nw" -> 315.0; else -> null
    }

    private fun readCache(c: Context): List<SafetyPoint> {
        val p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE); if (System.currentTimeMillis() - p.getLong("time", 0) > CACHE_TTL) return emptyList()
        return runCatching { parse(JSONArray(p.getString("points", "[]"))) }.getOrDefault(emptyList())
    }
    private fun writeCache(c: Context, points: List<SafetyPoint>) {
        val a = JSONArray(); points.forEach { p -> a.put(JSONObject().apply { put("id", p.id); put("lat", p.latitude); put("lon", p.longitude); p.directionBearingDegrees?.let { put("dir", it) }; p.speedLimitKmh?.let { put("speed", it) } }) }
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("points", a.toString()).putLong("time", System.currentTimeMillis()).apply()
    }
    private fun parse(a: JSONArray) = buildList<SafetyPoint> {
        for (i in 0 until a.length()) { val p = a.getJSONObject(i); add(SafetyPoint(p.getString("id"), p.getDouble("lat"), p.getDouble("lon"), SafetyPointType.FIXED_SPEED_CAMERA, Confidence.HIGH, DataSource.CACHE, p.optDouble("dir", Double.NaN).takeUnless(Double::isNaN), p.optInt("speed", 0).takeIf { it > 0 })) }
    }

    private fun show(a: Activity, alert: SafetyAlert.Approach) {
        val bucket = when {
            alert.level.name == "FINAL" -> "final"
            alert.remainingMeters <= 500.0 -> "500"
            else -> (alert.remainingMeters / 500.0).toInt().coerceAtLeast(1).toString()
        }
        val alertKey = "${alert.pointId}:$bucket"
        if (alertKey == lastAlertKey) return
        lastAlertKey = alertKey
        val label = when (alert.type) { SafetyPointType.FIXED_SPEED_CAMERA -> "Sabit hız kamerası"; SafetyPointType.AVERAGE_SPEED_ZONE -> "Ortalama hız bölgesi"; SafetyPointType.TRAFFIC_LIGHT_CAMERA -> "Trafik ışığı kamerası"; SafetyPointType.VERIFIED_TRAFFIC_CONTROL -> "Doğrulanmış trafik kontrolü"; SafetyPointType.USER_REPORTED_SAFETY_POINT -> "Kullanıcı bildirimi" }
        val d = if (alert.remainingMeters >= 1000) "%.1f km".format(Locale("tr", "TR"), alert.remainingMeters / 1000) else "%.0f m".format(Locale("tr", "TR"), alert.remainingMeters)
        val text = "⚠ $label • $d • Hız sınırına uyun"
        a.runOnUiThread {
            (field(a, "status") as? TextView)?.text = "Güvenlik uyarısı • $text"
            val root = a.findViewById<ViewGroup>(android.R.id.content)?.getChildAt(0) as? FrameLayout ?: return@runOnUiThread
            val v = card ?: TextView(a).also { card = it; root.addView(it, FrameLayout.LayoutParams(-1, -2).apply { gravity = Gravity.TOP; leftMargin = 16; rightMargin = 16; topMargin = 156 }) }
            v.text = text; v.textSize = if (alert.level.name == "FINAL") 20f else 18f; v.gravity = Gravity.CENTER; v.setTextColor(-1); v.setPadding(20, 18, 20, 18); v.background = GradientDrawable().apply { setColor(0xFFB71C1C.toInt()); cornerRadius = 22f }; v.visibility = View.VISIBLE
            v.removeCallbacksAndMessages(null); v.postDelayed({ v.visibility = View.GONE }, ALERT_VISIBLE_MS)
            runCatching { MainActivity::class.java.getDeclaredMethod("speak", String::class.java, Boolean::class.javaPrimitiveType).also { it.isAccessible = true }.invoke(a, "Dikkat. $text", true) }
        }
    }

    private fun hideCard() { handler.post { card?.visibility = View.GONE } }
    private fun field(a: Activity, name: String): Any? = runCatching { MainActivity::class.java.getDeclaredField(name).also { it.isAccessible = true }.get(a) }.getOrNull()
    private fun latLngs(a: Activity): List<Pair<Double, Double>> = runCatching { ((field(a, "routePoints") as? List<Any>).orEmpty()).map { it.javaClass.getMethod("getLatitude").invoke(it) as Double to it.javaClass.getMethod("getLongitude").invoke(it) as Double } }.getOrDefault(emptyList())
    private fun progress(route: List<Pair<Double, Double>>, cumulative: List<Double>, l: Location): Double { var best = Double.MAX_VALUE; var p = 0.0; for (i in 0 until route.lastIndex) { val x = project(l.latitude, l.longitude, route[i], route[i + 1]); if (x.second < best) { best = x.second; p = cumulative[i] + x.first * distance(route[i], route[i + 1]) } }; return max(0.0, p) }
    private fun project(lat: Double, lon: Double, a: Pair<Double, Double>, b: Pair<Double, Double>): Pair<Double, Double> { val m = Math.toRadians((a.first + b.first + lat) / 3); val sx = 111320 * cos(m); val sy = 110540.0; val ax = (a.second - lon) * sx; val ay = (a.first - lat) * sy; val bx = (b.second - lon) * sx; val by = (b.first - lat) * sy; val dx = bx - ax; val dy = by - ay; val den = dx * dx + dy * dy; val t = (if (den == 0.0) 0.0 else (-ax * dx - ay * dy) / den).coerceIn(0.0, 1.0); val cx = ax + t * dx; val cy = ay + t * dy; return t to sqrt(cx * cx + cy * cy) }
    private fun distance(a: Pair<Double, Double>, b: Pair<Double, Double>): Double { val r = 6371000.0; val dl = Math.toRadians(b.first - a.first); val dn = Math.toRadians(b.second - a.second); val x = sin(dl / 2).let { it * it } + cos(Math.toRadians(a.first)) * cos(Math.toRadians(b.first)) * sin(dn / 2).let { it * it }; return r * 2 * atan2(sqrt(x), sqrt(max(0.0, 1 - x))) }
}
