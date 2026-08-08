# Smart Hub — State Engine & Hysteresis Specification

The Smart Hub State Engine is a deterministic state machine that processes telemetry inputs and outputs an active profile.

## Priority Hierarchy (Highest to Lowest)

1. **P0 — Thermal Emergency (`P0_THERMAL_EMERGENCY`)**
   - *Trigger:* `ThermalStatus >= MODERATE` OR `BatteryTemp >= 43°C` OR `SoC AP Temp >= 48°C`.
   - *Actions:* Immediate cancellation of all boosts, force 60Hz display, restrict background tasks, disable compaction.
   - *Veto Power:* Absolute. Cannot be overridden by user preference or adaptive learner.

2. **P1 — Critical Battery (`P1_CRITICAL_BATTERY`)**
   - *Trigger:* `BatteryLevel <= 15%` AND `IsCharging == false`.
   - *Actions:* Force 60Hz display, suspend optional background refresh, demote non-essential apps to `RARE` / `RESTRICTED` standby bucket.

3. **P2 — Charging Thermal Guard (`P2_CHARGING_THERMAL_GUARD`)**
   - *Trigger:* `IsCharging == true` AND `BatteryTemp >= 39°C`.
   - *Actions:* Prevent CPU/GPU frequency boosts, maintain balanced 60Hz/120Hz adaptivity, limit thermal buildup.

4. **P3 — Gaming / High Load (`P3_GAMING_HIGH_LOAD`)**
   - *Trigger:* Foreground package matches Game Category (e.g., `com.pubg.imobile`, `com.dts.freefiremax`, `com.roblox.client`).
   - *Actions:* Force 120Hz display (via Shizuku `refresh_rate_mode=0`), elevate app to `ACTIVE` bucket (10), restrict unneeded background apps. Optional Tier 2 CPU min-freq raise if thermal headroom is green.

5. **P4 — Media / Reading Mode (`P4_MEDIA_READING`)**
   - *Trigger:* Active MediaSession playback (Video) OR Reading App foreground (e.g. Kindle, Chrome reading).
   - *Actions:* Force 60Hz display (saves ~15-20% screen power), preserve playback buffers, restrict CPU scaling spike.

6. **P5 — Daily Adaptive (`P5_DAILY_ADAPTIVE`)**
   - *Trigger:* General app usage (Social, Productivity, Messaging, Banking).
   - *Actions:* Dynamic 60Hz/120Hz adaptivity, preserve background push notification delivery for Teams/Outlook/WhatsApp.

7. **P6 — Overnight Deep Idle (`P6_OVERNIGHT_DEEP_IDLE`)**
   - *Trigger:* `ScreenState == OFF` AND `Time between 23:00 - 06:00` AND `No Media Playing` AND `No Navigation Active` AND `Idle Duration > 15 minutes`.
   - *Actions:* Force 60Hz display baseline, stage entry into Light Idle -> Deep Idle doze, execute optional memory compaction if memory PSI is high.

---

## Hysteresis & Debounce Rules

To prevent rapid profile thrashing (e.g. when opening a quick notification while gaming):

| Profile Transition | Debounce Delay | Minimum Dwell Time | Cooldown Period |
| :--- | :--- | :--- | :--- |
| Any -> `P0_THERMAL_EMERGENCY` | **0 ms (Immediate)** | 60 seconds | None |
| Game Exit -> `P5_DAILY_ADAPTIVE` | **3000 ms** | 10 seconds | 5 seconds |
| Screen Off -> `P6_OVERNIGHT_DEEP_IDLE` | **15 minutes** | 30 minutes | 10 minutes |
| Transient System UI Overlay | **Ignored** | N/A | N/A |
