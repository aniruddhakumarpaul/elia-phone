package com.antigravity.smarthub.core.telemetry

data class DeviceTelemetrySnapshot(
    val timestampMs: Long = System.currentTimeMillis(),
    val cpuFrequenciesHz: Map<Int, Long> = emptyMap(),
    val cpuOnlineStatus: Map<Int, Boolean> = emptyMap(),
    val memTotalKb: Long = 0L,
    val memAvailableKb: Long = 0L,
    val swapTotalKb: Long = 0L,
    val swapFreeKb: Long = 0L,
    val zramUsedKb: Long = 0L,
    val batteryPercent: Int = 100,
    val batteryTempC: Float = 25.0f,
    val batteryVoltageMv: Int = 4000,
    val batteryCurrentMa: Int = 0,
    val isCharging: Boolean = false,
    val thermalHeadroom: Float = 0.0f,
    val thermalStatus: Int = 0,
    val foregroundPackage: String = "com.sec.android.app.launcher",
    val refreshRateMode: Int = 0
) {
    val memoryUsageRatio: Float
        get() = if (memTotalKb > 0) (memTotalKb - memAvailableKb).toFloat() / memTotalKb.toFloat() else 0.0f

    val zramUsageRatio: Float
        get() = if (swapTotalKb > 0) (swapTotalKb - swapFreeKb).toFloat() / swapTotalKb.toFloat() else 0.0f
}
