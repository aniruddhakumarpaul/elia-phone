package com.antigravity.smarthub.core.state

import com.antigravity.smarthub.ISmartHubUserService
import com.antigravity.smarthub.core.persistence.BaselineRepository
import com.antigravity.smarthub.core.persistence.OptimizationSettingsRepository
import com.antigravity.smarthub.core.safety.SafetyGovernor
import com.antigravity.smarthub.core.telemetry.TelemetryAggregator
import com.antigravity.smarthub.platform.shizuku.SystemActionExecutor
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.io.File

class OwnershipReconciliationTest {
    private fun tempDir(name: String): File = File(System.getProperty("java.io.tmpdir"), "smarthub_reconcile_${name}_${System.nanoTime()}").also { it.mkdirs() }

    private fun buildController(
        service: ISmartHubUserService,
        ledger: ActionLedger,
        baseline: BaselineRepository,
        settings: OptimizationSettingsRepository,
        scope: kotlinx.coroutines.CoroutineScope
    ): OptimizationController = OptimizationController(
        telemetryAggregator = TelemetryAggregator(),
        stateMachineEngine = StateMachineEngine(),
        profileResolver = ProfileResolver(),
        safetyGovernor = SafetyGovernor(),
        actionExecutor = SystemActionExecutor(
            serviceProvider = { service },
            safetyGovernor = SafetyGovernor(),
            baselineRepository = baseline,
            ownershipLedger = ledger
        ),
        shizukuConnection = com.antigravity.smarthub.platform.shizuku.ShizukuServiceConnection(),
        actionLedger = ledger,
        settingsRepository = settings,
        baselineRepository = baseline,
        scope = scope
    )

    private fun prepareBaseline(dir: File): BaselineRepository = BaselineRepository(dir).also {
        assertTrue(it.saveSettingBaselineOnce("secure", "refresh_rate_mode", "0"))
    }

    @Test
    fun pendingRecordAfterProcessDeathIsRestoredBeforeAnyNewPolicy() = runTest {
        val baseDir = tempDir("pending_base")
        val ledgerDir = tempDir("pending_ledger")
        val base = prepareBaseline(baseDir)
        val action = com.antigravity.smarthub.core.model.SystemAction.SetRefreshRate(1)
        val oldLedger = ActionLedger(File(ledgerDir, "smart_hub_action_ownership.properties"))
        assertTrue(oldLedger.recordPendingAction(action, "0"))
        val ledger = ActionLedger(File(ledgerDir, "smart_hub_action_ownership.properties"))
        val service = mock(ISmartHubUserService::class.java)
        `when`(service.readSetting("secure", "refresh_rate_mode")).thenReturn("1", "0")
        `when`(service.setRefreshRateMode(0)).thenReturn(0)
        val controller = buildController(service, ledger, base, OptimizationSettingsRepository(), this)

        controller.manualRefresh()
        testScheduler.advanceUntilIdle()

        assertEquals(OwnershipJournalState.RESTORED, ledger.getJournalState(ActionLedger.REFRESH_RATE_KEY))
        verify(service).setRefreshRateMode(0)
        verify(service, never()).setRefreshRateMode(1)
    }

    @Test
    fun appliedRecordWithMatchingValueIsVerifiedWithoutReapplying() = runTest {
        val baseDir = tempDir("applied_base")
        val ledgerDir = tempDir("applied_ledger")
        val base = prepareBaseline(baseDir)
        val action = com.antigravity.smarthub.core.model.SystemAction.SetRefreshRate(1)
        val file = File(ledgerDir, "smart_hub_action_ownership.properties")
        val oldLedger = ActionLedger(file)
        assertTrue(oldLedger.recordPendingAction(action, "0"))
        assertTrue(oldLedger.recordAppliedAction(action, "1"))
        val ledger = ActionLedger(file)
        val settings = OptimizationSettingsRepository(File(tempDir("applied_settings"), "settings.properties"))
        assertTrue(settings.setOptimizationEnabled(true))
        val service = mock(ISmartHubUserService::class.java)
        `when`(service.readSetting("secure", "refresh_rate_mode")).thenReturn("1")
        val controller = buildController(service, ledger, base, settings, this)

        controller.manualRefresh()
        testScheduler.advanceUntilIdle()

        assertEquals(OwnershipJournalState.APPLIED, ledger.getJournalState(ActionLedger.REFRESH_RATE_KEY))
        verify(service, never()).setRefreshRateMode(anyInt())
    }

    @Test
    fun restorePendingRecordIsRestoredAndOnlyThenMarkedRestored() = runTest {
        val baseDir = tempDir("restore_pending_base")
        val ledgerDir = tempDir("restore_pending_ledger")
        val base = prepareBaseline(baseDir)
        val action = com.antigravity.smarthub.core.model.SystemAction.SetRefreshRate(1)
        val file = File(ledgerDir, "smart_hub_action_ownership.properties")
        val oldLedger = ActionLedger(file)
        assertTrue(oldLedger.recordPendingAction(action, "0"))
        assertTrue(oldLedger.recordAppliedAction(action, "1"))
        assertTrue(oldLedger.markRestorePending(ActionLedger.REFRESH_RATE_KEY))
        val ledger = ActionLedger(file)
        val service = mock(ISmartHubUserService::class.java)
        `when`(service.readSetting("secure", "refresh_rate_mode")).thenReturn("1", "0")
        `when`(service.setRefreshRateMode(0)).thenReturn(0)
        val controller = buildController(service, ledger, base, OptimizationSettingsRepository(), this)

        controller.manualRefresh()
        testScheduler.advanceUntilIdle()

        assertEquals(OwnershipJournalState.RESTORED, ledger.getJournalState(ActionLedger.REFRESH_RATE_KEY))
        verify(service).setRefreshRateMode(0)
    }
}
