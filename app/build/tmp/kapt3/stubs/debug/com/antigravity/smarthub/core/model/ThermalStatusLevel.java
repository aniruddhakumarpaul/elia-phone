package com.antigravity.smarthub.core.model;

/**
 * Normalized Thermal Status from PowerManager HAL 2.0 & Battery sensors.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0002\b\u0007\b\u0086\u0081\u0002\u0018\u00002\b\u0012\u0004\u0012\u00020\u00000\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002j\u0002\b\u0003j\u0002\b\u0004j\u0002\b\u0005j\u0002\b\u0006j\u0002\b\u0007\u00a8\u0006\b"}, d2 = {"Lcom/antigravity/smarthub/core/model/ThermalStatusLevel;", "", "(Ljava/lang/String;I)V", "NOMINAL", "WARM", "MODERATE", "SEVERE", "CRITICAL", "app_debug"})
public enum ThermalStatusLevel {
    /*public static final*/ NOMINAL /* = new NOMINAL() */,
    /*public static final*/ WARM /* = new WARM() */,
    /*public static final*/ MODERATE /* = new MODERATE() */,
    /*public static final*/ SEVERE /* = new SEVERE() */,
    /*public static final*/ CRITICAL /* = new CRITICAL() */;
    
    ThermalStatusLevel() {
    }
    
    @org.jetbrains.annotations.NotNull()
    public static kotlin.enums.EnumEntries<com.antigravity.smarthub.core.model.ThermalStatusLevel> getEntries() {
        return null;
    }
}