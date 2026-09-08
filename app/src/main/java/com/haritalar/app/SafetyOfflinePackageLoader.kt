package com.haritalar.app

import android.content.Context
import android.util.Log
import com.haritalar.core.safety.Confidence
import com.haritalar.core.safety.DataSource
import com.haritalar.core.safety.SafetyOfflinePackage
import com.haritalar.core.safety.SafetyOfflinePackageValidator
import com.haritalar.core.safety.SafetyOfflineRegion
import com.haritalar.core.safety.SafetyPoint
import com.haritalar.core.safety.SafetyPointType
import org.json.JSONObject
import java.io.InputStream

/** Strict parser for the bundled offline safety package. */
object SafetyOfflinePackageLoader {
    private const val TAG = "SafetyOfflinePackage"
    private const val ASSET = "safety_offline.json"
    private const val MAX_REGIONS = 1000
    private const val MAX_POINTS_PER_REGION = 10_000

    data class RegionBounds(
        val id: String,
        val south: Double,
        val west: Double,
        val north: Double,
        val east: Double,
        val points: List<SafetyPoint>,
    )

    data class Loaded(val version: Int, val regions: List<RegionBounds>)

    fun load(context: Context): Loaded? = runCatching {
        context.assets.open(ASSET).use(::parse)
    }.onFailure { Log.w(TAG, "Offline safety package rejected", it) }.getOrNull()

    fun pointsForRoute(context: Context, route: List<Pair<Double, Double>>): List<SafetyPoint> {
        if (route.isEmpty()) return emptyList()
        val loaded = load(context) ?: return emptyList()
        val south = route.minOf { it.first } - .002
        val north = route.maxOf { it.first } + .002
        val west = route.minOf { it.second } - .002
        val east = route.maxOf { it.second } + .002
        return loaded.regions
            .filter { it.north >= south && it.south <= north && it.east >= west && it.west <= east }
            .flatMap { it.points }
    }

    private fun parse(input: InputStream): Loaded {
        val root = JSONObject(input.bufferedReader().use { it.readText() })
        require(root.has("version")) { "missing version" }
        val version = root.getInt("version")
        require(root.has("regions")) { "missing regions" }
        val regionsJson = root.getJSONArray("regions")
        require(regionsJson.length() <= MAX_REGIONS) { "too many regions" }

        val regions = ArrayList<RegionBounds>(regionsJson.length())
        for (i in 0 until regionsJson.length()) {
            val region = regionsJson.getJSONObject(i)
            val id = region.getString("id").trim()
            require(id.isNotEmpty()) { "region id is blank" }
            val box = region.getJSONObject("bbox")
            val south = box.getDouble("south")
            val west = box.getDouble("west")
            val north = box.getDouble("north")
            val east = box.getDouble("east")
            require(south.isFinite() && north.isFinite() && west.isFinite() && east.isFinite()) { "invalid bbox" }
            require(south in -90.0..90.0 && north in -90.0..90.0) { "invalid bbox latitude" }
            require(west in -180.0..180.0 && east in -180.0..180.0) { "invalid bbox longitude" }
            require(south <= north && west <= east) { "inverted bbox" }
            val pointsJson = region.getJSONArray("points")
            require(pointsJson.length() <= MAX_POINTS_PER_REGION) { "too many points" }
            val points = ArrayList<SafetyPoint>(pointsJson.length())
            for (j in 0 until pointsJson.length()) points += parsePoint(pointsJson.getJSONObject(j))
            regions += RegionBounds(id, south, west, north, east, points)
        }

        val pkg = SafetyOfflinePackage(version, regions.map { SafetyOfflineRegion(it.id, it.points) })
        require(SafetyOfflinePackageValidator.validate(pkg)) { "offline trust validation failed" }
        return Loaded(version, regions)
    }

    private fun parsePoint(p: JSONObject): SafetyPoint {
        val id = p.getString("id").trim()
        val lat = p.getDouble("lat")
        val lon = p.getDouble("lon")
        val type = SafetyPointType.valueOf(p.getString("type"))
        val confidence = Confidence.valueOf(p.getString("confidence"))
        val bearing = if (p.has("dir") && !p.isNull("dir")) p.getDouble("dir") else null
        val speed = if (p.has("speed") && !p.isNull("speed")) p.getInt("speed") else null
        val active = if (p.has("active")) p.getBoolean("active") else true
        val source = if (p.has("source")) DataSource.valueOf(p.getString("source")) else DataSource.OFFLINE
        require(id.isNotEmpty()) { "point id is blank" }
        require(lat.isFinite() && lon.isFinite()) { "invalid point coordinates" }
        require(source == DataSource.OFFLINE) { "offline point has non-offline source" }
        require(bearing == null || bearing.isFinite()) { "invalid point direction" }
        require(speed == null || speed > 0) { "invalid point speed" }
        return SafetyPoint(id, lat, lon, type, confidence, DataSource.OFFLINE, bearing, speed, active)
    }
}
