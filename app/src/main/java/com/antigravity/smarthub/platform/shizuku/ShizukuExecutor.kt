package com.antigravity.smarthub.platform.shizuku

import com.antigravity.smarthub.core.model.SystemAction
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

class ShizukuExecutor {

    fun isShizukuAvailable(): Boolean {
        return try {
            Shizuku.pingBinder() && Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
        }
    }

    fun executeAction(action: SystemAction): Boolean {
        return when (action) {
            is SystemAction.SetRefreshRate -> {
                executeShellCommand("settings put secure refresh_rate_mode ${action.targetMode}")
            }
            is SystemAction.SetStandbyBucket -> {
                executeShellCommand("am set-standby-bucket ${action.packageName} ${action.targetBucket}")
            }
            is SystemAction.SetAppOpsBackground -> {
                val mode = if (action.allow) "allow" else "ignore"
                executeShellCommand("cmd appops set ${action.packageName} RUN_ANY_IN_BACKGROUND $mode")
            }
        }
    }

    private fun executeShellCommand(command: String): Boolean {
        if (!isShizukuAvailable()) return false
        return try {
            val process = Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (e: Exception) {
            false
        }
    }

    fun readSetting(table: String, key: String): String? {
        if (!isShizukuAvailable()) return null
        return try {
            val process = Shizuku.newProcess(arrayOf("sh", "-c", "settings get $table $key"), null, null)
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val result = reader.readLine()?.trim()
            process.waitFor()
            result
        } catch (e: Exception) {
            null
        }
    }
}
