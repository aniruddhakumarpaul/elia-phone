package com.antigravity.smarthub.core.telemetry

import android.os.PowerManager
import com.antigravity.smarthub.core.model.ThermalStatusLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class ThermalHeadroomObserverTest {

    @Test
    fun testNullPowerManagerReturnsUnavailable() {
        val observer = ThermalHeadroomObserver(powerManager = null, executor = null, sdkInt = 34)
        val status = observer.getThermalStatus()
        assertEquals(TelemetryState.UNAVAILABLE, status.state)
        assertNull(status.value)

        val headroom = observer.getThermalHeadroom(10)
        assertEquals(TelemetryState.UNAVAILABLE, headroom.state)
    }

    @Test
    fun testLowSdkReturnsUnsupported() {
        val mockPm = mock(PowerManager::class.java)
        val observer = ThermalHeadroomObserver(powerManager = mockPm, executor = null, sdkInt = 28)
        val headroom = observer.getThermalHeadroom(10)
        assertEquals(TelemetryState.UNSUPPORTED, headroom.state)
    }

    @Test
    fun testParseThermalStatusLevels() {
        val observer = ThermalHeadroomObserver(powerManager = null, executor = null, sdkInt = 34)
        assertEquals(ThermalStatusLevel.NOMINAL, observer.parseThermalStatus(PowerManager.THERMAL_STATUS_NONE))
        assertEquals(ThermalStatusLevel.WARM, observer.parseThermalStatus(PowerManager.THERMAL_STATUS_LIGHT))
        assertEquals(ThermalStatusLevel.MODERATE, observer.parseThermalStatus(PowerManager.THERMAL_STATUS_MODERATE))
        assertEquals(ThermalStatusLevel.SEVERE, observer.parseThermalStatus(PowerManager.THERMAL_STATUS_SEVERE))
        assertEquals(ThermalStatusLevel.CRITICAL, observer.parseThermalStatus(PowerManager.THERMAL_STATUS_CRITICAL))
        assertEquals(ThermalStatusLevel.CRITICAL, observer.parseThermalStatus(PowerManager.THERMAL_STATUS_EMERGENCY))
        assertEquals(ThermalStatusLevel.CRITICAL, observer.parseThermalStatus(PowerManager.THERMAL_STATUS_SHUTDOWN))
    }

    @Test
    fun testNaNHeadroomRejectedAsUnavailable() {
        val mockPm = mock(PowerManager::class.java)
        `when`(mockPm.getThermalHeadroom(10)).thenReturn(Float.NaN)

        val observer = ThermalHeadroomObserver(powerManager = mockPm, executor = null, sdkInt = 34)
        val headroom = observer.getThermalHeadroom(10)
        assertEquals(TelemetryState.UNAVAILABLE, headroom.state)
        assertNull(headroom.value)
    }

    @Test
    fun testNegativeHeadroomRejectedAsUnavailable() {
        val mockPm = mock(PowerManager::class.java)
        `when`(mockPm.getThermalHeadroom(10)).thenReturn(-1.0f)

        val observer = ThermalHeadroomObserver(powerManager = mockPm, executor = null, sdkInt = 34)
        val headroom = observer.getThermalHeadroom(10)
        assertEquals(TelemetryState.UNAVAILABLE, headroom.state)
        assertNull(headroom.value)
    }

    @Test
    fun testValidHeadroomAcceptedAsAvailable() {
        val mockPm = mock(PowerManager::class.java)
        `when`(mockPm.getThermalHeadroom(10)).thenReturn(0.65f)

        val observer = ThermalHeadroomObserver(powerManager = mockPm, executor = null, sdkInt = 34)
        val headroom = observer.getThermalHeadroom(10)
        assertEquals(TelemetryState.AVAILABLE, headroom.state)
        assertEquals(0.65f, headroom.value!!, 0.001f)
    }
}
