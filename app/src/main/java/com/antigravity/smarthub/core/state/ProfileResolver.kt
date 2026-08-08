package com.antigravity.smarthub.core.state

import com.antigravity.smarthub.core.model.DeviceState
import com.antigravity.smarthub.core.model.SmartHubProfile
import com.antigravity.smarthub.core.model.SystemAction
import com.antigravity.smarthub.core.model.ThermalStatusLevel

data class ResolvedState(
    val activeProfile: SmartHubProfile,
    val rationale: String,
    val recommendedActions: List<SystemAction>
)

class ProfileResolver {

    private val gamingPackages = setOf(
        "com.pubg.imobile",
        "com.dts.freefiremax",
        "com.roblox.client",
        "com.firsttouchgames.dls7",
        "com.vng.circle.tribe.idle",
        "com.plarium.mechlegion",
        "com.kunpo88.baba2.tay"
    )

    private val mediaPackages = setOf(
        "com.google.android.apps.youtube.music",
        "com.google.android.videos",
        "in.startv.hotstar",
        "com.amazon.avod.thirdpartyclient",
        "com.reelwave.shorttv"
    )

    fun resolve(state: DeviceState): ResolvedState {
        // P0: Thermal Emergency Check
        if (state.thermalStatus == ThermalStatusLevel.CRITICAL ||
            state.thermalStatus == ThermalStatusLevel.SEVERE ||
            state.batteryTempC >= 43.0f ||
            state.apTempC >= 48.0f
        ) {
            return ResolvedState(
                activeProfile = SmartHubProfile.P0_THERMAL_EMERGENCY,
                rationale = "Thermal status ${state.thermalStatus} (AP: ${state.apTempC}°C, Battery: ${state.batteryTempC}°C). Emergency cooling active.",
                recommendedActions = listOf(
                    SystemAction.SetRefreshRate(targetMode = 1) // Force 60Hz
                )
            )
        }

        // P1: Critical Battery Check
        if (state.batteryPercent <= 15 && !state.isCharging) {
            return ResolvedState(
                activeProfile = SmartHubProfile.P1_CRITICAL_BATTERY,
                rationale = "Battery level at ${state.batteryPercent}%. Saver policy active.",
                recommendedActions = listOf(
                    SystemAction.SetRefreshRate(targetMode = 1) // Force 60Hz
                )
            )
        }

        // P2: Charging Thermal Guard
        if (state.isCharging && state.batteryTempC >= 39.0f) {
            return ResolvedState(
                activeProfile = SmartHubProfile.P2_CHARGING_THERMAL_GUARD,
                rationale = "Device charging with elevated temperature (${state.batteryTempC}°C). Thermal guard active.",
                recommendedActions = listOf(
                    SystemAction.SetRefreshRate(targetMode = 1)
                )
            )
        }

        // P3: Gaming & High Load
        if (gamingPackages.contains(state.foregroundPackage)) {
            return ResolvedState(
                activeProfile = SmartHubProfile.P3_GAMING_HIGH_LOAD,
                rationale = "Foreground app ${state.foregroundPackage} classified as Game. High performance mode active.",
                recommendedActions = listOf(
                    SystemAction.SetRefreshRate(targetMode = 0), // 120Hz
                    SystemAction.SetStandbyBucket(state.foregroundPackage, "active")
                )
            )
        }

        // P4: Media Playback
        if (mediaPackages.contains(state.foregroundPackage)) {
            return ResolvedState(
                activeProfile = SmartHubProfile.P4_MEDIA_READING,
                rationale = "Media application ${state.foregroundPackage} active. Media energy saver profile selected.",
                recommendedActions = listOf(
                    SystemAction.SetRefreshRate(targetMode = 1) // 60Hz for video energy save
                )
            )
        }

        // P6: Overnight Deep Idle
        if (!state.isScreenOn && state.batteryPercent > 15) {
            return ResolvedState(
                activeProfile = SmartHubProfile.P6_OVERNIGHT_DEEP_IDLE,
                rationale = "Screen off & idle detected. Deep idle energy conservation active.",
                recommendedActions = listOf(
                    SystemAction.SetRefreshRate(targetMode = 1)
                )
            )
        }

        // P5: Daily Adaptive Default
        return ResolvedState(
            activeProfile = SmartHubProfile.P5_DAILY_ADAPTIVE,
            rationale = "Standard daily usage context active. Balanced 120Hz/60Hz adaptive mode.",
            recommendedActions = listOf(
                SystemAction.SetRefreshRate(targetMode = 0) // Adaptive 120Hz
            )
        )
    }

    fun resolveProfile(
        profile: SmartHubProfile,
        state: DeviceState,
        rationale: String
    ): ResolvedState {
        val defaultResolved = resolve(state)
        return ResolvedState(
            activeProfile = profile,
            rationale = rationale.ifBlank { defaultResolved.rationale },
            recommendedActions = defaultResolved.recommendedActions
        )
    }
}
