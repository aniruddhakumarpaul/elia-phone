package com.antigravity.smarthub.platform.shizuku

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import com.antigravity.smarthub.ISmartHubUserService
import rikka.shizuku.Shizuku

/**
 * Manages Shizuku UserService lifecycle, binder death recipient, and auto-rebind.
 */
class ShizukuServiceConnection : ServiceConnection, IBinder.DeathRecipient {

    var userService: ISmartHubUserService? = null
        private set

    var isConnected: Boolean = false
        private set

    val isBound: Boolean
        get() = isConnected && userService != null

    private val userServiceArgs = Shizuku.UserServiceArgs(
        ComponentName("com.antigravity.smarthub", SmartHubUserService::class.java.name)
    )
        .daemon(false)
        .processNameSuffix("smarthub_user_service")
        .version(1)

    fun bind() {
        try {
            if (Shizuku.pingBinder() && Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Shizuku.bindUserService(userServiceArgs, this)
            }
        } catch (e: Exception) {
            isConnected = false
            userService = null
        }
    }

    fun unbind() {
        try {
            Shizuku.unbindUserService(userServiceArgs, this, true)
        } catch (e: Exception) {
            // Ignore unbind exception on teardown
        } finally {
            isConnected = false
            userService = null
        }
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        if (service != null) {
            try {
                service.linkToDeath(this, 0)
                userService = ISmartHubUserService.Stub.asInterface(service)
                isConnected = true
            } catch (e: Exception) {
                isConnected = false
                userService = null
            }
        }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        isConnected = false
        userService = null
        // Auto-rebind attempt
        bind()
    }

    override fun binderDied() {
        isConnected = false
        userService = null
        // Auto-rebind on binder death
        bind()
    }
}
