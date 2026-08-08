package com.antigravity.smarthub.core.safety

import com.antigravity.smarthub.core.model.DeviceState
import com.antigravity.smarthub.core.model.SafetyVetoResult
import com.antigravity.smarthub.core.model.SystemAction
import com.antigravity.smarthub.core.model.ThermalStatusLevel

/** Independent fail-closed safety layer for every privileged action. */
class SafetyGovernor(
    private val appClassifier: AppClassifier = AppClassifier()
) {
    fun evaluateAction(action: SystemAction, state: DeviceState): SafetyVetoResult {
        val nowMs = System.currentTimeMillis()
        val thermalReady = hasFreshThermalEvidence(state, nowMs)

        if (action is SystemAction.SetRefreshRate && action.targetMode == 0 && !thermalReady) {
            return SafetyVetoResult(false, "Thermal safety telemetry is unavailable or stale; performance boost withheld.")
        }

        val thermalElevated =
            (state.thermalStatus.isAvailableAndFresh(5_000L, nowMs) &&
                    (state.thermalStatus.value == ThermalStatusLevel.CRITICAL ||
                            state.thermalStatus.value == ThermalStatusLevel.SEVERE)) ||
                    (state.apTempC.isAvailableAndFresh(5_000L, nowMs) &&
                            (state.apTempC.value ?: Float.NEGATIVE_INFINITY) >= 48.0f) ||
                    (state.batteryTempC.isAvailableAndFresh(5_000L, nowMs) &&
                            (state.batteryTempC.value ?: Float.NEGATIVE_INFINITY) >= 43.0f) ||
                    (state.thermalHeadroom.isAvailableAndFresh(5_000L, nowMs) &&
                            (state.thermalHeadroom.value ?: 1.0f) <= 0f)

        if (thermalElevated && action is SystemAction.SetRefreshRate && action.targetMode == 0) {
            return SafetyVetoResult(
                false,
                "Thermal safety limit reached (status=${state.thermalStatus.value?.name ?: "UNAVAILABLE"}); 120Hz boost vetoed."
            )
        }

        when (action) {
            is SystemAction.SetStandbyBucket -> {
                if (isRestrictiveBucket(action.targetBucket) &&
                    !appClassifier.canAutomaticallyRestrict(action.packageName)
                ) {
                    return SafetyVetoResult(
                        false,
                        "Package ${action.packageName} is not explicitly eligible for automatic restriction " +
                                "(${appClassifier.classifyApp(action.packageName)})."
                    )
                }
            }
            is SystemAction.SetAppOpsBackground -> {
                if (!action.allow && !appClassifier.canAutomaticallyRestrict(action.packageName)) {
                    return SafetyVetoResult(
                        false,
                        "Package ${action.packageName} is not explicitly eligible for background restriction."
                    )
                }
            }
            else -> Unit
        }

        return SafetyVetoResult(true)
    }

    fun isProtectedPackage(packageName: String): Boolean = appClassifier.isProtected(packageName)

    private fun hasFreshThermalEvidence(state: DeviceState, nowMs: Long): Boolean =
        state.thermalStatus.isAvailableAndFresh(5_000L, nowMs) ||
                state.thermalHeadroom.isAvailableAndFresh(5_000L, nowMs) ||
                state.apTempC.isAvailableAndFresh(5_000L, nowMs) ||
                state.batteryTempC.isAvailableAndFresh(5_000L, nowMs)

    private fun isRestrictiveBucket(bucket: String): Boolean =
        bucket == "restricted" || bucket == "rare" || bucket == "frequent"
}
