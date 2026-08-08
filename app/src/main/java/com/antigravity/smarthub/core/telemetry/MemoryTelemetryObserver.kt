package com.antigravity.smarthub.core.telemetry

import java.io.File

data class MemoryStats(
    val memTotalKb: Long = 0L,
    val memAvailableKb: Long = 0L,
    val swapTotalKb: Long = 0L,
    val swapFreeKb: Long = 0L,
    val zramUsedKb: Long = 0L
)

class MemoryTelemetryObserver(
    private val memInfoFile: File = File("/proc/meminfo")
) {

    fun readMemoryStats(): MemoryStats {
        if (!memInfoFile.exists() || !memInfoFile.canRead()) {
            return MemoryStats()
        }

        var total = 0L
        var available = 0L
        var swapTotal = 0L
        var swapFree = 0L

        try {
            memInfoFile.useLines { lines ->
                lines.forEach { line ->
                    val tokens = line.split("\\s+".toRegex())
                    if (tokens.size >= 2) {
                        val key = tokens[0].removeSuffix(":")
                        val value = tokens[1].toLongOrNull() ?: 0L
                        when (key) {
                            "MemTotal" -> total = value
                            "MemAvailable" -> available = value
                            "SwapTotal" -> swapTotal = value
                            "SwapFree" -> swapFree = value
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore file read exceptions
        }

        val zramUsed = (swapTotal - swapFree).coerceAtLeast(0L)
        return MemoryStats(
            memTotalKb = total,
            memAvailableKb = available,
            swapTotalKb = swapTotal,
            swapFreeKb = swapFree,
            zramUsedKb = zramUsed
        )
    }
}
