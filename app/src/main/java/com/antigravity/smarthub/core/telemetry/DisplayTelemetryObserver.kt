package com.antigravity.smarthub.core.telemetry

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.PowerManager
import android.provider.Settings
import android.view.Display

class DisplayTelemetryObserver(
    private val context: Context? = null
) {

    fun getDisplayMetrics(): TelemetryValue<DisplayMetrics> {
        if (context == null) return TelemetryValue.unavailable()

        try {
            val secureSettingMode = try {
                Settings.Secure.getInt(context.contentResolver, "refresh_rate_mode")
            } catch (_: Exception) { null }

            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            val isScreenOn = pm?.isInteractive

            val dm = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
            val defaultDisplay = dm?.getDisplay(Display.DEFAULT_DISPLAY)

            val physicalRefreshRateHz = defaultDisplay?.refreshRate
            val displayStateStr = when (defaultDisplay?.state) {
                Display.STATE_OFF -> "STATE_OFF"
                Display.STATE_ON -> "STATE_ON"
                Display.STATE_DOZE -> "STATE_DOZE"
                Display.STATE_DOZE_SUSPEND -> "STATE_DOZE_SUSPEND"
                Display.STATE_ON_SUSPEND -> "STATE_ON_SUSPEND"
                else -> "STATE_UNKNOWN"
            }

            val metrics = DisplayMetrics(
                secureSettingMode = secureSettingMode?.let { TelemetryValue.available(it) }
                    ?: TelemetryValue.unavailable(),
                physicalRefreshRateHz = physicalRefreshRateHz?.takeIf { it.isFinite() && it > 0f }
                    ?.let { TelemetryValue.available(it) }
                    ?: TelemetryValue.unavailable(),
                isScreenOn = isScreenOn?.let { TelemetryValue.available(it) }
                    ?: TelemetryValue.unavailable(),
                displayStateStr = if (defaultDisplay != null) TelemetryValue.available(displayStateStr)
                    else TelemetryValue.unavailable(),
                supportedModesHz = defaultDisplay?.supportedModes
                    ?.map { it.refreshRate }
                    ?.filter { it.isFinite() && it > 0f }
                    ?.distinct()
                    ?.sorted()
                    ?.let { TelemetryValue.available(it) }
                    ?: TelemetryValue.unavailable()
            )

            // The container is available when the observer ran; individual fields retain
            // their own quality so a missing PowerManager/display cannot become a fake value.
            return TelemetryValue.available(metrics)
        } catch (e: Exception) {
            return TelemetryValue.unavailable()
        }
    }
}
