package com.antigravity.smarthub.core.state

import com.antigravity.smarthub.core.model.DeviceState
import com.antigravity.smarthub.core.model.SmartHubProfile
import com.antigravity.smarthub.core.model.ThermalStatusLevel
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class StateMachineEngineTest {

    private lateinit var engine: StateMachineEngine

    @Before
    fun setUp() {
        engine = StateMachineEngine()
    }

    @Test
    fun testThermalEmergencyOverridesGamingImmediately() {
        val gamingState = ExtendedDeviceState(
            baseState = DeviceState(
                foregroundPackage = "com.pubg.imobile",
                thermalStatus = ThermalStatusLevel.NOMINAL
            )
        )
        val resolvedGaming = engine.updateState(gamingState, currentTimeMs = 1000L)
        assertEquals(SmartHubProfile.P3_GAMING_HIGH_LOAD, resolvedGaming.activeProfile)

        // Escalate to Thermal Emergency (Immediate, 0ms debounce)
        val thermalEmergencyState = ExtendedDeviceState(
            baseState = DeviceState(
                foregroundPackage = "com.pubg.imobile",
                thermalStatus = ThermalStatusLevel.CRITICAL,
                apTempC = 50.0f
            )
        )
        val resolvedEmergency = engine.updateState(thermalEmergencyState, currentTimeMs = 1100L)
        assertEquals(SmartHubProfile.P0_THERMAL_EMERGENCY, resolvedEmergency.activeProfile)
    }

    @Test
    fun testTransient2SecondForegroundChangeIgnoredDuringGaming() {
        val gamingState = ExtendedDeviceState(
            baseState = DeviceState(
                foregroundPackage = "com.pubg.imobile",
                thermalStatus = ThermalStatusLevel.NOMINAL
            )
        )
        // 1. Establish Gaming profile (T = 10,000ms)
        val resolved1 = engine.updateState(gamingState, currentTimeMs = 10_000L)
        assertEquals(SmartHubProfile.P3_GAMING_HIGH_LOAD, resolved1.activeProfile)

        // 2. User pulls down notification shade / opens System UI for 2 seconds (T = 10,500ms to 12,500ms)
        val transientSystemUiState = ExtendedDeviceState(
            baseState = DeviceState(
                foregroundPackage = "com.android.systemui",
                thermalStatus = ThermalStatusLevel.NOMINAL
            )
        )

        // First sample of candidate (T = 10,500ms)
        val resolved2 = engine.updateState(transientSystemUiState, currentTimeMs = 10_500L)
        assertEquals(SmartHubProfile.P3_GAMING_HIGH_LOAD, resolved2.activeProfile) // Holds Gaming!

        // Sample at 2.0s candidate age (T = 12,500ms, candidate age 2000ms < 3000ms threshold)
        val resolved3 = engine.updateState(transientSystemUiState, currentTimeMs = 12_500L)
        assertEquals(SmartHubProfile.P3_GAMING_HIGH_LOAD, resolved3.activeProfile) // Still Holds Gaming!

        // 3. User returns to game (T = 12,800ms) - Candidate cleared!
        val resolved4 = engine.updateState(gamingState, currentTimeMs = 12_800L)
        assertEquals(SmartHubProfile.P3_GAMING_HIGH_LOAD, resolved4.activeProfile)
    }

    @Test
    fun testPermanentGameExitAfter3SecondDebounce() {
        val gamingState = ExtendedDeviceState(
            baseState = DeviceState(
                foregroundPackage = "com.pubg.imobile",
                thermalStatus = ThermalStatusLevel.NOMINAL
            )
        )
        engine.updateState(gamingState, currentTimeMs = 10_000L)

        // Exit to launcher (T = 11_000ms)
        val launcherState = ExtendedDeviceState(
            baseState = DeviceState(
                foregroundPackage = "com.sec.android.app.launcher",
                thermalStatus = ThermalStatusLevel.NOMINAL
            )
        )

        engine.updateState(launcherState, currentTimeMs = 11_000L) // Candidate set at 11,000ms
        val resolvedDebouncing = engine.updateState(launcherState, currentTimeMs = 13_500L) // 2.5s age < 3.0s
        assertEquals(SmartHubProfile.P3_GAMING_HIGH_LOAD, resolvedDebouncing.activeProfile)

        // T = 14_100ms (3.1s age >= 3.0s threshold) -> Profile transitions!
        val resolvedExited = engine.updateState(launcherState, currentTimeMs = 14_100L)
        assertEquals(SmartHubProfile.P5_DAILY_ADAPTIVE, resolvedExited.activeProfile)
    }

    @Test
    fun testScreenOffAt2PMDoesNotTriggerOvernightDeepIdle() {
        val afternoonScreenOffState = ExtendedDeviceState(
            baseState = DeviceState(
                isScreenOn = false,
                batteryPercent = 80
            ),
            currentHourOfDay = 14, // 2:00 PM
            screenOffDurationMs = 1_200_000L
        )
        val resolved = engine.updateState(afternoonScreenOffState, currentTimeMs = 10_000L)
        assertEquals(SmartHubProfile.P5_DAILY_ADAPTIVE, resolved.activeProfile)
    }
}
