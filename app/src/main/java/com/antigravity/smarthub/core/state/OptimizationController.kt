package com.antigravity.smarthub.core.state

import com.antigravity.smarthub.core.model.ActionHistoryRecord
import com.antigravity.smarthub.core.model.CapabilityResult
import com.antigravity.smarthub.core.model.DeviceState
import com.antigravity.smarthub.core.model.PrivilegeTier
import com.antigravity.smarthub.core.model.SafetyVetoResult
import com.antigravity.smarthub.core.model.SmartHubProfile
import com.antigravity.smarthub.core.model.SystemAction
import com.antigravity.smarthub.core.safety.SafetyGovernor
import com.antigravity.smarthub.core.telemetry.DeviceTelemetrySnapshot
import com.antigravity.smarthub.core.telemetry.TelemetryAggregator
import com.antigravity.smarthub.core.telemetry.TelemetryValue
import com.antigravity.smarthub.platform.shizuku.ShizukuServiceConnection
import com.antigravity.smarthub.platform.shizuku.ShizukuState
import com.antigravity.smarthub.platform.shizuku.SystemActionExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class PolicyReadiness(
    val hasTelemetrySnapshot: Boolean = false,
    val isReady: Boolean = false,
    val reasons: List<String> = emptyList(),
    val capturedAtMs: Long? = null
) {
    val displayText: String
        get() = when {
            !hasTelemetrySnapshot -> "Waiting for first telemetry snapshot"
            isReady -> "Ready"
            else -> reasons.joinToString("; ")
        }
}

data class ControllerUiState(
    val deviceState: DeviceState = DeviceState(),
    val extendedState: ExtendedDeviceState = ExtendedDeviceState(baseState = DeviceState()),
    val resolvedState: ResolvedState = ResolvedState(
        activeProfile = SmartHubProfile.P5_DAILY_ADAPTIVE,
        rationale = "Waiting for trustworthy telemetry",
        recommendedActions = emptyList()
    ),
    val shizukuState: ShizukuState = ShizukuState.DISCONNECTED,
    val historyLog: List<ActionHistoryRecord> = emptyList(),
    val readiness: PolicyReadiness = PolicyReadiness(),
    val lastOptimizationMessage: String = "No privileged action has run"
)

class OptimizationController(
    private val telemetryAggregator: TelemetryAggregator,
    private val stateMachineEngine: StateMachineEngine,
    private val profileResolver: ProfileResolver,
    private val safetyGovernor: SafetyGovernor,
    private val actionExecutor: SystemActionExecutor,
    private val shizukuConnection: ShizukuServiceConnection,
    private val actionLedger: ActionLedger = ActionLedger(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val _uiState = MutableStateFlow(ControllerUiState())
    val uiState: StateFlow<ControllerUiState> = _uiState.asStateFlow()

    private val decisionMutex = Mutex()
    private var observationJob: Job? = null
    private var screenOffStartMs: Long = 0L
    private var lastProfile: SmartHubProfile = SmartHubProfile.P5_DAILY_ADAPTIVE
    private var hasTelemetrySnapshot = false
    private var latestSnapshot: DeviceTelemetrySnapshot? = null

    fun start() {
        if (observationJob?.isActive == true) return

        shizukuConnection.bind()
        telemetryAggregator.startSampling()
        observationJob = scope.launch {
            launch {
                shizukuConnection.shizukuState.collect { sState ->
                    _uiState.update { it.copy(shizukuState = sState) }
                    if (hasTelemetrySnapshot) {
                        val state = _uiState.value.extendedState
                        evaluateAndOptimize(state)
                    }
                }
            }
            launch {
                actionLedger.historyLog.collect { history ->
                    _uiState.update { it.copy(historyLog = history) }
                }
            }
            launch {
                telemetryAggregator.telemetryStream.collect { snapshot ->
                    latestSnapshot = snapshot
                    hasTelemetrySnapshot = hasTelemetrySnapshot || snapshotHasMeaningfulTelemetry(snapshot)
                    val extState = snapshotToExtendedDeviceState(snapshot)
                    _uiState.update {
                        it.copy(deviceState = extState.baseState, extendedState = extState)
                    }
                    evaluateAndOptimize(extState)
                }
            }
        }
    }

    fun stop() {
        observationJob?.cancel()
        observationJob = null
        telemetryAggregator.stopSampling()
        shizukuConnection.unbind()
    }

    fun manualRefresh(): DeviceTelemetrySnapshot {
        val snapshot = telemetryAggregator.refreshTelemetry()
        latestSnapshot = snapshot
        hasTelemetrySnapshot = hasTelemetrySnapshot || snapshotHasMeaningfulTelemetry(snapshot)
        val extState = snapshotToExtendedDeviceState(snapshot)
        _uiState.update { it.copy(deviceState = extState.baseState, extendedState = extState) }
        scope.launch { evaluateAndOptimize(extState) }
        return snapshot
    }

    private suspend fun evaluateAndOptimize(extState: ExtendedDeviceState) = decisionMutex.withLock {
        val profile = stateMachineEngine.updateState(extState)
        val resolved = profileResolver.resolveActionsForProfile(profile, extState)
        val readiness = assessReadiness(profile, extState)
        _uiState.update {
            it.copy(
                resolvedState = resolved,
                readiness = readiness
            )
        }

        // A connection-state event can arrive before the first trustworthy sample.
        // Until the gate is ready this controller remains observational and performs zero IPC mutations.
        if (!readiness.isReady || shizukuConnection.shizukuState.value != ShizukuState.CONNECTED) {
            _uiState.update {
                it.copy(lastOptimizationMessage = readiness.displayText)
            }
            return@withLock
        }

        val previousProfile = lastProfile
        lastProfile = profile
        val desired = resolved.recommendedActions.associateBy { actionLedger.getActionKey(it) }
        val owned = actionLedger.getCurrentlyAppliedActions()

        // Restore settings that the newly selected profile no longer owns.
        for ((key, ownedAction) in owned) {
            if (key !in desired && !actionLedger.isCooldownActive(ownedAction, bypass = profile == SmartHubProfile.P0_THERMAL_EMERGENCY)) {
                actionLedger.recordAttempt(ownedAction)
                val result = actionExecutor.restoreOriginal(key, extState.baseState)
                recordResult(
                    previousProfile,
                    profile,
                    extState,
                    actionId = "RESTORE_$key",
                    actionDescription = "Restore original baseline for $key",
                    veto = SafetyVetoResult(true),
                    result = result,
                    restored = result.success
                )
                if (result.success) actionLedger.recordRestoredAction(key)
            }
        }

        // Apply only newly desired/changed values. Identical desired values are no-ops.
        for ((key, action) in desired) {
            if (owned[key] == action) continue
            if (actionLedger.isCapabilitySuppressed(key)) continue
            if (actionLedger.isCooldownActive(action, bypass = profile == SmartHubProfile.P0_THERMAL_EMERGENCY)) continue

            val veto = safetyGovernor.evaluateAction(action, extState.baseState)
            if (!veto.isAllowed) {
                recordResult(
                    previousProfile,
                    profile,
                    extState,
                    action.actionId,
                    action.description,
                    veto,
                    null,
                    restored = false
                )
                continue
            }

            actionLedger.recordAttempt(action)
            val result = actionExecutor.executeTransaction(action, extState.baseState)
            recordResult(
                previousProfile,
                profile,
                extState,
                action.actionId,
                action.description,
                veto,
                result,
                restored = false
            )
            if (result.success) actionLedger.recordAppliedAction(action)
            else if (result.capabilityResult == CapabilityResult.IGNORED_BY_OEM ||
                result.errorMessage?.contains("effective display", ignoreCase = true) == true
            ) {
                actionLedger.recordCapabilitySuppression(key)
            }
        }
        _uiState.update { it.copy(lastOptimizationMessage = "Reconciled desired state for ${profile.displayName}") }
    }

    private fun recordResult(
        previousProfile: SmartHubProfile,
        profile: SmartHubProfile,
        state: ExtendedDeviceState,
        actionId: String,
        actionDescription: String,
        veto: SafetyVetoResult,
        result: com.antigravity.smarthub.platform.shizuku.ActionExecutionResult?,
        restored: Boolean
    ) {
        actionLedger.recordHistory(
            ActionHistoryRecord(
                previousProfile = previousProfile,
                newProfile = profile,
                triggeringTelemetrySummary = telemetrySummary(state),
                actionId = actionId,
                actionDescription = actionDescription,
                safetyVetoResult = veto,
                capabilityResult = result?.capabilityResult ?: CapabilityResult.UNAVAILABLE,
                previousValue = result?.baselineCaptured,
                requestedValue = result?.requestedValue ?: actionId,
                verifiedValue = result?.verifiedValue,
                rolledBack = result?.rolledBack ?: false,
                restored = restored
            )
        )
    }

    private fun assessReadiness(profile: SmartHubProfile, state: ExtendedDeviceState): PolicyReadiness {
        val nowMs = System.currentTimeMillis()
        val reasons = mutableListOf<String>()
        if (!hasTelemetrySnapshot) reasons += "first telemetry snapshot unavailable"

        val thermalReady = state.baseState.thermalStatus.isAvailableAndFresh(5_000L, nowMs) ||
                state.baseState.thermalHeadroom.isAvailableAndFresh(5_000L, nowMs) ||
                state.baseState.apTempC.isAvailableAndFresh(5_000L, nowMs) ||
                state.baseState.batteryTempC.isAvailableAndFresh(5_000L, nowMs)
        if (!thermalReady) reasons += "thermal safety telemetry unavailable/stale"

        when (profile) {
            SmartHubProfile.P1_CRITICAL_BATTERY -> {
                if (!state.baseState.batteryPercent.isAvailableAndFresh(10_000L, nowMs)) reasons += "battery telemetry unavailable/stale"
                if (!state.baseState.isCharging.isAvailableAndFresh(10_000L, nowMs)) reasons += "charging telemetry unavailable/stale"
            }
            SmartHubProfile.P2_CHARGING_THERMAL_GUARD -> {
                if (!state.baseState.isCharging.isAvailableAndFresh(10_000L, nowMs)) reasons += "charging telemetry unavailable/stale"
                if (!state.baseState.batteryTempC.isAvailableAndFresh(5_000L, nowMs)) reasons += "battery temperature unavailable/stale"
            }
            SmartHubProfile.P3_GAMING_HIGH_LOAD -> {
                if (!state.baseState.foregroundPackage.isAvailableAndFresh(5_000L, nowMs)) reasons += "foreground package unavailable/stale"
            }
            SmartHubProfile.P4_MEDIA_READING -> {
                if (!state.mediaPlayback.isAvailableAndFresh(5_000L, nowMs)) reasons += "media playback state unavailable/stale"
            }
            SmartHubProfile.P6_OVERNIGHT_DEEP_IDLE -> {
                if (!state.baseState.isScreenOn.isAvailableAndFresh(5_000L, nowMs)) reasons += "screen state unavailable/stale"
                if (!state.mediaPlayback.isAvailableAndFresh(5_000L, nowMs)) reasons += "media state unavailable/stale"
                if (!state.navigationContext.isAvailableAndFresh(5_000L, nowMs)) reasons += "navigation state unavailable/stale"
            }
            SmartHubProfile.P0_THERMAL_EMERGENCY,
            SmartHubProfile.P5_DAILY_ADAPTIVE -> Unit
        }
        return PolicyReadiness(hasTelemetrySnapshot, reasons.isEmpty(), reasons, latestSnapshot?.capturedAtMs)
    }

    fun snapshotToExtendedDeviceState(snapshot: DeviceTelemetrySnapshot): ExtendedDeviceState {
        val tier = if (shizukuConnection.shizukuState.value == ShizukuState.CONNECTED) {
            PrivilegeTier.TIER_1_SHIZUKU
        } else PrivilegeTier.TIER_0_STOCK

        val screenOn = snapshot.display.value?.isScreenOn
        val nowMs = snapshot.capturedAtMs
        when (screenOn?.value) {
            false -> if (screenOffStartMs == 0L) screenOffStartMs = nowMs
            true -> screenOffStartMs = 0L
            null -> screenOffStartMs = 0L
        }
        val screenOffDurationMs = if (screenOffStartMs > 0L) nowMs - screenOffStartMs else 0L
        val display = snapshot.display.value
        val battery = snapshot.battery.value
        val deviceState = DeviceState(
            batteryPercent = battery?.percent?.let { TelemetryValue.available(it) } ?: TelemetryValue.unavailable(),
            isCharging = battery?.let { TelemetryValue.available(it.isCharging) } ?: TelemetryValue.unavailable(),
            batteryTempC = battery?.tempC?.takeIf { it.isFinite() }?.let { TelemetryValue.available(it) } ?: TelemetryValue.unavailable(),
            apTempC = snapshot.measuredApTempC,
            thermalStatus = snapshot.thermalStatus,
            thermalHeadroom = snapshot.thermalHeadroom,
            thermalForecastHeadroom = snapshot.thermalForecastHeadroom,
            memoryAvailableMb = snapshot.memAvailableKb.value?.let { TelemetryValue.available(it / 1024L) } ?: TelemetryValue.unavailable(),
            memoryPsiAvg10 = snapshot.memoryPsi.value?.let { TelemetryValue.available(it.someAvg10) } ?: TelemetryValue.unavailable(),
            isScreenOn = screenOn ?: TelemetryValue.unavailable(),
            foregroundPackage = snapshot.foregroundPackage,
            activeRefreshRateMode = display?.secureSettingMode ?: TelemetryValue.unavailable(),
            effectiveRefreshRateHz = display?.physicalRefreshRateHz ?: TelemetryValue.unavailable(),
            displayState = display?.displayStateStr ?: TelemetryValue.unavailable(),
            privilegeTier = tier
        )
        return ExtendedDeviceState(
            baseState = deviceState,
            mediaPlayback = snapshot.isMediaPlaying,
            navigationContext = snapshot.isNavigationActive,
            screenOffDurationMs = screenOffDurationMs
        )
    }

    private fun telemetrySummary(state: ExtendedDeviceState): String =
        "thermal=${state.baseState.thermalStatus.value?.name ?: "UNAVAILABLE"}, " +
                "battery=${state.baseState.batteryPercent.value?.toString() ?: "UNAVAILABLE"}, " +
                "foreground=${state.baseState.foregroundPackage.value ?: "UNAVAILABLE"}"

    private fun snapshotHasMeaningfulTelemetry(snapshot: DeviceTelemetrySnapshot): Boolean =
        snapshot.cpuMetrics.state != com.antigravity.smarthub.core.telemetry.TelemetryState.UNAVAILABLE ||
                snapshot.battery.state != com.antigravity.smarthub.core.telemetry.TelemetryState.UNAVAILABLE ||
                snapshot.thermalStatus.state != com.antigravity.smarthub.core.telemetry.TelemetryState.UNAVAILABLE ||
                snapshot.display.state != com.antigravity.smarthub.core.telemetry.TelemetryState.UNAVAILABLE ||
                snapshot.foregroundPackage.state != com.antigravity.smarthub.core.telemetry.TelemetryState.UNAVAILABLE
}
