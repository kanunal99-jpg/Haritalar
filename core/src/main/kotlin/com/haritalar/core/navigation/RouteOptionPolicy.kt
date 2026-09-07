package com.haritalar.core.navigation

/** Deterministic policy for choosing the user-facing recommended route. */
object RouteOptionPolicy {
    fun recommended(options: List<RouteAlternative>): RouteAlternative? =
        options.minWithOrNull(
            compareBy<RouteAlternative> { it.route.durationSeconds }
                .thenBy { it.route.distanceMeters }
                .thenBy { it.id },
        )

    fun ordered(options: List<RouteAlternative>): List<RouteAlternative> =
        options.sortedWith(
            compareBy<RouteAlternative> { it.route.durationSeconds }
                .thenBy { it.route.distanceMeters }
                .thenBy { it.id },
        )
}
