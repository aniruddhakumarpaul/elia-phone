package com.antigravity.smarthub.core.telemetry

import java.io.File

class CpuTelemetryObserver(
    private val sysCpuDir: File = File("/sys/devices/system/cpu")
) {

    fun readCpuMetrics(): TelemetryValue<List<CpuCoreMetric>> {
        val possibleCores = getPossibleCpuCores()
        if (possibleCores.isEmpty()) {
            return TelemetryValue.unavailable()
        }

        val onlineSet = getOnlineCpuCores()
        val metricsList = mutableListOf<CpuCoreMetric>()

        for (coreId in possibleCores) {
            val isOnline = onlineSet.contains(coreId)
            val freqHz = if (isOnline) readCoreFrequencyHz(coreId) else null
            val governor = if (isOnline) readCoreGovernor(coreId) else null

            metricsList.add(
                CpuCoreMetric(
                    coreId = coreId,
                    isOnline = isOnline,
                    frequencyHz = freqHz,
                    governor = governor
                )
            )
        }

        return TelemetryValue(metricsList, TelemetryState.AVAILABLE)
    }

    fun getPossibleCpuCores(): List<Int> {
        val possibleFile = File(sysCpuDir, "possible")
        if (possibleFile.exists() && possibleFile.canRead()) {
            val text = possibleFile.readText().trim()
            val parsed = parseRangeString(text)
            if (parsed.isNotEmpty()) return parsed.sorted()
        }

        val presentFile = File(sysCpuDir, "present")
        if (presentFile.exists() && presentFile.canRead()) {
            val text = presentFile.readText().trim()
            val parsed = parseRangeString(text)
            if (parsed.isNotEmpty()) return parsed.sorted()
        }

        // Fallback check cpu0..15 directories
        val detected = mutableListOf<Int>()
        for (i in 0..15) {
            if (File(sysCpuDir, "cpu$i").exists()) {
                detected.add(i)
            }
        }
        return detected
    }

    fun getOnlineCpuCores(): Set<Int> {
        val onlineFile = File(sysCpuDir, "online")
        if (onlineFile.exists() && onlineFile.canRead()) {
            val text = onlineFile.readText().trim()
            val parsed = parseRangeString(text)
            if (parsed.isNotEmpty()) return parsed
        }
        return getPossibleCpuCores().toSet()
    }

    private fun readCoreFrequencyHz(coreId: Int): Long? {
        val freqFile = File(sysCpuDir, "cpu$coreId/cpufreq/scaling_cur_freq")
        if (freqFile.exists() && freqFile.canRead()) {
            try {
                val freqKhz = freqFile.readText().trim().toLongOrNull()
                if (freqKhz != null && freqKhz > 0L) {
                    return freqKhz * 1000L
                }
            } catch (e: Exception) {
                return null
            }
        }
        return null
    }

    private fun readCoreGovernor(coreId: Int): String? {
        val govFile = File(sysCpuDir, "cpu$coreId/cpufreq/scaling_governor")
        if (govFile.exists() && govFile.canRead()) {
            try {
                return govFile.readText().trim().ifBlank { null }
            } catch (e: Exception) {
                return null
            }
        }
        return null
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
