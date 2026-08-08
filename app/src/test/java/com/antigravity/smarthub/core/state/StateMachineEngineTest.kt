package com.antigravity.smarthub.core.state

import com.antigravity.smarthub.core.model.DeviceState
import com.antigravity.smarthub.core.model.SmartHubProfile
import com.antigravity.smarthub.core.model.ThermalStatusLevel
import com.antigravity.smarthub.core.telemetry.TelemetryState
import com.antigravity.smarthub.core.telemetry.TelemetryValue
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
                foregroundPackage = TelemetryValue("com.pubg.imobile", TelemetryState.AVAILABLE),
                thermalStatus = TelemetryValue(ThermalStatusLevel.NOMINAL, TelemetryState.AVAILABLE)
            )
        )
        val resolvedGaming = engine.updateState(gamingState, currentTimeMs = 1000L)
        assertEquals(SmartHubProfile.P3_GAMING_HIGH_LOAD, resolvedGaming)

        // Escalate to Thermal Emergency (Immediate, 0ms debounce)
        val thermalEmergencyState = ExtendedDeviceState(
            baseState = DeviceState(
                foregroundPackage = TelemetryValue("com.pubg.imobile", TelemetryState.AVAILABLE),
                thermalStatus = TelemetryValue(ThermalStatusLevel.CRITICAL, TelemetryState.AVAILABLE),
                apTempC = TelemetryValue(50.0f, TelemetryState.AVAILABLE)
            )
        )
        val resolvedEmergency = engine.updateState(thermalEmergencyState, currentTimeMs = 1100L)
        assertEquals(SmartHubProfile.P0_THERMAL_EMERGENCY, resolvedEmergency)
    }

    @Test
    fun testUnavailableBatteryDoesNotTriggerCriticalBattery() {
        val unavailableBatteryState = ExtendedDeviceState(
            baseState = DeviceState(
                batteryPercent = TelemetryValue.unavailable(),
                isCharging = TelemetryValue.unavailable()
            )
        )
        val profile = engine.updateState(unavailableBatteryState, currentTimeMs = 1000L)
        assertEquals(SmartHubProfile.P5_DAILY_ADAPTIVE, profile)
    }

    @Test
    fun testUnavailableThermalDoesNotTriggerThermalEmergency() {
        val unavailableThermalState = ExtendedDeviceState(
            baseState = DeviceState(
                thermalStatus = TelemetryValue.unavailable(),
                apTempC = TelemetryValue.unavailable(),
                batteryTempC = TelemetryValue.unavailable()
            )
        )
        val profile = engine.updateState(unavailableThermalState, currentTimeMs = 1000L)
        assertEquals(SmartHubProfile.P5_DAILY_ADAPTIVE, profile)
    }

    @Test
    fun testMediaTelemetryReachesP4() {
        val mediaState = ExtendedDeviceState(
            baseState = DeviceState(
                foregroundPackage = TelemetryValue("com.google.android.apps.youtube.music", TelemetryState.AVAILABLE)
            ),
            isMediaPlaying = true
        )
        engine.updateState(mediaState, currentTimeMs = 1000L) // Candidate set
        val profile = engine.updateState(mediaState, currentTimeMs = 2500L) // Dwell time satisfied (>1000ms)
        assertEquals(SmartHubProfile.P4_MEDIA_READING, profile)
    }

    @Test
    fun testNavigationPreventsOvernightDeepIdle() {
        val navOvernightState = ExtendedDeviceState(
            baseState = DeviceState(
                isScreenOn = TelemetryValue(false, TelemetryState.AVAILABLE)
            ),
            isNavigationActive = true,
            currentHourOfDay = 2, // 2:00 AM
            screenOffDurationMs = 1_200_000L // >15 min
        )
        val profile = engine.updateState(navOvernightState, currentTimeMs = 1000L)
        assertEquals(SmartHubProfile.P5_DAILY_ADAPTIVE, profile)
    }

    @Test
    fun testRealOvernightTimingReachesP6() {
        val overnightState = ExtendedDeviceState(
            baseState = DeviceState(
                isScreenOn = TelemetryValue(false, TelemetryState.AVAILABLE)
            ),
            isNavigationActive = false,
            isMediaPlaying = false,
            currentHourOfDay = 2, // 2:00 AM
            screenOffDurationMs = 1_200_000L // >15 min
        )
        engine.updateState(overnightState, currentTimeMs = 1000L) // Candidate set
        val profile = engine.updateState(overnightState, currentTimeMs = 2500L) // Dwell time satisfied (>1000ms)
        assertEquals(SmartHubProfile.P6_OVERNIGHT_DEEP_IDLE, profile)
    }

    @Test
    fun testScreenOffAt2PMDoesNotTriggerOvernightDeepIdle() {
        val afternoonScreenOffState = ExtendedDeviceState(
            baseState = DeviceState(
                isScreenOn = TelemetryValue(false, TelemetryState.AVAILABLE),
                batteryPercent = TelemetryValue(80, TelemetryState.AVAILABLE)
            ),
            currentHourOfDay = 14, // 2:00 PM
            screenOffDurationMs = 1_200_000L
        )
        val profile = engine.updateState(afternoonScreenOffState, currentTimeMs = 10_000L)
        assertEquals(SmartHubProfile.P5_DAILY_ADAPTIVE, profile)
    }
}
