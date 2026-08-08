package com.antigravity.smarthub.core.safety

import com.antigravity.smarthub.core.model.DeviceState
import com.antigravity.smarthub.core.model.SafetyVetoResult
import com.antigravity.smarthub.core.model.SystemAction
import com.antigravity.smarthub.core.model.ThermalStatusLevel
import com.antigravity.smarthub.core.telemetry.TelemetryState

/**
 * Safety Governor - Central authority for approving or vetoing any system action.
 */
class SafetyGovernor(
    private val appClassifier: AppClassifier = AppClassifier()
) {

    /**
     * Evaluates whether a proposed SystemAction is safe to execute under current DeviceState.
     */
    fun evaluateAction(action: SystemAction, state: DeviceState): SafetyVetoResult {
        // Rule 1: Thermal Emergency absolute veto on performance actions (120Hz boost)
        val isThermalElevated = (state.thermalStatus.state == TelemetryState.AVAILABLE &&
                (state.thermalStatus.value == ThermalStatusLevel.CRITICAL || state.thermalStatus.value == ThermalStatusLevel.SEVERE)) ||
                (state.apTempC.state == TelemetryState.AVAILABLE && (state.apTempC.value ?: 0f) >= 48.0f) ||
                (state.batteryTempC.state == TelemetryState.AVAILABLE && (state.batteryTempC.value ?: 0f) >= 43.0f)

        if (isThermalElevated) {
            if (action is SystemAction.SetRefreshRate && action.targetMode == 0) {
                return SafetyVetoResult(
                    isAllowed = false,
                    vetoReason = "Thermal elevated (Status: ${state.thermalStatus.value}, AP: ${state.apTempC.value}°C). 120Hz boost vetoed."
                )
            }
        }

        // Rule 2: Never allow restricting protected apps under AppClassifier
        when (action) {
            is SystemAction.SetStandbyBucket -> {
                if (appClassifier.isProtected(action.packageName) &&
                    (action.targetBucket == "restricted" || action.targetBucket == "rare" || action.targetBucket == "frequent")
                ) {
                    return SafetyVetoResult(
                        isAllowed = false,
                        vetoReason = "Package ${action.packageName} is protected under AppClassifier policy (${appClassifier.classifyApp(action.packageName)})."
                    )
                }
            }
            is SystemAction.SetAppOpsBackground -> {
                if (appClassifier.isProtected(action.packageName) && !action.allow) {
                    return SafetyVetoResult(
                        isAllowed = false,
                        vetoReason = "Package ${action.packageName} background execution is protected under AppClassifier policy."
                    )
                }
            }
            else -> {}
        }

        return SafetyVetoResult(isAllowed = true)
    }

    fun isProtectedPackage(packageName: String): Boolean {
        return appClassifier.isProtected(packageName)
    }
}
