# Android 16 (API 36) & One UI Behavior Constraints

## Android 16 Behavior & Security Policies

### 1. App Standby Buckets & Background Restrictions
- **Mechanism:** Android 16 assigns apps to `ACTIVE` (10), `WORKING_SET` (20), `FREQUENT` (30), `RARE` (40), and `RESTRICTED` (45) buckets based on UsageStats.
- **Privilege Requirement:** Changing buckets programmatically requires Shizuku/ADB shell permission via `am set-standby-bucket <pkg> <bucket>`.
- **Constraint:** Restricting system apps or user-designated protected apps (e.g. Teams, WhatsApp, Alarms) will delay high-priority push notifications (FCM). Smart Hub must enforce a strict `NEVER_TOUCH` blacklist.

### 2. Display Refresh Rate Controls on One UI
- **AOSP Standard:** AOSP uses `settings put system min_refresh_rate` and `peak_refresh_rate`.
- **Samsung One UI Customization:** Samsung overrides standard AOSP keys and uses `settings put secure refresh_rate_mode`.
  - `0` = Motion Smoothness Adaptive (120 Hz)
  - `1` = Motion Smoothness Standard (60 Hz)
- **Privilege Requirement:** Writing to `secure` settings table requires `WRITE_SECURE_SETTINGS` permission (granted via Shizuku or ADB once: `pm grant <pkg> android.permission.WRITE_SECURE_SETTINGS`).

### 3. Thermal Headroom & Status Telemetry
- **API 36 Capability:** Android 16 exposes `PowerManager.addThermalStatusListener()` and `PowerManager.getThermalHeadroom()`.
- **Constraint:** SoC Thermal Headroom is distinct from Battery Temperature. A warm battery (38°C) during fast charging does not necessarily imply SoC thermal throttling, whereas `AP` temp at 45°C indicates active GPU/CPU throttling. Smart Hub monitors both `PowerManager` thermal status and battery temperature.

### 4. Memory Management (LMKD & PSI)
- **AOSP Behavior:** Android 16 manages memory via kernel LMKD driven by PSI (`/proc/pressure/memory`).
- **Constraint:** Arbitrary process killing ("RAM cleaning") triggers aggressive cold restarts, increasing CPU spike cycles and draining battery faster. Smart Hub relies on PSI monitoring and graceful Standby Bucket adjustments instead of process killing.

### 5. Foreground Context Detection
- **Accessibility Service:** Provides instant, event-driven window state updates (`TYPE_WINDOW_STATE_CHANGED`) without polling. Requires explicit user opt-in.
- **UsageStatsManager Fallback:** Non-intrusive fallback if Accessibility is declined. Uses batched event queries every 5–10 seconds when screen is ON.
