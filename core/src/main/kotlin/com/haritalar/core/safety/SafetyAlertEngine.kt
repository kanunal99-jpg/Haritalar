package com.haritalar.core.safety

/**
 * Deterministic, side-effect-free alert state machine.
 * The caller supplies the route distance to the next safety point.
 */
class SafetyAlertEngine(
    private val firstAlertMeters: Int = 4_000,
    private val intervalMeters: Int = 500,
    private val finalAlertMeters: Int = 500,
    private val passThresholdMeters: Int = 25,
) {
    private val states = mutableMapOf<String, AlertState>()

    fun evaluate(point: SafetyPoint, position: RoutePosition): SafetyAlert? {
        if (!point.active || !isDirectionRelevant(point, position)) return null
        val distance = position.routeDistanceRemainingMeters
        val state = states.getOrPut(point.id) { AlertState() }

        if (distance <= passThresholdMeters) {
            state.passed = true
            return SafetyAlert.Passed(point.id)
        }
        if (state.passed || distance > firstAlertMeters) return null

        val bucket = bucketFor(distance)
        if (bucket == state.lastBucket) return null
        state.lastBucket = bucket

        val level = if (distance <= finalAlertMeters) AlertLevel.FINAL else confidenceLevel(point.confidence)
        return SafetyAlert.Approach(point.id, point.type, distance, level)
    }

    fun reset(pointId: String) { states.remove(pointId) }

    private fun bucketFor(distance: Double): Int {
        if (distance <= finalAlertMeters) return 0
        return kotlin.math.ceil(distance / intervalMeters).toInt()
    }

    private fun confidenceLevel(confidence: Confidence): AlertLevel = when (confidence) {
        Confidence.HIGH -> AlertLevel.NORMAL
        Confidence.MEDIUM -> AlertLevel.CAUTION
        Confidence.LOW -> AlertLevel.LOW_CONFIDENCE
    }

    private fun isDirectionRelevant(point: SafetyPoint, position: RoutePosition): Boolean {
        val pointBearing = point.directionBearingDegrees ?: return true
        val userBearing = position.bearingDegrees ?: return true
        val delta = kotlin.math.abs(((pointBearing - userBearing + 540.0) % 360.0) - 180.0)
        return delta <= 55.0
    }

    private data class AlertState(var lastBucket: Int? = null, var passed: Boolean = false)
}

sealed interface SafetyAlert {
    data class Approach(
        val pointId: String,
        val type: SafetyPointType,
        val remainingMeters: Double,
        val level: AlertLevel,
    ) : SafetyAlert

    data class Passed(val pointId: String) : SafetyAlert
}

enum class AlertLevel { NORMAL, CAUTION, LOW_CONFIDENCE, FINAL }
