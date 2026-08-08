package com.antigravity.smarthub.core.state

import com.antigravity.smarthub.ISmartHubUserService
import com.antigravity.smarthub.core.persistence.BaselineRepository
import com.antigravity.smarthub.core.safety.SafetyGovernor
import com.antigravity.smarthub.core.telemetry.TelemetryAggregator
import com.antigravity.smarthub.platform.shizuku.ShizukuServiceConnection
import com.antigravity.smarthub.platform.shizuku.ShizukuState
import com.antigravity.smarthub.platform.shizuku.SystemActionExecutor
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions

class OptimizationControllerReadinessTest {

    @Test
    fun shizukuConnectedBeforeFirstTrustworthyTelemetryCausesZeroMutations() = runTest {
        val connection = ShizukuServiceConnection()
        val stateField = ShizukuServiceConnection::class.java.getDeclaredField("_shizukuState")
        stateField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val stateFlow = stateField.get(connection) as kotlinx.coroutines.flow.MutableStateFlow<ShizukuState>
        stateFlow.value = ShizukuState.CONNECTED

        val service = mock(ISmartHubUserService::class.java)
        val controller = OptimizationController(
            telemetryAggregator = TelemetryAggregator(),
            stateMachineEngine = StateMachineEngine(),
            profileResolver = ProfileResolver(),
            safetyGovernor = SafetyGovernor(),
            actionExecutor = SystemActionExecutor(service, SafetyGovernor(), BaselineRepository()),
            shizukuConnection = connection,
            baselineRepository = BaselineRepository(),
            scope = this
        )

        controller.manualRefresh()
        testScheduler.advanceUntilIdle()

        verifyNoInteractions(service)
        assert(!controller.uiState.value.readiness.isReady)
    }
}
