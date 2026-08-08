# Samsung Galaxy A25 5G - Feature Capability Matrix

| Feature / Capability | Stock Tier 0 | Shizuku Tier 1 | Root Tier 2 | Verification Status | Risk Level | Rollback Mechanism | Status |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| Battery and charging telemetry | BatteryManager API | dumpsys battery | Direct sysfs | Verified read-only on device | None | Read-only | SUPPORTED |
| Thermal telemetry (HAL 2.0) | PowerManager API | dumpsys thermalservice | Thermal sysfs | Verified read-only on device | None | Read-only | SUPPORTED |
| Refresh rate control (60/120 Hz) | Needs secure setting | settings put secure refresh_rate_mode | Direct sysfs | Setting and effective-display readback on device | Low | Restore exact baseline refresh_rate_mode | PARTIALLY_SUPPORTED - 60 Hz verified; adaptive request remained physically 60 Hz while idle |
| App standby buckets | System only | am set-standby-bucket | Shell/sysfs | **COMMAND/ADB VERIFIED** on `com.google.android.youtube`; exact baseline 45 restored | Low | Restore exact captured baseline | AVAILABLE — ADB verified; Smart Hub Tier-1 runtime pending Shizuku |
| AppOps background restriction | System only | cmd appops set RUN_ANY_IN_BACKGROUND | Shell/sysfs | **COMMAND/ADB VERIFIED** on `com.google.android.youtube`; exact baseline allow restored | Medium | Restore exact captured baseline | AVAILABLE — ADB verified; Smart Hub Tier-1 runtime pending Shizuku |
| Memory PSI telemetry | Read procfs | Read procfs | Read procfs | Verified read-only on device | None | Read-only | SUPPORTED |
| ZRAM telemetry and status | Permission dependent | Read sysfs | Direct sysfs | Existing probe evidence | Low | Read-only | PARTIAL |
| CPU cluster frequencies | Permission dependent | Read sysfs | Write scaling frequency | Not part of release candidate | High | Restore baseline | DEFERRED_ROOT |
| GPU devfreq control | Permission dependent | Permission dependent | Requires root | Not part of release candidate | High | Restore baseline | DEFERRED_ROOT |
| VM sysctl changes | Permission dependent | Permission dependent | Root write | Not part of release candidate | High | Restore baseline | DEFERRED_ROOT |
