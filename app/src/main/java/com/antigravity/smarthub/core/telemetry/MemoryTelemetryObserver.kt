package com.antigravity.smarthub.core.telemetry

import java.io.File

class MemoryTelemetryObserver(
    private val memInfoFile: File = File("/proc/meminfo"),
    private val psiMemoryFile: File = File("/proc/pressure/memory")
) {

    fun readMemTotalKb(): TelemetryValue<Long> = readMeminfoKey("MemTotal")
    fun readMemAvailableKb(): TelemetryValue<Long> = readMeminfoKey("MemAvailable")
    fun readSwapTotalKb(): TelemetryValue<Long> = readMeminfoKey("SwapTotal")
    fun readSwapFreeKb(): TelemetryValue<Long> = readMeminfoKey("SwapFree")

    fun readMemoryPsi(): TelemetryValue<PsiMetric> {
        if (!psiMemoryFile.exists() || !psiMemoryFile.canRead()) {
            return TelemetryValue.unsupported()
        }

        try {
            var someAvg10 = 0.0f
            var someAvg60 = 0.0f
            var someAvg300 = 0.0f
            var fullAvg10 = 0.0f
            var fullAvg60 = 0.0f
            var fullAvg300 = 0.0f

            psiMemoryFile.useLines { lines ->
                lines.forEach { line ->
                    if (line.startsWith("some")) {
                        someAvg10 = parsePsiValue(line, "avg10")
                        someAvg60 = parsePsiValue(line, "avg60")
                        someAvg300 = parsePsiValue(line, "avg300")
                    } else if (line.startsWith("full")) {
                        fullAvg10 = parsePsiValue(line, "avg10")
                        fullAvg60 = parsePsiValue(line, "avg60")
                        fullAvg300 = parsePsiValue(line, "avg300")
                    }
                }
            }

            val psi = PsiMetric(someAvg10, someAvg60, someAvg300, fullAvg10, fullAvg60, fullAvg300)
            return TelemetryValue(psi, TelemetryState.AVAILABLE)
        } catch (e: Exception) {
            return TelemetryValue.unavailable()
        }
    }

    fun calculateMemoryPressureLevel(
        memTotalKb: Long?,
        memAvailKb: Long?,
        psi: PsiMetric?
    ): TelemetryValue<MemoryPressureLevel> {
        if (memTotalKb == null || memTotalKb <= 0L || memAvailKb == null) {
            return TelemetryValue.unavailable()
        }

        val availRatio = memAvailKb.toFloat() / memTotalKb.toFloat()
        val some10 = psi?.someAvg10 ?: 0.0f
        val full10 = psi?.fullAvg10 ?: 0.0f

        val level = when {
            availRatio <= 0.05f || full10 >= 40.0f -> MemoryPressureLevel.CRITICAL
            availRatio <= 0.10f || full10 >= 20.0f || some10 >= 50.0f -> MemoryPressureLevel.THRASHING
            availRatio <= 0.20f || some10 >= 25.0f -> MemoryPressureLevel.PRESSURED
            else -> MemoryPressureLevel.NORMAL
        }

        return TelemetryValue(level, TelemetryState.AVAILABLE)
    }

    private fun readMeminfoKey(targetKey: String): TelemetryValue<Long> {
        if (!memInfoFile.exists() || !memInfoFile.canRead()) {
            return TelemetryValue.unavailable()
        }

        try {
            memInfoFile.useLines { lines ->
                lines.forEach { line ->
                    val tokens = line.split("\\s+".toRegex())
                    if (tokens.size >= 2) {
                        val key = tokens[0].removeSuffix(":")
                        if (key == targetKey) {
                            val value = tokens[1].toLongOrNull()
                            if (value != null) {
                                return TelemetryValue(value, TelemetryState.AVAILABLE)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            return TelemetryValue.unavailable()
        }
        return TelemetryValue.unavailable()
    }

    fun parsePsiValue(line: String, key: String): Float {
        val pattern = "$key=([0-9.]+)".toRegex()
        val match = pattern.find(line)
        return match?.groupValues?.getOrNull(1)?.toFloatOrNull() ?: 0.0f
    }
}
