package com.antigravity.smarthub.core.model;

/**
 * Real-time aggregated Device Telemetry State.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000<\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0007\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\t\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\"\b\u0086\b\u0018\u00002\u00020\u0001Bs\u0012\b\b\u0002\u0010\u0002\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0004\u001a\u00020\u0005\u0012\b\b\u0002\u0010\u0006\u001a\u00020\u0007\u0012\b\b\u0002\u0010\b\u001a\u00020\u0007\u0012\b\b\u0002\u0010\t\u001a\u00020\n\u0012\b\b\u0002\u0010\u000b\u001a\u00020\f\u0012\b\b\u0002\u0010\r\u001a\u00020\u0007\u0012\b\b\u0002\u0010\u000e\u001a\u00020\u0005\u0012\b\b\u0002\u0010\u000f\u001a\u00020\u0010\u0012\b\b\u0002\u0010\u0011\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0012\u001a\u00020\u0013\u00a2\u0006\u0002\u0010\u0014J\t\u0010%\u001a\u00020\u0003H\u00c6\u0003J\t\u0010&\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\'\u001a\u00020\u0013H\u00c6\u0003J\t\u0010(\u001a\u00020\u0005H\u00c6\u0003J\t\u0010)\u001a\u00020\u0007H\u00c6\u0003J\t\u0010*\u001a\u00020\u0007H\u00c6\u0003J\t\u0010+\u001a\u00020\nH\u00c6\u0003J\t\u0010,\u001a\u00020\fH\u00c6\u0003J\t\u0010-\u001a\u00020\u0007H\u00c6\u0003J\t\u0010.\u001a\u00020\u0005H\u00c6\u0003J\t\u0010/\u001a\u00020\u0010H\u00c6\u0003Jw\u00100\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00052\b\b\u0002\u0010\u0006\u001a\u00020\u00072\b\b\u0002\u0010\b\u001a\u00020\u00072\b\b\u0002\u0010\t\u001a\u00020\n2\b\b\u0002\u0010\u000b\u001a\u00020\f2\b\b\u0002\u0010\r\u001a\u00020\u00072\b\b\u0002\u0010\u000e\u001a\u00020\u00052\b\b\u0002\u0010\u000f\u001a\u00020\u00102\b\b\u0002\u0010\u0011\u001a\u00020\u00032\b\b\u0002\u0010\u0012\u001a\u00020\u0013H\u00c6\u0001J\u0013\u00101\u001a\u00020\u00052\b\u00102\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u00103\u001a\u00020\u0003H\u00d6\u0001J\t\u00104\u001a\u00020\u0010H\u00d6\u0001R\u0011\u0010\u0011\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0015\u0010\u0016R\u0011\u0010\b\u001a\u00020\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0017\u0010\u0018R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0019\u0010\u0016R\u0011\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001a\u0010\u0018R\u0011\u0010\u000f\u001a\u00020\u0010\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001b\u0010\u001cR\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0004\u0010\u001dR\u0011\u0010\u000e\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\u001dR\u0011\u0010\u000b\u001a\u00020\f\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001e\u0010\u001fR\u0011\u0010\r\u001a\u00020\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b \u0010\u0018R\u0011\u0010\u0012\u001a\u00020\u0013\u00a2\u0006\b\n\u0000\u001a\u0004\b!\u0010\"R\u0011\u0010\t\u001a\u00020\n\u00a2\u0006\b\n\u0000\u001a\u0004\b#\u0010$\u00a8\u00065"}, d2 = {"Lcom/antigravity/smarthub/core/model/DeviceState;", "", "batteryPercent", "", "isCharging", "", "batteryTempC", "", "apTempC", "thermalStatus", "Lcom/antigravity/smarthub/core/model/ThermalStatusLevel;", "memoryAvailableMb", "", "memoryPsiAvg10", "isScreenOn", "foregroundPackage", "", "activeRefreshRateMode", "privilegeTier", "Lcom/antigravity/smarthub/core/model/PrivilegeTier;", "(IZFFLcom/antigravity/smarthub/core/model/ThermalStatusLevel;JFZLjava/lang/String;ILcom/antigravity/smarthub/core/model/PrivilegeTier;)V", "getActiveRefreshRateMode", "()I", "getApTempC", "()F", "getBatteryPercent", "getBatteryTempC", "getForegroundPackage", "()Ljava/lang/String;", "()Z", "getMemoryAvailableMb", "()J", "getMemoryPsiAvg10", "getPrivilegeTier", "()Lcom/antigravity/smarthub/core/model/PrivilegeTier;", "getThermalStatus", "()Lcom/antigravity/smarthub/core/model/ThermalStatusLevel;", "component1", "component10", "component11", "component2", "component3", "component4", "component5", "component6", "component7", "component8", "component9", "copy", "equals", "other", "hashCode", "toString", "app_debug"})
public final class DeviceState {
    private final int batteryPercent = 0;
    private final boolean isCharging = false;
    private final float batteryTempC = 0.0F;
    private final float apTempC = 0.0F;
    @org.jetbrains.annotations.NotNull()
    private final com.antigravity.smarthub.core.model.ThermalStatusLevel thermalStatus = null;
    private final long memoryAvailableMb = 0L;
    private final float memoryPsiAvg10 = 0.0F;
    private final boolean isScreenOn = false;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String foregroundPackage = null;
    private final int activeRefreshRateMode = 0;
    @org.jetbrains.annotations.NotNull()
    private final com.antigravity.smarthub.core.model.PrivilegeTier privilegeTier = null;
    
    public DeviceState(int batteryPercent, boolean isCharging, float batteryTempC, float apTempC, @org.jetbrains.annotations.NotNull()
    com.antigravity.smarthub.core.model.ThermalStatusLevel thermalStatus, long memoryAvailableMb, float memoryPsiAvg10, boolean isScreenOn, @org.jetbrains.annotations.NotNull()
    java.lang.String foregroundPackage, int activeRefreshRateMode, @org.jetbrains.annotations.NotNull()
    com.antigravity.smarthub.core.model.PrivilegeTier privilegeTier) {
        super();
    }
    
    public final int getBatteryPercent() {
        return 0;
    }
    
    public final boolean isCharging() {
        return false;
    }
    
    public final float getBatteryTempC() {
        return 0.0F;
    }
    
    public final float getApTempC() {
        return 0.0F;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.antigravity.smarthub.core.model.ThermalStatusLevel getThermalStatus() {
        return null;
    }
    
    public final long getMemoryAvailableMb() {
        return 0L;
    }
    
    public final float getMemoryPsiAvg10() {
        return 0.0F;
    }
    
    public final boolean isScreenOn() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getForegroundPackage() {
        return null;
    }
    
    public final int getActiveRefreshRateMode() {
        return 0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.antigravity.smarthub.core.model.PrivilegeTier getPrivilegeTier() {
        return null;
    }
    
    public DeviceState() {
        super();
    }
    
    public final int component1() {
        return 0;
    }
    
    public final int component10() {
        return 0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.antigravity.smarthub.core.model.PrivilegeTier component11() {
        return null;
    }
    
    public final boolean component2() {
        return false;
    }
    
    public final float component3() {
        return 0.0F;
    }
    
    public final float component4() {
        return 0.0F;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.antigravity.smarthub.core.model.ThermalStatusLevel component5() {
        return null;
    }
    
    public final long component6() {
        return 0L;
    }
    
    public final float component7() {
        return 0.0F;
    }
    
    public final boolean component8() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component9() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.antigravity.smarthub.core.model.DeviceState copy(int batteryPercent, boolean isCharging, float batteryTempC, float apTempC, @org.jetbrains.annotations.NotNull()
    com.antigravity.smarthub.core.model.ThermalStatusLevel thermalStatus, long memoryAvailableMb, float memoryPsiAvg10, boolean isScreenOn, @org.jetbrains.annotations.NotNull()
    java.lang.String foregroundPackage, int activeRefreshRateMode, @org.jetbrains.annotations.NotNull()
    com.antigravity.smarthub.core.model.PrivilegeTier privilegeTier) {
        return null;
    }
    
    @java.lang.Override()
    public boolean equals(@org.jetbrains.annotations.Nullable()
    java.lang.Object other) {
        return false;
    }
    
    @java.lang.Override()
    public int hashCode() {
        return 0;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public java.lang.String toString() {
        return null;
    }
}