package com.antigravity.smarthub.platform.shizuku;

/**
 * Manages Shizuku UserService lifecycle, binder death recipient, and auto-rebind.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000>\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\u0018\u00002\u00020\u00012\u00020\u0002B\u0005\u00a2\u0006\u0002\u0010\u0003J\u0006\u0010\u000f\u001a\u00020\u0010J\b\u0010\u0011\u001a\u00020\u0010H\u0016J\u001c\u0010\u0012\u001a\u00020\u00102\b\u0010\u0013\u001a\u0004\u0018\u00010\u00142\b\u0010\u0015\u001a\u0004\u0018\u00010\u0016H\u0016J\u0012\u0010\u0017\u001a\u00020\u00102\b\u0010\u0013\u001a\u0004\u0018\u00010\u0014H\u0016J\u0006\u0010\u0018\u001a\u00020\u0010R\u001e\u0010\u0006\u001a\u00020\u00052\u0006\u0010\u0004\u001a\u00020\u0005@BX\u0086\u000e\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0006\u0010\u0007R\"\u0010\t\u001a\u0004\u0018\u00010\b2\b\u0010\u0004\u001a\u0004\u0018\u00010\b@BX\u0086\u000e\u00a2\u0006\b\n\u0000\u001a\u0004\b\n\u0010\u000bR\u0016\u0010\f\u001a\n \u000e*\u0004\u0018\u00010\r0\rX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0019"}, d2 = {"Lcom/antigravity/smarthub/platform/shizuku/ShizukuServiceConnection;", "Landroid/content/ServiceConnection;", "Landroid/os/IBinder$DeathRecipient;", "()V", "<set-?>", "", "isConnected", "()Z", "Lcom/antigravity/smarthub/ISmartHubUserService;", "userService", "getUserService", "()Lcom/antigravity/smarthub/ISmartHubUserService;", "userServiceArgs", "Lrikka/shizuku/Shizuku$UserServiceArgs;", "kotlin.jvm.PlatformType", "bind", "", "binderDied", "onServiceConnected", "name", "Landroid/content/ComponentName;", "service", "Landroid/os/IBinder;", "onServiceDisconnected", "unbind", "app_debug"})
public final class ShizukuServiceConnection implements android.content.ServiceConnection, android.os.IBinder.DeathRecipient {
    @org.jetbrains.annotations.Nullable()
    private com.antigravity.smarthub.ISmartHubUserService userService;
    private boolean isConnected = false;
    private final rikka.shizuku.Shizuku.UserServiceArgs userServiceArgs = null;
    
    public ShizukuServiceConnection() {
        super();
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.antigravity.smarthub.ISmartHubUserService getUserService() {
        return null;
    }
    
    public final boolean isConnected() {
        return false;
    }
    
    public final void bind() {
    }
    
    public final void unbind() {
    }
    
    @java.lang.Override()
    public void onServiceConnected(@org.jetbrains.annotations.Nullable()
    android.content.ComponentName name, @org.jetbrains.annotations.Nullable()
    android.os.IBinder service) {
    }
    
    @java.lang.Override()
    public void onServiceDisconnected(@org.jetbrains.annotations.Nullable()
    android.content.ComponentName name) {
    }
    
    @java.lang.Override()
    public void binderDied() {
    }
}