package com.antigravity.smarthub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.antigravity.smarthub.core.model.DeviceState
import com.antigravity.smarthub.core.model.PrivilegeTier
import com.antigravity.smarthub.core.model.ThermalStatusLevel
import com.antigravity.smarthub.core.safety.SafetyGovernor
import com.antigravity.smarthub.core.state.ProfileResolver
import com.antigravity.smarthub.core.state.ResolvedState
import com.antigravity.smarthub.core.state.StateMachineEngine
import com.antigravity.smarthub.core.telemetry.DeviceTelemetrySnapshot
import com.antigravity.smarthub.core.telemetry.TelemetryAggregator
import com.antigravity.smarthub.platform.shizuku.ShizukuServiceConnection
import com.antigravity.smarthub.ui.dashboard.DashboardScreen
import com.antigravity.smarthub.ui.theme.SmartHubTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var telemetryAggregator: TelemetryAggregator

    @Inject
    lateinit var stateMachineEngine: StateMachineEngine

    @Inject
    lateinit var safetyGovernor: SafetyGovernor

    @Inject
    lateinit var profileResolver: ProfileResolver

    @Inject
    lateinit var shizukuConnection: ShizukuServiceConnection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SmartHubTheme {
                SmartHubMainContent()
            }
        }
    }

    @Composable
    private fun SmartHubMainContent() {
        val telemetrySnapshot by telemetryAggregator.telemetryStream.collectAsState()

        val isShizukuBound = remember(shizukuConnection) {
            shizukuConnection.isBound
        }

        val deviceState = remember(telemetrySnapshot, isShizukuBound) {
            snapshotToDeviceState(telemetrySnapshot, isShizukuBound)
        }

        val (profile, rationale) = remember(deviceState) {
            stateMachineEngine.evaluateState(deviceState)
        }

        val resolvedState = remember(profile, deviceState, rationale) {
            profileResolver.resolveProfile(profile, deviceState, rationale)
        }

        DashboardScreen(
            deviceState = deviceState,
            resolvedState = resolvedState,
            telemetrySnapshot = telemetrySnapshot,
            onRefresh = {
                telemetryAggregator.sampleCurrentState()
            }
        )
    }

    override fun onStart() {
        super.onStart()
        shizukuConnection.bind()
        telemetryAggregator.startSampling()
    }

    override fun onStop() {
        super.onStop()
        telemetryAggregator.stopSampling()
    }

    override fun onDestroy() {
        super.onDestroy()
        shizukuConnection.unbind()
        telemetryAggregator.stopSampling()
    }

    companion object {
        fun snapshotToDeviceState(
            snapshot: DeviceTelemetrySnapshot,
            isShizukuConnected: Boolean
        ): DeviceState {
            val battery = snapshot.battery.value
            val memAvailKb = snapshot.memAvailableKb.value ?: 0L
            val memPsi = snapshot.memoryPsi.value
            val display = snapshot.display.value
            val thermal = snapshot.thermalStatus.value ?: ThermalStatusLevel.NOMINAL
            val measuredApTemp = snapshot.measuredApTempC.value ?: battery?.tempC ?: 0.0f

            return DeviceState(
                batteryPercent = battery?.percent ?: 0,
                isCharging = battery?.isCharging ?: false,
                batteryTempC = battery?.tempC ?: 0.0f,
                apTempC = measuredApTemp,
                thermalStatus = thermal,
                memoryAvailableMb = memAvailKb / 1024L,
                memoryPsiAvg10 = memPsi?.someAvg10 ?: 0.0f,
                isScreenOn = display?.isScreenOn ?: true,
                foregroundPackage = snapshot.foregroundPackage.value ?: "",
                activeRefreshRateMode = display?.secureSettingMode ?: 0,
                privilegeTier = if (isShizukuConnected) PrivilegeTier.TIER_1_SHIZUKU else PrivilegeTier.TIER_0_STOCK
            )
        }
    }
}
