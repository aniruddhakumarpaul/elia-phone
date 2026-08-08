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
 * On Verification Failure: Rollback -> Verify Rollback -> Set rolledBack = true ONLY when rollback succeeds.
 */
class SystemActionExecutor(
    private val userService: ISmartHubUserService?,
    private val safetyGovernor: SafetyGovernor,
    private val baselineRepository: BaselineRepository
) {

    fun executeTransaction(action: SystemAction, currentState: DeviceState): ActionExecutionResult {
        // 1. Safety Governor Check
        val vetoResult = safetyGovernor.evaluateAction(action, currentState)
        if (!vetoResult.isAllowed) {
            return ActionExecutionResult(
                success = false,
                baselineCaptured = null,
                verifiedValue = null,
                errorMessage = "VETOED by SafetyGovernor: ${vetoResult.vetoReason}",
                rolledBack = false
            )
        }

        if (userService == null) {
            return ActionExecutionResult(
                success = false,
                baselineCaptured = null,
                verifiedValue = null,
                errorMessage = "Shizuku UserService unavailable",
                rolledBack = false
            )
        }

        return when (action) {
            is SystemAction.SetRefreshRate -> {
                // Snapshot Baseline
                val baseline = userService.readSetting("secure", "refresh_rate_mode")
                    ?: return ActionExecutionResult(false, null, null, "Failed to read setting baseline", false)

                baselineRepository.saveSettingBaselineOnce("secure", "refresh_rate_mode", baseline)

                // Execute Mutation
                val status = userService.setRefreshRateMode(action.targetMode)
                if (status != 0) {
                    return ActionExecutionResult(false, baseline, null, "IPC setRefreshRateMode failed with code $status", false)
                }

                // Readback Verification
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
                        errorMessage = "Refresh rate verification failed (expected ${action.targetMode}, got $verified). Rollback verified: $rollbackSuccess",
                        rolledBack = rollbackSuccess
                    )
                }

                ActionExecutionResult(true, baseline, verified, rolledBack = false)
            }

            is SystemAction.SetStandbyBucket -> {
                // Snapshot Baseline
                val baselineCode = userService.readStandbyBucket(action.packageName)
                if (baselineCode == -1) {
                    return ActionExecutionResult(false, null, null, "Failed to read standby bucket baseline for ${action.packageName}", false)
                }
                val baselineBucket = bucketCodeToString(baselineCode)
                if (baselineBucket.startsWith("UNKNOWN_")) {
                    return ActionExecutionResult(false, baselineBucket, null, "Unsafe: Unknown standby bucket code $baselineCode for ${action.packageName}", false)
                }

                baselineRepository.saveStandbyBucketBaselineOnce(action.packageName, baselineBucket)

                // Execute Mutation
                val status = userService.setStandbyBucket(action.packageName, action.targetBucket)
                if (status != 0) {
                    return ActionExecutionResult(false, baselineBucket, null, "IPC setStandbyBucket failed with code $status", false)
                }

                // Readback Verification
                val readbackCode = userService.readStandbyBucket(action.packageName)
                val readbackBucket = bucketCodeToString(readbackCode)
                if (readbackBucket != action.targetBucket) {
                    // Rollback
                    userService.setStandbyBucket(action.packageName, baselineBucket)
                    val verifyRollbackCode = userService.readStandbyBucket(action.packageName)
                    val verifyRollbackBucket = bucketCodeToString(verifyRollbackCode)
                    val rollbackSuccess = (verifyRollbackBucket == baselineBucket)
                    return ActionExecutionResult(
                        success = false,
                        baselineCaptured = baselineBucket,
                        verifiedValue = readbackBucket,
                        errorMessage = "Standby bucket verification failed (expected ${action.targetBucket}, got $readbackBucket). Rollback verified: $rollbackSuccess",
                        rolledBack = rollbackSuccess
                    )
                }

                ActionExecutionResult(true, baselineBucket, readbackBucket, rolledBack = false)
            }

            is SystemAction.SetAppOpsBackground -> {
                // Snapshot Baseline
                val rawBaselineOps = userService.readAppOpsBackground(action.packageName)
                    ?: return ActionExecutionResult(false, null, null, "Failed to read AppOps baseline for ${action.packageName}", false)

                val baselineMode = parseExactAppOpsMode(rawBaselineOps)
                baselineRepository.saveAppOpsBaselineOnce(action.packageName, baselineMode)

                // Execute Mutation
                val targetMode = if (action.allow) "allow" else "ignore"
                val status = userService.setAppOpsBackground(action.packageName, targetMode)
                if (status != 0) {
                    return ActionExecutionResult(false, baselineMode, null, "IPC setAppOpsBackground failed with code $status", false)
                }

                // Readback Verification
                val readbackOps = userService.readAppOpsBackground(action.packageName) ?: ""
                val actualReadbackMode = parseExactAppOpsMode(readbackOps)
                val isVerified = (actualReadbackMode == targetMode)

                if (!isVerified) {
                    // Rollback using exact original baseline string
                    userService.setAppOpsBackground(action.packageName, baselineMode)
                    val verifyRollbackOps = userService.readAppOpsBackground(action.packageName) ?: ""
                    val actualRollbackMode = parseExactAppOpsMode(verifyRollbackOps)
                    val rollbackSuccess = (actualRollbackMode == baselineMode)
                    return ActionExecutionResult(
                        success = false,
                        baselineCaptured = baselineMode,
                        verifiedValue = actualReadbackMode,
                        errorMessage = "AppOps verification failed (expected $targetMode, got $actualReadbackMode). Rollback verified: $rollbackSuccess",
                        rolledBack = rollbackSuccess
                    )
                }

                ActionExecutionResult(true, baselineMode, targetMode, rolledBack = false)
            }
        }
    }

    private fun bucketCodeToString(code: Int): String {
        return when (code) {
            5 -> "exempted"
            10 -> "active"
            20 -> "working_set"
            30 -> "frequent"
            40 -> "rare"
            45 -> "restricted"
            else -> "UNKNOWN_$code"
        }
    }

    private fun parseExactAppOpsMode(rawOutput: String): String {
        val lower = rawOutput.lowercase()
        return when {
            lower.contains("allow") -> "allow"
            lower.contains("ignore") -> "ignore"
            lower.contains("deny") -> "deny"
            lower.contains("default") -> "default"
            lower.contains("errored") -> "errored"
            lower.contains("foreground") -> "foreground"
            else -> rawOutput.trim()
        }
    }
}
