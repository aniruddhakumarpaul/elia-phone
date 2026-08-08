package com.antigravity.smarthub.platform.shizuku

import com.antigravity.smarthub.ISmartHubUserService
import com.antigravity.smarthub.core.model.CapabilityResult
import com.antigravity.smarthub.core.model.DeviceState
import com.antigravity.smarthub.core.model.SystemAction
import com.antigravity.smarthub.core.persistence.BaselineRepository
import com.antigravity.smarthub.core.state.ActionLedger
import com.antigravity.smarthub.core.safety.SafetyGovernor

data class ActionExecutionResult(
    val success: Boolean,
    val baselineCaptured: String?,
    val verifiedValue: String?,
    val capabilityResult: CapabilityResult = CapabilityResult.UNAVAILABLE,
    val errorMessage: String? = null,
    val rolledBack: Boolean = false,
    val requestedValue: String? = null
)

/**
 * Strict Transactional Action Executor.
 * Resolves ISmartHubUserService dynamically at call time for resiliency against late binds and binder death.
 * Guarantees: Snapshot -> Safety Check -> Execute -> Verify Readback -> Persist.
 * On Verification Failure: Rollback -> Verify Rollback -> Set rolledBack = true ONLY when rollback succeeds.
 */
class SystemActionExecutor(
    private val serviceProvider: () -> ISmartHubUserService?,
    private val safetyGovernor: SafetyGovernor,
    private val baselineRepository: BaselineRepository,
    private val effectiveRefreshRateReader: (() -> Float?)? = null,
    private val stabilizationDelayMs: Long = 500L,
    private val ownershipLedger: ActionLedger? = null
) {

    constructor(
        connection: ShizukuServiceConnection,
        safetyGovernor: SafetyGovernor,
        baselineRepository: BaselineRepository
    ) : this({ connection.userService }, safetyGovernor, baselineRepository)

    constructor(
        connection: ShizukuServiceConnection,
        safetyGovernor: SafetyGovernor,
        baselineRepository: BaselineRepository,
        effectiveRefreshRateReader: (() -> Float?)?,
        stabilizationDelayMs: Long
    ) : this({ connection.userService }, safetyGovernor, baselineRepository, effectiveRefreshRateReader, stabilizationDelayMs)

    constructor(
        userService: ISmartHubUserService?,
        safetyGovernor: SafetyGovernor,
        baselineRepository: BaselineRepository
    ) : this({ userService }, safetyGovernor, baselineRepository)

    constructor(
        userService: ISmartHubUserService?,
        safetyGovernor: SafetyGovernor,
        baselineRepository: BaselineRepository,
        effectiveRefreshRateReader: (() -> Float?)?,
        stabilizationDelayMs: Long
    ) : this({ userService }, safetyGovernor, baselineRepository, effectiveRefreshRateReader, stabilizationDelayMs)

    fun executeTransaction(action: SystemAction, currentState: DeviceState): ActionExecutionResult {
        // 1. Safety Governor Check
        val vetoResult = safetyGovernor.evaluateAction(action, currentState)
        if (!vetoResult.isAllowed) {
            return ActionExecutionResult(
                success = false,
                baselineCaptured = null,
                verifiedValue = null,
                capabilityResult = CapabilityResult.UNAVAILABLE,
                errorMessage = "VETOED by SafetyGovernor: ${vetoResult.vetoReason}",
                rolledBack = false
            )
        }

        val userService = serviceProvider()
        if (userService == null) {
            return ActionExecutionResult(
                success = false,
                baselineCaptured = null,
                verifiedValue = null,
                capabilityResult = CapabilityResult.UNAVAILABLE,
                errorMessage = "Shizuku UserService unavailable",
                rolledBack = false
            )
        }

        return try {
            when (action) {
                is SystemAction.SetRefreshRate -> executeRefreshRate(action, userService)
                is SystemAction.SetStandbyBucket -> executeStandbyBucket(action, userService)
                is SystemAction.SetAppOpsBackground -> executeAppOpsBackground(action, userService)
            }
        } catch (e: Exception) {
            ActionExecutionResult(
                success = false,
                baselineCaptured = null,
                verifiedValue = null,
                capabilityResult = CapabilityResult.UNAVAILABLE,
                errorMessage = "IPC execution error: ${e.message}",
                rolledBack = false
            )
        }
    }

    private fun executeRefreshRate(action: SystemAction.SetRefreshRate, userService: ISmartHubUserService): ActionExecutionResult {
        // Snapshot Baseline
        val baseline = userService.readSetting("secure", "refresh_rate_mode")
            ?: return ActionExecutionResult(false, null, null, CapabilityResult.UNAVAILABLE, "Failed to read setting baseline", false)
        val baselineMode = baseline.toIntOrNull()?.takeIf { it == 0 || it == 1 }
            ?: return ActionExecutionResult(false, baseline, null, CapabilityResult.UNAVAILABLE, "Invalid refresh baseline '$baseline'; mutation refused", false, action.targetMode.toString())

        if (!baselineRepository.isHealthy() || !baselineRepository.saveSettingBaselineOnce("secure", "refresh_rate_mode", baseline)) {
            return ActionExecutionResult(false, baseline, null, CapabilityResult.UNAVAILABLE, "Durable baseline persistence failed; mutation refused", false, action.targetMode.toString())
        }
        if (!beginOwnership(action, baseline)) {
            return ActionExecutionResult(false, baseline, null, CapabilityResult.UNAVAILABLE, "Durable ownership journal failed; mutation refused", false, action.targetMode.toString())
        }

        // Execute Mutation
        val status = userService.setRefreshRateMode(action.targetMode)
        if (status != 0) {
            val rollback = restoreAfterFailedMutation(action, baseline) { restoreRefreshRate(baselineMode, baseline, userService) }
            return ActionExecutionResult(false, baseline, null, CapabilityResult.UNAVAILABLE, "IPC setRefreshRateMode failed with code $status; restoration verified=${rollback.success}", rollback.success, action.targetMode.toString())
        }

        // Readback Verification
        val verified = userService.readSetting("secure", "refresh_rate_mode")
        if (verified != action.targetMode.toString()) {
            val rollbackSuccess = restoreAfterFailedMutation(action, baseline) { restoreRefreshRate(baselineMode, baseline, userService) }.success
            val capability = when {
                verified == null -> CapabilityResult.UNAVAILABLE
                verified == baseline -> CapabilityResult.IGNORED_BY_OEM
                else -> CapabilityResult.PARTIALLY_SUPPORTED
            }
            return ActionExecutionResult(
                success = false,
                baselineCaptured = baseline,
                verifiedValue = verified,
                capabilityResult = capability,
                errorMessage = "Refresh setting verification failed (expected ${action.targetMode}, got $verified). Rollback verified: $rollbackSuccess",
                rolledBack = rollbackSuccess,
                requestedValue = action.targetMode.toString()
            )
        }

        val effective = verifyEffectiveRefreshRate(action.targetMode)
        if (effective != null && !effective.first) {
            val rollbackSuccess = restoreAfterFailedMutation(action, baseline) { restoreRefreshRate(baselineMode, baseline, userService) }.success
            return ActionExecutionResult(
                success = false,
                baselineCaptured = baseline,
                verifiedValue = "setting=$verified,effective=${effective.second}",
                capabilityResult = CapabilityResult.PARTIALLY_SUPPORTED,
                errorMessage = "Samsung setting accepted but effective display refresh disagreed (target=${action.targetMode}, effective=${effective.second}). Rollback verified: $rollbackSuccess",
                rolledBack = rollbackSuccess,
                requestedValue = action.targetMode.toString()
            )
        }

        val ownershipFailure = commitOwnershipOrRollback(action, baseline, verified, if (effectiveRefreshRateReader == null) CapabilityResult.PARTIALLY_SUPPORTED else CapabilityResult.SUPPORTED) {
            restoreRefreshRate(baselineMode, baseline, userService)
        }
        if (ownershipFailure != null) return ownershipFailure
        return ActionExecutionResult(
            success = true,
            baselineCaptured = baseline,
            verifiedValue = verified,
            capabilityResult = if (effectiveRefreshRateReader == null) CapabilityResult.PARTIALLY_SUPPORTED else CapabilityResult.SUPPORTED,
            rolledBack = false,
            requestedValue = action.targetMode.toString()
        )
    }

    private fun executeStandbyBucket(action: SystemAction.SetStandbyBucket, userService: ISmartHubUserService): ActionExecutionResult {
        val validBuckets = setOf("exempted", "active", "working_set", "frequent", "rare", "restricted")

        // Snapshot Baseline
        val baselineCode = userService.readStandbyBucket(action.packageName)
        if (baselineCode == -1) {
            return ActionExecutionResult(false, null, null, CapabilityResult.UNAVAILABLE, "Failed to read standby bucket baseline for ${action.packageName}", false, action.targetBucket)
        }
        val baselineBucket = bucketCodeToString(baselineCode)
        if (!validBuckets.contains(baselineBucket)) {
            return ActionExecutionResult(false, baselineBucket, null, CapabilityResult.UNAVAILABLE, "Unsafe: Captured standby bucket '$baselineBucket' cannot be safely restored for ${action.packageName}", false, action.targetBucket)
        }

        if (!baselineRepository.isHealthy() || !baselineRepository.saveStandbyBucketBaselineOnce(action.packageName, baselineBucket)) {
            return ActionExecutionResult(false, baselineBucket, null, CapabilityResult.UNAVAILABLE, "Durable baseline persistence failed; mutation refused", false, action.targetBucket)
        }
        if (!beginOwnership(action, baselineBucket)) {
            return ActionExecutionResult(false, baselineBucket, null, CapabilityResult.UNAVAILABLE, "Durable ownership journal failed; mutation refused", false, action.targetBucket)
        }

        // Execute Mutation
        val status = userService.setStandbyBucket(action.packageName, action.targetBucket)
        if (status != 0) {
            val rollback = restoreAfterFailedMutation(action, baselineBucket) { restoreStandbyBucket(action.packageName, baselineBucket, userService) }
            return ActionExecutionResult(false, baselineBucket, null, CapabilityResult.UNAVAILABLE, "IPC setStandbyBucket failed with code $status; restoration verified=${rollback.success}", rollback.success, action.targetBucket)
        }

        // Readback Verification
        val readbackCode = userService.readStandbyBucket(action.packageName)
        val readbackBucket = bucketCodeToString(readbackCode)
        if (readbackBucket != action.targetBucket) {
            // Rollback
            val rollbackSuccess = restoreAfterFailedMutation(action, baselineBucket) { restoreStandbyBucket(action.packageName, baselineBucket, userService) }.success
            return ActionExecutionResult(
                success = false,
                baselineCaptured = baselineBucket,
                verifiedValue = readbackBucket,
                capabilityResult = CapabilityResult.PARTIALLY_SUPPORTED,
                errorMessage = "Standby bucket verification failed (expected ${action.targetBucket}, got $readbackBucket). Rollback verified: $rollbackSuccess",
                rolledBack = rollbackSuccess,
                requestedValue = action.targetBucket
            )
        }

        val ownershipFailure = commitOwnershipOrRollback(action, baselineBucket, readbackBucket, CapabilityResult.SUPPORTED) {
            restoreStandbyBucket(action.packageName, baselineBucket, userService)
        }
        if (ownershipFailure != null) return ownershipFailure
        return ActionExecutionResult(
            success = true,
            baselineCaptured = baselineBucket,
            verifiedValue = readbackBucket,
            capabilityResult = CapabilityResult.SUPPORTED,
            rolledBack = false,
            requestedValue = action.targetBucket
        )
    }

    private fun executeAppOpsBackground(action: SystemAction.SetAppOpsBackground, userService: ISmartHubUserService): ActionExecutionResult {
        val validModes = setOf("allow", "ignore", "deny", "default", "errored", "foreground")

        // Snapshot Baseline
        val rawBaselineOps = userService.readAppOpsBackground(action.packageName)
            ?: return ActionExecutionResult(false, null, null, CapabilityResult.UNAVAILABLE, "Failed to read AppOps baseline for ${action.packageName}", false, if (action.allow) "allow" else "ignore")

        val baselineMode = parseExactAppOpsMode(rawBaselineOps)
        if (!validModes.contains(baselineMode)) {
            return ActionExecutionResult(false, baselineMode, null, CapabilityResult.UNAVAILABLE, "Unsafe: Captured AppOps mode '$baselineMode' cannot be safely restored for ${action.packageName}", false, if (action.allow) "allow" else "ignore")
        }

        if (!baselineRepository.isHealthy() || !baselineRepository.saveAppOpsBaselineOnce(action.packageName, baselineMode)) {
            return ActionExecutionResult(false, baselineMode, null, CapabilityResult.UNAVAILABLE, "Durable baseline persistence failed; mutation refused", false, if (action.allow) "allow" else "ignore")
        }
        if (!beginOwnership(action, baselineMode)) {
            return ActionExecutionResult(false, baselineMode, null, CapabilityResult.UNAVAILABLE, "Durable ownership journal failed; mutation refused", false, if (action.allow) "allow" else "ignore")
        }

        // Execute Mutation
        val targetMode = if (action.allow) "allow" else "ignore"
        val status = userService.setAppOpsBackground(action.packageName, targetMode)
        if (status != 0) {
            val rollback = restoreAfterFailedMutation(action, baselineMode) { restoreAppOps(action.packageName, baselineMode, userService) }
            return ActionExecutionResult(false, baselineMode, null, CapabilityResult.UNAVAILABLE, "IPC setAppOpsBackground failed with code $status; restoration verified=${rollback.success}", rollback.success, targetMode)
        }

        // Readback Verification
        val readbackOps = userService.readAppOpsBackground(action.packageName) ?: ""
        val actualReadbackMode = parseExactAppOpsMode(readbackOps)
        val isVerified = (actualReadbackMode == targetMode)

        if (!isVerified) {
            // Rollback using exact original baseline string (including 'default')
            val rollbackSuccess = restoreAfterFailedMutation(action, baselineMode) { restoreAppOps(action.packageName, baselineMode, userService) }.success
            return ActionExecutionResult(
                success = false,
                baselineCaptured = baselineMode,
                verifiedValue = actualReadbackMode,
                capabilityResult = CapabilityResult.PARTIALLY_SUPPORTED,
                errorMessage = "AppOps verification failed (expected $targetMode, got $actualReadbackMode). Rollback verified: $rollbackSuccess",
                rolledBack = rollbackSuccess,
                requestedValue = targetMode
            )
        }

        val ownershipFailure = commitOwnershipOrRollback(action, baselineMode, targetMode, CapabilityResult.SUPPORTED) {
            restoreAppOps(action.packageName, baselineMode, userService)
        }
        if (ownershipFailure != null) return ownershipFailure
        return ActionExecutionResult(
            success = true,
            baselineCaptured = baselineMode,
            verifiedValue = targetMode,
            capabilityResult = CapabilityResult.SUPPORTED,
            rolledBack = false,
            requestedValue = targetMode
        )
    }

    /** Restore one Smart-Hub-owned key to its exact captured baseline. */
    fun readCurrentOwnedValue(actionKey: String): String? {
        val service = serviceProvider() ?: return null
        return try {
            when {
                actionKey == "REFRESH_RATE" -> service.readSetting("secure", "refresh_rate_mode")
                actionKey.startsWith("STANDBY_BUCKET_") -> {
                    val packageName = actionKey.removePrefix("STANDBY_BUCKET_")
                    bucketCodeToString(service.readStandbyBucket(packageName))
                }
                actionKey.startsWith("APPOPS_BG_") -> {
                    val packageName = actionKey.removePrefix("APPOPS_BG_")
                    parseExactAppOpsMode(service.readAppOpsBackground(packageName) ?: "")
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun beginOwnership(action: SystemAction, baseline: String): Boolean =
        ownershipLedger?.recordPendingAction(action, baseline) ?: true

    private fun restoreAfterFailedMutation(
        action: SystemAction,
        baseline: String,
        restore: () -> ActionExecutionResult
    ): ActionExecutionResult {
        val key = ownershipLedger?.getActionKey(action)
        if (key != null) ownershipLedger?.markRestorePending(key)
        val restored = restore()
        if (!restored.success || key == null) return restored
        val cleared = ownershipLedger?.recordRestoredAction(key) == true
        return if (cleared) restored else restored.copy(
            success = false,
            errorMessage = "Baseline restored but durable journal clear failed; ownership remains pending",
            rolledBack = true
        )
    }

    private fun commitOwnershipOrRollback(
        action: SystemAction,
        baseline: String,
        verified: String?,
        capability: CapabilityResult,
        restore: () -> ActionExecutionResult
    ): ActionExecutionResult? {
        val ledger = ownershipLedger ?: return null
        if (ledger.recordAppliedAction(action, verified, capability)) return null

        val key = ledger.getActionKey(action)
        ledger.markRestorePending(key)
        val restored = restore()
        val cleared = restored.success && ledger.recordRestoredAction(key)
        return ActionExecutionResult(
            success = false,
            baselineCaptured = baseline,
            verifiedValue = verified,
            capabilityResult = CapabilityResult.UNAVAILABLE,
            errorMessage = "Durable APPLIED journal failed; baseline restoration verified=${restored.success}, journal cleared=$cleared",
            rolledBack = restored.success,
            requestedValue = verified
        )
    }

    /** Restore one Smart-Hub-owned key to its exact captured baseline. */
    fun restoreOriginal(actionKey: String, currentState: DeviceState): ActionExecutionResult {
        val userService = serviceProvider() ?: return ActionExecutionResult(
            false, null, null, CapabilityResult.UNAVAILABLE, "Shizuku UserService unavailable for restoration"
        )
        return try {
            when {
                actionKey == "REFRESH_RATE" -> {
                    val raw = baselineRepository.getSettingBaseline("secure", "refresh_rate_mode")
                        ?: return ActionExecutionResult(false, null, null, CapabilityResult.UNAVAILABLE, "No refresh baseline captured")
                    val baseline = raw.toIntOrNull()?.takeIf { it == 0 || it == 1 }
                        ?: return ActionExecutionResult(false, raw, null, CapabilityResult.UNAVAILABLE, "Invalid refresh baseline '$raw'; restoration refused")
                    // Restoration returns to the user's captured baseline. It is not a new policy
                    // request and must remain possible even when fresh thermal evidence is absent.
                    restoreRefreshRate(baseline, raw, userService)
                }
                actionKey.startsWith("STANDBY_BUCKET_") -> {
                    val packageName = actionKey.removePrefix("STANDBY_BUCKET_")
                    val baseline = baselineRepository.getStandbyBucketBaseline(packageName)
                        ?: return ActionExecutionResult(false, null, null, CapabilityResult.UNAVAILABLE, "No standby baseline captured")
                    restoreStandbyBucket(packageName, baseline, userService)
                }
                actionKey.startsWith("APPOPS_BG_") -> {
                    val packageName = actionKey.removePrefix("APPOPS_BG_")
                    val baseline = baselineRepository.getAppOpsBaseline(packageName)
                        ?: return ActionExecutionResult(false, null, null, CapabilityResult.UNAVAILABLE, "No AppOps baseline captured")
                    restoreAppOps(packageName, baseline, userService)
                }
                else -> ActionExecutionResult(false, null, null, CapabilityResult.UNAVAILABLE, "Unknown action key '$actionKey'")
            }
        } catch (e: Exception) {
            ActionExecutionResult(false, null, null, CapabilityResult.UNAVAILABLE, "Restoration IPC error: ${e.message}")
        }
    }

    private fun restoreRefreshRate(baseline: Int, raw: String, service: ISmartHubUserService): ActionExecutionResult {
        val status = service.setRefreshRateMode(baseline)
        if (status != 0) return ActionExecutionResult(false, raw, null, CapabilityResult.UNAVAILABLE, "Refresh restoration failed with code $status", false, raw)
        val readback = service.readSetting("secure", "refresh_rate_mode")
        if (readback != raw) return ActionExecutionResult(false, raw, readback, CapabilityResult.PARTIALLY_SUPPORTED, "Refresh restoration setting verification failed", false, raw)
        val effective = verifyEffectiveRefreshRate(baseline)
        if (effective != null && !effective.first) return ActionExecutionResult(false, raw, "setting=$readback,effective=${effective.second}", CapabilityResult.PARTIALLY_SUPPORTED, "Refresh restoration effective verification failed", false, raw)
        return ActionExecutionResult(true, raw, readback, if (effectiveRefreshRateReader == null) CapabilityResult.PARTIALLY_SUPPORTED else CapabilityResult.SUPPORTED, requestedValue = raw)
    }

    private fun restoreStandbyBucket(packageName: String, baseline: String, service: ISmartHubUserService): ActionExecutionResult {
        if (baseline !in setOf("exempted", "active", "working_set", "frequent", "rare", "restricted"))
            return ActionExecutionResult(false, baseline, null, CapabilityResult.UNAVAILABLE, "Invalid standby baseline; restoration refused")
        if (service.setStandbyBucket(packageName, baseline) != 0) return ActionExecutionResult(false, baseline, null, CapabilityResult.UNAVAILABLE, "Standby restoration failed")
        val readback = bucketCodeToString(service.readStandbyBucket(packageName))
        return ActionExecutionResult(readback == baseline, baseline, readback, if (readback == baseline) CapabilityResult.SUPPORTED else CapabilityResult.PARTIALLY_SUPPORTED, if (readback == baseline) null else "Standby restoration verification failed", requestedValue = baseline)
    }

    private fun restoreAppOps(packageName: String, baseline: String, service: ISmartHubUserService): ActionExecutionResult {
        if (baseline !in setOf("allow", "ignore", "deny", "default", "errored", "foreground"))
            return ActionExecutionResult(false, baseline, null, CapabilityResult.UNAVAILABLE, "Invalid AppOps baseline; restoration refused")
        if (service.setAppOpsBackground(packageName, baseline) != 0) return ActionExecutionResult(false, baseline, null, CapabilityResult.UNAVAILABLE, "AppOps restoration failed")
        val readback = parseExactAppOpsMode(service.readAppOpsBackground(packageName) ?: "")
        return ActionExecutionResult(readback == baseline, baseline, readback, if (readback == baseline) CapabilityResult.SUPPORTED else CapabilityResult.PARTIALLY_SUPPORTED, if (readback == baseline) null else "AppOps restoration verification failed", requestedValue = baseline)
    }

    private fun rollbackRefreshRate(baseline: Int, raw: String, service: ISmartHubUserService): Boolean =
        restoreRefreshRate(baseline, raw, service).success

    private fun verifyEffectiveRefreshRate(targetMode: Int): Pair<Boolean, Float>? {
        val reader = effectiveRefreshRateReader ?: return null
        if (stabilizationDelayMs > 0L) Thread.sleep(stabilizationDelayMs)
        val effective = reader() ?: return false to Float.NaN
        val matches = if (targetMode == 0) effective >= 100f else effective <= 90f
        return matches to effective
    }

    fun bucketCodeToString(code: Int): String {
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

    fun parseExactAppOpsMode(rawOutput: String): String {
        val lower = rawOutput.lowercase().trim()
        return when {
            lower.contains("no operations") || lower.contains("default") -> "default"
            lower.contains("allow") -> "allow"
            lower.contains("ignore") -> "ignore"
            lower.contains("deny") -> "deny"
            lower.contains("errored") -> "errored"
            lower.contains("foreground") -> "foreground"
            else -> lower
        }
    }
}
