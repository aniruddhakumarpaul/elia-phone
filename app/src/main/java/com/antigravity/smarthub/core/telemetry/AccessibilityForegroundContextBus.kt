package com.antigravity.smarthub.core.telemetry

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Process-local package-only bridge; no window text or content is collected. */
object AccessibilityForegroundContextBus {
    private val _foregroundPackage = MutableStateFlow<String?>(null)
    val foregroundPackage = _foregroundPackage.asStateFlow()
    fun publish(packageName: String?) { _foregroundPackage.value = packageName?.takeIf { it.isNotBlank() } }
}
