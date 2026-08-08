package com.antigravity.smarthub.core.telemetry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class MemoryTelemetryObserverTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testParseMeminfoAndPsiFiles() {
        val meminfoFile = tempFolder.newFile("meminfo")
        meminfoFile.writeText(
            """
            MemTotal:        7765432 kB
            MemFree:          543210 kB
            MemAvailable:    3210987 kB
            SwapTotal:       4000000 kB
            SwapFree:        1500000 kB
            """.trimIndent()
        )

        val psiFile = tempFolder.newFile("psi_memory")
        psiFile.writeText(
            """
            some avg10=12.50 avg60=5.20 avg300=1.10 total=123456
            full avg10=2.10 avg60=0.50 avg300=0.10 total=45678
            """.trimIndent()
        )

        val observer = MemoryTelemetryObserver(meminfoFile, psiFile)

        val total = observer.readMemTotalKb()
        val avail = observer.readMemAvailableKb()
        val swapTotal = observer.readSwapTotalKb()
        val swapFree = observer.readSwapFreeKb()
        val psi = observer.readMemoryPsi()

        assertEquals(7765432L, total.value)
        assertEquals(3210987L, avail.value)
        assertEquals(4000000L, swapTotal.value)
        assertEquals(1500000L, swapFree.value)
        assertEquals(TelemetryState.AVAILABLE, psi.state)

        val psiMetrics = psi.value
        assertNotNull(psiMetrics)
        assertEquals(12.50f, psiMetrics!!.someAvg10, 0.01f)
        assertEquals(2.10f, psiMetrics.fullAvg10, 0.01f)

        val pressure = observer.calculateMemoryPressureLevel(total.value, avail.value, psiMetrics)
        assertEquals(MemoryPressureLevel.NORMAL, pressure.value)
    }

    @Test
    fun testMemoryPressureClassificationCritical() {
        val observer = MemoryTelemetryObserver()
        val psiCritical = PsiMetric(someAvg10 = 60.0f, fullAvg10 = 45.0f)
        val pressure = observer.calculateMemoryPressureLevel(8_000_000L, 300_000L, psiCritical)

        assertEquals(MemoryPressureLevel.CRITICAL, pressure.value)
    }
}
