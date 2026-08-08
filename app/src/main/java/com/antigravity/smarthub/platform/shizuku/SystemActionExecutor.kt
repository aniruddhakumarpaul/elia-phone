package com.antigravity.smarthub.platform.shizuku

import com.antigravity.smarthub.ISmartHubUserService
import com.antigravity.smarthub.core.model.DeviceState
import com.antigravity.smarthub.core.model.SystemAction
import com.antigravity.smarthub.core.persistence.BaselineRepository
import com.antigravity.smarthub.core.safety.SafetyGovernor

data class ActionExecutionResult(
    val success: Boolean,
    val baselineCaptured: String?,
    val verifiedValue: String?,
    val errorMessage: String? = null,
    val rolledBack: Boolean = false
)

/**
 * Strict Transactional Action Executor.
 * Lifecycle: Snapshot Baseline -> Safety Check -> Execute -> Verify Readback -> Persist.
 * On Failure: Rollback -> Verify Rollback.
 */
class SystemActionExecutor(
    private val userService: ISmartHubUserService?,
    private val safetyGovernor: SafetyGovernor,
    private val baselineRepository: BaselineRepository
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
                // Snapshot original baseline
                val baseline = userService.readSetting("secure", "refresh_rate_mode")
                    ?: return ActionExecutionResult(false, null, null, "Failed to read setting baseline")

                // Preserve initial baseline once in repository
                baselineRepository.saveSettingBaselineOnce("secure", "refresh_rate_mode", baseline)

                // Execute
                val status = userService.setRefreshRateMode(action.targetMode)
                if (status != 0) {
                    return ActionExecutionResult(false, baseline, null, "IPC setRefreshRateMode failed")
                }

                // Verify Readback
                val verified = userService.readSetting("secure", "refresh_rate_mode")
                if (verified != action.targetMode.toString()) {
                    // Rollback
                    userService.setRefreshRateMode(baseline.toIntOrNull() ?: 0)
                    val verifyRollback = userService.readSetting("secure", "refresh_rate_mode")
                    val rollbackSuccess = (verifyRollback == baseline)
                    return ActionExecutionResult(
                        success = false,
                        baselineCaptured = baseline,
                        verifiedValue = verified,
                        errorMessage = "Verification failed (expected ${action.targetMode}, got $verified). Rollback result: $rollbackSuccess",
                        rolledBack = true
                    )
                }

                ActionExecutionResult(true, baseline, verified)
            }

            is SystemAction.SetStandbyBucket -> {
                val baselineBucket = userService.readStandbyBucket(action.packageName).toString()
                baselineRepository.saveStandbyBucketBaselineOnce(action.packageName, baselineBucket)

                val status = userService.setStandbyBucket(action.packageName, action.targetBucket)
                if (status != 0) {
                    return ActionExecutionResult(false, baselineBucket, null, "IPC setStandbyBucket failed")
                }

                val verifiedBucket = userService.readStandbyBucket(action.packageName).toString()
                ActionExecutionResult(true, baselineBucket, verifiedBucket)
            }

            is SystemAction.SetAppOpsBackground -> {
                val baselineOps = userService.readAppOpsBackground(action.packageName) ?: "allow"
                baselineRepository.saveAppOpsBaselineOnce(action.packageName, baselineOps)

                val targetMode = if (action.allow) "allow" else "ignore"
                val status = userService.setAppOpsBackground(action.packageName, targetMode)
                if (status != 0) {
                    return ActionExecutionResult(false, baselineOps, null, "IPC setAppOpsBackground failed")
                }

                val verifiedOps = userService.readAppOpsBackground(action.packageName) ?: ""
                ActionExecutionResult(true, baselineOps, verifiedOps)
            }
        }
    }
}
