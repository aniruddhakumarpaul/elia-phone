package com.antigravity.smarthub.core.model;

/**
 * High-Level Operating Profiles determined by ProfileResolver.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0002\b\r\b\u0086\u0081\u0002\u0018\u00002\b\u0012\u0004\u0012\u00020\u00000\u0001B\u0017\b\u0002\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006R\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0007\u0010\bR\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\t\u0010\nj\u0002\b\u000bj\u0002\b\fj\u0002\b\rj\u0002\b\u000ej\u0002\b\u000fj\u0002\b\u0010j\u0002\b\u0011\u00a8\u0006\u0012"}, d2 = {"Lcom/antigravity/smarthub/core/model/SmartHubProfile;", "", "priority", "", "displayName", "", "(Ljava/lang/String;IILjava/lang/String;)V", "getDisplayName", "()Ljava/lang/String;", "getPriority", "()I", "P0_THERMAL_EMERGENCY", "P1_CRITICAL_BATTERY", "P2_CHARGING_THERMAL_GUARD", "P3_GAMING_HIGH_LOAD", "P4_MEDIA_READING", "P5_DAILY_ADAPTIVE", "P6_OVERNIGHT_DEEP_IDLE", "app_debug"})
public enum SmartHubProfile {
    /*public static final*/ P0_THERMAL_EMERGENCY /* = new P0_THERMAL_EMERGENCY(0, null) */,
    /*public static final*/ P1_CRITICAL_BATTERY /* = new P1_CRITICAL_BATTERY(0, null) */,
    /*public static final*/ P2_CHARGING_THERMAL_GUARD /* = new P2_CHARGING_THERMAL_GUARD(0, null) */,
    /*public static final*/ P3_GAMING_HIGH_LOAD /* = new P3_GAMING_HIGH_LOAD(0, null) */,
    /*public static final*/ P4_MEDIA_READING /* = new P4_MEDIA_READING(0, null) */,
    /*public static final*/ P5_DAILY_ADAPTIVE /* = new P5_DAILY_ADAPTIVE(0, null) */,
    /*public static final*/ P6_OVERNIGHT_DEEP_IDLE /* = new P6_OVERNIGHT_DEEP_IDLE(0, null) */;
    private final int priority = 0;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String displayName = null;
    
    SmartHubProfile(int priority, java.lang.String displayName) {
    }
    
    public final int getPriority() {
        return 0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getDisplayName() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public static kotlin.enums.EnumEntries<com.antigravity.smarthub.core.model.SmartHubProfile> getEntries() {
        return null;
    }
}