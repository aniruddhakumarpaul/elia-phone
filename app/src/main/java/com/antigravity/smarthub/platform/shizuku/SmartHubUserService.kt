package com.antigravity.smarthub.platform.shizuku

import com.antigravity.smarthub.ISmartHubUserService
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.system.exitProcess

/**
 * Shizuku UserService implementation executing in privileged process context.
 * Replaces deprecated Shizuku.newProcess() architecture.
 */
class SmartHubUserService : ISmartHubUserService.Stub() {

    override fun executeShellCommand(command: String?): Int {
        if (command.isNullOrBlank()) return -1
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            process.waitFor()
        } catch (e: Exception) {
            -1
        }
    }

    override fun readSetting(table: String?, key: String?): String? {
        if (table.isNullOrBlank() || key.isNullOrBlank()) return null
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", "settings get $table $key"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readLine()?.trim()
            process.waitFor()
            output
        } catch (e: Exception) {
            null
        }
    }

    override fun destroy() {
        exitProcess(0)
    }
}
