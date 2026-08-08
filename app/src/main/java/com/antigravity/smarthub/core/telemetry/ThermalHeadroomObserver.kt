package com.antigravity.smarthub.core.telemetry

import android.os.Build
import android.os.PowerManager
import com.antigravity.smarthub.core.model.ThermalStatusLevel

data class ThermalRiskState(
    val statusLevel: ThermalStatusLevel = ThermalStatusLevel.NOMINAL,
    val headroomForecast: Float = 0.0f, // 0.0 to 1.0 (1.0 = Throttling threshold)
    val batteryTempC: Float = 25.0f,
    val apTempC: Float = 28.0f
)

class ThermalHeadroomObserver(
    private val powerManager: PowerManager? = null
) {

    fun calculateThermalRisk(batteryTempC: Float, apTempC: Float): ThermalRiskState {
        val headroom = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && powerManager != null) {
            try {
                powerManager.getThermalHeadroom(30) // 30-second forecast headroom
            } catch (e: Exception) {
                0.0f
            }
        } else {
            0.0f
        }

        val statusLevel = when {
            apTempC >= 48.0f || batteryTempC >= 45.0f || headroom >= 1.0f -> ThermalStatusLevel.CRITICAL
            apTempC >= 45.0f || batteryTempC >= 42.0f || headroom >= 0.85f -> ThermalStatusLevel.SEVERE
            apTempC >= 42.0f || batteryTempC >= 39.0f || headroom >= 0.70f -> ThermalStatusLevel.MODERATE
            apTempC >= 38.0f || batteryTempC >= 37.0f -> ThermalStatusLevel.WARM
            else -> ThermalStatusLevel.NOMINAL
        }

        return ThermalRiskState(
            statusLevel = statusLevel,
            headroomForecast = headroom,
            batteryTempC = batteryTempC,
            apTempC = apTempC
        )
    }
}
