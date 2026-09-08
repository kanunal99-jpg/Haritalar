package com.haritalar.core.safety

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SafetyRouteAlertCoordinatorTest {
    private val route = listOf(
        SafetyRouteAlertCoordinator.RoutePoint(41.00000, 29.00000),
        SafetyRouteAlertCoordinator.RoutePoint(41.00000, 29.01000),
    )
    private val cumulative = listOf(0.0, 839.0)
    private val point = SafetyPoint(
        id = "camera-1",
        latitude = 41.00000,
        longitude = 29.00500,
        type = SafetyPointType.FIXED_SPEED_CAMERA,
        confidence = Confidence.HIGH,
        source = DataSource.LIVE,
        directionBearingDegrees = 90.0,
    )

    @Test fun projectsOnlyPointsCloseToActiveRoute() {
        val coordinator = SafetyRouteAlertCoordinator(routeMatchToleranceMeters = 80.0)
        val tracked = coordinator.trackPoints(route, cumulative, listOf(point))
        assertEquals(1, tracked.size)
        assertTrue(tracked.single().routeProgressMeters in 400.0..440.0)
    }

    @Test fun ignoresPointsFarFromActiveRoute() {
        val coordinator = SafetyRouteAlertCoordinator(routeMatchToleranceMeters = 80.0)
        val far = point.copy(latitude = 41.01)
        assertTrue(coordinator.trackPoints(route, cumulative, listOf(far)).isEmpty())
    }

    @Test fun emitsOnlyPointsAheadOfCurrentProgress() {
        val coordinator = SafetyRouteAlertCoordinator()
        val tracked = coordinator.trackPoints(route, cumulative, listOf(point))
        val alerts = coordinator.evaluate(tracked, currentRouteProgressMeters = 0.0, bearingDegrees = 90.0)
        assertEquals(1, alerts.size)

        val later = coordinator.evaluate(tracked, currentRouteProgressMeters = 500.0, bearingDegrees = 90.0)
        assertTrue(later.isEmpty())
    }

    @Test fun passesThroughEngineDirectionFilter() {
        val coordinator = SafetyRouteAlertCoordinator()
        val tracked = coordinator.trackPoints(route, cumulative, listOf(point))
        assertTrue(coordinator.evaluate(tracked, 0.0, 270.0).isEmpty())
    }

    @Test fun rejectsInvalidOfflinePointBeforeRouteTracking() {
        val coordinator = SafetyRouteAlertCoordinator()
        val invalidOffline = point.copy(
            id = "offline-invalid",
            source = DataSource.OFFLINE,
            latitude = 95.0,
        )
        assertTrue(coordinator.trackPoints(route, cumulative, listOf(invalidOffline)).isEmpty())
    }

    @Test fun rejectsUserReportedPointFromOfflineTracking() {
        val coordinator = SafetyRouteAlertCoordinator()
        val userReportedOffline = point.copy(
            id = "offline-user-report",
            source = DataSource.OFFLINE,
            type = SafetyPointType.USER_REPORTED_SAFETY_POINT,
            confidence = Confidence.LOW,
        )
        assertTrue(coordinator.trackPoints(route, cumulative, listOf(userReportedOffline)).isEmpty())
    }
}
