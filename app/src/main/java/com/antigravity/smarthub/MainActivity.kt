package com.antigravity.smarthub

import android.os.Bundle
import android.content.Intent
import android.provider.Settings
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.antigravity.smarthub.core.state.OptimizationController
import com.antigravity.smarthub.core.model.SmartHubProfile
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
            optimizationEnabled = controllerState.optimizationEnabled,
            automaticMode = controllerState.automaticMode,
            manualProfileOverride = controllerState.manualProfileOverride,
            startupWarning = controllerState.startupWarning,
            restorationPending = controllerState.restorationPending,
            usageAccessGranted = controllerState.usageAccessGranted,
            accessibilityOptIn = controllerState.accessibilityOptIn,
            foregroundClassification = controllerState.foregroundClassification,
            lastVerificationResult = controllerState.lastVerificationResult,
            onRefresh = {
                optimizationController.manualRefresh()
            },
            onOptimizationEnabledChanged = optimizationController::setOptimizationEnabled,
            onAutomaticModeChanged = optimizationController::setAutomaticMode,
            onManualProfileSelected = optimizationController::setManualProfile,
            onRestoreOriginalSettings = optimizationController::restoreOriginalSettings,
            onOpenUsageAccess = { startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
            onOpenAccessibilitySettings = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
            onOpenShizukuSettings = {
                packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")?.let(::startActivity)
            }
        )
    }
}
