package com.antigravity.smarthub.core.telemetry

import com.antigravity.smarthub.core.model.ThermalStatusLevel

enum class MemoryPressureLevel {
    NORMAL,
    PRESSURED,
    THRASHING,
    CRITICAL
}

data class CpuCoreMetric(
    val coreId: Int,
    val isOnline: Boolean,
    val frequencyHz: Long?,
    val governor: String? = null
)

data class PsiMetric(
    val someAvg10: Float = 0.0f,
    val someAvg60: Float = 0.0f,
    val someAvg300: Float = 0.0f,
    val fullAvg10: Float = 0.0f,
    val fullAvg60: Float = 0.0f,
    val fullAvg300: Float = 0.0f
)

data class ZramMetrics(
    val origDataSizeByte: Long = 0L,
    val comprDataSizeByte: Long = 0L,
    val memUsedTotalByte: Long = 0L,
    val diskSizeBytes: Long = 0L,
    val compressionRatio: Float = 1.0f
)

data class BatteryMetrics(
    val percent: Int?,
    val tempC: Float?,
    val voltageMv: Int?,
    val currentNowMa: Int?,
    val currentAvgMa: Int?,
    val isCharging: Boolean,
    val plugType: String?
)

data class DisplayMetrics(
    val secureSettingMode: Int?,
    val physicalRefreshRateHz: Float?,
    val isScreenOn: Boolean,
    val displayStateStr: String
)

data class DeviceTelemetrySnapshot(
    val capturedAtMs: Long = System.currentTimeMillis(),
    val cpuMetrics: TelemetryValue<List<CpuCoreMetric>> = TelemetryValue.unavailable(),
    val memTotalKb: TelemetryValue<Long> = TelemetryValue.unavailable(),
    val memAvailableKb: TelemetryValue<Long> = TelemetryValue.unavailable(),
    val memoryPressure: TelemetryValue<MemoryPressureLevel> = TelemetryValue.unavailable(),
    val memoryPsi: TelemetryValue<PsiMetric> = TelemetryValue.unavailable(),
    val zram: TelemetryValue<ZramMetrics> = TelemetryValue.unavailable(),
    val battery: TelemetryValue<BatteryMetrics> = TelemetryValue.unavailable(),
    val thermalStatus: TelemetryValue<ThermalStatusLevel> = TelemetryValue.unavailable(),
    val thermalHeadroom: TelemetryValue<Float> = TelemetryValue.unavailable(),
    val thermalForecastHeadroom: TelemetryValue<Float> = TelemetryValue.unavailable(),
    val measuredApTempC: TelemetryValue<Float> = TelemetryValue.unavailable(),
    val display: TelemetryValue<DisplayMetrics> = TelemetryValue.unavailable(),
    val foregroundPackage: TelemetryValue<String> = TelemetryValue.unavailable(),
    val isMediaPlaying: TelemetryValue<Boolean> = TelemetryValue.unavailable(),
    val isNavigationActive: TelemetryValue<Boolean> = TelemetryValue.unavailable()
) {
    fun isStale(maxAgeMs: Long, currentTimeMs: Long = System.currentTimeMillis()): Boolean {
        return (currentTimeMs - capturedAtMs) > maxAgeMs
    }
}
