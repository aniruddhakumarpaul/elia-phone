package com.antigravity.smarthub.core.safety

import com.antigravity.smarthub.core.model.DeviceState
import com.antigravity.smarthub.core.model.SystemAction
import com.antigravity.smarthub.core.model.ThermalStatusLevel
import com.antigravity.smarthub.core.telemetry.TelemetryState
import com.antigravity.smarthub.core.telemetry.TelemetryValue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SafetyGovernorTest {

    private lateinit var governor: SafetyGovernor

    @Before
    fun setUp() {
        governor = SafetyGovernor()
    }

    @Test
    fun testVetoesRefreshRateBoostUnderThermalEmergency() {
        val criticalState = DeviceState(
            thermalStatus = TelemetryValue(ThermalStatusLevel.CRITICAL, TelemetryState.AVAILABLE)
        )
        val boostAction = SystemAction.SetRefreshRate(targetMode = 0) // 120Hz
        val result = governor.evaluateAction(boostAction, criticalState)
        assertFalse(result.isAllowed)
    }

    @Test
    fun testVetoesRefreshRateBoostWhenThermalIsUnavailable() {
        val result = governor.evaluateAction(SystemAction.SetRefreshRate(0), DeviceState())
        assertFalse(result.isAllowed)
    }

    @Test
    fun testVetoesRestrictingProtectedPackageTeams() {
        val normalState = DeviceState()
        val restrictAction = SystemAction.SetStandbyBucket("com.microsoft.teams", "restricted")
        val result = governor.evaluateAction(restrictAction, normalState)
        assertFalse(result.isAllowed)
    }

    @Test
    fun testVetoesRestrictingWhatsAppBackgroundOps() {
        val normalState = DeviceState()
        val restrictOps = SystemAction.SetAppOpsBackground("com.whatsapp", allow = false)
        val result = governor.evaluateAction(restrictOps, normalState)
        assertFalse(result.isAllowed)
    }

    @Test
    fun testAllowsStandbyBucketForUnprotectedApp() {
        val normalState = DeviceState()
        val action = SystemAction.SetStandbyBucket("com.unprotected.exampleapp", "working_set")
        val result = governor.evaluateAction(action, normalState)
        assertTrue(result.isAllowed)
    }
}
