package com.antigravity.smarthub.platform.shizuku

import com.antigravity.smarthub.ISmartHubUserService
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.system.exitProcess

/**
 * Shizuku UserService implementation executing in privileged process context.
 * Implements typed, allowlisted system commands only.
 */
class SmartHubUserService : ISmartHubUserService.Stub() {

    override fun setRefreshRateMode(mode: Int): Int {
        if (mode != 0 && mode != 1) return -1
        return runCommand(arrayOf("sh", "-c", "settings put secure refresh_rate_mode $mode"))
    }

    override fun setStandbyBucket(packageName: String?, bucket: String?): Int {
        if (packageName.isNullOrBlank() || bucket.isNullOrBlank()) return -1
        val validBuckets = setOf("exempted", "active", "working_set", "frequent", "rare", "restricted")
        if (!validBuckets.contains(bucket)) return -1
        return runCommand(arrayOf("sh", "-c", "am set-standby-bucket $packageName $bucket"))
    }

    override fun setAppOpsBackground(packageName: String?, mode: String?): Int {
        if (packageName.isNullOrBlank() || mode.isNullOrBlank()) return -1
        val validModes = setOf("allow", "ignore", "deny", "default", "errored", "foreground")
        if (!validModes.contains(mode)) return -1
        return runCommand(arrayOf("sh", "-c", "cmd appops set $packageName RUN_ANY_IN_BACKGROUND $mode"))
    }

    override fun readSetting(table: String?, key: String?): String? {
        if (table.isNullOrBlank() || key.isNullOrBlank()) return null
        return runOutputCommand(arrayOf("sh", "-c", "settings get $table $key"))
    }

    override fun readStandbyBucket(packageName: String?): Int {
        if (packageName.isNullOrBlank()) return -1
        val output = runOutputCommand(arrayOf("sh", "-c", "am get-standby-bucket $packageName"))
        return output?.toIntOrNull() ?: -1
    }

    override fun readAppOpsBackground(packageName: String?): String? {
        if (packageName.isNullOrBlank()) return null
        return runOutputCommand(arrayOf("sh", "-c", "cmd appops get $packageName RUN_ANY_IN_BACKGROUND"))
    }

    override fun destroy() {
        exitProcess(0)
    }

    private fun runCommand(cmd: Array<String>): Int {
        return try {
            val process = Runtime.getRuntime().exec(cmd)
            process.waitFor()
        } catch (e: Exception) {
            -1
        }
    }

    private fun runOutputCommand(cmd: Array<String>): String? {
        return try {
            val process = Runtime.getRuntime().exec(cmd)
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readLine()?.trim()
            process.waitFor()
            output
        } catch (e: Exception) {
            null
        }
    }
}
