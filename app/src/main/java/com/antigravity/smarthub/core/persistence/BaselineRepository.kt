package com.antigravity.smarthub.core.persistence

import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe, persistent repository for initial device baseline values.
 * Saves baseline data to persistent storage (survives app process death & reboot).
 * Strictly preserves initial values; NEVER overwrites an established baseline during subsequent profile switches.
 */
class BaselineRepository(
    private val storageDir: File? = null
) {

    private val settingBaselines = ConcurrentHashMap<String, String>()
    private val standbyBucketBaselines = ConcurrentHashMap<String, String>()
    private val appOpsBaselines = ConcurrentHashMap<String, String>()

    private val storageFile: File? = storageDir?.let { File(it, "smart_hub_baselines.properties") }

    init {
        loadFromStorage()
    }

    @Synchronized
    fun saveSettingBaselineOnce(table: String, key: String, originalValue: String) {
        val compositeKey = "setting:$table:$key"
        if (!settingBaselines.containsKey(compositeKey)) {
            settingBaselines[compositeKey] = originalValue
            persistToStorage()
        }
    }

    @Synchronized
    fun getSettingBaseline(table: String, key: String): String? {
        return settingBaselines["setting:$table:$key"]
    }

    @Synchronized
    fun saveStandbyBucketBaselineOnce(packageName: String, originalBucket: String) {
        if (!standbyBucketBaselines.containsKey(packageName)) {
            standbyBucketBaselines[packageName] = originalBucket
            persistToStorage()
        }
    }

    @Synchronized
    fun getStandbyBucketBaseline(packageName: String): String? {
        return standbyBucketBaselines[packageName]
    }

    @Synchronized
    fun saveAppOpsBaselineOnce(packageName: String, originalMode: String) {
        if (!appOpsBaselines.containsKey(packageName)) {
            appOpsBaselines[packageName] = originalMode
            persistToStorage()
        }
    }

    @Synchronized
    fun getAppOpsBaseline(packageName: String): String? {
        return appOpsBaselines[packageName]
    }

    private fun persistToStorage() {
        storageFile?.let { file ->
            try {
                val props = java.util.Properties()
                settingBaselines.forEach { (k, v) -> props.setProperty(k, v) }
                standbyBucketBaselines.forEach { (k, v) -> props.setProperty("bucket:$k", v) }
                appOpsBaselines.forEach { (k, v) -> props.setProperty("appops:$k", v) }
                file.outputStream().use { props.store(it, "Smart Hub Baseline Snapshots") }
            } catch (e: Exception) {
                // Ignore storage write exceptions in non-file unit test contexts
            }
        }
    }

    private fun loadFromStorage() {
        storageFile?.let { file ->
            if (file.exists()) {
                try {
                    val props = java.util.Properties()
                    file.inputStream().use { props.load(it) }
                    props.stringPropertyNames().forEach { name ->
                        val value = props.getProperty(name)
                        when {
                            name.startsWith("setting:") -> settingBaselines[name] = value
                            name.startsWith("bucket:") -> standbyBucketBaselines[name.removePrefix("bucket:")] = value
                            name.startsWith("appops:") -> appOpsBaselines[name.removePrefix("appops:")] = value
                        }
                    }
                } catch (e: Exception) {
                    // Ignore storage read errors
                }
            }
        }
    }
}
