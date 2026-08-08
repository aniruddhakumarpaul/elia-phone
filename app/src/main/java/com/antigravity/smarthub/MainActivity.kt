package com.antigravity.smarthub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.antigravity.smarthub.core.state.OptimizationController
import com.antigravity.smarthub.ui.dashboard.DashboardScreen
import com.antigravity.smarthub.ui.theme.SmartHubTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var optimizationController: OptimizationController

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
        val controllerState by optimizationController.uiState.collectAsState()

        DashboardScreen(
            deviceState = controllerState.deviceState,
            resolvedState = controllerState.resolvedState,
            historyLog = controllerState.historyLog,
            shizukuState = controllerState.shizukuState,
            readiness = controllerState.readiness,
            onRefresh = {
                optimizationController.manualRefresh()
            }
        )
    }

    override fun onStart() {
        super.onStart()
        optimizationController.start()
    }

    override fun onStop() {
        super.onStop()
        optimizationController.stop()
    }
}
