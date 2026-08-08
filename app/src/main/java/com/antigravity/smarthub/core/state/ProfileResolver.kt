package com.antigravity.smarthub.core.state

import com.antigravity.smarthub.core.model.DeviceState
import com.antigravity.smarthub.core.model.SmartHubProfile
import com.antigravity.smarthub.core.model.SystemAction
import com.antigravity.smarthub.core.telemetry.TelemetryState

data class ResolvedState(
    val activeProfile: SmartHubProfile,
    val rationale: String,
    val recommendedActions: List<SystemAction>
)

class ProfileResolver {

    /**
     * Resolves recommended SystemActions specifically for the single profile selected by StateMachineEngine.
     * Does NOT independently re-evaluate or select a competing profile.
     */
    fun resolveActionsForProfile(
        profile: SmartHubProfile,
        state: ExtendedDeviceState
    ): ResolvedState {
        val base = state.baseState
        val fgPkg = base.foregroundPackage.value ?: ""

        return when (profile) {
            SmartHubProfile.P0_THERMAL_EMERGENCY -> ResolvedState(
                activeProfile = profile,
                rationale = "Thermal status elevated (${base.thermalStatus.value?.name ?: "UNAVAILABLE"}). Cooling override active.",
                recommendedActions = listOf(
                    SystemAction.SetRefreshRate(targetMode = 1) // Force 60Hz to reduce GPU/display power
                )
            )
            SmartHubProfile.P1_CRITICAL_BATTERY -> ResolvedState(
                activeProfile = profile,
                rationale = "Battery level at ${base.batteryPercent.value?.let { "$it%" } ?: "UNAVAILABLE"}. Energy saver active.",
                recommendedActions = listOf(
                    SystemAction.SetRefreshRate(targetMode = 1) // Force 60Hz
                )
            )
            SmartHubProfile.P2_CHARGING_THERMAL_GUARD -> ResolvedState(
                activeProfile = profile,
                rationale = "Device charging with elevated temperature (${base.batteryTempC.value?.let { "$it°C" } ?: "UNAVAILABLE"}). Thermal guard active.",
                recommendedActions = listOf(
                    SystemAction.SetRefreshRate(targetMode = 1)
                )
            )
            SmartHubProfile.P3_GAMING_HIGH_LOAD -> ResolvedState(
                activeProfile = profile,
                rationale = "Game foreground package '$fgPkg' active. High performance mode.",
                recommendedActions = listOf(
                    SystemAction.SetRefreshRate(targetMode = 0), // Adaptive 120Hz
                    SystemAction.SetStandbyBucket(fgPkg, "active")
                )
            )
            SmartHubProfile.P4_MEDIA_READING -> ResolvedState(
                activeProfile = profile,
                rationale = "Active MediaSession playback detected. 60Hz video playback energy saver active.",
                recommendedActions = listOf(
                    SystemAction.SetRefreshRate(targetMode = 1)
                )
            )
            SmartHubProfile.P6_OVERNIGHT_DEEP_IDLE -> ResolvedState(
                activeProfile = profile,
                rationale = "Screen off overnight idle (>15m). Deep idle energy saver active.",
                recommendedActions = listOf(
                    SystemAction.SetRefreshRate(targetMode = 1)
                )
            )
            SmartHubProfile.P5_DAILY_ADAPTIVE -> ResolvedState(
                activeProfile = profile,
                rationale = "Standard daily usage context active. Balanced 120Hz/60Hz adaptive mode.",
                recommendedActions = listOf(
                    SystemAction.SetRefreshRate(targetMode = 0)
                )
            )
        }
    }

    fun resolve(state: DeviceState): ResolvedState {
        val extState = ExtendedDeviceState(baseState = state)
        val profile = StateMachineEngine().updateState(extState)
        return resolveActionsForProfile(profile, extState)
    }

    fun resolveProfile(
        profile: SmartHubProfile,
        state: DeviceState,
        rationale: String
    ): ResolvedState {
        val extState = ExtendedDeviceState(baseState = state)
        val resolved = resolveActionsForProfile(profile, extState)
        return ResolvedState(
            activeProfile = profile,
            rationale = rationale.ifBlank { resolved.rationale },
            recommendedActions = resolved.recommendedActions
        )
    }
}
