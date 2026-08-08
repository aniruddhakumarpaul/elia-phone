package com.antigravity.smarthub.core.persistence

import com.antigravity.smarthub.core.model.CapabilityResult
import com.antigravity.smarthub.core.model.SmartHubProfile
import com.antigravity.smarthub.core.model.SystemAction
import com.antigravity.smarthub.core.state.ActionLedger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class RuntimePersistenceTest {
    @Test
    fun settingsRoundTripAndFailSafeCorruptState() {
        val file = File.createTempFile("smarthub-settings", ".properties")
        file.deleteOnExit()
        val first = OptimizationSettingsRepository(file)
        first.setOptimizationEnabled(true)
        first.setManualProfile(SmartHubProfile.P4_MEDIA_READING)

        val restored = OptimizationSettingsRepository(file)
        assertTrue(restored.isOptimizationEnabled())
        assertFalse(restored.isAutomaticMode())
        assertEquals(SmartHubProfile.P4_MEDIA_READING, restored.getManualProfile())

        file.writeText("optimizationEnabled=not-a-boolean\nautomaticMode=true\n")
        val corrupt = OptimizationSettingsRepository(file)
        assertTrue(corrupt.isCorrupt())
        assertFalse(corrupt.isOptimizationEnabled())
        assertTrue(corrupt.isAutomaticMode())
    }

    @Test
    fun ownershipRoundTripAndVerificationPersist() {
        val file = File.createTempFile("smarthub-ownership", ".properties")
        file.deleteOnExit()
        val action = SystemAction.SetRefreshRate(1)
        val first = ActionLedger(file)
        first.recordAppliedAction(action, "1", CapabilityResult.SUPPORTED)

        val restored = ActionLedger(file)
        val key = restored.getActionKey(action)
        assertEquals(action, restored.getCurrentlyAppliedActions()[key])
        assertEquals("1", restored.getLastVerifiedValue(key))
        restored.recordVerification(key, "1")
        assertEquals("1", ActionLedger(file).getLastVerifiedValue(key))
    }

    @Test
    fun corruptOwnershipFailsClosed() {
        val file = File.createTempFile("smarthub-ownership-corrupt", ".properties")
        file.deleteOnExit()
        file.writeText("action.bad.spec=not-a-valid-action\n")
        val ledger = ActionLedger(file)
        assertTrue(ledger.persistenceCorrupt)
        assertTrue(ledger.getCurrentlyAppliedActions().isEmpty())
    }
}
