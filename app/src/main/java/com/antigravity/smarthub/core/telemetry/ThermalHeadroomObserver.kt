package com.antigravity.smarthub.core.telemetry

import android.annotation.SuppressLint
import android.os.Build
import android.os.PowerManager
import com.antigravity.smarthub.core.model.ThermalStatusLevel
import java.io.File
import java.util.concurrent.Executor

class ThermalHeadroomObserver(
    private val powerManager: PowerManager? = null,
    private val executor: Executor? = null,
    private val sdkInt: Int = Build.VERSION.SDK_INT
) {
    private var currentThermalStatus: TelemetryValue<ThermalStatusLevel> = TelemetryValue.unavailable()
    private var callbackHeadroom: TelemetryValue<Float> = TelemetryValue.unavailable()
    private var callbackForecastHeadroom: TelemetryValue<Float> = TelemetryValue.unavailable()
    private var callbackForecastSeconds: Int? = null
    private var callbackThresholds: Map<Int, Float> = emptyMap()

    private var thermalStatusListener: PowerManager.OnThermalStatusChangedListener? = null
    private var thermalHeadroomListener: PowerManager.OnThermalHeadroomChangedListener? = null
    private var isObserving = false

    @SuppressLint("NewApi")
    fun startObserving() {
        if (isObserving) return

        if (sdkInt >= Build.VERSION_CODES.Q && powerManager != null) {
            try {
                val initialStatus = powerManager.currentThermalStatus
                currentThermalStatus = TelemetryValue(parseThermalStatus(initialStatus), TelemetryState.AVAILABLE)

                if (executor != null) {
                    val statusListener = PowerManager.OnThermalStatusChangedListener { status ->
                        currentThermalStatus = TelemetryValue(parseThermalStatus(status), TelemetryState.AVAILABLE)
                    }
                    powerManager.addThermalStatusListener(executor, statusListener)
                    thermalStatusListener = statusListener
                }
            } catch (e: Exception) {
                // Ignore listener registration error
            }
        }

        if (sdkInt >= 36 && powerManager != null && executor != null) {
            try {
                val headroomListener = PowerManager.OnThermalHeadroomChangedListener { currentHeadroom, forecastHeadroom, forecastSeconds, thresholds ->
                    callbackHeadroom = if (!currentHeadroom.isNaN() && currentHeadroom >= 0.0f) {
                        TelemetryValue(currentHeadroom, TelemetryState.AVAILABLE)
                    } else {
                        TelemetryValue.unavailable()
                    }

                    callbackForecastHeadroom = if (!forecastHeadroom.isNaN() && forecastHeadroom >= 0.0f) {
                        TelemetryValue(forecastHeadroom, TelemetryState.AVAILABLE)
                    } else {
                        TelemetryValue.unavailable()
                    }

                    callbackForecastSeconds = forecastSeconds
                    callbackThresholds = thresholds
                }
                powerManager.addThermalHeadroomListener(executor, headroomListener)
                thermalHeadroomListener = headroomListener
            } catch (e: Exception) {
                // Ignore headroom listener registration error
            }
        }

        isObserving = true
    }

    @SuppressLint("NewApi")
    fun stopObserving() {
        if (!isObserving) return

        if (sdkInt >= Build.VERSION_CODES.Q && powerManager != null && thermalStatusListener != null) {
            try {
                powerManager.removeThermalStatusListener(thermalStatusListener!!)
            } catch (e: Exception) {
                // Ignore unregister errors
            } finally {
                thermalStatusListener = null
            }
        }

        if (sdkInt >= 36 && powerManager != null && thermalHeadroomListener != null) {
            try {
                powerManager.removeThermalHeadroomListener(thermalHeadroomListener!!)
            } catch (e: Exception) {
                // Ignore unregister errors
            } finally {
                thermalHeadroomListener = null
            }
        }

        isObserving = false
    }

    fun getThermalStatus(): TelemetryValue<ThermalStatusLevel> {
        if (currentThermalStatus.state == TelemetryState.AVAILABLE) {
            return currentThermalStatus
        }

        if (sdkInt < Build.VERSION_CODES.Q) {
            return TelemetryValue.unsupported()
        }

        if (powerManager == null) {
            return TelemetryValue.unavailable()
        }

        return try {
            val status = powerManager.currentThermalStatus
            val level = parseThermalStatus(status)
            currentThermalStatus = TelemetryValue(level, TelemetryState.AVAILABLE)
            currentThermalStatus
        } catch (e: Exception) {
            TelemetryValue.unavailable()
        }
    }

    fun getThermalHeadroom(forecastSeconds: Int = 30): TelemetryValue<Float> {
        if (sdkInt < Build.VERSION_CODES.R) {
            return TelemetryValue.unsupported()
        }

        if (powerManager == null) {
            return TelemetryValue.unavailable()
        }

        return try {
            val headroom = powerManager.getThermalHeadroom(forecastSeconds)
            if (!headroom.isNaN() && headroom >= 0.0f) {
                TelemetryValue(headroom, TelemetryState.AVAILABLE)
            } else {
                TelemetryValue.unavailable()
            }
        } catch (e: Exception) {
            TelemetryValue.unavailable()
        }
    }

    fun getCallbackHeadroom(): TelemetryValue<Float> = callbackHeadroom
    fun getCallbackForecastHeadroom(): TelemetryValue<Float> = callbackForecastHeadroom
    fun getCallbackForecastSeconds(): Int? = callbackForecastSeconds
    fun getCallbackThresholds(): Map<Int, Float> = callbackThresholds

    fun readMeasuredApTempC(): TelemetryValue<Float> {
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

    fun parseThermalStatus(status: Int): ThermalStatusLevel {
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
