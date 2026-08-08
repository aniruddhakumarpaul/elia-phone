package com.antigravity.smarthub.core.telemetry

enum class TelemetryState {
    AVAILABLE,
    UNAVAILABLE,
    PERMISSION_REQUIRED,
    UNSUPPORTED,
    STALE
}

data class TelemetryValue<T>(
    val value: T?,
    val state: TelemetryState = TelemetryState.UNAVAILABLE,
    val capturedAtMs: Long = System.currentTimeMillis()
) {
    fun isFresh(maxAgeMs: Long, currentTimeMs: Long = System.currentTimeMillis()): Boolean {
        return state == TelemetryState.AVAILABLE && (currentTimeMs - capturedAtMs) <= maxAgeMs
    }

    companion object {
        fun <T> unavailable(): TelemetryValue<T> = TelemetryValue(null, TelemetryState.UNAVAILABLE)
        fun <T> unsupported(): TelemetryValue<T> = TelemetryValue(null, TelemetryState.UNSUPPORTED)
        fun <T> permissionRequired(): TelemetryValue<T> = TelemetryValue(null, TelemetryState.PERMISSION_REQUIRED)
        fun <T> available(v: T): TelemetryValue<T> = TelemetryValue(v, TelemetryState.AVAILABLE)
    }
}
