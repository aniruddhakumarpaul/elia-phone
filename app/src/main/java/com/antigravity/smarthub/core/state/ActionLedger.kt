package com.antigravity.smarthub.core.state

import com.antigravity.smarthub.core.model.ActionHistoryRecord
import com.antigravity.smarthub.core.model.CapabilityResult
import com.antigravity.smarthub.core.model.SystemAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.Base64

/** Durable Smart-Hub action ownership ledger. Corrupt state fails closed and never mutates. */
class ActionLedger(private val storageFile: File? = null) {
    private val appliedActions = mutableMapOf<String, SystemAction>()
    private val actionLastAttemptMs = mutableMapOf<String, Long>()
    private val lastVerifiedValues = mutableMapOf<String, String?>()
    private val capabilityStates = mutableMapOf<String, CapabilityResult>()
    private val capabilitySuppressedUntilMs = mutableMapOf<String, Long>()
    private val baselineReferences = mutableMapOf<String, String>()
    private val _historyLog = MutableStateFlow<List<ActionHistoryRecord>>(emptyList())
    val historyLog: StateFlow<List<ActionHistoryRecord>> = _historyLog.asStateFlow()
    @Volatile var persistenceCorrupt: Boolean = false
        private set

    init { load() }

    @Synchronized
    fun getActionKey(action: SystemAction): String = when (action) {
        is SystemAction.SetRefreshRate -> REFRESH_RATE_KEY
        is SystemAction.SetStandbyBucket -> "$STANDBY_PREFIX${action.packageName}"
        is SystemAction.SetAppOpsBackground -> "$APPOPS_PREFIX${action.packageName}"
    }

    @Synchronized fun isAlreadyApplied(action: SystemAction): Boolean = appliedActions[getActionKey(action)] == action

    @Synchronized
    fun isCooldownActive(action: SystemAction, cooldownMs: Long = 5_000L, nowMs: Long = System.currentTimeMillis(), bypass: Boolean = false): Boolean {
        if (bypass) return false
        val lastAttempt = actionLastAttemptMs[getActionKey(action)] ?: return false
        return nowMs - lastAttempt < cooldownMs
    }

    @Synchronized fun recordAttempt(action: SystemAction, nowMs: Long = System.currentTimeMillis()) {
        actionLastAttemptMs[getActionKey(action)] = nowMs
        persist()
    }

    @Synchronized
    fun recordAppliedAction(
        action: SystemAction,
        verifiedValue: String? = null,
        capabilityResult: CapabilityResult = CapabilityResult.SUPPORTED,
        nowMs: Long = System.currentTimeMillis()
    ) {
        val key = getActionKey(action)
        appliedActions[key] = action
        actionLastAttemptMs[key] = nowMs
        lastVerifiedValues[key] = verifiedValue
        capabilityStates[key] = capabilityResult
        baselineReferences[key] = key
        persist()
    }

    @Synchronized fun recordRestoredAction(actionKey: String, nowMs: Long = System.currentTimeMillis()) {
        appliedActions.remove(actionKey)
        actionLastAttemptMs[actionKey] = nowMs
        lastVerifiedValues.remove(actionKey)
        capabilityStates.remove(actionKey)
        capabilitySuppressedUntilMs.remove(actionKey)
        baselineReferences.remove(actionKey)
        persist()
    }

    @Synchronized fun recordCapabilitySuppression(actionKey: String, durationMs: Long = 24 * 60 * 60 * 1000L, nowMs: Long = System.currentTimeMillis()) {
        capabilitySuppressedUntilMs[actionKey] = nowMs + durationMs
        capabilityStates[actionKey] = CapabilityResult.PARTIALLY_SUPPORTED
        persist()
    }

    @Synchronized fun isCapabilitySuppressed(actionKey: String, nowMs: Long = System.currentTimeMillis()): Boolean =
        (capabilitySuppressedUntilMs[actionKey] ?: 0L) > nowMs

    @Synchronized fun getLastVerifiedValue(actionKey: String): String? = lastVerifiedValues[actionKey]
    @Synchronized fun recordVerification(actionKey: String, verifiedValue: String?, nowMs: Long = System.currentTimeMillis()) {
        if (appliedActions.containsKey(actionKey)) {
            lastVerifiedValues[actionKey] = verifiedValue
            actionLastAttemptMs[actionKey] = nowMs
            persist()
        }
    }
    @Synchronized fun getCapabilityState(actionKey: String): CapabilityResult? = capabilityStates[actionKey]
    @Synchronized fun getBaselineReference(actionKey: String): String? = baselineReferences[actionKey]
    @Synchronized fun removeAppliedAction(actionKey: String) { appliedActions.remove(actionKey); persist() }
    @Synchronized fun getCurrentlyAppliedActions(): Map<String, SystemAction> = appliedActions.toMap()

    @Synchronized fun recordHistory(record: ActionHistoryRecord) {
        val current = _historyLog.value.toMutableList()
        current.add(0, record)
        if (current.size > 50) current.removeAt(current.size - 1)
        _historyLog.value = current
    }

    private fun load() {
        val file = storageFile ?: return
        if (!file.exists()) return
        try {
            val props = java.util.Properties()
            file.inputStream().use { props.load(it) }
            val ids = props.stringPropertyNames().filter { it.startsWith("action.") && it.endsWith(".spec") }
            ids.forEach { specKey ->
                val encoded = specKey.removePrefix("action.").removeSuffix(".spec")
                val key = decode(encoded)
                val action = deserialize(props.getProperty(specKey)) ?: throw IllegalArgumentException("Invalid action record")
                appliedActions[key] = action
                actionLastAttemptMs[key] = props.getProperty("action.$encoded.lastAttempt")?.toLongOrNull() ?: 0L
                lastVerifiedValues[key] = props.getProperty("action.$encoded.verified")
                capabilityStates[key] = props.getProperty("action.$encoded.capability")?.let { CapabilityResult.valueOf(it) }
                    ?: CapabilityResult.UNAVAILABLE
                capabilitySuppressedUntilMs[key] = props.getProperty("action.$encoded.suppressedUntil")?.toLongOrNull() ?: 0L
                baselineReferences[key] = props.getProperty("action.$encoded.baselineRef") ?: key
            }
        } catch (_: Exception) {
            persistenceCorrupt = true
            appliedActions.clear()
            actionLastAttemptMs.clear()
            lastVerifiedValues.clear()
            capabilityStates.clear()
            capabilitySuppressedUntilMs.clear()
            baselineReferences.clear()
        }
    }

    private fun persist() {
        val file = storageFile ?: return
        if (persistenceCorrupt) return
        try {
            file.parentFile?.mkdirs()
            val props = java.util.Properties()
            appliedActions.forEach { (key, action) ->
                val encoded = encode(key)
                val prefix = "action.$encoded"
                props.setProperty("$prefix.spec", serialize(action))
                props.setProperty("$prefix.lastAttempt", (actionLastAttemptMs[key] ?: 0L).toString())
                lastVerifiedValues[key]?.let { props.setProperty("$prefix.verified", it) }
                props.setProperty("$prefix.capability", (capabilityStates[key] ?: CapabilityResult.UNAVAILABLE).name)
                props.setProperty("$prefix.suppressedUntil", (capabilitySuppressedUntilMs[key] ?: 0L).toString())
                props.setProperty("$prefix.baselineRef", baselineReferences[key] ?: key)
            }
            file.outputStream().use { props.store(it, "Smart Hub action ownership") }
        } catch (_: Exception) {
            // Failing to persist cannot safely be repaired here; controller exposes the pending state.
        }
    }

    private fun serialize(action: SystemAction): String = when (action) {
        is SystemAction.SetRefreshRate -> "refresh|${action.targetMode}"
        is SystemAction.SetStandbyBucket -> "standby|${action.packageName}|${action.targetBucket}"
        is SystemAction.SetAppOpsBackground -> "appops|${action.packageName}|${action.allow}"
    }

    private fun deserialize(value: String?): SystemAction? {
        val parts = value?.split('|') ?: return null
        return when (parts.firstOrNull()) {
            "refresh" -> parts.getOrNull(1)?.toIntOrNull()?.takeIf { it == 0 || it == 1 }?.let { SystemAction.SetRefreshRate(it) }
            "standby" -> if (parts.size == 3) SystemAction.SetStandbyBucket(parts[1], parts[2]) else null
            "appops" -> if (parts.size == 3) SystemAction.SetAppOpsBackground(parts[1], parts[2].toBooleanStrict()) else null
            else -> null
        }
    }

    private fun encode(value: String): String = Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray())
    private fun decode(value: String): String = String(Base64.getUrlDecoder().decode(value))

    companion object {
        const val REFRESH_RATE_KEY = "REFRESH_RATE"
        const val STANDBY_PREFIX = "STANDBY_BUCKET_"
        const val APPOPS_PREFIX = "APPOPS_BG_"
    }
}
