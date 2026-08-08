package com.antigravity.smarthub.core.persistence

import com.antigravity.smarthub.core.model.SmartHubProfile
import java.io.File

/** App-private durable user runtime settings. Defaults are fail-safe: optimization is OFF. */
class OptimizationSettingsRepository(private val storageFile: File? = null) {
    private var enabled = false
    private var automatic = true
    private var manualProfile: SmartHubProfile? = null
    private var corrupt = false

    init {
        load()
    }

    @Synchronized fun isOptimizationEnabled(): Boolean = enabled
    @Synchronized fun isAutomaticMode(): Boolean = automatic
    @Synchronized fun getManualProfile(): SmartHubProfile? = manualProfile
    @Synchronized fun isCorrupt(): Boolean = corrupt

    @Synchronized fun setOptimizationEnabled(value: Boolean) {
        enabled = value
        persist()
    }

    @Synchronized fun setAutomaticMode(value: Boolean) {
        automatic = value
        if (value) manualProfile = null
        persist()
    }

    @Synchronized fun setManualProfile(profile: SmartHubProfile?) {
        manualProfile = profile
        automatic = profile == null
        persist()
    }

    private fun load() {
        val file = storageFile ?: return
        if (!file.exists()) return
        try {
            val props = java.util.Properties()
            file.inputStream().use { props.load(it) }
            enabled = props.getProperty("optimizationEnabled", "false").toBooleanStrict()
            automatic = props.getProperty("automaticMode", "true").toBooleanStrict()
            manualProfile = props.getProperty("manualProfile")?.let { SmartHubProfile.valueOf(it) }
            if (automatic) manualProfile = null
        } catch (_: Exception) {
            corrupt = true
            enabled = false
            automatic = true
            manualProfile = null
        }
    }

    private fun persist() {
        val file = storageFile ?: return
        try {
            file.parentFile?.mkdirs()
            val props = java.util.Properties()
            props.setProperty("optimizationEnabled", enabled.toString())
            props.setProperty("automaticMode", automatic.toString())
            manualProfile?.let { props.setProperty("manualProfile", it.name) }
            file.outputStream().use { props.store(it, "Smart Hub runtime settings") }
        } catch (_: Exception) {
            // Runtime remains fail-safe; the next process will treat missing persistence conservatively.
        }
    }
}
