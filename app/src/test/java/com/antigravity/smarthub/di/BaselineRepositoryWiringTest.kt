package com.antigravity.smarthub.di

import com.antigravity.smarthub.core.persistence.BaselineRepository
import com.antigravity.smarthub.core.persistence.PersistenceFailureInjector
import com.antigravity.smarthub.core.state.ActionLedger
import com.antigravity.smarthub.core.state.OptimizationController
import com.antigravity.smarthub.core.state.ProfileResolver
import com.antigravity.smarthub.core.state.StateMachineEngine
import com.antigravity.smarthub.core.safety.AppClassifier
import com.antigravity.smarthub.core.safety.SafetyGovernor
import com.antigravity.smarthub.core.telemetry.TelemetryAggregator
import com.antigravity.smarthub.platform.shizuku.ShizukuServiceConnection
import com.antigravity.smarthub.platform.shizuku.SystemActionExecutor
import com.antigravity.smarthub.core.model.DeviceState
import com.antigravity.smarthub.core.model.SystemAction
import org.junit.Assert.assertSame
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.io.File

class BaselineRepositoryWiringTest {
    private class FailAlways : PersistenceFailureInjector {
        override fun beforeAtomicRename(target: File) = error("injected baseline persistence failure")
    }

    @Test
    fun hiltProviderRequiresAnInjectedBaselineRepository() {
        val provider = AppModule::class.java.declaredMethods.single {
            it.name == "provideOptimizationController"
        }
        assertTrue(provider.parameterTypes.contains(BaselineRepository::class.java))
    }

    @Test
    fun controllerAndExecutorUseTheExactSameDurableBaselineRepository() {
        val baseline = BaselineRepository(File(System.getProperty("java.io.tmpdir"), "smarthub_wiring_${System.nanoTime()}"), FailAlways())
        val service = mock(com.antigravity.smarthub.ISmartHubUserService::class.java)
        val ledger = ActionLedger()
        val executor = SystemActionExecutor(serviceProvider = { service }, safetyGovernor = SafetyGovernor(), baselineRepository = baseline, ownershipLedger = ledger)
        val controller = AppModule.provideOptimizationController(
            telemetryAggregator = TelemetryAggregator(),
            stateMachineEngine = StateMachineEngine(),
            profileResolver = ProfileResolver(),
            safetyGovernor = SafetyGovernor(),
            systemActionExecutor = executor,
            shizukuConnection = ShizukuServiceConnection(),
            actionLedger = ledger,
            settingsRepository = com.antigravity.smarthub.core.persistence.OptimizationSettingsRepository(),
            context = mock(android.content.Context::class.java),
            appClassifier = AppClassifier(),
            baselineRepository = baseline
        )

        val controllerRepoField = OptimizationController::class.java.getDeclaredField("baselineRepository").apply { isAccessible = true }
        val executorRepoField = SystemActionExecutor::class.java.getDeclaredField("baselineRepository").apply { isAccessible = true }
        assertSame(baseline, controllerRepoField.get(controller))
        assertSame(baseline, executorRepoField.get(executor))
    }

    @Test
    fun injectedBaselinePersistenceFailureBlocksPrivilegedExecutorMutation() {
        val baseline = BaselineRepository(File(System.getProperty("java.io.tmpdir"), "smarthub_wiring_failure_${System.nanoTime()}"), FailAlways())
        val service = mock(com.antigravity.smarthub.ISmartHubUserService::class.java)
        `when`(service.readSetting("secure", "refresh_rate_mode")).thenReturn("0")
        val executor = SystemActionExecutor(
            serviceProvider = { service },
            safetyGovernor = SafetyGovernor(),
            baselineRepository = baseline,
            ownershipLedger = ActionLedger()
        )

        val result = executor.executeTransaction(SystemAction.SetRefreshRate(1), DeviceState())

        assertFalse(result.success)
        verify(service, never()).setRefreshRateMode(anyInt())
    }
}
