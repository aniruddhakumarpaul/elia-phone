package com.antigravity.smarthub.core.model;

/**
 * Base sealed interface for typed system actions.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000(\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\bv\u0018\u00002\u00020\u0001:\u0003\f\r\u000eR\u0012\u0010\u0002\u001a\u00020\u0003X\u00a6\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0004\u0010\u0005R\u0012\u0010\u0006\u001a\u00020\u0003X\u00a6\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0007\u0010\u0005R\u0012\u0010\b\u001a\u00020\tX\u00a6\u0004\u00a2\u0006\u0006\u001a\u0004\b\n\u0010\u000b\u0082\u0001\u0003\u000f\u0010\u0011\u00a8\u0006\u0012"}, d2 = {"Lcom/antigravity/smarthub/core/model/SystemAction;", "", "actionId", "", "getActionId", "()Ljava/lang/String;", "description", "getDescription", "requiredTier", "Lcom/antigravity/smarthub/core/model/PrivilegeTier;", "getRequiredTier", "()Lcom/antigravity/smarthub/core/model/PrivilegeTier;", "SetAppOpsBackground", "SetRefreshRate", "SetStandbyBucket", "Lcom/antigravity/smarthub/core/model/SystemAction$SetAppOpsBackground;", "Lcom/antigravity/smarthub/core/model/SystemAction$SetRefreshRate;", "Lcom/antigravity/smarthub/core/model/SystemAction$SetStandbyBucket;", "app_debug"})
public abstract interface SystemAction {
    
    @org.jetbrains.annotations.NotNull()
    public abstract java.lang.String getActionId();
    
    @org.jetbrains.annotations.NotNull()
    public abstract com.antigravity.smarthub.core.model.PrivilegeTier getRequiredTier();
    
    @org.jetbrains.annotations.NotNull()
    public abstract java.lang.String getDescription();
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000.\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u000b\n\u0002\b\n\n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B\u0015\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J\t\u0010\u0013\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u0014\u001a\u00020\u0005H\u00c6\u0003J\u001d\u0010\u0015\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u0005H\u00c6\u0001J\u0013\u0010\u0016\u001a\u00020\u00052\b\u0010\u0017\u001a\u0004\u0018\u00010\u0018H\u00d6\u0003J\t\u0010\u0019\u001a\u00020\u001aH\u00d6\u0001J\t\u0010\u001b\u001a\u00020\u0003H\u00d6\u0001R\u0014\u0010\u0007\u001a\u00020\u0003X\u0096\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\b\u0010\tR\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\n\u0010\u000bR\u0014\u0010\f\u001a\u00020\u0003X\u0096\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\r\u0010\tR\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\tR\u0014\u0010\u000f\u001a\u00020\u0010X\u0096\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0011\u0010\u0012\u00a8\u0006\u001c"}, d2 = {"Lcom/antigravity/smarthub/core/model/SystemAction$SetAppOpsBackground;", "Lcom/antigravity/smarthub/core/model/SystemAction;", "packageName", "", "allow", "", "(Ljava/lang/String;Z)V", "actionId", "getActionId", "()Ljava/lang/String;", "getAllow", "()Z", "description", "getDescription", "getPackageName", "requiredTier", "Lcom/antigravity/smarthub/core/model/PrivilegeTier;", "getRequiredTier", "()Lcom/antigravity/smarthub/core/model/PrivilegeTier;", "component1", "component2", "copy", "equals", "other", "", "hashCode", "", "toString", "app_debug"})
    public static final class SetAppOpsBackground implements com.antigravity.smarthub.core.model.SystemAction {
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String packageName = null;
        private final boolean allow = false;
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String actionId = null;
        @org.jetbrains.annotations.NotNull()
        private final com.antigravity.smarthub.core.model.PrivilegeTier requiredTier = com.antigravity.smarthub.core.model.PrivilegeTier.TIER_1_SHIZUKU;
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String description = null;
        
        public SetAppOpsBackground(@org.jetbrains.annotations.NotNull()
        java.lang.String packageName, boolean allow) {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getPackageName() {
            return null;
        }
        
        public final boolean getAllow() {
            return false;
        }
        
        @java.lang.Override()
        @org.jetbrains.annotations.NotNull()
        public java.lang.String getActionId() {
            return null;
        }
        
        @java.lang.Override()
        @org.jetbrains.annotations.NotNull()
        public com.antigravity.smarthub.core.model.PrivilegeTier getRequiredTier() {
            return null;
        }
        
        @java.lang.Override()
        @org.jetbrains.annotations.NotNull()
        public java.lang.String getDescription() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component1() {
            return null;
        }
        
        public final boolean component2() {
            return false;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.antigravity.smarthub.core.model.SystemAction.SetAppOpsBackground copy(@org.jetbrains.annotations.NotNull()
        java.lang.String packageName, boolean allow) {
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
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0002\b\u0003\b\u0086\b\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\t\u0010\u0011\u001a\u00020\u0003H\u00c6\u0003J\u0013\u0010\u0012\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\u0013\u001a\u00020\u00142\b\u0010\u0015\u001a\u0004\u0018\u00010\u0016H\u00d6\u0003J\t\u0010\u0017\u001a\u00020\u0003H\u00d6\u0001J\t\u0010\u0018\u001a\u00020\u0006H\u00d6\u0001R\u0014\u0010\u0005\u001a\u00020\u0006X\u0096\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0007\u0010\bR\u0014\u0010\t\u001a\u00020\u0006X\u0096\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\n\u0010\bR\u0014\u0010\u000b\u001a\u00020\fX\u0096\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\r\u0010\u000eR\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000f\u0010\u0010\u00a8\u0006\u0019"}, d2 = {"Lcom/antigravity/smarthub/core/model/SystemAction$SetRefreshRate;", "Lcom/antigravity/smarthub/core/model/SystemAction;", "targetMode", "", "(I)V", "actionId", "", "getActionId", "()Ljava/lang/String;", "description", "getDescription", "requiredTier", "Lcom/antigravity/smarthub/core/model/PrivilegeTier;", "getRequiredTier", "()Lcom/antigravity/smarthub/core/model/PrivilegeTier;", "getTargetMode", "()I", "component1", "copy", "equals", "", "other", "", "hashCode", "toString", "app_debug"})
    public static final class SetRefreshRate implements com.antigravity.smarthub.core.model.SystemAction {
        private final int targetMode = 0;
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String actionId = null;
        @org.jetbrains.annotations.NotNull()
        private final com.antigravity.smarthub.core.model.PrivilegeTier requiredTier = com.antigravity.smarthub.core.model.PrivilegeTier.TIER_1_SHIZUKU;
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String description = null;
        
        public SetRefreshRate(int targetMode) {
            super();
        }
        
        public final int getTargetMode() {
            return 0;
        }
        
        @java.lang.Override()
        @org.jetbrains.annotations.NotNull()
        public java.lang.String getActionId() {
            return null;
        }
        
        @java.lang.Override()
        @org.jetbrains.annotations.NotNull()
        public com.antigravity.smarthub.core.model.PrivilegeTier getRequiredTier() {
            return null;
        }
        
        @java.lang.Override()
        @org.jetbrains.annotations.NotNull()
        public java.lang.String getDescription() {
            return null;
        }
        
        public final int component1() {
            return 0;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.antigravity.smarthub.core.model.SystemAction.SetRefreshRate copy(int targetMode) {
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
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000.\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\t\n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B\u0015\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0005J\t\u0010\u0011\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u0012\u001a\u00020\u0003H\u00c6\u0003J\u001d\u0010\u0013\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\u0014\u001a\u00020\u00152\b\u0010\u0016\u001a\u0004\u0018\u00010\u0017H\u00d6\u0003J\t\u0010\u0018\u001a\u00020\u0019H\u00d6\u0001J\t\u0010\u001a\u001a\u00020\u0003H\u00d6\u0001R\u0014\u0010\u0006\u001a\u00020\u0003X\u0096\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0007\u0010\bR\u0014\u0010\t\u001a\u00020\u0003X\u0096\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\n\u0010\bR\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\bR\u0014\u0010\f\u001a\u00020\rX\u0096\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\u000fR\u0011\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0010\u0010\b\u00a8\u0006\u001b"}, d2 = {"Lcom/antigravity/smarthub/core/model/SystemAction$SetStandbyBucket;", "Lcom/antigravity/smarthub/core/model/SystemAction;", "packageName", "", "targetBucket", "(Ljava/lang/String;Ljava/lang/String;)V", "actionId", "getActionId", "()Ljava/lang/String;", "description", "getDescription", "getPackageName", "requiredTier", "Lcom/antigravity/smarthub/core/model/PrivilegeTier;", "getRequiredTier", "()Lcom/antigravity/smarthub/core/model/PrivilegeTier;", "getTargetBucket", "component1", "component2", "copy", "equals", "", "other", "", "hashCode", "", "toString", "app_debug"})
    public static final class SetStandbyBucket implements com.antigravity.smarthub.core.model.SystemAction {
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String packageName = null;
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String targetBucket = null;
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String actionId = null;
        @org.jetbrains.annotations.NotNull()
        private final com.antigravity.smarthub.core.model.PrivilegeTier requiredTier = com.antigravity.smarthub.core.model.PrivilegeTier.TIER_1_SHIZUKU;
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String description = null;
        
        public SetStandbyBucket(@org.jetbrains.annotations.NotNull()
        java.lang.String packageName, @org.jetbrains.annotations.NotNull()
        java.lang.String targetBucket) {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getPackageName() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getTargetBucket() {
            return null;
        }
        
        @java.lang.Override()
        @org.jetbrains.annotations.NotNull()
        public java.lang.String getActionId() {
            return null;
        }
        
        @java.lang.Override()
        @org.jetbrains.annotations.NotNull()
        public com.antigravity.smarthub.core.model.PrivilegeTier getRequiredTier() {
            return null;
        }
        
        @java.lang.Override()
        @org.jetbrains.annotations.NotNull()
        public java.lang.String getDescription() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component1() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component2() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.antigravity.smarthub.core.model.SystemAction.SetStandbyBucket copy(@org.jetbrains.annotations.NotNull()
        java.lang.String packageName, @org.jetbrains.annotations.NotNull()
        java.lang.String targetBucket) {
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
}