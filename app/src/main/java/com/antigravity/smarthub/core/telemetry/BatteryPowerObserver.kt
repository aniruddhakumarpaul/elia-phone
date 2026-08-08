package com.antigravity.smarthub.core.telemetry

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

data class BatteryStats(
    val percent: Int = 100,
    val tempC: Float = 25.0f,
    val voltageMv: Int = 4000,
    val currentMa: Int = 0,
    val isCharging: Boolean = false
)

class BatteryPowerObserver(
    private val context: Context? = null
) {

    fun readBatteryStats(): BatteryStats {
        if (context == null) return BatteryStats()

        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return BatteryStats()

        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val percent = if (level >= 0 && scale > 0) (level * 100) / scale else 100

        val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 250)
        val tempC = tempTenths / 10.0f

        val voltageMv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 4000)

        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val currentMicroAmps = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) ?: 0
        val currentMa = currentMicroAmps / 1000

        return BatteryStats(
            percent = percent,
            tempC = tempC,
            voltageMv = voltageMv,
            currentMa = currentMa,
            isCharging = isCharging
        )
    }
}
