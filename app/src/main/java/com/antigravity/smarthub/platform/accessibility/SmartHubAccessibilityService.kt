package com.antigravity.smarthub.platform.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.antigravity.smarthub.core.telemetry.AccessibilityForegroundContextBus

/** Optional package-only foreground context integration; window text is deliberately ignored. */
class SmartHubAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            event?.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) {
            AccessibilityForegroundContextBus.publish(event.packageName?.toString())
        }
    }

    override fun onInterrupt() = Unit
}
