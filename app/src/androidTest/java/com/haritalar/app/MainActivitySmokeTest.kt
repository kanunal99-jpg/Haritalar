package com.haritalar.app

import android.Manifest
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withHint
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.AndroidJUnit4

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
            assertInitialControls()
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
            assertInitialControls()
        } finally {
            first.close()
        }

        val second = try {
            ActivityScenario.launch(MainActivity::class.java)
        } catch (error: Throwable) {
            throw AssertionError("Second MainActivity launch failed after destroying the first activity. Live diagnostics:\n${liveDiagnostics()}", error)
        }
        try {
            assertInitialControls()
        } catch (error: Throwable) {
            throw AssertionError("Second MainActivity launch succeeded but controls were not verified. Live diagnostics:\n${liveDiagnostics()}", error)
        } finally {
            second.close()
        }
    }

    private fun assertInitialControls() {
        onView(withHint("Nereye gitmek istiyorsun?"))
            .check(matches(isDisplayed()))
        onView(withText("Ara"))
            .check(matches(isDisplayed()))
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
