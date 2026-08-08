package com.antigravity.smarthub.core.state

import com.antigravity.smarthub.core.model.ActionHistoryRecord
import com.antigravity.smarthub.core.model.CapabilityResult
import com.antigravity.smarthub.core.model.SafetyVetoResult
import com.antigravity.smarthub.core.model.SystemAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Action Ownership Ledger & Explainability History Log.
 * Tracks currently applied Smart Hub actions, prevents thrashing, and records execution history.
 */
class ActionLedger {

    // Keyed by action category (e.g. "REFRESH_RATE", "STANDBY_BUCKET_com.example")
    private val appliedActions = mutableMapOf<String, SystemAction>()
    private val actionLastExecutedMs = mutableMapOf<String, Long>()

    private val _historyLog = MutableStateFlow<List<ActionHistoryRecord>>(emptyList())
    val historyLog: StateFlow<List<ActionHistoryRecord>> = _historyLog.asStateFlow()

    fun getActionKey(action: SystemAction): String {
        return when (action) {
            is SystemAction.SetRefreshRate -> "REFRESH_RATE"
            is SystemAction.SetStandbyBucket -> "STANDBY_BUCKET_${action.packageName}"
            is SystemAction.SetAppOpsBackground -> "APPOPS_BG_${action.packageName}"
        }
    }

    fun isAlreadyApplied(action: SystemAction): Boolean {
        val key = getActionKey(action)
        return appliedActions[key] == action
    }

    fun isCooldownActive(action: SystemAction, cooldownMs: Long = 5000L, nowMs: Long = System.currentTimeMillis()): Boolean {
        val key = getActionKey(action)
        val lastExecuted = actionLastExecutedMs[key] ?: 0L
        return (nowMs - lastExecuted) < cooldownMs
    }

    fun recordAppliedAction(action: SystemAction, nowMs: Long = System.currentTimeMillis()) {
        val key = getActionKey(action)
        appliedActions[key] = action
        actionLastExecutedMs[key] = nowMs
    }

    fun removeAppliedAction(actionKey: String) {
        appliedActions.remove(actionKey)
    }

    fun getCurrentlyAppliedActions(): Map<String, SystemAction> {
        return appliedActions.toMap()
    }

    fun recordHistory(record: ActionHistoryRecord) {
        val current = _historyLog.value.toMutableList()
        current.add(0, record) // Newest first
        if (current.size > 50) {
            current.removeAt(current.size - 1)
        }
        _historyLog.value = current
    }
}
