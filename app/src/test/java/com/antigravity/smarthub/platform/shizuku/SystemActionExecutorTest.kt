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
import org.mockito.ArgumentMatchers.anyString
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
    fun testSuccessfulTransaction() {
        `when`(mockUserService.readSetting("secure", "refresh_rate_mode")).thenReturn("0", "1")
        `when`(mockUserService.setRefreshRateMode(1)).thenReturn(0)

        val executor = SystemActionExecutor(mockUserService, safetyGovernor, baselineRepository)
        val action = SystemAction.SetRefreshRate(targetMode = 1)
        val result = executor.executeTransaction(action, DeviceState())

        assertTrue(result.success)
        assertFalse(result.rolledBack)
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

    @Test
    fun testFailedExecutionFails() {
        `when`(mockUserService.readSetting("secure", "refresh_rate_mode")).thenReturn("0")
        `when`(mockUserService.setRefreshRateMode(1)).thenReturn(-1) // Failed IPC

        val executor = SystemActionExecutor(mockUserService, safetyGovernor, baselineRepository)
        val action = SystemAction.SetRefreshRate(targetMode = 1)
        val result = executor.executeTransaction(action, DeviceState())

        assertFalse(result.success)
        assertTrue(result.errorMessage!!.contains("IPC setRefreshRateMode failed"))
    }

    @Test
    fun testFailedVerificationTriggersRollback() {
        // Baseline = "0", Target = 1, Readback returns "0" (Verification Failed)
        `when`(mockUserService.readSetting("secure", "refresh_rate_mode")).thenReturn("0", "0", "0")
        `when`(mockUserService.setRefreshRateMode(anyInt())).thenReturn(0)

        val executor = SystemActionExecutor(mockUserService, safetyGovernor, baselineRepository)
        val action = SystemAction.SetRefreshRate(targetMode = 1)
        val result = executor.executeTransaction(action, DeviceState())

        assertFalse(result.success)
        assertTrue(result.rolledBack)
        assertTrue(result.errorMessage!!.contains("Verification failed"))
    }
}
