package com.antigravity.smarthub.platform.shizuku

import com.antigravity.smarthub.ISmartHubUserService
import com.antigravity.smarthub.core.model.DeviceState
import com.antigravity.smarthub.core.model.SystemAction
import com.antigravity.smarthub.core.model.ThermalStatusLevel
import com.antigravity.smarthub.core.persistence.BaselineRepository
import com.antigravity.smarthub.core.safety.SafetyGovernor
import com.antigravity.smarthub.core.telemetry.TelemetryState
import com.antigravity.smarthub.core.telemetry.TelemetryValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class SystemActionExecutorTest {

    private lateinit var mockUserService: ISmartHubUserService
    private lateinit var safetyGovernor: SafetyGovernor
    private lateinit var baselineRepository: BaselineRepository

    @Before
    fun setUp() {
        mockUserService = mock(ISmartHubUserService::class.java)
        safetyGovernor = SafetyGovernor()
        baselineRepository = BaselineRepository()
    }

    @Test
    fun testSuccessfulRefreshRateTransaction() {
        `when`(mockUserService.readSetting("secure", "refresh_rate_mode")).thenReturn("0", "1")
        `when`(mockUserService.setRefreshRateMode(1)).thenReturn(0)

        val executor = SystemActionExecutor(mockUserService, safetyGovernor, baselineRepository)
        val action = SystemAction.SetRefreshRate(targetMode = 1)
        val result = executor.executeTransaction(action, DeviceState())

        assertTrue(result.success)
        assertFalse(result.rolledBack)
        assertEquals("0", result.baselineCaptured)
        assertEquals("1", result.verifiedValue)
    }

    @Test
    fun testFailedRefreshRateVerificationTriggersRollback() {
        `when`(mockUserService.readSetting("secure", "refresh_rate_mode")).thenReturn("0", "0", "0")
        `when`(mockUserService.setRefreshRateMode(anyInt())).thenReturn(0)

        val executor = SystemActionExecutor(mockUserService, safetyGovernor, baselineRepository)
        val action = SystemAction.SetRefreshRate(targetMode = 1)
        val result = executor.executeTransaction(action, DeviceState())

        assertFalse(result.success)
        assertTrue(result.rolledBack)
        assertTrue(result.errorMessage!!.contains("verification failed", ignoreCase = true))
    }

    @Test
    fun testFailedStandbyBucketVerificationTriggersRollback() {
        // Baseline = 10 (active), Target = rare, Readback returns 10 (Verification Failed)
        `when`(mockUserService.readStandbyBucket("com.google.android.youtube")).thenReturn(10, 10, 10)
        `when`(mockUserService.setStandbyBucket("com.google.android.youtube", "rare")).thenReturn(0)
        `when`(mockUserService.setStandbyBucket("com.google.android.youtube", "active")).thenReturn(0)

        val executor = SystemActionExecutor(mockUserService, safetyGovernor, baselineRepository)
        val action = SystemAction.SetStandbyBucket("com.google.android.youtube", "rare")
        val result = executor.executeTransaction(action, DeviceState())

        assertFalse(result.success)
        assertTrue(result.rolledBack)
        assertTrue(result.errorMessage!!.contains("verification failed", ignoreCase = true))
        verify(mockUserService).setStandbyBucket("com.google.android.youtube", "active")
    }

    @Test
    fun testExemptedStandbyBucketRollback() {
        // Baseline = 5 (exempted), Target = rare, Readback returns 10 (Verification Failed) -> Rollback to exempted (5)
        `when`(mockUserService.readStandbyBucket("com.google.android.youtube")).thenReturn(5, 10, 5)
        `when`(mockUserService.setStandbyBucket("com.google.android.youtube", "rare")).thenReturn(0)
        `when`(mockUserService.setStandbyBucket("com.google.android.youtube", "exempted")).thenReturn(0)

        val executor = SystemActionExecutor(mockUserService, safetyGovernor, baselineRepository)
        val action = SystemAction.SetStandbyBucket("com.google.android.youtube", "rare")
        val result = executor.executeTransaction(action, DeviceState())

        assertFalse(result.success)
        assertTrue(result.rolledBack)
        assertEquals("exempted", result.baselineCaptured)
        verify(mockUserService).setStandbyBucket("com.google.android.youtube", "exempted")
    }

    @Test
    fun testFailedAppOpsVerificationTriggersDefaultRollback() {
        // Baseline = "No operations." (default), Target = ignore, Readback returns "allow" -> Rollback to default
        `when`(mockUserService.readAppOpsBackground("com.google.android.youtube")).thenReturn("No operations.", "allow", "No operations.")
        `when`(mockUserService.setAppOpsBackground("com.google.android.youtube", "ignore")).thenReturn(0)
        `when`(mockUserService.setAppOpsBackground("com.google.android.youtube", "default")).thenReturn(0)

        val executor = SystemActionExecutor(mockUserService, safetyGovernor, baselineRepository)
        val action = SystemAction.SetAppOpsBackground("com.google.android.youtube", allow = false)
        val result = executor.executeTransaction(action, DeviceState())

        assertFalse(result.success)
        assertTrue(result.rolledBack)
        assertEquals("default", result.baselineCaptured)
        verify(mockUserService).setAppOpsBackground("com.google.android.youtube", "default")
    }

    @Test
    fun testDynamicBinderResolutionLateBinding() {
        var currentService: ISmartHubUserService? = null
        val executor = SystemActionExecutor({ currentService }, safetyGovernor, baselineRepository)

        // 1. Initial invocation when service is not yet bound
        val action = SystemAction.SetRefreshRate(targetMode = 1)
        val earlyResult = executor.executeTransaction(action, DeviceState())
        assertFalse(earlyResult.success)
        assertTrue(earlyResult.errorMessage!!.contains("unavailable"))

        // 2. Service binds later
        `when`(mockUserService.readSetting("secure", "refresh_rate_mode")).thenReturn("0", "1")
        `when`(mockUserService.setRefreshRateMode(1)).thenReturn(0)
        currentService = mockUserService

        val lateResult = executor.executeTransaction(action, DeviceState())
        assertTrue(lateResult.success)
    }

    @Test
    fun testBinderDeathAndReplacement() {
        var currentService: ISmartHubUserService? = mockUserService
        val executor = SystemActionExecutor({ currentService }, safetyGovernor, baselineRepository)

        // Initial call works
        `when`(mockUserService.readSetting("secure", "refresh_rate_mode")).thenReturn("0", "1")
        `when`(mockUserService.setRefreshRateMode(1)).thenReturn(0)
        assertTrue(executor.executeTransaction(SystemAction.SetRefreshRate(1), DeviceState()).success)

        // Binder dies and replaced with new mock
        val newMockService = mock(ISmartHubUserService::class.java)
        `when`(newMockService.readSetting("secure", "refresh_rate_mode")).thenReturn("1", "0")
        `when`(newMockService.setRefreshRateMode(0)).thenReturn(0)
        currentService = newMockService

        val secondResult = executor.executeTransaction(
            SystemAction.SetRefreshRate(0),
            DeviceState(thermalStatus = TelemetryValue(ThermalStatusLevel.NOMINAL, TelemetryState.AVAILABLE))
        )
        assertTrue(secondResult.success)
        verify(newMockService).setRefreshRateMode(0)
    }

    @Test
    fun testSafetyGovernorVetoFails() {
        val executor = SystemActionExecutor(mockUserService, safetyGovernor, baselineRepository)
        val action = SystemAction.SetRefreshRate(targetMode = 0)
        val criticalState = DeviceState(
            thermalStatus = TelemetryValue(ThermalStatusLevel.CRITICAL, TelemetryState.AVAILABLE)
        )

        val result = executor.executeTransaction(action, criticalState)
        assertFalse(result.success)
        assertTrue(result.errorMessage!!.contains("VETOED"))
    }

    @Test
    fun testInvalidRefreshBaselinePreventsMutation() {
        `when`(mockUserService.readSetting("secure", "refresh_rate_mode")).thenReturn("not-a-mode")
        val executor = SystemActionExecutor(mockUserService, safetyGovernor, baselineRepository)

        val result = executor.executeTransaction(SystemAction.SetRefreshRate(1), DeviceState())

        assertFalse(result.success)
        assertTrue(result.errorMessage!!.contains("Invalid refresh baseline"))
        verify(mockUserService, org.mockito.Mockito.never()).setRefreshRateMode(anyInt())
    }

    @Test
    fun testSettingAcceptedButEffectiveRefreshDisagrees() {
        `when`(mockUserService.readSetting("secure", "refresh_rate_mode")).thenReturn("1", "0", "1")
        `when`(mockUserService.setRefreshRateMode(anyInt())).thenReturn(0)
        val executor = SystemActionExecutor(
            mockUserService,
            safetyGovernor,
            baselineRepository,
            effectiveRefreshRateReader = { 60.0f },
            stabilizationDelayMs = 0L
        )

        val result = executor.executeTransaction(
            SystemAction.SetRefreshRate(0),
            DeviceState(thermalStatus = TelemetryValue(ThermalStatusLevel.NOMINAL, TelemetryState.AVAILABLE))
        )

        assertFalse(result.success)
        assertEquals(com.antigravity.smarthub.core.model.CapabilityResult.PARTIALLY_SUPPORTED, result.capabilityResult)
        assertTrue(result.errorMessage!!.contains("effective display"))
        assertTrue(result.rolledBack)
    }

    @Test
    fun testRestoreOriginalRefreshBaselineExactly() {
        `when`(mockUserService.readSetting("secure", "refresh_rate_mode")).thenReturn("0", "1", "0")
        `when`(mockUserService.setRefreshRateMode(anyInt())).thenReturn(0)
        val executor = SystemActionExecutor(mockUserService, safetyGovernor, baselineRepository)

        assertTrue(executor.executeTransaction(SystemAction.SetRefreshRate(1), DeviceState()).success)
        val restored = executor.restoreOriginal(
            "REFRESH_RATE",
            DeviceState(thermalStatus = TelemetryValue(ThermalStatusLevel.NOMINAL, TelemetryState.AVAILABLE))
        )

        assertTrue(restored.success)
        assertEquals("0", restored.requestedValue)
        verify(mockUserService).setRefreshRateMode(0)
    }
}
