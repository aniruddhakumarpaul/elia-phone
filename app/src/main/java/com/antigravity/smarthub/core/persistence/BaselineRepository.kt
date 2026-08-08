package com.antigravity.smarthub.core.persistence

/**
 * Thread-safe persistent repository for initial device baseline values.
 * Strictly preserves ORIGINAL setting values before Smart Hub takes ownership.
 * Never overwrites an established baseline during subsequent profile transitions.
 */
class BaselineRepository {

    private val settingBaselines = mutableMapOf<String, String>()
    private val standbyBucketBaselines = mutableMapOf<String, String>()
    private val appOpsBaselines = mutableMapOf<String, String>()

    @Synchronized
    fun saveSettingBaselineOnce(table: String, key: String, originalValue: String) {
        val compositeKey = "$table:$key"
        if (!settingBaselines.containsKey(compositeKey)) {
            settingBaselines[compositeKey] = originalValue
        }
    }

    @Synchronized
    fun getSettingBaseline(table: String, key: String): String? {
        return settingBaselines["$table:$key"]
    }

    @Synchronized
    fun saveStandbyBucketBaselineOnce(packageName: String, originalBucket: String) {
        if (!standbyBucketBaselines.containsKey(packageName)) {
            standbyBucketBaselines[packageName] = originalBucket
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
        }
    }

    @Synchronized
    fun getAppOpsBaseline(packageName: String): String? {
        return appOpsBaselines[packageName]
    }
}
