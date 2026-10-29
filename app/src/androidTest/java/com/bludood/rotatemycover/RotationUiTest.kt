package com.bludood.rotatemycover

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.bludood.rotatemycover.ui.DashboardScreen
import com.bludood.rotatemycover.ui.OnboardingScreen
import com.bludood.rotatemycover.ui.theme.RotateMyCoverTheme
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class RotationUiTest {
    @get:Rule val compose = createComposeRule()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private lateinit var preferences: Map<String, *>
    private var systemRotation = ""

    @Before fun prepare() {
        check(Build.MODEL.contains("sdk_gphone") || Build.FINGERPRINT.contains("emulator")) {
            "These state-changing checks are emulator-only"
        }
        preferences = context.getSharedPreferences("rotation_prefs", Context.MODE_PRIVATE).all.toMap()
        systemRotation = shell("settings get system accelerometer_rotation").trim()
        setSystemRotation(true)
        RotationManager.init(context)
        RotationManager.enableAutoRotation(context)
        RotationManager.setFollowSystemRotation(context, true)
        RotationManager.setKeepRotationOnDisable(context, false)
    }

    @After fun restore() {
        if (!::preferences.isInitialized) return
        context.getSharedPreferences("rotation_prefs", Context.MODE_PRIVATE).edit().clear().apply {
            preferences.forEach { (key, value) -> putBoolean(key, value as Boolean) }
        }.commit()
        RotationManager.init(context)
        if (systemRotation == "null") shell("settings delete system accelerometer_rotation")
        else shell("settings put system accelerometer_rotation $systemRotation")
    }

    @Test fun dashboardFollowsSystemLockAndPersistsRowChanges() {
        dashboard()
        compose.onNodeWithText("Automatic rotation enabled").assertIsDisplayed()
        capture("dashboard-dark")
        setSystemRotation(false)
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Paused by system rotation lock").fetchSemanticsNodes().isNotEmpty() }
        capture("dashboard-paused")
        compose.onNodeWithText("Follow system rotation lock").performClick().assertIsOff()
        compose.onNodeWithText("Automatic rotation enabled").assertIsDisplayed()
        compose.onNodeWithText("Disable rotation").performClick()
        compose.onNodeWithText("Automatic rotation off · Portrait mode").assertIsDisplayed()
        compose.onNodeWithText("Keep orientation when rotation stops").performClick().assertIsOn()
        compose.onNodeWithText("Automatic rotation off · Keep current orientation").assertIsDisplayed()
        capture("dashboard-off-keep")
        compose.runOnIdle {
            RotationManager.init(context)
            assertEquals(false, RotationManager.isAutoRotationEnabled)
            assertEquals(false, RotationManager.followSystemRotation)
            assertEquals(true, RotationManager.keepRotationOnDisable)
        }
        compose.onNodeWithText("Enable rotation").performClick()
        compose.onNodeWithText("Automatic rotation enabled").assertIsDisplayed()
        compose.onNodeWithText("Permissions ready").performClick()
        compose.onNodeWithText("Allowed").assertIsDisplayed()
        compose.onNodeWithText("Enabled").assertIsDisplayed()
        capture("permissions-expanded-dark")
    }

    @Test fun setupDark() {
        render { OnboardingScreen(false, false) }
        compose.onNodeWithText("Allow").assertIsDisplayed()
        compose.onNodeWithText("Open settings").assertIsDisplayed()
        capture("setup-dark")
    }

    @Test fun pressedRotationButton() {
        dashboard()
        compose.mainClock.autoAdvance = false
        try {
            compose.onNodeWithText("Disable rotation").performTouchInput { down(center) }
            compose.mainClock.advanceTimeBy(100)
            capture("rotation-button-pressed")
            compose.onNodeWithText("Disable rotation").performTouchInput { up() }
        } finally {
            compose.mainClock.autoAdvance = true
        }
        compose.onNodeWithText("Enable rotation").assertIsDisplayed()
    }

    @Test fun lightTheme() {
        dashboard(dark = false)
        compose.onNodeWithText("Disable rotation").assertIsDisplayed()
        capture("dashboard-light")
    }

    @Test fun dynamicTheme() {
        dashboard(dynamic = true)
        compose.onNodeWithText("Disable rotation").assertIsDisplayed()
        capture("dashboard-dynamic-dark")
    }

    @Test fun rtlTheme() {
        dashboard(rtl = true)
        compose.onNodeWithText("Follow system rotation lock").assertIsDisplayed()
        capture("dashboard-rtl")
    }

    @Test fun narrowLargeTextDashboardRemainsReachable() {
        render(narrow = true) { DashboardScreen(true, true) }
        compose.onNodeWithText("Disable rotation").assertIsDisplayed()
        compose.onNodeWithText("Keep orientation when rotation stops").performScrollTo().assertIsDisplayed().performClick().assertIsOn()
        capture("dashboard-320dp-2x-scrolled")
    }

    @Test fun narrowLargeTextSetupRemainsReachable() {
        render(narrow = true) { OnboardingScreen(false, false) }
        compose.onNodeWithText("Open settings").performScrollTo().assertIsDisplayed()
        capture("setup-320dp-2x-scrolled")
    }

    @Test fun shortCoverWindowRemainsReachable() {
        render(short = true) { DashboardScreen(true, true) }
        compose.onNodeWithText("Disable rotation").assertIsDisplayed()
        compose.onNodeWithText("Keep orientation when rotation stops").performScrollTo().assertIsDisplayed()
        capture("dashboard-short-window")
    }

    private fun dashboard(dark: Boolean = true, dynamic: Boolean = false, rtl: Boolean = false) {
        render(dark, dynamic, rtl) { DashboardScreen(true, true) }
    }

    private fun render(dark: Boolean = true, dynamic: Boolean = false, rtl: Boolean = false,
        narrow: Boolean = false, short: Boolean = false, content: @androidx.compose.runtime.Composable () -> Unit) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, if (narrow) 2f else density.fontScale),
                LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr
            ) {
                RotateMyCoverTheme(darkTheme = dark, dynamicColor = dynamic) {
                    val modifier = when {
                        narrow -> Modifier.width(320.dp).fillMaxHeight()
                        short -> Modifier.size(width = 360.dp, height = 320.dp)
                        else -> Modifier.fillMaxSize()
                    }
                    Scaffold(modifier = modifier) { padding ->
                        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) { content() }
                    }
                }
            }
        }
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val image = compose.onRoot().captureToImage().asAndroidBitmap()
        val directory = File(context.getExternalFilesDir(null), "ui-verification").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun setSystemRotation(enabled: Boolean) {
        shell("settings put system accelerometer_rotation ${if (enabled) 1 else 0}")
    }

    private fun shell(command: String): String = instrumentation.uiAutomation.executeShellCommand(command).use {
        android.os.ParcelFileDescriptor.AutoCloseInputStream(it).bufferedReader().readText()
    }
}
