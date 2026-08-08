package com.antigravity.smarthub.core.state

import com.antigravity.smarthub.core.model.ActionHistoryRecord
import com.antigravity.smarthub.core.model.CapabilityResult
import com.antigravity.smarthub.core.model.SystemAction
import com.antigravity.smarthub.core.persistence.AtomicPropertiesStore
import com.antigravity.smarthub.core.persistence.NoPersistenceFailure
import com.antigravity.smarthub.core.persistence.PersistenceFailureInjector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.Base64
import java.util.Properties

enum class OwnershipJournalState { PENDING, APPLIED, RESTORE_PENDING, RESTORED }

data class OwnershipJournalEntry(
    val key: String,
    val action: SystemAction,
    val baselineValue: String?,
    val verifiedValue: String?,
    val state: OwnershipJournalState,
    val capabilityResult: CapabilityResult,
    val lastAttemptMs: Long
)

/** Durable Smart-Hub ownership journal. Persistence failures remain fail-closed. */
class ActionLedger(
    private val storageFile: File? = null,
    failureInjector: PersistenceFailureInjector = NoPersistenceFailure
) {
    private val entries = mutableMapOf<String, OwnershipJournalEntry>()
    private val capabilitySuppressedUntilMs = mutableMapOf<String, Long>()
    private val _historyLog = MutableStateFlow<List<ActionHistoryRecord>>(emptyList())
    private val store = storageFile?.let { AtomicPropertiesStore(it, failureInjector) }
    val historyLog: StateFlow<List<ActionHistoryRecord>> = _historyLog.asStateFlow()

    @Volatile var persistenceCorrupt: Boolean = false
        private set
    @Volatile var persistenceFailed: Boolean = false
        private set
    @Volatile var lastPersistenceError: String? = null
        private set

    init { load() }

    @Synchronized
    fun getActionKey(action: SystemAction): String = when (action) {
        is SystemAction.SetRefreshRate -> REFRESH_RATE_KEY
        is SystemAction.SetStandbyBucket -> "$STANDBY_PREFIX${action.packageName}"
        is SystemAction.SetAppOpsBackground -> "$APPOPS_PREFIX${action.packageName}"
    }

    @Synchronized fun isAlreadyApplied(action: SystemAction): Boolean =
        entries[getActionKey(action)]?.let { it.state == OwnershipJournalState.APPLIED && it.action == action } == true

    @Synchronized
    fun isCooldownActive(action: SystemAction, cooldownMs: Long = 5_000L, nowMs: Long = System.currentTimeMillis(), bypass: Boolean = false): Boolean {
        if (bypass) return false
        val lastAttempt = entries[getActionKey(action)]?.lastAttemptMs ?: return false
        return nowMs - lastAttempt < cooldownMs
    }

    @Synchronized fun recordAttempt(action: SystemAction, nowMs: Long = System.currentTimeMillis()): Boolean {
        val key = getActionKey(action)
        val existing = entries[key] ?: return true
        return replaceAndPersist(existing.copy(lastAttemptMs = nowMs))
    }

    @Synchronized
    fun recordPendingAction(action: SystemAction, baselineValue: String?, nowMs: Long = System.currentTimeMillis()): Boolean {
        if (!isPersistenceHealthy()) return false
        val key = getActionKey(action)
        val previous = entries[key]
        val pending = OwnershipJournalEntry(key, action, baselineValue, null, OwnershipJournalState.PENDING, CapabilityResult.UNAVAILABLE, nowMs)
        entries[key] = pending
        if (persist()) return true
        if (previous == null) entries.remove(key) else entries[key] = previous
        return false
    }

    @Synchronized
    fun recordAppliedAction(
        action: SystemAction,
        verifiedValue: String? = null,
        capabilityResult: CapabilityResult = CapabilityResult.SUPPORTED,
        nowMs: Long = System.currentTimeMillis()
    ): Boolean {
        if (!isPersistenceHealthy()) return false
        val key = getActionKey(action)
        val previous = entries[key]
        val baseline = previous?.baselineValue
        entries[key] = OwnershipJournalEntry(key, action, baseline, verifiedValue, OwnershipJournalState.APPLIED, capabilityResult, nowMs)
        if (persist()) return true
        if (previous == null) entries.remove(key) else entries[key] = previous
        return false
    }

    @Synchronized fun markRestorePending(actionKey: String, nowMs: Long = System.currentTimeMillis()): Boolean {
        val previous = entries[actionKey] ?: return false
        val next = previous.copy(state = OwnershipJournalState.RESTORE_PENDING, lastAttemptMs = nowMs)
        return replaceAndPersist(next, previous)
    }

    /** Ownership is cleared only after the caller has verified the exact baseline. */
    @Synchronized fun recordRestoredAction(actionKey: String, nowMs: Long = System.currentTimeMillis()): Boolean {
        val previous = entries[actionKey] ?: return true
        val next = previous.copy(state = OwnershipJournalState.RESTORED, lastAttemptMs = nowMs)
        return replaceAndPersist(next, previous)
    }

    @Synchronized fun recordCapabilitySuppression(actionKey: String, durationMs: Long = 24 * 60 * 60 * 1000L, nowMs: Long = System.currentTimeMillis()): Boolean {
        val oldSuppression = capabilitySuppressedUntilMs[actionKey]
        val entry = entries[actionKey]
        capabilitySuppressedUntilMs[actionKey] = nowMs + durationMs
        if (entry != null) entries[actionKey] = entry.copy(capabilityResult = CapabilityResult.PARTIALLY_SUPPORTED)
        return if (persist()) true else {
            if (oldSuppression == null) capabilitySuppressedUntilMs.remove(actionKey) else capabilitySuppressedUntilMs[actionKey] = oldSuppression
            if (entry == null) entries.remove(actionKey) else entries[actionKey] = entry
            false
        }
    }

    @Synchronized fun isCapabilitySuppressed(actionKey: String, nowMs: Long = System.currentTimeMillis()): Boolean =
        (capabilitySuppressedUntilMs[actionKey] ?: 0L) > nowMs

    @Synchronized fun getLastVerifiedValue(actionKey: String): String? = entries[actionKey]?.verifiedValue
    @Synchronized fun recordVerification(actionKey: String, verifiedValue: String?, nowMs: Long = System.currentTimeMillis()): Boolean {
        val previous = entries[actionKey] ?: return false
        return replaceAndPersist(previous.copy(verifiedValue = verifiedValue, lastAttemptMs = nowMs))
    }
    @Synchronized fun getCapabilityState(actionKey: String): CapabilityResult? = entries[actionKey]?.capabilityResult
    @Synchronized fun getBaselineReference(actionKey: String): String? = entries[actionKey]?.baselineValue
    @Synchronized fun getJournalState(actionKey: String): OwnershipJournalState? = entries[actionKey]?.state
    @Synchronized fun getJournalEntries(): List<OwnershipJournalEntry> = entries.values.toList()

    @Synchronized fun removeAppliedAction(actionKey: String): Boolean = recordRestoredAction(actionKey)

    @Synchronized fun getCurrentlyAppliedActions(): Map<String, SystemAction> = entries
        .filterValues { it.state != OwnershipJournalState.RESTORED }
        .mapValues { it.value.action }

    @Synchronized fun recordHistory(record: ActionHistoryRecord) {
        val current = _historyLog.value.toMutableList()
        current.add(0, record)
        if (current.size > 50) current.removeAt(current.size - 1)
        _historyLog.value = current
    }

    private fun isPersistenceHealthy(): Boolean = !persistenceCorrupt && !persistenceFailed

    private fun replaceAndPersist(next: OwnershipJournalEntry, previous: OwnershipJournalEntry? = entries[next.key]): Boolean {
        if (!isPersistenceHealthy()) return false
        entries[next.key] = next
        if (persist()) return true
        if (previous == null) entries.remove(next.key) else entries[next.key] = previous
        return false
    }

    private fun persist(): Boolean {
        val atomicStore = store ?: return true
        if (persistenceCorrupt || persistenceFailed) return false
        return try {
            val props = Properties()
            props.setProperty("formatVersion", "1")
            entries.forEach { (key, entry) ->
                val encoded = encode(key)
                val prefix = "action.$encoded"
                props.setProperty("$prefix.spec", serialize(entry.action))
                props.setProperty("$prefix.state", entry.state.name)
                entry.baselineValue?.let { props.setProperty("$prefix.baseline", it) }
                entry.verifiedValue?.let { props.setProperty("$prefix.verified", it) }
                props.setProperty("$prefix.lastAttempt", entry.lastAttemptMs.toString())
                props.setProperty("$prefix.capability", entry.capabilityResult.name)
            }
            capabilitySuppressedUntilMs.forEach { (key, until) -> props.setProperty("suppressed.${encode(key)}", until.toString()) }
            atomicStore.write(props)
            true
        } catch (e: Exception) {
            persistenceFailed = true
            lastPersistenceError = e.message ?: "Unknown ownership persistence failure"
            false
        }
    }

    private fun load() {
        val file = storageFile ?: return
        if (!file.exists()) return
        try {
            val props = store!!.read()
            if (props.getProperty("formatVersion") != "1") throw IllegalArgumentException("Missing ownership format marker")
            props.stringPropertyNames().filter { it.startsWith("action.") }.forEach { property ->
                val encoded = property.removePrefix("action.").substringBefore('.')
                if (encoded.isBlank() || props.getProperty("action.$encoded.spec") == null) {
                    throw IllegalArgumentException("Truncated ownership record")
                }
            }
            props.stringPropertyNames().filter { it.startsWith("action.") && it.endsWith(".spec") }.forEach { specKey ->
                val encoded = specKey.removePrefix("action.").removeSuffix(".spec")
                val key = decode(encoded)
                val action = deserialize(props.getProperty(specKey)) ?: throw IllegalArgumentException("Invalid action record")
                val state = props.getProperty("action.$encoded.state")?.let { OwnershipJournalState.valueOf(it) } ?: OwnershipJournalState.APPLIED
                val capability = props.getProperty("action.$encoded.capability")?.let { CapabilityResult.valueOf(it) } ?: CapabilityResult.UNAVAILABLE
                entries[key] = OwnershipJournalEntry(
                    key = key,
                    action = action,
                    baselineValue = props.getProperty("action.$encoded.baseline"),
                    verifiedValue = props.getProperty("action.$encoded.verified"),
                    state = state,
                    capabilityResult = capability,
                    lastAttemptMs = props.getProperty("action.$encoded.lastAttempt")?.toLongOrNull()
                        ?: throw IllegalArgumentException("Invalid ownership timestamp")
                )
            }
            props.stringPropertyNames().filter { it.startsWith("suppressed.") }.forEach {
                capabilitySuppressedUntilMs[decode(it.removePrefix("suppressed."))] = props.getProperty(it).toLongOrNull()
                    ?: throw IllegalArgumentException("Invalid suppression timestamp")
            }
        } catch (e: Exception) {
            entries.clear()
            capabilitySuppressedUntilMs.clear()
            persistenceCorrupt = true
            lastPersistenceError = "Unreadable ownership storage: ${e.message}"
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
            "standby" -> if (parts.size == 3 && parts[1].isNotBlank() && parts[2] in setOf("exempted", "active", "working_set", "frequent", "rare", "restricted")) SystemAction.SetStandbyBucket(parts[1], parts[2]) else null
            "appops" -> if (parts.size == 3 && parts[1].isNotBlank()) runCatching { SystemAction.SetAppOpsBackground(parts[1], parts[2].toBooleanStrict()) }.getOrNull() else null
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
