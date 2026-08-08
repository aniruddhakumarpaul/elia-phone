package com.antigravity.smarthub.core.persistence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class BaselineRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testPreservesFirstOriginalValueOnly() {
        val repo = BaselineRepository(tempFolder.newFolder())

        // Save original baseline
        repo.saveSettingBaselineOnce("secure", "refresh_rate_mode", "0")
        assertEquals("0", repo.getSettingBaseline("secure", "refresh_rate_mode"))

        // Subsequent profile transition attempts to save new state "1" - should be IGNORED!
        repo.saveSettingBaselineOnce("secure", "refresh_rate_mode", "1")
        assertEquals("0", repo.getSettingBaseline("secure", "refresh_rate_mode"))
    }

    @Test
    fun testSurvivesProcessDeathAndReloadsFromStorage() {
        val folder = tempFolder.newFolder()

        // Phase 1: First Process Instance
        val repo1 = BaselineRepository(folder)
        repo1.saveSettingBaselineOnce("secure", "refresh_rate_mode", "0")
        repo1.saveStandbyBucketBaselineOnce("com.sec.android.app.launcher", "active")
        repo1.saveAppOpsBaselineOnce("com.whatsapp", "allow")

        // Phase 2: Process Death & Re-instantiation (Simulating reboot / new process)
        val repo2 = BaselineRepository(folder)
        assertEquals("0", repo2.getSettingBaseline("secure", "refresh_rate_mode"))
        assertEquals("active", repo2.getStandbyBucketBaseline("com.sec.android.app.launcher"))
        assertEquals("allow", repo2.getAppOpsBaseline("com.whatsapp"))
    }

    @Test
    fun testUnsavedKeysReturnNull() {
        val repo = BaselineRepository(tempFolder.newFolder())
        assertNull(repo.getSettingBaseline("secure", "non_existent_key"))
    }
}
