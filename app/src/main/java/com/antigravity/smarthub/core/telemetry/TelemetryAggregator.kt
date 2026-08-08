package com.antigravity.smarthub.core.telemetry

import com.antigravity.smarthub.core.model.ThermalStatusLevel
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
    private val zramObserver: ZramTelemetryObserver = ZramTelemetryObserver(),
    private val batteryObserver: BatteryPowerObserver = BatteryPowerObserver(),
    private val thermalObserver: ThermalHeadroomObserver = ThermalHeadroomObserver(),
    private val displayObserver: DisplayTelemetryObserver = DisplayTelemetryObserver(),
    private val appContextObserver: AppContextObserver = AppContextObserver(),
    private val mediaContextObserver: MediaContextObserver = MediaContextObserver(),
    private val navigationObserver: NavigationContextObserver = NavigationContextObserver(appContextObserver),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {

    private val _telemetryStream = MutableStateFlow(DeviceTelemetrySnapshot())
    val telemetryStream: StateFlow<DeviceTelemetrySnapshot> = _telemetryStream.asStateFlow()

    private var samplingJob: Job? = null

    fun startSampling() {
        if (samplingJob?.isActive == true) return

        batteryObserver.startObserving()
        thermalObserver.startObserving()

        samplingJob = scope.launch {
            while (isActive) {
                val snapshot = sampleCurrentState()
                _telemetryStream.value = snapshot

                val intervalMs = calculateAdaptiveSamplingInterval(snapshot)
                delay(intervalMs)
            }
        }
    }

    fun stopSampling() {
        samplingJob?.cancel()
        samplingJob = null
        batteryObserver.stopObserving()
        thermalObserver.stopObserving()
    }

    fun refreshTelemetry(): DeviceTelemetrySnapshot {
        val snapshot = sampleCurrentState()
        _telemetryStream.value = snapshot
        return snapshot
    }

    fun sampleCurrentState(): DeviceTelemetrySnapshot {
        val cpuMetrics = cpuObserver.readCpuMetrics()
        val memTotal = memoryObserver.readMemTotalKb()
        val memAvail = memoryObserver.readMemAvailableKb()
        val memPsi = memoryObserver.readMemoryPsi()
        val memPressure = memoryObserver.calculateMemoryPressureLevel(memTotal.value, memAvail.value, memPsi.value)
        val zramMetrics = zramObserver.readZramMetrics()
        val batteryMetrics = batteryObserver.getBatteryMetrics()
        val thermalStatus = thermalObserver.getThermalStatus()
        val thermalHeadroom = thermalObserver.getThermalHeadroom(10)
        val thermalForecast = thermalObserver.getThermalHeadroom(30)
        val apTemp = thermalObserver.readMeasuredApTempC()
        val displayMetrics = displayObserver.getDisplayMetrics()
        val fgPkg = appContextObserver.getForegroundPackage()
        val isMedia = mediaContextObserver.isMediaPlaying()
        val isNav = navigationObserver.isNavigationActive()

        return DeviceTelemetrySnapshot(
            capturedAtMs = System.currentTimeMillis(),
            cpuMetrics = cpuMetrics,
            memTotalKb = memTotal,
            memAvailableKb = memAvail,
            memoryPressure = memPressure,
            memoryPsi = memPsi,
            zram = zramMetrics,
            battery = batteryMetrics,
            thermalStatus = thermalStatus,
            thermalHeadroom = thermalHeadroom,
            thermalForecastHeadroom = thermalForecast,
            measuredApTempC = apTemp,
            display = displayMetrics,
            foregroundPackage = fgPkg,
            isMediaPlaying = isMedia,
            isNavigationActive = isNav
        )
    }

    fun calculateAdaptiveSamplingInterval(snapshot: DeviceTelemetrySnapshot): Long {
        val thermal = snapshot.thermalStatus.value ?: ThermalStatusLevel.NOMINAL
        val isScreenOn = snapshot.display.value?.isScreenOn?.value
        val fgPkg = snapshot.foregroundPackage.value ?: ""

        val isGaming = fgPkg.contains("pubg") || fgPkg.contains("freefire") || fgPkg.contains("roblox")

        return when {
            thermal == ThermalStatusLevel.CRITICAL || thermal == ThermalStatusLevel.SEVERE -> 500L
            isGaming -> 1000L
            isScreenOn == false -> 10000L // 10s screen-off
            else -> 2000L // 2s daily adaptive default
        }
    }
}
