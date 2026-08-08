package com.antigravity.smarthub.di

import android.content.Context
import android.os.PowerManager
import com.antigravity.smarthub.core.persistence.BaselineRepository
import com.antigravity.smarthub.core.safety.SafetyGovernor
import com.antigravity.smarthub.core.state.StateMachineEngine
import com.antigravity.smarthub.core.telemetry.*
import com.antigravity.smarthub.platform.shizuku.ShizukuServiceConnection
import com.antigravity.smarthub.platform.shizuku.SystemActionExecutor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import java.util.concurrent.Executors
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideBaselineRepository(@ApplicationContext context: Context): BaselineRepository {
        val storageDir = context.filesDir
        return BaselineRepository(storageDir)
    }

    @Provides
    @Singleton
    fun provideSafetyGovernor(): SafetyGovernor = SafetyGovernor()

    @Provides
    @Singleton
    fun provideStateMachineEngine(): StateMachineEngine = StateMachineEngine()

    @Provides
    @Singleton
    fun provideShizukuServiceConnection(): ShizukuServiceConnection = ShizukuServiceConnection()

    @Provides
    @Singleton
    fun provideProfileResolver(): com.antigravity.smarthub.core.state.ProfileResolver = com.antigravity.smarthub.core.state.ProfileResolver()

    @Provides
    @Singleton
    fun provideSystemActionExecutor(
        connection: ShizukuServiceConnection,
        safetyGovernor: SafetyGovernor,
        baselineRepository: BaselineRepository
    ): SystemActionExecutor {
        return SystemActionExecutor(connection, safetyGovernor, baselineRepository)
    }

    @Provides
    @Singleton
    fun provideTelemetryAggregator(@ApplicationContext context: Context): TelemetryAggregator {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val executor = Executors.newSingleThreadExecutor()

        val cpuObserver = CpuTelemetryObserver()
        val memObserver = MemoryTelemetryObserver()
        val zramObserver = ZramTelemetryObserver()
        val batteryObserver = BatteryPowerObserver(context)
        val thermalObserver = ThermalHeadroomObserver(pm, executor)
        val displayObserver = DisplayTelemetryObserver(context)
        val appContextObserver = AppContextObserver(context)
        val mediaObserver = MediaContextObserver(context)
        val navObserver = NavigationContextObserver(appContextObserver)

        return TelemetryAggregator(
            cpuObserver = cpuObserver,
            memoryObserver = memObserver,
            zramObserver = zramObserver,
            batteryObserver = batteryObserver,
            thermalObserver = thermalObserver,
            displayObserver = displayObserver,
            appContextObserver = appContextObserver,
            mediaContextObserver = mediaObserver,
            navigationObserver = navObserver
        )
    }
}
