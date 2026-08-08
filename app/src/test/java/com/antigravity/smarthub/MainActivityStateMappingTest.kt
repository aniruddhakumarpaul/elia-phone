package com.antigravity.smarthub

import com.antigravity.smarthub.core.model.PrivilegeTier
import com.antigravity.smarthub.core.model.ThermalStatusLevel
import com.antigravity.smarthub.core.persistence.BaselineRepository
import com.antigravity.smarthub.core.safety.SafetyGovernor
import com.antigravity.smarthub.core.state.OptimizationController
import com.antigravity.smarthub.core.state.ProfileResolver
import com.antigravity.smarthub.core.state.StateMachineEngine
import com.antigravity.smarthub.core.telemetry.*
import com.antigravity.smarthub.platform.shizuku.ShizukuServiceConnection
import com.antigravity.smarthub.platform.shizuku.SystemActionExecutor
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.File

class MainActivityStateMappingTest {

    private lateinit var controller: OptimizationController

    @Before
    fun setUp() {
        val connection = ShizukuServiceConnection()
        val tempDir = File(System.getProperty("java.io.tmpdir"), "smarthub_test_${System.currentTimeMillis()}")
        val repo = BaselineRepository(tempDir)
        val safety = SafetyGovernor()
        val executor = SystemActionExecutor(connection, safety, repo)
        val aggregator = TelemetryAggregator()

        controller = OptimizationController(
            telemetryAggregator = aggregator,
            stateMachineEngine = StateMachineEngine(),
            profileResolver = ProfileResolver(),
            safetyGovernor = safety,
            actionExecutor = executor,
            shizukuConnection = connection
        )
    }

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
                    secureSettingMode = TelemetryValue.available(0),
                    physicalRefreshRateHz = TelemetryValue.available(120.0f),
                    isScreenOn = TelemetryValue.available(true),
                    displayStateStr = TelemetryValue.available("ON")
                ),
                TelemetryState.AVAILABLE
            ),
            foregroundPackage = TelemetryValue("com.google.android.youtube", TelemetryState.AVAILABLE)
        )

        val extState = controller.snapshotToExtendedDeviceState(snapshot)
        val dState = extState.baseState

        assertEquals(85, dState.batteryPercent.value)
        assertEquals(true, dState.isCharging.value)
        assertEquals(31.5f, dState.batteryTempC.value ?: 0f, 0.01f)
        assertEquals(38.2f, dState.apTempC.value ?: 0f, 0.01f)
        assertEquals(ThermalStatusLevel.MODERATE, dState.thermalStatus.value)
        assertEquals(2000L, dState.memoryAvailableMb.value)
        assertEquals(0.12f, dState.memoryPsiAvg10.value ?: 0f, 0.01f)
        assertEquals(0, dState.activeRefreshRateMode.value)
        assertEquals("com.google.android.youtube", dState.foregroundPackage.value)
        assertEquals(PrivilegeTier.TIER_0_STOCK, dState.privilegeTier)
    }
}
