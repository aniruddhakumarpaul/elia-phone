package com.antigravity.smarthub.core.telemetry

import java.io.File

class ZramTelemetryObserver(
    private val zramDir: File = File("/sys/block/zram0")
) {

    fun readZramMetrics(): TelemetryValue<ZramMetrics> {
        if (!zramDir.exists() || !zramDir.canRead()) {
            return TelemetryValue.unsupported()
        }

        try {
            val mmStatFile = File(zramDir, "mm_stat")
            if (mmStatFile.exists() && mmStatFile.canRead()) {
                val tokens = mmStatFile.readText().trim().split("\\s+".toRegex())
                if (tokens.size >= 3) {
                    val origSize = tokens[0].toLongOrNull() ?: 0L
                    val comprSize = tokens[1].toLongOrNull() ?: 0L
                    val memUsed = tokens[2].toLongOrNull() ?: 0L
                    val diskSize = readLongNode("disksize")

                    val ratio = if (comprSize > 0L) origSize.toFloat() / comprSize.toFloat() else 1.0f

                    val metrics = ZramMetrics(
                        origDataSizeByte = origSize,
                        comprDataSizeByte = comprSize,
                        memUsedTotalByte = memUsed,
                        diskSizeBytes = diskSize,
                        compressionRatio = ratio
                    )
                    return TelemetryValue(metrics, TelemetryState.AVAILABLE)
                }
            }

            // Fallback individual sysfs nodes
            val origSize = readLongNode("orig_data_size")
            val comprSize = readLongNode("compr_data_size")
            val memUsed = readLongNode("mem_used_total")
            val diskSize = readLongNode("disksize")

            if (origSize > 0L || comprSize > 0L) {
                val ratio = if (comprSize > 0L) origSize.toFloat() / comprSize.toFloat() else 1.0f
                val metrics = ZramMetrics(
                    origDataSizeByte = origSize,
                    comprDataSizeByte = comprSize,
                    memUsedTotalByte = memUsed,
                    diskSizeBytes = diskSize,
                    compressionRatio = ratio
                )
                return TelemetryValue(metrics, TelemetryState.AVAILABLE)
            }
        } catch (e: Exception) {
            return TelemetryValue.unavailable()
        }

        return TelemetryValue.unavailable()
    }

    private fun readLongNode(nodeName: String): Long {
        val file = File(zramDir, nodeName)
        if (file.exists() && file.canRead()) {
            try {
                return file.readText().trim().toLongOrNull() ?: 0L
            } catch (e: Exception) {
                // Ignore node read exception
            }
        }
        return 0L
    }
}
