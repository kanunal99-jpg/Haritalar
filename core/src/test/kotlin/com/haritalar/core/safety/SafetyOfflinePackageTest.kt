package com.haritalar.core.safety

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SafetyOfflinePackageTest {
    private fun point(
        id: String = "offline-1",
        source: DataSource = DataSource.OFFLINE,
        type: SafetyPointType = SafetyPointType.FIXED_SPEED_CAMERA,
        confidence: Confidence = Confidence.HIGH,
        latitude: Double = 41.0,
        longitude: Double = 29.0,
    ) = SafetyPoint(id, latitude, longitude, type, confidence, source)

    @Test
    fun validOfflinePackageIsAccepted() {
        assertTrue(
            SafetyOfflinePackageValidator.validate(
                SafetyOfflinePackage(1, listOf(SafetyOfflineRegion("istanbul", listOf(point()))))
            )
        )
    }

    @Test
    fun unsupportedVersionIsRejected() {
        assertFalse(SafetyOfflinePackageValidator.validate(SafetyOfflinePackage(2, emptyList())))
    }

    @Test
    fun nonOfflineSourceIsRejected() {
        val pkg = SafetyOfflinePackage(1, listOf(SafetyOfflineRegion("r", listOf(point(source = DataSource.LIVE)))))
        assertFalse(SafetyOfflinePackageValidator.validate(pkg))
    }

    @Test
    fun userReportedPointsCannotEnterOfflinePackage() {
        val pkg = SafetyOfflinePackage(1, listOf(SafetyOfflineRegion("r", listOf(point(type = SafetyPointType.USER_REPORTED_SAFETY_POINT)))))
        assertFalse(SafetyOfflinePackageValidator.validate(pkg))
    }

    @Test
    fun invalidCoordinatesAreRejected() {
        val pkg = SafetyOfflinePackage(1, listOf(SafetyOfflineRegion("r", listOf(point(latitude = 91.0)))))
        assertFalse(SafetyOfflinePackageValidator.validate(pkg))
    }

    @Test
    fun invalidDirectionIsRejected() {
        val invalid = point().copy(directionBearingDegrees = 360.0)
        val pkg = SafetyOfflinePackage(1, listOf(SafetyOfflineRegion("r", listOf(invalid))))
        assertFalse(SafetyOfflinePackageValidator.validate(pkg))
    }

    @Test
    fun verifiedTrafficControlRequiresHighConfidence() {
        val invalid = point(
            type = SafetyPointType.VERIFIED_TRAFFIC_CONTROL,
            confidence = Confidence.MEDIUM,
        )
        val pkg = SafetyOfflinePackage(1, listOf(SafetyOfflineRegion("r", listOf(invalid))))
        assertFalse(SafetyOfflinePackageValidator.validate(pkg))
    }

    @Test
    fun duplicateRegionIdsAreRejected() {
        val pkg = SafetyOfflinePackage(
            1,
            listOf(
                SafetyOfflineRegion("same", emptyList()),
                SafetyOfflineRegion("same", emptyList()),
            ),
        )
        assertFalse(SafetyOfflinePackageValidator.validate(pkg))
    }

    @Test
    fun duplicatePointIdsAcrossRegionsAreRejected() {
        val pkg = SafetyOfflinePackage(
            1,
            listOf(
                SafetyOfflineRegion("a", listOf(point(id = "same"))),
                SafetyOfflineRegion("b", listOf(point(id = "same"))),
            ),
        )
        assertFalse(SafetyOfflinePackageValidator.validate(pkg))
    }

    @Test
    fun nonPositiveSpeedIsRejected() {
        val invalid = point().copy(speedLimitKmh = 0)
        val pkg = SafetyOfflinePackage(1, listOf(SafetyOfflineRegion("r", listOf(invalid))))
        assertFalse(SafetyOfflinePackageValidator.validate(pkg))
    }
}
