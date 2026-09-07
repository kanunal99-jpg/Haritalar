package com.haritalar.core.navigation

/** Deterministic turn-by-turn trigger state machine. */
class NavigationProgressEngine(
    private val earlyTriggerMeters: Double = 500.0,
    private val immediateTriggerMeters: Double = 40.0,
    private val arrivalMeters: Double = 40.0,
) {
    data class Maneuver(val index: Int, val text: String, val distanceFromRouteStartMeters: Double)
    sealed interface Event {
        data class Instruction(val maneuver: Maneuver, val distanceMeters: Double, val immediate: Boolean) : Event
        data object Arrived : Event
    }

    private val announced = mutableSetOf<Int>()
    private var arrived = false

    fun reset() { announced.clear(); arrived = false }

    fun update(routeDistanceRemainingMeters: Double, routeTotalMeters: Double, maneuvers: List<Maneuver>): Event? {
        if (arrived) return null
        if (routeDistanceRemainingMeters <= arrivalMeters) {
            arrived = true
            return Event.Arrived
        }
        val next = maneuvers.firstOrNull { it.index !in announced } ?: return null
        val distanceToNext = (next.distanceFromRouteStartMeters - (routeTotalMeters - routeDistanceRemainingMeters)).coerceAtLeast(0.0)
        if (distanceToNext <= earlyTriggerMeters) {
            announced += next.index
            return Event.Instruction(next, distanceToNext, distanceToNext <= immediateTriggerMeters)
        }
        return null
    }
}
