package com.haritalar.app

import android.Manifest
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.haritalar.core.navigation.GeoCoordinate
import com.haritalar.core.traffic.TomTomTrafficProvider
import com.haritalar.core.traffic.TrafficBounds
import com.haritalar.core.traffic.TrafficSnapshot
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.maplibre.android.maps.MapView
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class MainActivitySmokeTest {
    @get:Rule
    val locationPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )

    @Test
    fun mainActivityLaunchesAndShowsInitialControls() {
        val scenario = try {
            ActivityScenario.launch(MainActivity::class.java)
        } catch (error: Throwable) {
            throw AssertionError("MainActivity launch failed. Live diagnostics:\n${liveDiagnostics()}", error)
        }

        try {
            assertInitialControls(scenario)
        } catch (error: Throwable) {
            throw AssertionError("MainActivity launched but initial controls were not verified. Live diagnostics:\n${liveDiagnostics()}", error)
        } finally {
            scenario.close()
        }
    }

    @Test
    fun mainActivityCanBeOpenedTwiceAfterDestroy() {
        val first = try {
            ActivityScenario.launch(MainActivity::class.java)
        } catch (error: Throwable) {
            throw AssertionError("First MainActivity launch failed. Live diagnostics:\n${liveDiagnostics()}", error)
        }
        try {
            assertInitialControls(first)
            assertMapViewCreated(first)
        } finally {
            first.close()
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        }

        val second = try {
            ActivityScenario.launch(MainActivity::class.java)
        } catch (error: Throwable) {
            throw AssertionError("Second MainActivity launch failed after destroying the first activity. Live diagnostics:\n${liveDiagnostics()}", error)
        }
        try {
            // Allow MapLibre/native teardown from the first instance to settle before
            // checking that the second activity remains alive.
            SystemClock.sleep(1_000L)
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            second.onActivity { activity ->
                check(!activity.isFinishing && !activity.isDestroyed) {
                    "Second MainActivity was not left in a usable lifecycle state"
                }
            }
            assertInitialControls(second)
            assertMapViewCreated(second)
        } catch (error: Throwable) {
            throw AssertionError("Second MainActivity launch succeeded but controls were not verified. Live diagnostics:\n${liveDiagnostics()}", error)
        } finally {
            second.close()
        }
    }

    @Test
    fun packagedTomTomCredentialCanFetchLiveTraffic() {
        check(BuildConfig.TOMTOM_API_KEY.isNotBlank()) {
            "TOMTOM_API_KEY is empty in the packaged app BuildConfig"
        }

        val point = GeoCoordinate(latitude = 41.0082, longitude = 28.9784)
        val snapshot = try {
            fetchLiveSnapshot(point)
        } catch (error: Throwable) {
            throw AssertionError("Packaged TomTom credential failed at runtime. Live diagnostics:\n${liveDiagnostics()}", error)
        }

        check(snapshot.providerId == TomTomTrafficProvider.ID) {
            "Unexpected traffic provider: ${snapshot.providerId}"
        }
        check(snapshot.segments.isNotEmpty()) {
            "TomTom returned no verified traffic segment for the live smoke-test point"
        }
        check(snapshot.confidence.name == "HIGH") {
            "TomTom live snapshot confidence was ${snapshot.confidence}"
        }
    }

    private fun fetchLiveSnapshot(point: GeoCoordinate): TrafficSnapshot {
        val provider = TomTomTrafficProvider(
            apiKey = BuildConfig.TOMTOM_API_KEY,
            maxSamples = 1,
        )
        val completed = CountDownLatch(1)
        val result = AtomicReference<Result<TrafficSnapshot>?>()
        val block: suspend () -> TrafficSnapshot = {
            provider.fetchTraffic(
                bounds = TrafficBounds(
                    south = point.latitude,
                    west = point.longitude,
                    north = point.latitude,
                    east = point.longitude,
                ),
                route = null,
            )
        }
        block.startCoroutine(object : Continuation<TrafficSnapshot> {
            override val context = EmptyCoroutineContext

            override fun resumeWith(value: Result<TrafficSnapshot>) {
                result.set(value)
                completed.countDown()
            }
        })

        check(completed.await(30L, TimeUnit.SECONDS)) {
            "Timed out waiting for live TomTom traffic response"
        }
        return result.get()?.getOrThrow()
            ?: error("Live TomTom traffic response completed without a result")
    }

    private fun assertInitialControls(scenario: ActivityScenario<MainActivity>) {
        scenario.onActivity { activity ->
            val content = activity.findViewById<ViewGroup>(android.R.id.content)
            check(findViewWithHint(content, "Nereye gitmek istiyorsun?") != null) {
                "Destination input with expected hint was not created"
            }
            check(findTextView(content, "Ara") != null) {
                "Search control with expected text was not created"
            }
        }
    }

    private fun assertMapViewCreated(scenario: ActivityScenario<MainActivity>) {
        scenario.onActivity { activity ->
            val content = activity.findViewById<ViewGroup>(android.R.id.content)
            check(findMapView(content) != null) {
                "MapLibre MapView was not created in the activity view hierarchy"
            }
        }
    }

    private fun findViewWithHint(root: View, hint: String): EditText? {
        if (root is EditText && root.hint?.toString() == hint) return root
        if (root !is ViewGroup) return null
        for (index in 0 until root.childCount) {
            findViewWithHint(root.getChildAt(index), hint)?.let { return it }
        }
        return null
    }

    private fun findTextView(root: View, text: String): TextView? {
        if (root is TextView && root.text?.toString() == text) return root
        if (root !is ViewGroup) return null
        for (index in 0 until root.childCount) {
            findTextView(root.getChildAt(index), text)?.let { return it }
        }
        return null
    }

    private fun findMapView(root: View): MapView? {
        if (root is MapView) return root
        if (root !is ViewGroup) return null
        for (index in 0 until root.childCount) {
            findMapView(root.getChildAt(index))?.let { return it }
        }
        return null
    }

    private fun liveDiagnostics(): String {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val uiAutomation = instrumentation.uiAutomation
        return buildString {
            append("--- activity state ---\n")
            append(readShell(uiAutomation.executeShellCommand("dumpsys activity activities"), 12_000))
            append("\n--- package state ---\n")
            append(readShell(uiAutomation.executeShellCommand("dumpsys package com.haritalar.app"), 12_000))
            append("\n--- error logcat ---\n")
            append(readShell(uiAutomation.executeShellCommand("logcat -d -v threadtime *:E"), 24_000))
        }
    }

    private fun readShell(descriptor: ParcelFileDescriptor, maxChars: Int): String {
        return ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use { reader ->
            val text = reader.readText()
            if (text.length <= maxChars) text else text.takeLast(maxChars)
        }
    }
}
