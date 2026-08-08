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
            } catch (e: Exception) {
                null
            }

            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            val isScreenOn = pm?.isInteractive ?: true

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
                secureSettingMode = secureSettingMode,
                physicalRefreshRateHz = physicalRefreshRateHz,
                isScreenOn = isScreenOn,
                displayStateStr = displayStateStr
            )

            return TelemetryValue(metrics, TelemetryState.AVAILABLE)
        } catch (e: Exception) {
            return TelemetryValue.unavailable()
        }
    }
}
