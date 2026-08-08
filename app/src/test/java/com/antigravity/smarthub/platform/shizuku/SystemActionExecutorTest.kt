package com.antigravity.smarthub.platform.shizuku

import com.antigravity.smarthub.ISmartHubUserService
import com.antigravity.smarthub.core.model.DeviceState
import com.antigravity.smarthub.core.model.SystemAction
import com.antigravity.smarthub.core.model.ThermalStatusLevel
import com.antigravity.smarthub.core.persistence.BaselineRepository
import com.antigravity.smarthub.core.safety.SafetyGovernor
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

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
        // Baseline = 10 (active), Target = rare (40), Readback returns 10 (Verification Failed)
        `when`(mockUserService.readStandbyBucket("com.example.app")).thenReturn(10, 10, 10)
        `when`(mockUserService.setStandbyBucket("com.example.app", "rare")).thenReturn(0)

        val executor = SystemActionExecutor(mockUserService, safetyGovernor, baselineRepository)
        val action = SystemAction.SetStandbyBucket("com.example.app", "rare")
        val result = executor.executeTransaction(action, DeviceState())

        assertFalse(result.success)
        assertTrue(result.rolledBack)
        assertTrue(result.errorMessage!!.contains("verification failed", ignoreCase = true))
    }

    @Test
    fun testFailedAppOpsVerificationTriggersRollback() {
        // Baseline = "allow", Target = ignore (allow=false), Readback returns "allow" (Verification Failed)
        `when`(mockUserService.readAppOpsBackground("com.example.app")).thenReturn("allow", "allow", "allow")
        `when`(mockUserService.setAppOpsBackground("com.example.app", "ignore")).thenReturn(0)

        val executor = SystemActionExecutor(mockUserService, safetyGovernor, baselineRepository)
        val action = SystemAction.SetAppOpsBackground("com.example.app", allow = false)
        val result = executor.executeTransaction(action, DeviceState())

        assertFalse(result.success)
        assertTrue(result.rolledBack)
        assertTrue(result.errorMessage!!.contains("verification failed", ignoreCase = true))
    }

    @Test
    fun testMissingUserServiceFails() {
        val executor = SystemActionExecutor(null, safetyGovernor, baselineRepository)
        val action = SystemAction.SetRefreshRate(targetMode = 1)
        val result = executor.executeTransaction(action, DeviceState())

        assertFalse(result.success)
        assertTrue(result.errorMessage!!.contains("unavailable"))
    }

    @Test
    fun testSafetyGovernorVetoFails() {
        val executor = SystemActionExecutor(mockUserService, safetyGovernor, baselineRepository)
        val action = SystemAction.SetRefreshRate(targetMode = 0)
        val criticalState = DeviceState(thermalStatus = ThermalStatusLevel.CRITICAL)

        val result = executor.executeTransaction(action, criticalState)
        assertFalse(result.success)
        assertTrue(result.errorMessage!!.contains("VETOED"))
    }
}
