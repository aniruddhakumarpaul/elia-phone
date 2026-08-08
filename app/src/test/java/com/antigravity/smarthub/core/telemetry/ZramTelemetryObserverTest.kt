package com.antigravity.smarthub.core.telemetry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ZramTelemetryObserverTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testParseZramMmStat() {
        val root = tempFolder.newFolder()
        File(root, "mm_stat").writeText(" 2097152000  524288000  600000000 0 0 0 0 0")
        File(root, "disksize").writeText("4294967296")

        val observer = ZramTelemetryObserver(root)
        val result = observer.readZramMetrics()

        assertEquals(TelemetryState.AVAILABLE, result.state)
        val metrics = result.value
        assertNotNull(metrics)
        assertEquals(2097152000L, metrics!!.origDataSizeByte)
        assertEquals(524288000L, metrics.comprDataSizeByte)
        assertEquals(4.0f, metrics.compressionRatio, 0.01f)
    }

    @Test
    fun testUnavailableZramDirectory() {
        val root = File(tempFolder.root, "non_existent_zram")
        val observer = ZramTelemetryObserver(root)
        val result = observer.readZramMetrics()

        assertEquals(TelemetryState.UNSUPPORTED, result.state)
    }
}
