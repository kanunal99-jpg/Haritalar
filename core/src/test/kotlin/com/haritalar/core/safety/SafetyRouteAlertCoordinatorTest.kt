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
        val invalidOffline = point.copy(id = "offline-invalid", source = DataSource.OFFLINE, latitude = 95.0)
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

    @Test fun prefersLiveRecordWhenCacheAndLiveOverlap() {
        val coordinator = SafetyRouteAlertCoordinator()
        val cache = point.copy(id = "cache-camera", source = DataSource.CACHE, confidence = Confidence.HIGH)
        val deduplicated = coordinator.deduplicate(listOf(cache, point))
        assertEquals(1, deduplicated.size)
        assertEquals(DataSource.LIVE, deduplicated.single().source)
    }

    @Test fun prefersHighConfidenceWhenSourcesAreEqual() {
        val coordinator = SafetyRouteAlertCoordinator()
        val medium = point.copy(id = "medium", confidence = Confidence.MEDIUM)
        val high = point.copy(id = "high", confidence = Confidence.HIGH)
        val deduplicated = coordinator.deduplicate(listOf(medium, high))
        assertEquals(1, deduplicated.size)
        assertEquals("high", deduplicated.single().id)
    }

    @Test fun keepsDifferentSafetyTypesAtSameLocation() {
        val coordinator = SafetyRouteAlertCoordinator()
        val trafficLight = point.copy(id = "traffic", type = SafetyPointType.TRAFFIC_LIGHT_CAMERA)
        assertEquals(2, coordinator.deduplicate(listOf(point, trafficLight)).size)
    }

    @Test fun doesNotMergePointsBeyondTolerance() {
        val coordinator = SafetyRouteAlertCoordinator(duplicatePointToleranceMeters = 50.0)
        val far = point.copy(id = "far", longitude = 29.00600)
        assertEquals(2, coordinator.deduplicate(listOf(point, far)).size)
    }
}
