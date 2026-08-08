package com.antigravity.smarthub.core.telemetry

class NavigationContextObserver(
    private val appContextObserver: AppContextObserver? = null
) {
    private val navigationPackages = setOf(
        "com.google.android.apps.maps",
        "com.waze",
        "com.sygic.aura",
        "com.here.app.maps"
    )

    fun isNavigationActive(): TelemetryValue<Boolean> {
        val fgPkgValue = appContextObserver?.getForegroundPackage()
        val fgPkg = fgPkgValue?.value

        if (fgPkg != null && navigationPackages.contains(fgPkg)) {
            return TelemetryValue(true, TelemetryState.AVAILABLE)
        }
        return TelemetryValue(false, TelemetryState.AVAILABLE)
    }
}
