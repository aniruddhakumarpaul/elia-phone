# Smart Hub — Implementation Plan & Phase Roadmap

## User Review & Design Scope

> [!NOTE]
> Smart Hub is designed specifically for **Samsung Galaxy A25 5G (`SM-A256E`)** running **Android 16 (API 36)**. It utilizes event-driven observers, a deterministic state engine, Shizuku IPC, and an explainable Material 3 interface.

---

## Phase Roadmap

### Phase 0: Research, Probing & Baseline Documentation (COMPLETED)
- [x] Create isolated project workspace `smart-hub`.
- [x] Enforce absolute scope rule (Zero interaction with POCO F5 / BGMI projects).
- [x] Run read-only ADB hardware discovery on physical Samsung Galaxy A25 5G.
- [x] Discover CPU topology (2x A78 @ 2.4GHz, 6x A55 @ 2.0GHz, `energy_aware` governor).
- [x] Discover display refresh rate keys (`secure refresh_rate_mode`: 0=120Hz, 1=60Hz).
- [x] Discover ZRAM configuration (8GB disksize, `lzo-rle`) and PSI memory availability.
- [x] Discover Thermal HAL 2.0 sensors (`AP`, `BAT`, `SKIN`, `USB`, `PA`).
- [x] Create project rules & documentation matrix (`DEVICE_BASELINE.md`, `ANDROID_16_CONSTRAINTS.md`, `SAMSUNG_A25_CAPABILITY_MATRIX.md`, `PRIVILEGE_MATRIX.md`, `SYSTEM_COMMAND_MATRIX.md`, `ARCHITECTURE.md`, `STATE_ENGINE.md`, `SAFETY_MODEL.md`).

### Phase 1: Native Skeleton & Dependency Setup
- [ ] Initialize Android Kotlin project structure (`compileSdk 36`, `targetSdk 36`, `minSdk 31`).
- [ ] Configure Hilt DI, Jetpack Compose, Material 3, Room, DataStore, and Shizuku dependencies.
- [ ] Implement `BaselineRepository` and `ActionHistory` Room database entities.

### Phase 2: Telemetry & Observers Layer
- [ ] Build `BatteryObserver` (BatteryManager broadcasts, voltage/current/temp trends).
- [ ] Build `ThermalObserver` (PowerManager thermal status & headroom listeners).
- [ ] Build `PsiMemoryObserver` (Reading `/proc/pressure/memory` & ActivityManager memory info).
- [ ] Build `AppContextObserver` (AccessibilityService / UsageStatsManager event listener).
- [ ] Build `DisplayObserver` (Refresh rate & screen state monitoring).

### Phase 3: Safety Governor & Deterministic State Engine
- [ ] Implement `SafetyGovernor` with veto rules and `NEVER_TOUCH` package blacklist.
- [ ] Implement `ProfileResolver` with priority hierarchy (`P0_THERMAL` down to `P6_OVERNIGHT`).
- [ ] Implement Hysteresis & Debounce controller (preventing rapid state thrashing).

### Phase 4: Execution Backends & Shizuku Integration
- [ ] Build `StockBackend` for official API calls.
- [ ] Build `ShizukuBackend` for typed shell actions (`refresh_rate_mode`, `am set-standby-bucket`, `cmd appops`).
- [ ] Build verification & automatic rollback handler.

### Phase 5: Material 3 Dashboard & UI
- [ ] Build `DashboardScreen` (Live metrics, active profile card, state explanation).
- [ ] Build `ProfilesScreen` (Manual profile viewer & dynamic override toggles).
- [ ] Build `AppManagerScreen` (Protected apps editor & package classification).
- [ ] Build `DiagnosticsScreen` (Live log inspector & System Command Matrix).

### Phase 6: On-Device Verification & Benchmark Scripts
- [ ] Create `scripts/restore_baseline.ps1` and `scripts/restore_baseline.sh`.
- [ ] Run verification tests on Galaxy A25 over ADB.
- [ ] Perform battery overhead benchmark.

---

## Verification Plan

### Automated & Shell Tests
1. **Shizuku Command Verification:**
   - Execute `settings put secure refresh_rate_mode 1` and verify readback is `1` (60Hz).
   - Execute `settings put secure refresh_rate_mode 0` and verify readback is `0` (120Hz).
2. **Standby Bucket Verification:**
   - Query bucket with `am get-standby-bucket <pkg>`.

### Manual & UI Verification
1. Verify live dashboard metrics update in real-time without CPU lag.
2. Verify emergency thermal override immediately forces 60Hz and drops active boosts.
3. Verify baseline restoration restores original settings on demand.
