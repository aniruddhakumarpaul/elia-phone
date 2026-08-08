package com.antigravity.smarthub.core.telemetry

import android.app.ActivityManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build

class AppContextObserver(
    private val context: Context? = null
) {
    private var accessibilityForegroundPackage: String? = null

    fun onAccessibilityWindowStateChanged(packageName: String?) {
        if (!packageName.isNullOrBlank() && packageName != "com.android.systemui") {
            accessibilityForegroundPackage = packageName
        }
    }

    fun getForegroundPackage(): TelemetryValue<String> {
        // Priority 1: opt-in package-only AccessibilityService event.
        AccessibilityForegroundContextBus.foregroundPackage.value?.let {
            return TelemetryValue(it, TelemetryState.AVAILABLE)
        }

        // Priority 2: legacy in-process event hook retained for tests/callers.
        accessibilityForegroundPackage?.let {
            return TelemetryValue(it, TelemetryState.AVAILABLE)
        }

        if (context == null) return TelemetryValue.unavailable()

        // Priority 3: UsageStatsManager fallback
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        if (usm != null) {
            try {
                val time = System.currentTimeMillis()
                val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 10000, time)
                val recent = stats?.maxByOrNull { it.lastTimeUsed }
                if (recent != null && !recent.packageName.isNullOrBlank()) {
                    return TelemetryValue(recent.packageName, TelemetryState.AVAILABLE)
                }
            } catch (e: Exception) {
                // Requires PACKAGE_USAGE_STATS permission
            }
        }

        // Priority 4: ActivityManager fallback
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        if (am != null) {
            try {
                val processes = am.runningAppProcesses
                val fg = processes?.firstOrNull { it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND }
                if (fg != null && fg.pkgList != null && fg.pkgList.isNotEmpty()) {
                    return TelemetryValue(fg.pkgList[0], TelemetryState.AVAILABLE)
                }
            } catch (e: Exception) {
                // Ignore permission error
            }
        }

        return TelemetryValue.unavailable()
    }
}
