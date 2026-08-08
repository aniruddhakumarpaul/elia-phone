package com.antigravity.smarthub.platform.shizuku

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import com.antigravity.smarthub.ISmartHubUserService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import rikka.shizuku.Shizuku

enum class ShizukuState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    PERMISSION_REQUIRED,
    DEAD
}

/**
 * Manages Shizuku UserService lifecycle, binder death recipient, and auto-rebind.
 * Exposes reactive StateFlow<ShizukuState> for OptimizationController and UI observation.
 */
class ShizukuServiceConnection : ServiceConnection, IBinder.DeathRecipient {

    var userService: ISmartHubUserService? = null
        private set

    private val _shizukuState = MutableStateFlow(ShizukuState.DISCONNECTED)
    val shizukuState: StateFlow<ShizukuState> = _shizukuState.asStateFlow()

    val isConnected: Boolean
        get() = _shizukuState.value == ShizukuState.CONNECTED

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
            if (!Shizuku.pingBinder()) {
                _shizukuState.value = ShizukuState.DISCONNECTED
                return
            }

            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                _shizukuState.value = ShizukuState.PERMISSION_REQUIRED
                return
            }

            _shizukuState.value = ShizukuState.CONNECTING
            Shizuku.bindUserService(userServiceArgs, this)
        } catch (e: Exception) {
            _shizukuState.value = ShizukuState.DISCONNECTED
            userService = null
        }
    }

    fun unbind() {
        try {
            Shizuku.unbindUserService(userServiceArgs, this, true)
        } catch (e: Exception) {
            // Ignore unbind exception on teardown
        } finally {
            _shizukuState.value = ShizukuState.DISCONNECTED
            userService = null
        }
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        if (service != null) {
            try {
                service.linkToDeath(this, 0)
                userService = ISmartHubUserService.Stub.asInterface(service)
                _shizukuState.value = ShizukuState.CONNECTED
            } catch (e: Exception) {
                _shizukuState.value = ShizukuState.DISCONNECTED
                userService = null
            }
        }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        _shizukuState.value = ShizukuState.DEAD
        userService = null
        // Auto-rebind attempt
        bind()
    }

    override fun binderDied() {
        _shizukuState.value = ShizukuState.DEAD
        userService = null
        // Auto-rebind on binder death
        bind()
    }
}
