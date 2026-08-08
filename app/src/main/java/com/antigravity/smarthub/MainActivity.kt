package com.antigravity.smarthub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.antigravity.smarthub.core.model.DeviceState
import com.antigravity.smarthub.core.model.PrivilegeTier
import com.antigravity.smarthub.core.model.ThermalStatusLevel
import com.antigravity.smarthub.core.state.ProfileResolver
import com.antigravity.smarthub.ui.dashboard.DashboardScreen

class MainActivity : ComponentActivity() {

    private val profileResolver = ProfileResolver()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialized Telemetry State for Galaxy A25 5G
        val currentDeviceState = DeviceState(
            batteryPercent = 55,
            isCharging = true,
            batteryTempC = 37.3f,
            apTempC = 44.6f,
            thermalStatus = ThermalStatusLevel.NOMINAL,
            memoryAvailableMb = 2900,
            memoryPsiAvg10 = 2.80f,
            isScreenOn = true,
            foregroundPackage = "com.sec.android.app.launcher",
            activeRefreshRateMode = 0, // 120Hz Adaptive Baseline
            privilegeTier = PrivilegeTier.TIER_1_SHIZUKU
        )

        val resolvedState = profileResolver.resolve(currentDeviceState)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DashboardScreen(
                        deviceState = currentDeviceState,
                        resolvedState = resolvedState
                    )
                }
            }
        }
    }
}
