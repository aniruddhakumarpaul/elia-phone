package com.antigravity.smarthub.core.telemetry;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000 \n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0007\n\u0002\b\u0002\u0018\u00002\u00020\u0001B\u000f\u0012\b\u0010\u0002\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\u0002\u0010\u0004J\u0016\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\bR\u0010\u0010\u0002\u001a\u0004\u0018\u00010\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\n"}, d2 = {"Lcom/antigravity/smarthub/core/telemetry/ThermalHeadroomObserver;", "", "powerManager", "Landroid/os/PowerManager;", "(Landroid/os/PowerManager;)V", "calculateThermalRisk", "Lcom/antigravity/smarthub/core/telemetry/ThermalRiskState;", "batteryTempC", "", "apTempC", "app_debug"})
public final class ThermalHeadroomObserver {
    @org.jetbrains.annotations.Nullable()
    private final android.os.PowerManager powerManager = null;
    
    public ThermalHeadroomObserver(@org.jetbrains.annotations.Nullable()
    android.os.PowerManager powerManager) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.antigravity.smarthub.core.telemetry.ThermalRiskState calculateThermalRisk(float batteryTempC, float apTempC) {
        return null;
    }
}