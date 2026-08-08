package com.antigravity.smarthub.core.safety

import com.antigravity.smarthub.core.model.DeviceState
import com.antigravity.smarthub.core.model.SafetyVetoResult
import com.antigravity.smarthub.core.model.SystemAction
import com.antigravity.smarthub.core.model.ThermalStatusLevel

/**
 * Safety Governor - Central authority for approving or vetoing any system action.
 */
class SafetyGovernor {

    private val protectedPackageBlacklist = setOf(
        "com.sec.android.app.launcher",
        "com.android.systemui",
        "com.android.phone",
        "com.samsung.android.incallui",
        "com.google.android.dialer",
        "com.samsung.android.dialer",
        "com.sec.android.app.clockpackage",
        "com.whatsapp",
        "org.telegram.messenger",
        "com.microsoft.teams",
        "com.microsoft.office.outlook",
        "net.one97.paytm",
        "com.phonepe.app",
        "com.axis.mobile",
        "in.hsbc.hsbcindia",
        "in.gov.uidai.facerd",
        "com.digilocker.android",
        "com.antigravity.smarthub"
    )

    /**
     * Evaluates whether a proposed SystemAction is safe to execute under current DeviceState.
     */
    fun evaluateAction(action: SystemAction, state: DeviceState): SafetyVetoResult {
        // Rule 1: Thermal Emergency absolute veto on performance actions
        if (state.thermalStatus == ThermalStatusLevel.CRITICAL || state.thermalStatus == ThermalStatusLevel.SEVERE) {
            if (action is SystemAction.SetRefreshRate && action.targetMode == 0) {
                return SafetyVetoResult(
                    isAllowed = false,
                    vetoReason = "Thermal Status is ${state.thermalStatus}. 120Hz boost vetoed."
                )
            }
        }

        // Rule 2: Never allow restricting protected apps in blacklist
        when (action) {
            is SystemAction.SetStandbyBucket -> {
                if (protectedPackageBlacklist.contains(action.packageName) &&
                    (action.targetBucket == "restricted" || action.targetBucket == "rare")
                ) {
                    return SafetyVetoResult(
                        isAllowed = false,
                        vetoReason = "Package ${action.packageName} is protected under NEVER_TOUCH policy."
                    )
                }
            }
            is SystemAction.SetAppOpsBackground -> {
                if (protectedPackageBlacklist.contains(action.packageName) && !action.allow) {
                    return SafetyVetoResult(
                        isAllowed = false,
                        vetoReason = "Package ${action.packageName} background execution is protected under NEVER_TOUCH policy."
                    )
                }
            }
            else -> {}
        }

        return SafetyVetoResult(isAllowed = true)
    }

    fun isProtectedPackage(packageName: String): Boolean {
        return protectedPackageBlacklist.contains(packageName)
    }
}
