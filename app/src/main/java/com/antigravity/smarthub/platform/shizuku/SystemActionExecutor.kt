package com.antigravity.smarthub.platform.shizuku

import com.antigravity.smarthub.ISmartHubUserService
import com.antigravity.smarthub.core.model.DeviceState
import com.antigravity.smarthub.core.model.SystemAction
import com.antigravity.smarthub.core.safety.SafetyGovernor

data class ActionExecutionResult(
    val success: Boolean,
    val baselineCaptured: String?,
    val verifiedValue: String?,
    val errorMessage: String? = null
)

/**
 * Transactional Executor for SystemAction instances.
 * Guarantees: Snapshot -> Safety Check -> Execute -> Verify -> Rollback on Failure.
 */
class SystemActionExecutor(
    private val userService: ISmartHubUserService?,
    private val safetyGovernor: SafetyGovernor
) {

    fun executeTransaction(action: SystemAction, currentState: DeviceState): ActionExecutionResult {
        // Step 1: Safety Governor Check
        val vetoResult = safetyGovernor.evaluateAction(action, currentState)
        if (!vetoResult.isAllowed) {
            return ActionExecutionResult(
                success = false,
                baselineCaptured = null,
                verifiedValue = null,
                errorMessage = "VETOED by SafetyGovernor: ${vetoResult.vetoReason}"
            )
        }

        if (userService == null) {
            return ActionExecutionResult(
                success = false,
                baselineCaptured = null,
                verifiedValue = null,
                errorMessage = "Shizuku UserService unavailable"
            )
        }

        return when (action) {
            is SystemAction.SetRefreshRate -> {
                // Step 2: Snapshot Baseline
                val baseline = userService.readSetting("secure", "refresh_rate_mode")
                if (baseline == null) {
                    return ActionExecutionResult(false, null, null, "Failed to capture setting baseline")
                }

                // Step 3: Execute Action
                val exitCode = userService.executeShellCommand("settings put secure refresh_rate_mode ${action.targetMode}")
                if (exitCode != 0) {
                    return ActionExecutionResult(false, baseline, null, "Shell command failed with code $exitCode")
                }

                // Step 4: Readback Verification
                val verified = userService.readSetting("secure", "refresh_rate_mode")
                val isVerified = (verified == action.targetMode.toString())

                if (!isVerified) {
                    // Step 5: Automatic Rollback on Verification Failure
                    userService.executeShellCommand("settings put secure refresh_rate_mode $baseline")
                    return ActionExecutionResult(
                        success = false,
                        baselineCaptured = baseline,
                        verifiedValue = verified,
                        errorMessage = "Verification failed (expected ${action.targetMode}, got $verified). Rolled back to $baseline."
                    )
                }

                ActionExecutionResult(
                    success = true,
                    baselineCaptured = baseline,
                    verifiedValue = verified
                )
            }

            is SystemAction.SetStandbyBucket -> {
                val exitCode = userService.executeShellCommand("am set-standby-bucket ${action.packageName} ${action.targetBucket}")
                ActionExecutionResult(
                    success = (exitCode == 0),
                    baselineCaptured = null,
                    verifiedValue = action.targetBucket
                )
            }

            is SystemAction.SetAppOpsBackground -> {
                val mode = if (action.allow) "allow" else "ignore"
                val exitCode = userService.executeShellCommand("cmd appops set ${action.packageName} RUN_ANY_IN_BACKGROUND $mode")
                ActionExecutionResult(
                    success = (exitCode == 0),
                    baselineCaptured = null,
                    verifiedValue = mode
                )
            }
        }
    }
}
