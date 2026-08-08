package com.antigravity.smarthub.core.state

import com.antigravity.smarthub.core.model.ActionHistoryRecord
import com.antigravity.smarthub.core.model.CapabilityResult
import com.antigravity.smarthub.core.model.DeviceState
import com.antigravity.smarthub.core.model.PrivilegeTier
import com.antigravity.smarthub.core.model.SmartHubProfile
import com.antigravity.smarthub.core.safety.SafetyGovernor
import com.antigravity.smarthub.core.telemetry.DeviceTelemetrySnapshot
import com.antigravity.smarthub.core.telemetry.TelemetryAggregator
import com.antigravity.smarthub.core.telemetry.TelemetryState
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class ControllerUiState(
    val deviceState: DeviceState = DeviceState(),
    val extendedState: ExtendedDeviceState = ExtendedDeviceState(baseState = DeviceState()),
    val resolvedState: ResolvedState = ResolvedState(
        activeProfile = SmartHubProfile.P5_DAILY_ADAPTIVE,
        rationale = "Initializing Smart Hub Policy Controller",
        recommendedActions = emptyList()
    ),
    val shizukuState: ShizukuState = ShizukuState.DISCONNECTED,
    val historyLog: List<ActionHistoryRecord> = emptyList()
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

    private var observationJob: Job? = null
    private var screenOffStartMs: Long = 0L
    private var lastProfile: SmartHubProfile = SmartHubProfile.P5_DAILY_ADAPTIVE

    fun start() {
        if (observationJob?.isActive == true) return

        shizukuConnection.bind()
        telemetryAggregator.startSampling()

        observationJob = scope.launch {
            // Observe Shizuku connection state reactively
            launch {
                shizukuConnection.shizukuState.collectLatest { sState ->
                    _uiState.value = _uiState.value.copy(shizukuState = sState)
                    // Trigger evaluation on privilege tier change
                    evaluateAndOptimize(_uiState.value.extendedState)
                }
            }

            // Observe ActionLedger history reactively
            launch {
                actionLedger.historyLog.collectLatest { history ->
                    _uiState.value = _uiState.value.copy(historyLog = history)
                }
            }

            // Observe Telemetry Stream reactively
            launch {
                telemetryAggregator.telemetryStream.collectLatest { snapshot ->
                    val extState = snapshotToExtendedDeviceState(snapshot)
                    _uiState.value = _uiState.value.copy(
                        deviceState = extState.baseState,
                        extendedState = extState
                    )
                    evaluateAndOptimize(extState)
                }
            }
        }
    }

    fun stop() {
        observationJob?.cancel()
        observationJob = null
        telemetryAggregator.stopSampling()
    }

    fun manualRefresh(): DeviceTelemetrySnapshot {
        val snapshot = telemetryAggregator.refreshTelemetry()
        val extState = snapshotToExtendedDeviceState(snapshot)
        _uiState.value = _uiState.value.copy(
            deviceState = extState.baseState,
            extendedState = extState
        )
        evaluateAndOptimize(extState)
        return snapshot
    }

    private fun evaluateAndOptimize(extState: ExtendedDeviceState) {
        // 1. Single Profile Authority determines candidate profile
        val profile = stateMachineEngine.updateState(extState)

        // 2. ProfileResolver generates actions explicitly for this selected profile
        val resolved = profileResolver.resolveActionsForProfile(profile, extState)
        _uiState.value = _uiState.value.copy(resolvedState = resolved)

        val previousProfile = lastProfile
        lastProfile = profile

        // 3. Action Execution & Reconciliation Loop
        for (action in resolved.recommendedActions) {
            // Avoid reapplying identical values every cycle & respect cooldown
            if (actionLedger.isAlreadyApplied(action)) continue
            if (actionLedger.isCooldownActive(action)) continue

            // 4. Safety Veto Check
            val veto = safetyGovernor.evaluateAction(action, extState.baseState)
            if (!veto.isAllowed) {
                actionLedger.recordHistory(
                    ActionHistoryRecord(
                        previousProfile = previousProfile,
                        newProfile = profile,
                        triggeringTelemetrySummary = "Thermal: ${extState.baseState.thermalStatus.value}, App: ${extState.baseState.foregroundPackage.value}",
                        actionId = action.actionId,
                        actionDescription = action.description,
                        safetyVetoResult = veto,
                        capabilityResult = CapabilityResult.UNAVAILABLE,
                        previousValue = null,
                        requestedValue = action.actionId,
                        verifiedValue = null,
                        rolledBack = false
                    )
                )
                continue
            }

            // 5. Transactional Execution & Readback Verification
            val result = actionExecutor.executeTransaction(action, extState.baseState)

            if (result.success) {
                actionLedger.recordAppliedAction(action)
            }

            actionLedger.recordHistory(
                ActionHistoryRecord(
                    previousProfile = previousProfile,
                    newProfile = profile,
                    triggeringTelemetrySummary = "Profile: ${profile.name}, Pkg: ${extState.baseState.foregroundPackage.value}",
                    actionId = action.actionId,
                    actionDescription = action.description,
                    safetyVetoResult = veto,
                    capabilityResult = result.capabilityResult,
                    previousValue = result.baselineCaptured,
                    requestedValue = action.actionId,
                    verifiedValue = result.verifiedValue,
                    rolledBack = result.rolledBack
                )
            )
        }
    }

    fun snapshotToExtendedDeviceState(snapshot: DeviceTelemetrySnapshot): ExtendedDeviceState {
        val sState = shizukuConnection.shizukuState.value
        val tier = if (sState == ShizukuState.CONNECTED) PrivilegeTier.TIER_1_SHIZUKU else PrivilegeTier.TIER_0_STOCK

        val isScreenOn = snapshot.display.value?.isScreenOn ?: true
        val nowMs = snapshot.capturedAtMs

        if (!isScreenOn) {
            if (screenOffStartMs == 0L) screenOffStartMs = nowMs
        } else {
            screenOffStartMs = 0L
        }

        val screenOffDurationMs = if (screenOffStartMs > 0L) (nowMs - screenOffStartMs) else 0L

        val deviceState = DeviceState(
            batteryPercent = snapshot.battery.value?.percent?.let { TelemetryValue(it, TelemetryState.AVAILABLE) } ?: TelemetryValue.unavailable(),
            isCharging = snapshot.battery.value?.let { TelemetryValue(it.isCharging, TelemetryState.AVAILABLE) } ?: TelemetryValue.unavailable(),
            batteryTempC = snapshot.battery.value?.tempC?.let { TelemetryValue(it, TelemetryState.AVAILABLE) } ?: TelemetryValue.unavailable(),
            apTempC = snapshot.measuredApTempC,
            thermalStatus = snapshot.thermalStatus,
            memoryAvailableMb = snapshot.memAvailableKb.value?.let { TelemetryValue(it / 1024L, TelemetryState.AVAILABLE) } ?: TelemetryValue.unavailable(),
            memoryPsiAvg10 = snapshot.memoryPsi.value?.let { TelemetryValue(it.someAvg10, TelemetryState.AVAILABLE) } ?: TelemetryValue.unavailable(),
            isScreenOn = snapshot.display.value?.let { TelemetryValue(it.isScreenOn, TelemetryState.AVAILABLE) } ?: TelemetryValue.unavailable(),
            foregroundPackage = snapshot.foregroundPackage,
            activeRefreshRateMode = snapshot.display.value?.secureSettingMode?.let { TelemetryValue(it, TelemetryState.AVAILABLE) } ?: TelemetryValue.unavailable(),
            privilegeTier = tier
        )

        return ExtendedDeviceState(
            baseState = deviceState,
            isMediaPlaying = snapshot.isMediaPlaying.value ?: false,
            isNavigationActive = snapshot.isNavigationActive.value ?: false,
            screenOffDurationMs = screenOffDurationMs
        )
    }
}
