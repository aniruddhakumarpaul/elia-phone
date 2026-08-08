package com.antigravity.smarthub.core.telemetry

import java.io.File

class CpuTelemetryObserver(
    private val sysCpuDir: File = File("/sys/devices/system/cpu")
) {

    fun readCoreFrequencies(): Map<Int, Long> {
        val result = mutableMapOf<Int, Long>()
        for (core in 0..7) {
            val freqFile = File(sysCpuDir, "cpu$core/cpufreq/scaling_cur_freq")
            if (freqFile.exists() && freqFile.canRead()) {
                try {
                    val freqKhz = freqFile.readText().trim().toLongOrNull() ?: 0L
                    result[core] = freqKhz * 1000L // Convert kHz to Hz
                } catch (e: Exception) {
                    result[core] = 0L
                }
            } else {
                result[core] = 0L
            }
        }
        return result
    }

    fun readOnlineStatus(): Map<Int, Boolean> {
        val result = mutableMapOf<Int, Boolean>()
        val onlineFile = File(sysCpuDir, "online")
        val onlineCores = parseRangeString(if (onlineFile.exists() && onlineFile.canRead()) onlineFile.readText().trim() else "0-7")

        for (core in 0..7) {
            result[core] = onlineCores.contains(core)
        }
        return result
    }

    fun parseRangeString(rangeStr: String): Set<Int> {
        val cores = mutableSetOf<Int>()
        if (rangeStr.isBlank()) return cores
        val parts = rangeStr.split(",")
        for (part in parts) {
            if (part.contains("-")) {
                val bounds = part.split("-")
                val start = bounds.getOrNull(0)?.trim()?.toIntOrNull()
                val end = bounds.getOrNull(1)?.trim()?.toIntOrNull()
                if (start != null && end != null) {
                    for (i in start..end) cores.add(i)
                }
            } else {
                part.trim().toIntOrNull()?.let { cores.add(it) }
            }
        }
        return cores
    }
}
