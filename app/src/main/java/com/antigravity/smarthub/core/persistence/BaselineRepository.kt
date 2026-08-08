package com.antigravity.smarthub.core.persistence

import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Durable, exact baseline store. A corrupt or unreadable file is never treated as empty.
 * Production callers must check the Boolean result before crossing a mutation boundary.
 */
class BaselineRepository(
    private val storageDir: File? = null,
    failureInjector: PersistenceFailureInjector = NoPersistenceFailure
) {
    private val settingBaselines = ConcurrentHashMap<String, String>()
    private val standbyBucketBaselines = ConcurrentHashMap<String, String>()
    private val appOpsBaselines = ConcurrentHashMap<String, String>()
    private val storageFile: File? = storageDir?.let { File(it, "smart_hub_baselines.properties") }
    private val store: AtomicPropertiesStore? = storageFile?.let { AtomicPropertiesStore(it, failureInjector) }

    @Volatile var persistenceCorrupt: Boolean = false
        private set
    @Volatile var persistenceFailed: Boolean = false
        private set
    @Volatile var lastPersistenceError: String? = null
        private set

    init { loadFromStorage() }

    fun isHealthy(): Boolean = !persistenceCorrupt && !persistenceFailed

    private fun verifyExistingDurability(): Boolean {
        val file = storageFile ?: return true
        if (settingBaselines.isEmpty() && standbyBucketBaselines.isEmpty() && appOpsBaselines.isEmpty()) return true
        return try {
            if (!file.exists()) throw IllegalStateException("Baseline file disappeared")
            if (store!!.read().getProperty("formatVersion") != "1") throw IllegalStateException("Baseline format marker missing")
            true
        } catch (e: Exception) {
            persistenceCorrupt = true
            lastPersistenceError = "Baseline durability check failed: ${e.message}"
            false
        }
    }

    @Synchronized
    fun saveSettingBaselineOnce(table: String, key: String, originalValue: String): Boolean {
        if (!isHealthy() || !verifyExistingDurability()) return false
        val compositeKey = "setting:$table:$key"
        if (settingBaselines.containsKey(compositeKey)) return true
        val next = snapshotProperties()
        next.setProperty(compositeKey, originalValue)
        if (!persist(next)) return false
        settingBaselines[compositeKey] = originalValue
        return true
    }

    @Synchronized fun getSettingBaseline(table: String, key: String): String? =
        if (persistenceCorrupt) null else settingBaselines["setting:$table:$key"]

    @Synchronized
    fun saveStandbyBucketBaselineOnce(packageName: String, originalBucket: String): Boolean {
        if (!isHealthy() || !verifyExistingDurability()) return false
        if (standbyBucketBaselines.containsKey(packageName)) return true
        val next = snapshotProperties()
        next.setProperty("bucket:$packageName", originalBucket)
        if (!persist(next)) return false
        standbyBucketBaselines[packageName] = originalBucket
        return true
    }

    @Synchronized fun getStandbyBucketBaseline(packageName: String): String? =
        if (persistenceCorrupt) null else standbyBucketBaselines[packageName]

    @Synchronized
    fun saveAppOpsBaselineOnce(packageName: String, originalMode: String): Boolean {
        if (!isHealthy() || !verifyExistingDurability()) return false
        if (appOpsBaselines.containsKey(packageName)) return true
        val next = snapshotProperties()
        next.setProperty("appops:$packageName", originalMode)
        if (!persist(next)) return false
        appOpsBaselines[packageName] = originalMode
        return true
    }

    @Synchronized fun getAppOpsBaseline(packageName: String): String? =
        if (persistenceCorrupt) null else appOpsBaselines[packageName]

    private fun snapshotProperties(): java.util.Properties {
        val props = java.util.Properties()
        props.setProperty("formatVersion", "1")
        settingBaselines.forEach { (k, v) -> props.setProperty(k, v) }
        standbyBucketBaselines.forEach { (k, v) -> props.setProperty("bucket:$k", v) }
        appOpsBaselines.forEach { (k, v) -> props.setProperty("appops:$k", v) }
        return props
    }

    private fun persist(properties: java.util.Properties): Boolean {
        val atomicStore = store ?: return true
        return try {
            atomicStore.write(properties)
            true
        } catch (e: Exception) {
            persistenceFailed = true
            lastPersistenceError = e.message ?: "Unknown baseline persistence failure"
            false
        }
    }

    private fun loadFromStorage() {
        val atomicStore = store ?: return
        if (!storageFile!!.exists()) return
        try {
            val props = atomicStore.read()
            if (props.getProperty("formatVersion") != "1") throw IllegalArgumentException("Missing baseline format marker")
            props.stringPropertyNames().forEach { name ->
                val value = props.getProperty(name) ?: throw IllegalArgumentException("Null baseline value")
                when {
                    name.startsWith("setting:") -> settingBaselines[name] = value
                    name.startsWith("bucket:") -> standbyBucketBaselines[name.removePrefix("bucket:")] = value
                    name.startsWith("appops:") -> appOpsBaselines[name.removePrefix("appops:")] = value
                    name == "formatVersion" -> Unit
                    else -> throw IllegalArgumentException("Unknown baseline record '$name'")
                }
            }
        } catch (e: Exception) {
            settingBaselines.clear()
            standbyBucketBaselines.clear()
            appOpsBaselines.clear()
            persistenceCorrupt = true
            lastPersistenceError = "Unreadable baseline storage: ${e.message}"
        }
    }
}
