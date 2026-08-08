package com.antigravity.smarthub

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import com.antigravity.smarthub.core.state.OptimizationController
import javax.inject.Inject

@HiltAndroidApp
class SmartHubApp : Application() {
    @Inject lateinit var optimizationController: OptimizationController

    override fun onCreate() {
        super.onCreate()
        optimizationController.initialize()
    }
}
