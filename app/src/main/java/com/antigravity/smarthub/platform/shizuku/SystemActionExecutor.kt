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
 * Guarantees: Snapshot -> Safety Check -> Execute -> Verify Readback -> Persist.
 * On Verification Failure: Rollback -> Verify Rollback.
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
                // 1. Snapshot Baseline
                val baseline = userService.readSetting("secure", "refresh_rate_mode")
                    ?: return ActionExecutionResult(false, null, null, "Failed to read setting baseline")

                baselineRepository.saveSettingBaselineOnce("secure", "refresh_rate_mode", baseline)

                // 2. Execute Mutation
                val status = userService.setRefreshRateMode(action.targetMode)
                if (status != 0) {
                    return ActionExecutionResult(false, baseline, null, "IPC setRefreshRateMode failed with code $status")
                }

                // 3. Readback Verification
                val verified = userService.readSetting("secure", "refresh_rate_mode")
                if (verified != action.targetMode.toString()) {
                    // 4. Rollback
                    userService.setRefreshRateMode(baseline.toIntOrNull() ?: 0)
                    val verifyRollback = userService.readSetting("secure", "refresh_rate_mode")
                    val rollbackSuccess = (verifyRollback == baseline)
                    return ActionExecutionResult(
                        success = false,
                        baselineCaptured = baseline,
                        verifiedValue = verified,
                        errorMessage = "Refresh rate verification failed (expected ${action.targetMode}, got $verified). Rollback success: $rollbackSuccess",
                        rolledBack = true
                    )
                }

                ActionExecutionResult(true, baseline, verified)
            }

            is SystemAction.SetStandbyBucket -> {
                // 1. Snapshot Baseline
                val baselineCode = userService.readStandbyBucket(action.packageName)
                if (baselineCode == -1) {
                    return ActionExecutionResult(false, null, null, "Failed to read standby bucket baseline for ${action.packageName}")
                }
                val baselineBucket = bucketCodeToString(baselineCode)
                baselineRepository.saveStandbyBucketBaselineOnce(action.packageName, baselineBucket)

                // 2. Execute Mutation
                val status = userService.setStandbyBucket(action.packageName, action.targetBucket)
                if (status != 0) {
                    return ActionExecutionResult(false, baselineBucket, null, "IPC setStandbyBucket failed with code $status")
                }

                // 3. Readback Verification
                val readbackCode = userService.readStandbyBucket(action.packageName)
                val readbackBucket = bucketCodeToString(readbackCode)
                if (readbackBucket != action.targetBucket) {
                    // 4. Rollback
                    userService.setStandbyBucket(action.packageName, baselineBucket)
                    val verifyRollbackCode = userService.readStandbyBucket(action.packageName)
                    val verifyRollbackBucket = bucketCodeToString(verifyRollbackCode)
                    val rollbackSuccess = (verifyRollbackBucket == baselineBucket)
                    return ActionExecutionResult(
                        success = false,
                        baselineCaptured = baselineBucket,
                        verifiedValue = readbackBucket,
                        errorMessage = "Standby bucket verification failed (expected ${action.targetBucket}, got $readbackBucket). Rollback success: $rollbackSuccess",
                        rolledBack = true
                    )
                }

                ActionExecutionResult(true, baselineBucket, readbackBucket)
            }

            is SystemAction.SetAppOpsBackground -> {
                // 1. Snapshot Baseline
                val baselineOps = userService.readAppOpsBackground(action.packageName)
                    ?: return ActionExecutionResult(false, null, null, "Failed to read AppOps baseline for ${action.packageName}")

                val baselineMode = if (baselineOps.contains("ignore")) "ignore" else "allow"
                baselineRepository.saveAppOpsBaselineOnce(action.packageName, baselineMode)

                // 2. Execute Mutation
                val targetMode = if (action.allow) "allow" else "ignore"
                val status = userService.setAppOpsBackground(action.packageName, targetMode)
                if (status != 0) {
                    return ActionExecutionResult(false, baselineMode, null, "IPC setAppOpsBackground failed with code $status")
                }

                // 3. Readback Verification
                val readbackOps = userService.readAppOpsBackground(action.packageName) ?: ""
                val isVerified = if (action.allow) !readbackOps.contains("ignore") else readbackOps.contains("ignore")

                if (!isVerified) {
                    // 4. Rollback
                    userService.setAppOpsBackground(action.packageName, baselineMode)
                    val verifyRollbackOps = userService.readAppOpsBackground(action.packageName) ?: ""
                    val rollbackSuccess = if (baselineMode == "allow") !verifyRollbackOps.contains("ignore") else verifyRollbackOps.contains("ignore")
                    return ActionExecutionResult(
                        success = false,
                        baselineCaptured = baselineMode,
                        verifiedValue = readbackOps,
                        errorMessage = "AppOps verification failed (expected $targetMode, got $readbackOps). Rollback success: $rollbackSuccess",
                        rolledBack = true
                    )
                }

                ActionExecutionResult(true, baselineMode, targetMode)
            }
        }
    }

    private fun bucketCodeToString(code: Int): String {
        return when (code) {
            10 -> "active"
            20 -> "working_set"
            30 -> "frequent"
            40 -> "rare"
            45 -> "restricted"
            else -> "active"
        }
    }
}
