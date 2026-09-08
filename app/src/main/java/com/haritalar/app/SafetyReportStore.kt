package com.haritalar.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Offline-first queue for user safety reports. No location history or account identity is stored. */
object SafetyReportStore {
    private const val PREFS = "haritalar_safety_reports"
    private const val KEY = "queue"
    private const val MAX_QUEUE = 100

    enum class Type { ADD, REMOVED, WRONG_LOCATION, INCORRECT_TYPE }

    data class Report(
        val id: String,
        val type: Type,
        val latitude: Double,
        val longitude: Double,
        val pointId: String?,
        val createdAtEpochMs: Long,
    )

    /** Offline queue boundary: reject non-finite/out-of-range coordinates. */
    fun isValidCoordinate(latitude: Double, longitude: Double): Boolean =
        latitude.isFinite() && longitude.isFinite() &&
            latitude in -90.0..90.0 && longitude in -180.0..180.0

    @Synchronized
    fun enqueue(context: Context, type: Type, latitude: Double, longitude: Double, pointId: String? = null): Report? {
        if (!isValidCoordinate(latitude, longitude)) return null
        val report = Report(
            UUID.randomUUID().toString(),
            type,
            latitude,
            longitude,
            pointId?.trim()?.takeIf { it.isNotEmpty() },
            System.currentTimeMillis(),
        )
        val current = read(context).toMutableList()
        current.add(report)
        while (current.size > MAX_QUEUE) current.removeAt(0)
        write(context, current)
        return report
    }

    fun pending(context: Context): List<Report> = read(context)

    private fun read(context: Context): List<Report> = runCatching {
        val array = JSONArray(
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]")
        )
        buildList {
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                val type = runCatching { Type.valueOf(o.getString("type")) }.getOrNull() ?: continue
                val id = o.optString("id").trim()
                val lat = o.optDouble("lat", Double.NaN)
                val lon = o.optDouble("lon", Double.NaN)
                val createdAt = o.optLong("createdAt", 0L)
                if (id.isEmpty() || !isValidCoordinate(lat, lon) || createdAt <= 0L) continue
                add(
                    Report(
                        id = id,
                        type = type,
                        latitude = lat,
                        longitude = lon,
                        pointId = o.optString("pointId").trim().takeIf { it.isNotEmpty() },
                        createdAtEpochMs = createdAt,
                    )
                )
            }
        }.takeLast(MAX_QUEUE)
    }.getOrDefault(emptyList())

    private fun write(context: Context, reports: List<Report>) {
        val array = JSONArray()
        reports.takeLast(MAX_QUEUE).forEach { r ->
            array.put(JSONObject().apply {
                put("id", r.id)
                put("type", r.type.name)
                put("lat", r.latitude)
                put("lon", r.longitude)
                r.pointId?.let { put("pointId", it) }
                put("createdAt", r.createdAtEpochMs)
            })
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, array.toString())
            .apply()
    }
}
