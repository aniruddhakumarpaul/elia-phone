package com.antigravity.smarthub.core.telemetry

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TelemetryAggregator(
    private val cpuObserver: CpuTelemetryObserver = CpuTelemetryObserver(),
    private val memoryObserver: MemoryTelemetryObserver = MemoryTelemetryObserver(),
    private val batteryObserver: BatteryPowerObserver = BatteryPowerObserver(),
    private val thermalObserver: ThermalHeadroomObserver = ThermalHeadroomObserver(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {

    private val _telemetryStream = MutableStateFlow(DeviceTelemetrySnapshot())
    val telemetryStream: StateFlow<DeviceTelemetrySnapshot> = _telemetryStream.asStateFlow()

    private var samplingJob: Job? = null

    fun startSampling(intervalMs: Long = 1000L) {
        if (samplingJob?.isActive == true) return

        samplingJob = scope.launch {
            while (isActive) {
                val snapshot = sampleCurrentState()
                _telemetryStream.value = snapshot
                delay(intervalMs)
            }
        }
    }

    fun stopSampling() {
        samplingJob?.cancel()
        samplingJob = null
    }

    fun sampleCurrentState(): DeviceTelemetrySnapshot {
        val cpuFreqs = cpuObserver.readCoreFrequencies()
        val cpuOnline = cpuObserver.readOnlineStatus()
        val memStats = memoryObserver.readMemoryStats()
        val batteryStats = batteryObserver.readBatteryStats()
        val thermalRisk = thermalObserver.calculateThermalRisk(batteryStats.tempC, batteryStats.tempC + 3.0f)

        return DeviceTelemetrySnapshot(
            timestampMs = System.currentTimeMillis(),
            cpuFrequenciesHz = cpuFreqs,
            cpuOnlineStatus = cpuOnline,
            memTotalKb = memStats.memTotalKb,
            memAvailableKb = memStats.memAvailableKb,
            swapTotalKb = memStats.swapTotalKb,
            swapFreeKb = memStats.swapFreeKb,
            zramUsedKb = memStats.zramUsedKb,
            batteryPercent = batteryStats.percent,
            batteryTempC = batteryStats.tempC,
            batteryVoltageMv = batteryStats.voltageMv,
            batteryCurrentMa = batteryStats.currentMa,
            isCharging = batteryStats.isCharging,
            thermalHeadroom = thermalRisk.headroomForecast,
            thermalStatus = thermalRisk.statusLevel.ordinal,
            foregroundPackage = "com.sec.android.app.launcher",
            refreshRateMode = 0
        )
    }
}
