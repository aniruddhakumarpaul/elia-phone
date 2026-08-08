package com.antigravity.smarthub.core.telemetry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppContextObserverTest {

    @Test
    fun testAccessibilityForegroundPackageReported() {
        val observer = AppContextObserver()
        observer.onAccessibilityWindowStateChanged("com.pubg.imobile")

        val result = observer.getForegroundPackage()
        assertEquals(TelemetryState.AVAILABLE, result.state)
        assertEquals("com.pubg.imobile", result.value)
    }

    @Test
    fun testSystemUiWindowIgnored() {
        val observer = AppContextObserver()
        observer.onAccessibilityWindowStateChanged("com.pubg.imobile")
        observer.onAccessibilityWindowStateChanged("com.android.systemui")

        val result = observer.getForegroundPackage()
        assertEquals("com.pubg.imobile", result.value)
    }

    @Test
    fun testNoFallbackToFakeLauncher() {
        val observer = AppContextObserver(context = null)
        val result = observer.getForegroundPackage()

        assertEquals(TelemetryState.UNAVAILABLE, result.state)
        assertNull(result.value)
        assertNotEquals("com.sec.android.app.launcher", result.value)
    }
}
