package com.antigravity.smarthub

import com.antigravity.smarthub.core.model.PrivilegeTier
import com.antigravity.smarthub.core.model.ThermalStatusLevel
import com.antigravity.smarthub.core.telemetry.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class MainActivityStateMappingTest {

    @Test
    fun testAuthenticSnapshotMapping() {
        val snapshot = DeviceTelemetrySnapshot(
            capturedAtMs = 123456789L,
            battery = TelemetryValue(
                BatteryMetrics(
                    percent = 85,
                    tempC = 31.5f,
                    voltageMv = 4100,
                    currentNowMa = 500,
                    currentAvgMa = 500,
                    isCharging = true,
                    plugType = "AC"
                ),
                TelemetryState.AVAILABLE
            ),
            thermalStatus = TelemetryValue(ThermalStatusLevel.MODERATE, TelemetryState.AVAILABLE),
            measuredApTempC = TelemetryValue(38.2f, TelemetryState.AVAILABLE),
            memAvailableKb = TelemetryValue(2048000L, TelemetryState.AVAILABLE),
            memoryPsi = TelemetryValue(
                PsiMetric(
                    someAvg10 = 0.12f,
                    someAvg60 = 0.05f,
                    someAvg300 = 0.01f,
                    fullAvg10 = 0.0f,
                    fullAvg60 = 0.0f,
                    fullAvg300 = 0.0f
                ),
                TelemetryState.AVAILABLE
            ),
            display = TelemetryValue(
                DisplayMetrics(
                    secureSettingMode = 0,
                    physicalRefreshRateHz = 120.0f,
                    isScreenOn = true,
                    displayStateStr = "ON"
                ),
                TelemetryState.AVAILABLE
            ),
            foregroundPackage = TelemetryValue("com.google.android.youtube", TelemetryState.AVAILABLE)
        )

        val deviceState = MainActivity.snapshotToDeviceState(snapshot, isShizukuConnected = true)

        assertEquals(85, deviceState.batteryPercent)
        assertEquals(true, deviceState.isCharging)
        assertEquals(31.5f, deviceState.batteryTempC, 0.01f)
        assertEquals(38.2f, deviceState.apTempC, 0.01f)
        assertEquals(ThermalStatusLevel.MODERATE, deviceState.thermalStatus)
        assertEquals(2000L, deviceState.memoryAvailableMb)
        assertEquals(0.12f, deviceState.memoryPsiAvg10, 0.01f)
        assertEquals(0, deviceState.activeRefreshRateMode)
        assertEquals("com.google.android.youtube", deviceState.foregroundPackage)
        assertEquals(PrivilegeTier.TIER_1_SHIZUKU, deviceState.privilegeTier)
    }

    @Test
    fun testFallbackApTempUsesBatteryTempWithoutFabrication() {
        val snapshot = DeviceTelemetrySnapshot(
            battery = TelemetryValue(
                BatteryMetrics(
                    percent = 50,
                    tempC = 29.0f,
                    voltageMv = 3800,
                    currentNowMa = -300,
                    currentAvgMa = -300,
                    isCharging = false,
                    plugType = null
                ),
                TelemetryState.AVAILABLE
            ),
            measuredApTempC = TelemetryValue.unavailable()
        )

        val state = MainActivity.snapshotToDeviceState(snapshot, isShizukuConnected = false)
        assertEquals(29.0f, state.apTempC, 0.01f)
        assertNotEquals(32.0f, state.apTempC, 0.01f)
        assertEquals(PrivilegeTier.TIER_0_STOCK, state.privilegeTier)
    }

    @Test
    fun testUnavailableForegroundPackageIsEmpty() {
        val snapshot = DeviceTelemetrySnapshot(
            foregroundPackage = TelemetryValue.unavailable()
        )

        val state = MainActivity.snapshotToDeviceState(snapshot, isShizukuConnected = false)
        assertEquals("", state.foregroundPackage)
        assertNotEquals("com.sec.android.app.launcher", state.foregroundPackage)
    }
}
