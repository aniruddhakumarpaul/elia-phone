package com.antigravity.smarthub.core.telemetry

import com.antigravity.smarthub.core.model.ThermalStatusLevel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TelemetryAggregatorTest {

    @Test
    fun testSnapshotFreshness() {
        val snapshot = DeviceTelemetrySnapshot(capturedAtMs = System.currentTimeMillis() - 5000L)
        assertTrue(snapshot.isStale(maxAgeMs = 3000L, currentTimeMs = System.currentTimeMillis()))
        assertFalse(snapshot.isStale(maxAgeMs = 10000L, currentTimeMs = System.currentTimeMillis()))
    }

    @Test
    fun testAdaptiveSamplingIntervals() {
        val aggregator = TelemetryAggregator()

        // 1. Daily adaptive default
        val dailySnapshot = DeviceTelemetrySnapshot(
            display = TelemetryValue(DisplayMetrics(TelemetryValue.available(0), TelemetryValue.available(120.0f), TelemetryValue.available(true), TelemetryValue.available("STATE_ON")), TelemetryState.AVAILABLE),
            foregroundPackage = TelemetryValue("com.android.settings", TelemetryState.AVAILABLE)
        )
        assertEquals(2000L, aggregator.calculateAdaptiveSamplingInterval(dailySnapshot))

        // 2. Gaming high-load
        val gamingSnapshot = DeviceTelemetrySnapshot(
            display = TelemetryValue(DisplayMetrics(TelemetryValue.available(0), TelemetryValue.available(120.0f), TelemetryValue.available(true), TelemetryValue.available("STATE_ON")), TelemetryState.AVAILABLE),
            foregroundPackage = TelemetryValue("com.pubg.imobile", TelemetryState.AVAILABLE)
        )
        assertEquals(1000L, aggregator.calculateAdaptiveSamplingInterval(gamingSnapshot))

        // 3. Screen-off / overnight
        val screenOffSnapshot = DeviceTelemetrySnapshot(
            display = TelemetryValue(DisplayMetrics(TelemetryValue.available(0), TelemetryValue.available(60.0f), TelemetryValue.available(false), TelemetryValue.available("STATE_OFF")), TelemetryState.AVAILABLE)
        )
        assertEquals(10000L, aggregator.calculateAdaptiveSamplingInterval(screenOffSnapshot))

        // 4. Thermal Emergency
        val thermalSnapshot = DeviceTelemetrySnapshot(
            thermalStatus = TelemetryValue(ThermalStatusLevel.CRITICAL, TelemetryState.AVAILABLE)
        )
        assertEquals(500L, aggregator.calculateAdaptiveSamplingInterval(thermalSnapshot))
    }

    @Test
    fun testStartStopLifecycle() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val aggregator = TelemetryAggregator(scope = testScope)

        aggregator.startSampling()
        val snapshot = aggregator.sampleCurrentState()
        assertNotNull(snapshot)
        aggregator.stopSampling()
    }
}
