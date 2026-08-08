package com.antigravity.smarthub.core.persistence

import com.antigravity.smarthub.ISmartHubUserService
import com.antigravity.smarthub.core.model.DeviceState
import com.antigravity.smarthub.core.model.SystemAction
import com.antigravity.smarthub.core.safety.SafetyGovernor
import com.antigravity.smarthub.core.state.ActionLedger
import com.antigravity.smarthub.core.state.OwnershipJournalState
import com.antigravity.smarthub.platform.shizuku.SystemActionExecutor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.io.File

class TransactionalPersistenceFaultTest {
    private class FailAlways : PersistenceFailureInjector {
        override fun beforeAtomicRename(target: File) = error("injected failure")
    }

    private class FailOnCall(private val failingCall: Int) : PersistenceFailureInjector {
        var calls = 0
        override fun beforeAtomicRename(target: File) {
            calls++
            if (calls == failingCall) error("injected failure on call $calls")
        }
    }

    private fun tempDir(name: String): File = File(System.getProperty("java.io.tmpdir"), "smarthub_${name}_${System.nanoTime()}").also { it.mkdirs() }

    @Test
    fun baselineFileFailureCausesZeroDeviceMutation() {
        val service = mock(ISmartHubUserService::class.java)
        `when`(service.readSetting("secure", "refresh_rate_mode")).thenReturn("0")
        val executor = SystemActionExecutor(
            serviceProvider = { service },
            safetyGovernor = SafetyGovernor(),
            baselineRepository = BaselineRepository(tempDir("baseline_fail"), FailAlways()),
            ownershipLedger = ActionLedger(tempDir("ledger_ok"))
        )

        val result = executor.executeTransaction(SystemAction.SetRefreshRate(1), DeviceState())

        assertFalse(result.success)
        verify(service, never()).setRefreshRateMode(anyInt())
    }

    @Test
    fun ownershipWriteFailureBeforeMutationCausesZeroDeviceMutation() {
        val service = mock(ISmartHubUserService::class.java)
        `when`(service.readSetting("secure", "refresh_rate_mode")).thenReturn("0")
        val executor = SystemActionExecutor(
            serviceProvider = { service },
            safetyGovernor = SafetyGovernor(),
            baselineRepository = BaselineRepository(tempDir("ownership_before_baseline")),
            ownershipLedger = ActionLedger(tempDir("ownership_before_ledger"), FailAlways())
        )

        val result = executor.executeTransaction(SystemAction.SetRefreshRate(1), DeviceState())

        assertFalse(result.success)
        verify(service, never()).setRefreshRateMode(anyInt())
    }

    @Test
    fun ownershipWriteFailureAfterMutationImmediatelyRestoresAndKeepsPendingKnowledge() {
        val service = mock(ISmartHubUserService::class.java)
        `when`(service.readSetting("secure", "refresh_rate_mode")).thenReturn("0", "1", "0")
        `when`(service.setRefreshRateMode(anyInt())).thenReturn(0)
        val baselineDir = tempDir("ownership_after_baseline")
        val ledgerDir = tempDir("ownership_after_ledger")
        val ledgerFile = File(ledgerDir, "smart_hub_action_ownership.properties")
        val ledger = ActionLedger(ledgerFile, FailOnCall(2))
        val executor = SystemActionExecutor(
            serviceProvider = { service },
            safetyGovernor = SafetyGovernor(),
            baselineRepository = BaselineRepository(baselineDir),
            ownershipLedger = ledger
        )

        val result = executor.executeTransaction(SystemAction.SetRefreshRate(1), DeviceState())

        assertFalse(result.success)
        assertTrue(result.rolledBack)
        verify(service).setRefreshRateMode(1)
        verify(service).setRefreshRateMode(0)
        assertEquals(OwnershipJournalState.PENDING, ActionLedger(ledgerFile).getJournalState(ActionLedger.REFRESH_RATE_KEY))
    }

    @Test
    fun corruptBaselineAndTruncatedOrInvalidOwnershipFilesFailClosed() {
        val baselineDir = tempDir("corrupt_baseline")
        File(baselineDir, "smart_hub_baselines.properties").writeText("not=valid\n")
        val baseline = BaselineRepository(baselineDir)
        assertTrue(baseline.persistenceCorrupt)
        assertNull(baseline.getSettingBaseline("secure", "refresh_rate_mode"))

        val truncatedDir = tempDir("truncated_ledger")
        File(truncatedDir, "smart_hub_action_ownership.properties").writeText("formatVersion=1\naction.bad.state=PENDING\n")
        assertTrue(ActionLedger(File(truncatedDir, "smart_hub_action_ownership.properties")).persistenceCorrupt)

        val invalidDir = tempDir("invalid_ledger")
        File(invalidDir, "smart_hub_action_ownership.properties").writeText(
            "formatVersion=1\naction.UkV felt.spec=not-an-action\naction.UkV felt.state=PENDING\naction.UkV felt.lastAttempt=1\n"
                .replace(" ", "")
        )
        assertTrue(ActionLedger(File(invalidDir, "smart_hub_action_ownership.properties")).persistenceCorrupt)
    }

    @Test
    fun pendingAppliedAndRestorePendingJournalStatesSurviveRestart() {
        val dir = tempDir("journal_states")
        val file = File(dir, "smart_hub_action_ownership.properties")
        val action = SystemAction.SetRefreshRate(1)
        val first = ActionLedger(file)
        assertTrue(first.recordPendingAction(action, "0"))
        assertEquals(OwnershipJournalState.PENDING, ActionLedger(file).getJournalState(ActionLedger.REFRESH_RATE_KEY))
        assertTrue(first.recordAppliedAction(action, "1"))
        assertEquals(OwnershipJournalState.APPLIED, ActionLedger(file).getJournalState(ActionLedger.REFRESH_RATE_KEY))
        assertTrue(first.markRestorePending(ActionLedger.REFRESH_RATE_KEY))
        assertEquals(OwnershipJournalState.RESTORE_PENDING, ActionLedger(file).getJournalState(ActionLedger.REFRESH_RATE_KEY))
        assertTrue(first.recordRestoredAction(ActionLedger.REFRESH_RATE_KEY))
        assertEquals(OwnershipJournalState.RESTORED, ActionLedger(file).getJournalState(ActionLedger.REFRESH_RATE_KEY))
        assertTrue(ActionLedger(file).getCurrentlyAppliedActions().isEmpty())
    }

    @Test
    fun settingsWriteFailureDoesNotClaimNewState() {
        val file = File(tempDir("settings_fail"), "settings.properties")
        val settings = OptimizationSettingsRepository(file, FailAlways())
        assertFalse(settings.setOptimizationEnabled(true))
        assertFalse(settings.isOptimizationEnabled())
        assertTrue(settings.persistenceFailed)
    }
}
