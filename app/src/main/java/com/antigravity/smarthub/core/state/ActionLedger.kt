package com.antigravity.smarthub.core.state

import com.antigravity.smarthub.core.model.ActionHistoryRecord
import com.antigravity.smarthub.core.model.SystemAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Single-writer-owned action state used to reconcile desired profile state. */
class ActionLedger {
    private val appliedActions = mutableMapOf<String, SystemAction>()
    private val actionLastAttemptMs = mutableMapOf<String, Long>()
    private val capabilitySuppressedUntilMs = mutableMapOf<String, Long>()
    private val _historyLog = MutableStateFlow<List<ActionHistoryRecord>>(emptyList())
    val historyLog: StateFlow<List<ActionHistoryRecord>> = _historyLog.asStateFlow()

    @Synchronized
    fun getActionKey(action: SystemAction): String = when (action) {
        is SystemAction.SetRefreshRate -> REFRESH_RATE_KEY
        is SystemAction.SetStandbyBucket -> "$STANDBY_PREFIX${action.packageName}"
        is SystemAction.SetAppOpsBackground -> "$APPOPS_PREFIX${action.packageName}"
    }

    @Synchronized
    fun isAlreadyApplied(action: SystemAction): Boolean = appliedActions[getActionKey(action)] == action

    @Synchronized
    fun isCooldownActive(
        action: SystemAction,
        cooldownMs: Long = 5_000L,
        nowMs: Long = System.currentTimeMillis(),
        bypass: Boolean = false
    ): Boolean {
        if (bypass) return false
        val lastAttempt = actionLastAttemptMs[getActionKey(action)] ?: return false
        return nowMs - lastAttempt < cooldownMs
    }

    @Synchronized
    fun recordAttempt(action: SystemAction, nowMs: Long = System.currentTimeMillis()) {
        actionLastAttemptMs[getActionKey(action)] = nowMs
    }

    @Synchronized
    fun recordAppliedAction(action: SystemAction, nowMs: Long = System.currentTimeMillis()) {
        val key = getActionKey(action)
        appliedActions[key] = action
        actionLastAttemptMs[key] = nowMs
    }

    @Synchronized
    fun recordRestoredAction(actionKey: String, nowMs: Long = System.currentTimeMillis()) {
        appliedActions.remove(actionKey)
        actionLastAttemptMs[actionKey] = nowMs
    }

    @Synchronized
    fun recordCapabilitySuppression(actionKey: String, durationMs: Long = 24 * 60 * 60 * 1000L, nowMs: Long = System.currentTimeMillis()) {
        capabilitySuppressedUntilMs[actionKey] = nowMs + durationMs
    }

    @Synchronized
    fun isCapabilitySuppressed(actionKey: String, nowMs: Long = System.currentTimeMillis()): Boolean =
        (capabilitySuppressedUntilMs[actionKey] ?: 0L) > nowMs

    @Synchronized
    fun removeAppliedAction(actionKey: String) {
        appliedActions.remove(actionKey)
    }

    @Synchronized
    fun getCurrentlyAppliedActions(): Map<String, SystemAction> = appliedActions.toMap()

    @Synchronized
    fun recordHistory(record: ActionHistoryRecord) {
        val current = _historyLog.value.toMutableList()
        current.add(0, record)
        if (current.size > 50) current.removeAt(current.size - 1)
        _historyLog.value = current
    }

    companion object {
        const val REFRESH_RATE_KEY = "REFRESH_RATE"
        const val STANDBY_PREFIX = "STANDBY_BUCKET_"
        const val APPOPS_PREFIX = "APPOPS_BG_"
    }
}
