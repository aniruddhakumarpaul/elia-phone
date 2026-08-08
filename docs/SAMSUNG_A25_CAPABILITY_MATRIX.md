# Samsung Galaxy A25 5G — Feature Capability Matrix

| Feature / Capability | Stock Tier 0 | Shizuku Tier 1 | Root Tier 2 | Verification Status | Risk Level | Rollback Mechanism | Status |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Battery & Charging Telemetry** | ✅ `BatteryManager` API | ✅ `dumpsys battery` | ✅ Direct Sysfs | `VERIFIED_ON_DEVICE` | None | Read-only | **SUPPORTED** |
| **Thermal Telemetry (HAL 2.0)** | ✅ `PowerManager` API | ✅ `dumpsys thermalservice` | ✅ Thermal Sysfs | `VERIFIED_ON_DEVICE` | None | Read-only | **SUPPORTED** |
| **Refresh Rate Control (60/120Hz)**| ❌ Needs Secure Setting | ✅ `settings put secure refresh_rate_mode` | ✅ Direct Sysfs | `VERIFIED_ON_DEVICE` | Low | Restore baseline `refresh_rate_mode` | **SUPPORTED** |
| **App Standby Buckets** | ❌ System Only | ✅ `am set-standby-bucket` | ✅ Shell/Sysfs | `VERIFIED_ON_DEVICE` | Low | Reset bucket to `WORKING_SET` / `ACTIVE` | **SUPPORTED** |
| **AppOps Background Restriction** | ❌ System Only | ✅ `cmd appops set RUN_ANY_IN_BACKGROUND` | ✅ Shell/Sysfs | `VERIFIED_ON_DEVICE` | Medium | Restore original AppOps mode | **SUPPORTED** |
| **Memory PSI Telemetry** | ✅ Read `/proc/pressure/memory` | ✅ Read `/proc/pressure/memory` | ✅ Read `/proc/pressure/memory` | `VERIFIED_ON_DEVICE` | None | Read-only | **SUPPORTED** |
| **ZRAM Telemetry & Status** | ❌ Permission Denied | ✅ `cat /sys/block/zram0/comp_algorithm` | ✅ Direct Sysfs | `VERIFIED_ON_DEVICE` | Low | Read-only | **PARTIAL** |
| **CPU Cluster Frequencies** | ❌ Permission Denied | ✅ `cat /sys/devices/system/cpu/cpufreq/...` | ✅ Write Scaling Freq | `VERIFIED_ON_DEVICE` | High | Restore `energy_aware` governor | **EXPERIMENTAL** |
| **GPU Devfreq Control** | ❌ Permission Denied | ❌ Permission Denied | 🧪 Requires Root Node Write | `PROBABLE` | High | Restore devfreq governor | **EXPERIMENTAL** |
| **VM Sysctl (Swappiness/FreeKB)** | ❌ Permission Denied | ❌ Permission Denied | 🧪 Root Write `/proc/sys/vm/*` | `VERIFIED_ON_DEVICE` | High | Restore baseline sysctl values | **EXPERIMENTAL** |
