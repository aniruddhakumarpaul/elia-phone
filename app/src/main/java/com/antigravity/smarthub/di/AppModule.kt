package com.antigravity.smarthub.di

import android.content.Context
import android.os.PowerManager
import com.antigravity.smarthub.core.persistence.BaselineRepository
import com.antigravity.smarthub.core.safety.AppClassifier
import com.antigravity.smarthub.core.safety.SafetyGovernor
import com.antigravity.smarthub.core.state.ActionLedger
import com.antigravity.smarthub.core.state.OptimizationController
import com.antigravity.smarthub.core.state.ProfileResolver
import com.antigravity.smarthub.core.state.StateMachineEngine
import com.antigravity.smarthub.core.telemetry.*
import com.antigravity.smarthub.platform.shizuku.ShizukuServiceConnection
import com.antigravity.smarthub.platform.shizuku.SystemActionExecutor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
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
    fun provideAppClassifier(): AppClassifier = AppClassifier()

    @Provides
    @Singleton
    fun provideSafetyGovernor(appClassifier: AppClassifier): SafetyGovernor = SafetyGovernor(appClassifier)

    @Provides
    @Singleton
    fun provideStateMachineEngine(): StateMachineEngine = StateMachineEngine()

    @Provides
    @Singleton
    fun provideProfileResolver(): ProfileResolver = ProfileResolver()

    @Provides
    @Singleton
    fun provideShizukuServiceConnection(): ShizukuServiceConnection = ShizukuServiceConnection()

    @Provides
    @Singleton
    fun provideActionLedger(): ActionLedger = ActionLedger()

    @Provides
    @Singleton
    fun provideSystemActionExecutor(
        connection: ShizukuServiceConnection,
        safetyGovernor: SafetyGovernor,
        baselineRepository: BaselineRepository,
        displayObserver: DisplayTelemetryObserver
    ): SystemActionExecutor {
        return SystemActionExecutor(
            connection,
            safetyGovernor,
            baselineRepository,
            effectiveRefreshRateReader = {
                displayObserver.getDisplayMetrics().value?.physicalRefreshRateHz?.value
            },
            stabilizationDelayMs = 750L
        )
    }

    @Provides
    @Singleton
    fun provideDisplayTelemetryObserver(@ApplicationContext context: Context): DisplayTelemetryObserver =
        DisplayTelemetryObserver(context)

    @Provides
    @Singleton
    fun provideTelemetryAggregator(
        @ApplicationContext context: Context,
        displayObserver: DisplayTelemetryObserver
    ): TelemetryAggregator {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val executor = Executors.newSingleThreadExecutor()

        val cpuObserver = CpuTelemetryObserver()
        val memObserver = MemoryTelemetryObserver()
        val zramObserver = ZramTelemetryObserver()
        val batteryObserver = BatteryPowerObserver(context)
        val thermalObserver = ThermalHeadroomObserver(pm, executor)
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

    @Provides
    @Singleton
    fun provideOptimizationController(
        telemetryAggregator: TelemetryAggregator,
        stateMachineEngine: StateMachineEngine,
        profileResolver: ProfileResolver,
        safetyGovernor: SafetyGovernor,
        systemActionExecutor: SystemActionExecutor,
        shizukuConnection: ShizukuServiceConnection,
        actionLedger: ActionLedger
    ): OptimizationController {
        return OptimizationController(
            telemetryAggregator = telemetryAggregator,
            stateMachineEngine = stateMachineEngine,
            profileResolver = profileResolver,
            safetyGovernor = safetyGovernor,
            actionExecutor = systemActionExecutor,
            shizukuConnection = shizukuConnection,
            actionLedger = actionLedger
        )
    }
}
