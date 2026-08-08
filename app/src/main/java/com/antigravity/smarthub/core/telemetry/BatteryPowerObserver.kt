package com.antigravity.smarthub.core.telemetry

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

class BatteryPowerObserver(
    private val context: Context? = null
) {
    private var lastSnapshot: TelemetryValue<BatteryMetrics> = TelemetryValue.unavailable()
    private var isReceiverRegistered = false

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent != null && intent.action == Intent.ACTION_BATTERY_CHANGED) {
                updateSnapshot(intent)
            }
        }
    }

    fun startObserving() {
        if (context != null && !isReceiverRegistered) {
            try {
                val intent = context.registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                if (intent != null) {
                    updateSnapshot(intent)
                }
                isReceiverRegistered = true
            } catch (e: Exception) {
                lastSnapshot = TelemetryValue.unavailable()
            }
        }
    }

    fun stopObserving() {
        if (context != null && isReceiverRegistered) {
            try {
                context.unregisterReceiver(batteryReceiver)
            } catch (e: Exception) {
                // Ignore unregister exception
            } finally {
                isReceiverRegistered = false
            }
        }
    }

    fun getBatteryMetrics(): TelemetryValue<BatteryMetrics> {
        if (lastSnapshot.state == TelemetryState.UNAVAILABLE && context != null) {
            startObserving()
        }
        return lastSnapshot
    }

    private fun updateSnapshot(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val percent = if (level >= 0 && scale > 0) (level * 100) / scale else null

        val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
        val tempC = if (tempTenths != Int.MIN_VALUE) tempTenths / 10.0f else null

        val voltageMv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, Int.MIN_VALUE).let {
            if (it != Int.MIN_VALUE) it else null
        }

        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        val plugType = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "WIRELESS"
            0 -> "UNPLUGGED"
            else -> null
        }

        val bm = context?.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val currentNowMicro = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) ?: Int.MIN_VALUE
        val currentNowMa = if (currentNowMicro != Int.MIN_VALUE && currentNowMicro != 0) currentNowMicro / 1000 else null

        val currentAvgMicro = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE) ?: Int.MIN_VALUE
        val currentAvgMa = if (currentAvgMicro != Int.MIN_VALUE && currentAvgMicro != 0) currentAvgMicro / 1000 else null

        val metrics = BatteryMetrics(
            percent = percent,
            tempC = tempC,
            voltageMv = voltageMv,
            currentNowMa = currentNowMa,
            currentAvgMa = currentAvgMa,
            isCharging = isCharging,
            plugType = plugType
        )

        lastSnapshot = TelemetryValue(metrics, TelemetryState.AVAILABLE)
    }
}
