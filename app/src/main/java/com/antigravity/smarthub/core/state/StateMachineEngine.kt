package com.antigravity.smarthub.core.state

import com.antigravity.smarthub.core.model.DeviceState
import com.antigravity.smarthub.core.model.SmartHubProfile
import com.antigravity.smarthub.core.model.SystemAction
import com.antigravity.smarthub.core.model.ThermalStatusLevel
import java.util.Calendar

data class ExtendedDeviceState(
    val baseState: DeviceState,
    val isMediaPlaying: Boolean = false,
    val isNavigationActive: Boolean = false,
    val currentHourOfDay: Int = 12,
    val screenOffDurationMs: Long = 0L
)

class StateMachineEngine {

    var currentProfile: SmartHubProfile = SmartHubProfile.P5_DAILY_ADAPTIVE
        private set

    var lastStateChangeTimeMs: Long = System.currentTimeMillis()
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

    fun updateState(state: ExtendedDeviceState, currentTimeMs: Long = System.currentTimeMillis()): ResolvedState {
        val base = state.baseState
        val targetProfile = determineTargetProfile(state)

        // Hysteresis Rule 1: Immediate escalation for P0_THERMAL_EMERGENCY
        if (targetProfile == SmartHubProfile.P0_THERMAL_EMERGENCY) {
            currentProfile = targetProfile
            lastStateChangeTimeMs = currentTimeMs
            return buildResolvedState(currentProfile, base, state)
        }

        // Hysteresis Rule 2: Debounce state exit (minimum dwell time 3000ms for games)
        val timeInCurrentProfileMs = currentTimeMs - lastStateChangeTimeMs
        if (currentProfile == SmartHubProfile.P3_GAMING_HIGH_LOAD && targetProfile != SmartHubProfile.P3_GAMING_HIGH_LOAD) {
            if (timeInCurrentProfileMs < 3000L) {
                // Hold gaming profile during transient notification shade pull-downs
                return buildResolvedState(currentProfile, base, state)
            }
        }

        if (currentProfile != targetProfile) {
            currentProfile = targetProfile
            lastStateChangeTimeMs = currentTimeMs
        }

        return buildResolvedState(currentProfile, base, state)
    }

    private fun determineTargetProfile(state: ExtendedDeviceState): SmartHubProfile {
        val base = state.baseState

        // P0: Thermal Emergency Check
        if (base.thermalStatus == ThermalStatusLevel.CRITICAL ||
            base.thermalStatus == ThermalStatusLevel.SEVERE ||
            base.batteryTempC >= 43.0f ||
            base.apTempC >= 48.0f
        ) {
            return SmartHubProfile.P0_THERMAL_EMERGENCY
        }

        // P1: Critical Battery Check
        if (base.batteryPercent <= 15 && !base.isCharging) {
            return SmartHubProfile.P1_CRITICAL_BATTERY
        }

        // P2: Charging Thermal Guard
        if (base.isCharging && base.batteryTempC >= 39.0f) {
            return SmartHubProfile.P2_CHARGING_THERMAL_GUARD
        }

        // P3: Gaming & High Load
        if (gamingPackages.contains(base.foregroundPackage)) {
            return SmartHubProfile.P3_GAMING_HIGH_LOAD
        }

        // P4: Media Playback (Requires active MediaSession Playback, NOT just package foreground)
        if (state.isMediaPlaying) {
            return SmartHubProfile.P4_MEDIA_READING
        }

        // P6: Overnight Deep Idle Condition
        // Strictly requires: Screen OFF AND (23:00 - 06:00) AND no Media AND no Navigation AND Idle > 15 mins (900,000 ms)
        val isOvernightHours = state.currentHourOfDay >= 23 || state.currentHourOfDay < 6
        if (!base.isScreenOn && isOvernightHours && !state.isMediaPlaying && !state.isNavigationActive && state.screenOffDurationMs >= 900_000L) {
            return SmartHubProfile.P6_OVERNIGHT_DEEP_IDLE
        }

        // P5: Daily Adaptive Default
        return SmartHubProfile.P5_DAILY_ADAPTIVE
    }

    private fun buildResolvedState(
        profile: SmartHubProfile,
        base: DeviceState,
        extended: ExtendedDeviceState
    ): ResolvedState {
        return when (profile) {
            SmartHubProfile.P0_THERMAL_EMERGENCY -> ResolvedState(
                activeProfile = profile,
                rationale = "Thermal status ${base.thermalStatus}. Cooling override active.",
                recommendedActions = listOf(SystemAction.SetRefreshRate(targetMode = 1))
            )
            SmartHubProfile.P1_CRITICAL_BATTERY -> ResolvedState(
                activeProfile = profile,
                rationale = "Battery at ${base.batteryPercent}%. Energy saver active.",
                recommendedActions = listOf(SystemAction.SetRefreshRate(targetMode = 1))
            )
            SmartHubProfile.P2_CHARGING_THERMAL_GUARD -> ResolvedState(
                activeProfile = profile,
                rationale = "Device charging with elevated temp (${base.batteryTempC}°C). Thermal guard active.",
                recommendedActions = listOf(SystemAction.SetRefreshRate(targetMode = 1))
            )
            SmartHubProfile.P3_GAMING_HIGH_LOAD -> ResolvedState(
                activeProfile = profile,
                rationale = "Game foreground package ${base.foregroundPackage} active. High performance mode.",
                recommendedActions = listOf(
                    SystemAction.SetRefreshRate(targetMode = 0),
                    SystemAction.SetStandbyBucket(base.foregroundPackage, "active")
                )
            )
            SmartHubProfile.P4_MEDIA_READING -> ResolvedState(
                activeProfile = profile,
                rationale = "Active MediaSession playback detected. 60Hz playback energy saver active.",
                recommendedActions = listOf(SystemAction.SetRefreshRate(targetMode = 1))
            )
            SmartHubProfile.P6_OVERNIGHT_DEEP_IDLE -> ResolvedState(
                activeProfile = profile,
                rationale = "Screen off overnight idle (>15m). Deep idle energy saver active.",
                recommendedActions = listOf(SystemAction.SetRefreshRate(targetMode = 1))
            )
            SmartHubProfile.P5_DAILY_ADAPTIVE -> ResolvedState(
                activeProfile = profile,
                rationale = "Standard daily usage context. Adaptive 120Hz/60Hz active.",
                recommendedActions = listOf(SystemAction.SetRefreshRate(targetMode = 0))
            )
        }
    }
}
