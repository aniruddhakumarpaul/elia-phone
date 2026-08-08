package com.antigravity.smarthub.core.telemetry

import android.os.Build
import android.os.PowerManager
import com.antigravity.smarthub.core.model.ThermalStatusLevel
import java.io.File
import java.util.concurrent.Executor

class ThermalHeadroomObserver(
    private val powerManager: PowerManager? = null,
    private val executor: Executor? = null
) {
    private var currentThermalStatus: ThermalStatusLevel = ThermalStatusLevel.NOMINAL
    private var thermalStatusListener: Any? = null
    private var isObserving = false

    fun startObserving() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && powerManager != null && executor != null && !isObserving) {
            try {
                val listener = PowerManager.OnThermalStatusChangedListener { status ->
                    currentThermalStatus = parseThermalStatus(status)
                }
                powerManager.addThermalStatusListener(executor, listener)
                thermalStatusListener = listener
                isObserving = true
            } catch (e: Exception) {
                isObserving = false
            }
        }
    }

    fun stopObserving() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && powerManager != null && isObserving && thermalStatusListener != null) {
            try {
                (thermalStatusListener as? PowerManager.OnThermalStatusChangedListener)?.let {
                    powerManager.removeThermalStatusListener(it)
                }
            } catch (e: Exception) {
                // Ignore lifecycle cleanup errors
            } finally {
                isObserving = false
                thermalStatusListener = null
            }
        }
    }

    fun getThermalStatus(): TelemetryValue<ThermalStatusLevel> {
        return TelemetryValue(currentThermalStatus, TelemetryState.AVAILABLE)
    }

    fun getThermalHeadroom(forecastSeconds: Int = 30): TelemetryValue<Float> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && powerManager != null) {
            try {
                val headroom = powerManager.getThermalHeadroom(forecastSeconds)
                if (headroom >= 0.0f) {
                    return TelemetryValue(headroom, TelemetryState.AVAILABLE)
                }
            } catch (e: Exception) {
                return TelemetryValue.unavailable()
            }
        }
        return TelemetryValue.unsupported()
    }

    fun readMeasuredApTempC(): TelemetryValue<Float> {
        // Verified Exynos thermal zones probe (only returns genuine measured temp, never synthesized)
        val thermalDir = File("/sys/class/thermal")
        if (!thermalDir.exists() || !thermalDir.canRead()) return TelemetryValue.unavailable()

        try {
            thermalDir.listFiles()?.forEach { zone ->
                val typeFile = File(zone, "type")
                if (typeFile.exists() && typeFile.canRead()) {
                    val type = typeFile.readText().trim().lowercase()
                    if (type.contains("cpu") || type.contains("ap") || type.contains("soc")) {
                        val tempFile = File(zone, "temp")
                        if (tempFile.exists() && tempFile.canRead()) {
                            val rawTemp = tempFile.readText().trim().toFloatOrNull()
                            if (rawTemp != null) {
                                val tempC = if (rawTemp > 1000f) rawTemp / 1000f else rawTemp
                                return TelemetryValue(tempC, TelemetryState.AVAILABLE)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore sysfs read errors
        }
        return TelemetryValue.unavailable()
    }

    private fun parseThermalStatus(status: Int): ThermalStatusLevel {
        return when (status) {
            PowerManager.THERMAL_STATUS_NONE -> ThermalStatusLevel.NOMINAL
            PowerManager.THERMAL_STATUS_LIGHT -> ThermalStatusLevel.WARM
            PowerManager.THERMAL_STATUS_MODERATE -> ThermalStatusLevel.MODERATE
            PowerManager.THERMAL_STATUS_SEVERE -> ThermalStatusLevel.SEVERE
            PowerManager.THERMAL_STATUS_CRITICAL,
            PowerManager.THERMAL_STATUS_EMERGENCY,
            PowerManager.THERMAL_STATUS_SHUTDOWN -> ThermalStatusLevel.CRITICAL
            else -> ThermalStatusLevel.NOMINAL
        }
    }
}
