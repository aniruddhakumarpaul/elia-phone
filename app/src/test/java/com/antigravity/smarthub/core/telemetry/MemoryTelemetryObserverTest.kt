package com.antigravity.smarthub.core.telemetry

import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class MemoryTelemetryObserverTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testParseMeminfoFile() {
        val file = tempFolder.newFile("meminfo")
        file.writeText(
            """
            MemTotal:        7765432 kB
            MemFree:          543210 kB
            MemAvailable:    3210987 kB
            SwapTotal:       4000000 kB
            SwapFree:        1500000 kB
            """.trimIndent()
        )

        val observer = MemoryTelemetryObserver(file)
        val stats = observer.readMemoryStats()

        assertEquals(7765432L, stats.memTotalKb)
        assertEquals(3210987L, stats.memAvailableKb)
        assertEquals(4000000L, stats.swapTotalKb)
        assertEquals(1500000L, stats.swapFreeKb)
        assertEquals(2500000L, stats.zramUsedKb)
    }
}
