package com.antigravity.smarthub.core.telemetry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class CpuTelemetryObserverTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testParseRangeString() {
        val observer = CpuTelemetryObserver()
        assertEquals(setOf(0, 1, 2, 3, 4, 5, 6, 7), observer.parseRangeString("0-7"))
        assertEquals(setOf(0, 1, 2, 3, 6, 7), observer.parseRangeString("0-3,6-7"))
        assertEquals(setOf(0), observer.parseRangeString("0"))
    }

    @Test
    fun testReadCoreFrequenciesFromSysfsMock() {
        val root = tempFolder.newFolder()
        for (i in 0..7) {
            val cpuDir = File(root, "cpu$i/cpufreq")
            cpuDir.mkdirs()
            File(cpuDir, "scaling_cur_freq").writeText("${2000000 + i * 100000}")
        }

        val observer = CpuTelemetryObserver(root)
        val freqs = observer.readCoreFrequencies()

        assertEquals(8, freqs.size)
        assertEquals(2_000_000_000L, freqs[0])
        assertEquals(2_700_000_000L, freqs[7])
    }
}
