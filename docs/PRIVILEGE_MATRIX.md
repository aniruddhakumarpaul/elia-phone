# Smart Hub Capability & Privilege Tier Matrix

Smart Hub is structured around 3 strict privilege tiers. Higher tiers enhance capability but are NEVER required for base application operation.

```
┌─────────────────────────────────────────────────────────────┐
│                    Tier 0: Stock Android                    │
│   Official APIs (BatteryManager, PowerManager, PSI Read)   │
├─────────────────────────────────────────────────────────────┤
│                    Tier 1: Shizuku / ADB                     │
│  Refresh Rate, Standby Buckets, AppOps, Advanced Dumpsys   │
├─────────────────────────────────────────────────────────────┤
│                     Tier 2: Root (Kernel)                   │
│   cpufreq Governors, Devfreq, VM Sysctl, Memory Compaction  │
└─────────────────────────────────────────────────────────────┘
```

## Tier Breakdown

### Tier 0 — Stock Android (Zero Prerequisites)
- **Permissions Required:** Standard Android permissions (`PACKAGE_USAGE_STATS`, `ACCESSIBILITY_SERVICE` (optional), `POST_NOTIFICATIONS`).
- **Available Actions:**
  - Real-time battery current, voltage, temperature monitoring.
  - PowerManager thermal status & headroom callbacks.
  - Reading Memory PSI from `/proc/pressure/memory`.
  - App foreground context classification.
  - Storage space analysis & caching recommendations.
  - Explainable state machine dashboard.

### Tier 1 — Shizuku / ADB Shell (Recommended)
- **Permissions Required:** Shizuku Service active OR `android.permission.WRITE_SECURE_SETTINGS` granted via ADB once.
- **Available Actions:**
  - Samsung `refresh_rate_mode` switching (0=120Hz for Gaming/Scrolling, 1=60Hz for Video/Idle).
  - Setting App Standby Buckets (`am set-standby-bucket <pkg> <bucket>`).
  - Toggling background execution appops (`cmd appops set <pkg> RUN_ANY_IN_BACKGROUND ignore|allow`).
  - Privileged device diagnostic collection via `dumpsys`.

### Tier 2 — Root / Kernel (Advanced / Experimental)
- **Permissions Required:** Root access (Magisk / KernelSU / APatch via `libsu`).
- **Available Actions:**
  - Dynamic CPU min frequency scaling adjustments on Cortex-A78 (`policy6`).
  - GPU devfreq governor tuning.
  - Kernel VM sysctl parameters (`swappiness`, `watermark_scale_factor`).
  - Idle memory compaction (`echo 1 > /proc/sys/vm/compact_memory`).
  - Strictly requires prior baseline snapshot and verified rollback script.
