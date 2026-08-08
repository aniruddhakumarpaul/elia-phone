package com.antigravity.smarthub.core.persistence

import com.antigravity.smarthub.core.model.SmartHubProfile
import java.io.File
import java.util.Properties

/** Atomic app-private runtime settings. A failed write never reports the new value as durable. */
class OptimizationSettingsRepository(
    private val storageFile: File? = null,
    failureInjector: PersistenceFailureInjector = NoPersistenceFailure
) {
    private val store = storageFile?.let { AtomicPropertiesStore(it, failureInjector) }
    private var enabled = false
    private var automatic = true
    private var manualProfile: SmartHubProfile? = null

    @Volatile var persistenceCorrupt: Boolean = false
        private set
    @Volatile var persistenceFailed: Boolean = false
        private set
    @Volatile var lastPersistenceError: String? = null
        private set

    init { load() }

    @Synchronized fun isOptimizationEnabled(): Boolean = enabled
    @Synchronized fun isAutomaticMode(): Boolean = automatic
    @Synchronized fun getManualProfile(): SmartHubProfile? = manualProfile
    @Synchronized fun isCorrupt(): Boolean = persistenceCorrupt
    @Synchronized fun isPersistenceHealthy(): Boolean = !persistenceCorrupt && !persistenceFailed

    @Synchronized fun setOptimizationEnabled(value: Boolean): Boolean {
        val old = enabled
        enabled = value
        return if (persist()) true else { enabled = old; false }
    }

    @Synchronized fun setAutomaticMode(value: Boolean): Boolean {
        val oldAutomatic = automatic
        val oldProfile = manualProfile
        automatic = value
        if (value) manualProfile = null
        return if (persist()) true else { automatic = oldAutomatic; manualProfile = oldProfile; false }
    }

    @Synchronized fun setManualProfile(profile: SmartHubProfile?): Boolean {
        val oldAutomatic = automatic
        val oldProfile = manualProfile
        manualProfile = profile
        automatic = profile == null
        return if (persist()) true else { automatic = oldAutomatic; manualProfile = oldProfile; false }
    }

    private fun persist(): Boolean {
        val atomicStore = store ?: return true
        if (!isPersistenceHealthy()) return false
        return try {
            val props = Properties()
            props.setProperty("formatVersion", "1")
            props.setProperty("optimizationEnabled", enabled.toString())
            props.setProperty("automaticMode", automatic.toString())
            manualProfile?.let { props.setProperty("manualProfile", it.name) }
            atomicStore.write(props)
            true
        } catch (e: Exception) {
            persistenceFailed = true
            lastPersistenceError = e.message ?: "Unknown runtime settings persistence failure"
            false
        }
    }

    private fun load() {
        val file = storageFile ?: return
        if (!file.exists()) return
        try {
            val props = store!!.read()
            if (props.getProperty("formatVersion") != "1") throw IllegalArgumentException("Missing settings format marker")
            enabled = props.getProperty("optimizationEnabled")?.toBooleanStrict()
                ?: throw IllegalArgumentException("Missing optimizationEnabled")
            automatic = props.getProperty("automaticMode")?.toBooleanStrict()
                ?: throw IllegalArgumentException("Missing automaticMode")
            manualProfile = props.getProperty("manualProfile")?.let { SmartHubProfile.valueOf(it) }
            if (automatic) manualProfile = null
        } catch (e: Exception) {
            persistenceCorrupt = true
            enabled = false
            automatic = true
            manualProfile = null
            lastPersistenceError = "Unreadable runtime settings: ${e.message}"
        }
    }
}
