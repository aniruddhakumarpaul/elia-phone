package com.antigravity.smarthub.core.state

import com.antigravity.smarthub.core.model.DeviceState
import com.antigravity.smarthub.core.model.SmartHubProfile
import com.antigravity.smarthub.core.model.ThermalStatusLevel
import com.antigravity.smarthub.core.telemetry.TelemetryState
import java.util.Calendar

data class ExtendedDeviceState(
    val baseState: DeviceState,
    val isMediaPlaying: Boolean = false,
    val isNavigationActive: Boolean = false,
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

        // P0: Thermal Emergency Check (Quality-aware: require thermal value to be AVAILABLE)
        val isThermalStatusCritical = base.thermalStatus.state == TelemetryState.AVAILABLE &&
                (base.thermalStatus.value == ThermalStatusLevel.CRITICAL || base.thermalStatus.value == ThermalStatusLevel.SEVERE)

        val isBatteryTempCritical = base.batteryTempC.state == TelemetryState.AVAILABLE &&
                (base.batteryTempC.value ?: 0f) >= 43.0f

        val isApTempCritical = base.apTempC.state == TelemetryState.AVAILABLE &&
                (base.apTempC.value ?: 0f) >= 48.0f

        if (isThermalStatusCritical || isBatteryTempCritical || isApTempCritical) {
            return SmartHubProfile.P0_THERMAL_EMERGENCY
        }

        // P1: Critical Battery Check (Quality-aware: battery percent must be AVAILABLE and <= 15%)
        val isBatteryAvailable = base.batteryPercent.state == TelemetryState.AVAILABLE
        val batteryLevel = base.batteryPercent.value ?: 100
        val isCharging = base.isCharging.value ?: false

        if (isBatteryAvailable && batteryLevel <= 15 && !isCharging) {
            return SmartHubProfile.P1_CRITICAL_BATTERY
        }

        // P2: Charging Thermal Guard (Quality-aware: requires charging AVAILABLE and batteryTemp AVAILABLE)
        val isBatteryTempWarm = base.batteryTempC.state == TelemetryState.AVAILABLE && (base.batteryTempC.value ?: 0f) >= 39.0f
        if (isCharging && isBatteryTempWarm) {
            return SmartHubProfile.P2_CHARGING_THERMAL_GUARD
        }

        // P3: Gaming & High Load
        val fgPkg = base.foregroundPackage.value ?: ""
        if (base.foregroundPackage.state == TelemetryState.AVAILABLE && gamingPackages.contains(fgPkg)) {
            return SmartHubProfile.P3_GAMING_HIGH_LOAD
        }

        // P4: Media Playback
        if (state.isMediaPlaying) {
            return SmartHubProfile.P4_MEDIA_READING
        }

        // P6: Overnight Deep Idle Condition
        val isScreenOn = base.isScreenOn.value ?: true
        val isOvernightHours = state.currentHourOfDay >= 23 || state.currentHourOfDay < 6
        val isScreenOffLongEnough = !isScreenOn && state.screenOffDurationMs >= 900_000L

        if (!isScreenOn && isOvernightHours && !state.isMediaPlaying && !state.isNavigationActive && isScreenOffLongEnough) {
            return SmartHubProfile.P6_OVERNIGHT_DEEP_IDLE
        }

        // P5: Daily Adaptive Default
        return SmartHubProfile.P5_DAILY_ADAPTIVE
    }

    fun evaluateState(state: DeviceState): Pair<SmartHubProfile, String> {
        val profile = updateState(ExtendedDeviceState(baseState = state))
        return Pair(profile, "State profile selected: ${profile.displayName}")
    }
}
