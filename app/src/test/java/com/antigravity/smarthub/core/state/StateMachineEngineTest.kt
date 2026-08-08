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
        val resolvedGaming = engine.updateState(gamingState)
        assertEquals(SmartHubProfile.P3_GAMING_HIGH_LOAD, resolvedGaming.activeProfile)

        // Escalate to Thermal Emergency
        val thermalEmergencyState = ExtendedDeviceState(
            baseState = DeviceState(
                foregroundPackage = "com.pubg.imobile",
                thermalStatus = ThermalStatusLevel.CRITICAL,
                apTempC = 50.0f
            )
        )
        val resolvedEmergency = engine.updateState(thermalEmergencyState)
        assertEquals(SmartHubProfile.P0_THERMAL_EMERGENCY, resolvedEmergency.activeProfile)
    }

    @Test
    fun testScreenOffAt2PMDoesNotTriggerOvernightDeepIdle() {
        val afternoonScreenOffState = ExtendedDeviceState(
            baseState = DeviceState(
                isScreenOn = false,
                batteryPercent = 80
            ),
            currentHourOfDay = 14, // 2:00 PM
            screenOffDurationMs = 1_200_000L // 20 mins
        )
        val resolved = engine.updateState(afternoonScreenOffState)
        // Should NOT be P6_OVERNIGHT_DEEP_IDLE because hour is 14 (not between 23:00 and 06:00)
        assertEquals(SmartHubProfile.P5_DAILY_ADAPTIVE, resolved.activeProfile)
    }

    @Test
    fun testOvernightDeepIdleTriggersWithExactConditions() {
        val overnightState = ExtendedDeviceState(
            baseState = DeviceState(
                isScreenOn = false,
                batteryPercent = 80
            ),
            currentHourOfDay = 2, // 2:00 AM
            isMediaPlaying = false,
            isNavigationActive = false,
            screenOffDurationMs = 1_000_000L // > 15 mins
        )
        val resolved = engine.updateState(overnightState)
        assertEquals(SmartHubProfile.P6_OVERNIGHT_DEEP_IDLE, resolved.activeProfile)
    }

    @Test
    fun testMediaBrowsingVsActivePlayback() {
        // App in foreground but NO active playback
        val browsingState = ExtendedDeviceState(
            baseState = DeviceState(
                foregroundPackage = "in.startv.hotstar"
            ),
            isMediaPlaying = false
        )
        val resolvedBrowsing = engine.updateState(browsingState)
        assertEquals(SmartHubProfile.P5_DAILY_ADAPTIVE, resolvedBrowsing.activeProfile)

        // Active MediaSession Playback
        val playbackState = ExtendedDeviceState(
            baseState = DeviceState(
                foregroundPackage = "in.startv.hotstar"
            ),
            isMediaPlaying = true
        )
        val resolvedPlayback = engine.updateState(playbackState)
        assertEquals(SmartHubProfile.P4_MEDIA_READING, resolvedPlayback.activeProfile)
    }
}
