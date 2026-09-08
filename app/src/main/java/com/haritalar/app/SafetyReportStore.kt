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

    @Synchronized
    fun enqueue(context: Context, type: Type, latitude: Double, longitude: Double, pointId: String? = null): Report? {
        if (!latitude.isFinite() || !longitude.isFinite() || latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return null
        val report = Report(UUID.randomUUID().toString(), type, latitude, longitude, pointId, System.currentTimeMillis())
        val current = read(context).toMutableList()
        current.add(report)
        while (current.size > MAX_QUEUE) current.removeAt(0)
        write(context, current)
        return report
    }

    fun pending(context: Context): List<Report> = read(context)

    private fun read(context: Context): List<Report> = runCatching {
        val array = JSONArray(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]"))
        buildList {
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                val type = runCatching { Type.valueOf(o.getString("type")) }.getOrNull() ?: continue
                add(Report(o.getString("id"), type, o.getDouble("lat"), o.getDouble("lon"), o.optString("pointId").takeIf { it.isNotBlank() }, o.getLong("createdAt")))
            }
        }
    }.getOrDefault(emptyList())

    private fun write(context: Context, reports: List<Report>) {
        val array = JSONArray()
        reports.forEach { r ->
            array.put(JSONObject().apply {
                put("id", r.id); put("type", r.type.name); put("lat", r.latitude); put("lon", r.longitude)
                r.pointId?.let { put("pointId", it) }; put("createdAt", r.createdAtEpochMs)
            })
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, array.toString()).apply()
    }
}
