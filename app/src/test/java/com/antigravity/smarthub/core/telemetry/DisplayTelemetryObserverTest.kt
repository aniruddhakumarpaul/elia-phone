package com.antigravity.smarthub.core.telemetry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DisplayTelemetryObserverTest {

    @Test
    fun testNullContextReturnsUnavailable() {
        val observer = DisplayTelemetryObserver(null)
        val result = observer.getDisplayMetrics()
        assertEquals(TelemetryState.UNAVAILABLE, result.state)
        assertNull(result.value)
    }
}
