package com.antigravity.smarthub.core.state

import com.antigravity.smarthub.core.model.DeviceState
import com.antigravity.smarthub.core.model.SmartHubProfile
import com.antigravity.smarthub.core.model.ThermalStatusLevel
import com.antigravity.smarthub.core.telemetry.TelemetryState
import com.antigravity.smarthub.core.telemetry.TelemetryValue
import java.util.Calendar

data class ExtendedDeviceState(
    val baseState: DeviceState,
    val mediaPlayback: TelemetryValue<Boolean> = TelemetryValue.unavailable(),
    val navigationContext: TelemetryValue<Boolean> = TelemetryValue.unavailable(),
    val currentHourOfDay: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
    val screenOffDurationMs: Long = 0L
)

class StateMachineEngine {

    var currentProfile: SmartHubProfile = SmartHubProfile.P5_DAILY_ADAPTIVE
        private set

    var candidateProfile: SmartHubProfile? = null
        private set

    var candidateSinceMs: Long = 0L
        private set

    private val gamingPackages = setOf(
        "com.pubg.imobile",
        "com.dts.freefiremax",
        "com.roblox.client",
        "com.firsttouchgames.dls7",
        "com.vng.circle.tribe.idle",
        "com.plarium.mechlegion",
        "com.kunpo88.baba2.tay"
    )

    fun updateState(state: ExtendedDeviceState, currentTimeMs: Long = System.currentTimeMillis()): SmartHubProfile {
        val targetCandidate = determineTargetProfile(state)

        if (targetCandidate != currentProfile) {
            val requiredDwellMs = when {
                targetCandidate == SmartHubProfile.P0_THERMAL_EMERGENCY -> 0L
                targetCandidate == SmartHubProfile.P3_GAMING_HIGH_LOAD -> 0L
                currentProfile == SmartHubProfile.P3_GAMING_HIGH_LOAD -> 3000L
                else -> 1000L
            }

            if (requiredDwellMs == 0L) {
                currentProfile = targetCandidate
                candidateProfile = null
                candidateSinceMs = 0L
            } else if (candidateProfile != targetCandidate) {
                candidateProfile = targetCandidate
                candidateSinceMs = currentTimeMs
            } else {
                val candidateAgeMs = currentTimeMs - candidateSinceMs
                if (candidateAgeMs >= requiredDwellMs) {
                    currentProfile = targetCandidate
                    candidateProfile = null
                    candidateSinceMs = 0L
                }
            }
        } else {
            candidateProfile = null
            candidateSinceMs = 0L
        }

        return currentProfile
    }

    private fun determineTargetProfile(state: ExtendedDeviceState): SmartHubProfile {
        val base = state.baseState
        val nowMs = System.currentTimeMillis()

        // P0: Thermal Emergency Check (Quality-aware: require thermal value to be AVAILABLE)
        val isThermalStatusCritical = base.thermalStatus.isAvailableAndFresh(5_000L, nowMs) &&
                (base.thermalStatus.value == ThermalStatusLevel.CRITICAL || base.thermalStatus.value == ThermalStatusLevel.SEVERE)

        val isBatteryTempCritical = hasFreshFiniteValue(base.batteryTempC, 5_000L, nowMs) &&
                base.batteryTempC.value!! >= 43.0f

        val isApTempCritical = hasFreshFiniteValue(base.apTempC, 5_000L, nowMs) &&
                base.apTempC.value!! >= 48.0f

        if (isThermalStatusCritical || isBatteryTempCritical || isApTempCritical) {
            return SmartHubProfile.P0_THERMAL_EMERGENCY
        }

        // P1: Critical Battery Check (Quality-aware: battery percent must be AVAILABLE and <= 15%)
        val isBatteryAvailable = base.batteryPercent.isAvailableAndFresh(10_000L, nowMs)
        val isChargingAvailable = base.isCharging.isAvailableAndFresh(10_000L, nowMs)
        val batteryLevel = base.batteryPercent.value
        val isCharging = base.isCharging.value

        if (isBatteryAvailable && isChargingAvailable && batteryLevel != null && isCharging == false && batteryLevel <= 15) {
            return SmartHubProfile.P1_CRITICAL_BATTERY
        }

        // P2: Charging Thermal Guard (Quality-aware: requires charging AVAILABLE and batteryTemp AVAILABLE)
        val isBatteryTempWarm = hasFreshFiniteValue(base.batteryTempC, 5_000L, nowMs) &&
                base.batteryTempC.value!! >= 39.0f
        if (isChargingAvailable && isCharging == true && isBatteryTempWarm) {
            return SmartHubProfile.P2_CHARGING_THERMAL_GUARD
        }

        // P3: Gaming & High Load
        val fgPkg = base.foregroundPackage.value
        if (base.foregroundPackage.isAvailableAndFresh(5_000L, nowMs) && fgPkg != null && gamingPackages.contains(fgPkg)) {
            return SmartHubProfile.P3_GAMING_HIGH_LOAD
        }

        // P4: Media Playback
        if (state.mediaPlayback.isAvailableAndFresh(5_000L, nowMs) && state.mediaPlayback.value == true) {
            return SmartHubProfile.P4_MEDIA_READING
        }

        // P6: Overnight Deep Idle Condition
        val isScreenOn = base.isScreenOn.value
        val isOvernightHours = state.currentHourOfDay >= 23 || state.currentHourOfDay < 6
        val isScreenOffLongEnough = base.isScreenOn.isAvailableAndFresh(5_000L, nowMs) &&
                isScreenOn == false && state.screenOffDurationMs >= 900_000L
        val mediaInactive = state.mediaPlayback.isAvailableAndFresh(5_000L, nowMs) && state.mediaPlayback.value == false
        val navigationInactive = state.navigationContext.isAvailableAndFresh(5_000L, nowMs) && state.navigationContext.value == false

        if (isOvernightHours && mediaInactive && navigationInactive && isScreenOffLongEnough) {
            return SmartHubProfile.P6_OVERNIGHT_DEEP_IDLE
        }

        // P5: Daily Adaptive Default
        return SmartHubProfile.P5_DAILY_ADAPTIVE
    }

    fun evaluateState(state: DeviceState): Pair<SmartHubProfile, String> {
        val profile = updateState(ExtendedDeviceState(baseState = state))
        return Pair(profile, "State profile selected: ${profile.displayName}")
    }

    private fun hasFreshFiniteValue(value: TelemetryValue<Float>, maxAgeMs: Long, nowMs: Long): Boolean =
        value.isAvailableAndFresh(maxAgeMs, nowMs) && value.value?.isFinite() == true
}
