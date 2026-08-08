package com.antigravity.smarthub.core.telemetry

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TelemetryAggregatorTest {

    @Test
    fun testSampleCurrentStateReturnsValidSnapshot() {
        val aggregator = TelemetryAggregator()
        val snapshot = aggregator.sampleCurrentState()

        assertNotNull(snapshot)
        assertTrue(snapshot.timestampMs > 0L)
    }

    @Test
    fun testStartSamplingUpdatesStream() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val aggregator = TelemetryAggregator(scope = testScope)

        aggregator.startSampling(intervalMs = 100L)
        val initial = aggregator.telemetryStream.value
        assertNotNull(initial)
        aggregator.stopSampling()
    }

    private fun assertTrue(condition: Boolean) {
        org.junit.Assert.assertTrue(condition)
    }
}
