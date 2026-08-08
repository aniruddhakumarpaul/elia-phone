package com.antigravity.smarthub.core.state;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000<\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\t\n\u0002\b\u0006\n\u0002\u0010\"\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J \u0010\u0011\u001a\u00020\u00122\u0006\u0010\u0013\u001a\u00020\u00042\u0006\u0010\u0014\u001a\u00020\u00152\u0006\u0010\u0016\u001a\u00020\u0017H\u0002J\u0010\u0010\u0018\u001a\u00020\u00042\u0006\u0010\u0019\u001a\u00020\u0017H\u0002J\u0018\u0010\u001a\u001a\u00020\u00122\u0006\u0010\u0019\u001a\u00020\u00172\b\b\u0002\u0010\u001b\u001a\u00020\bR\"\u0010\u0005\u001a\u0004\u0018\u00010\u00042\b\u0010\u0003\u001a\u0004\u0018\u00010\u0004@BX\u0086\u000e\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0006\u0010\u0007R\u001e\u0010\t\u001a\u00020\b2\u0006\u0010\u0003\u001a\u00020\b@BX\u0086\u000e\u00a2\u0006\b\n\u0000\u001a\u0004\b\n\u0010\u000bR\u001e\u0010\f\u001a\u00020\u00042\u0006\u0010\u0003\u001a\u00020\u0004@BX\u0086\u000e\u00a2\u0006\b\n\u0000\u001a\u0004\b\r\u0010\u0007R\u0014\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u00100\u000fX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u001c"}, d2 = {"Lcom/antigravity/smarthub/core/state/StateMachineEngine;", "", "()V", "<set-?>", "Lcom/antigravity/smarthub/core/model/SmartHubProfile;", "candidateProfile", "getCandidateProfile", "()Lcom/antigravity/smarthub/core/model/SmartHubProfile;", "", "candidateSinceMs", "getCandidateSinceMs", "()J", "currentProfile", "getCurrentProfile", "gamingPackages", "", "", "buildResolvedState", "Lcom/antigravity/smarthub/core/state/ResolvedState;", "profile", "base", "Lcom/antigravity/smarthub/core/model/DeviceState;", "extended", "Lcom/antigravity/smarthub/core/state/ExtendedDeviceState;", "determineTargetProfile", "state", "updateState", "currentTimeMs", "app_debug"})
public final class StateMachineEngine {
    @org.jetbrains.annotations.NotNull()
    private com.antigravity.smarthub.core.model.SmartHubProfile currentProfile = com.antigravity.smarthub.core.model.SmartHubProfile.P5_DAILY_ADAPTIVE;
    @org.jetbrains.annotations.Nullable()
    private com.antigravity.smarthub.core.model.SmartHubProfile candidateProfile;
    private long candidateSinceMs = 0L;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Set<java.lang.String> gamingPackages = null;
    
    public StateMachineEngine() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.antigravity.smarthub.core.model.SmartHubProfile getCurrentProfile() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.antigravity.smarthub.core.model.SmartHubProfile getCandidateProfile() {
        return null;
    }
    
    public final long getCandidateSinceMs() {
        return 0L;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.antigravity.smarthub.core.state.ResolvedState updateState(@org.jetbrains.annotations.NotNull()
    com.antigravity.smarthub.core.state.ExtendedDeviceState state, long currentTimeMs) {
        return null;
    }
    
    private final com.antigravity.smarthub.core.model.SmartHubProfile determineTargetProfile(com.antigravity.smarthub.core.state.ExtendedDeviceState state) {
        return null;
    }
    
    private final com.antigravity.smarthub.core.state.ResolvedState buildResolvedState(com.antigravity.smarthub.core.model.SmartHubProfile profile, com.antigravity.smarthub.core.model.DeviceState base, com.antigravity.smarthub.core.state.ExtendedDeviceState extended) {
        return null;
    }
}