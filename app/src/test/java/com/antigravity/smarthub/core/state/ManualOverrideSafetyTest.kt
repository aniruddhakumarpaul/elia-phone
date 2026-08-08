package com.antigravity.smarthub.core.state

import com.antigravity.smarthub.core.model.DeviceState
import com.antigravity.smarthub.core.model.SmartHubProfile
import com.antigravity.smarthub.core.model.ThermalStatusLevel
import com.antigravity.smarthub.core.telemetry.TelemetryValue
import org.junit.Assert.assertEquals
import org.junit.Test

class ManualOverrideSafetyTest {
    @Test
    fun manualProfileIsUsedWhenThermalSafetyIsNormal() {
        val engine = StateMachineEngine()
        engine.setManualProfileOverride(SmartHubProfile.P4_MEDIA_READING)
        val profile = engine.updateState(ExtendedDeviceState(DeviceState()), System.currentTimeMillis())
        assertEquals(SmartHubProfile.P4_MEDIA_READING, profile)
    }

    @Test
    fun thermalEmergencyVetoesManualOverrideImmediately() {
        val engine = StateMachineEngine()
        engine.setManualProfileOverride(SmartHubProfile.P3_GAMING_HIGH_LOAD)
        val now = System.currentTimeMillis()
        val critical = DeviceState(
            thermalStatus = TelemetryValue(ThermalStatusLevel.CRITICAL, com.antigravity.smarthub.core.telemetry.TelemetryState.AVAILABLE, now),
            batteryTempC = TelemetryValue(45f, com.antigravity.smarthub.core.telemetry.TelemetryState.AVAILABLE, now)
        )
        assertEquals(SmartHubProfile.P0_THERMAL_EMERGENCY, engine.updateState(ExtendedDeviceState(critical), now))
    }
}
