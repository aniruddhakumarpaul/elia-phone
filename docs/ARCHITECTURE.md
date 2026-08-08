# Smart Hub — Software Architecture Document

Smart Hub follows **Clean Architecture** principles and **Unidirectional Data Flow (UDF)**.

## High-Level Data Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    Telemetry Collectors                     │
│  BatteryObserver, ThermalObserver, PsiObserver, AppContext  │
└──────────────────────────────┬──────────────────────────────┘
                               │ Flow<DeviceState>
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                     Context Classifier                      │
│        (GAME, MEDIA, PRODUCTIVITY, SOCIAL, IDLE, CHARGING)  │
└──────────────────────────────┬──────────────────────────────┘
                               │ ClassifiedContext
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                      Safety Governor                        │
│     Enforces Vetoes: Thermal Emergency, Battery Critical    │
└──────────────────────────────┬──────────────────────────────┘
                               │ ApprovedContext
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                       Profile Resolver                      │
│        P0_THERMAL > P1_CRITICAL_BATTERY > P2_GAME ...       │
└──────────────────────────────┬──────────────────────────────┘
                               │ TargetProfile + SystemActions
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                      Execution Backend                      │
│        StockBackend | ShizukuBackend | RootBackend          │
└──────────────────────────────┬──────────────────────────────┘
                               │ SystemAction Execution
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                   Verification & History                    │
│      Rereads state, logs to Room DB, updates UI State       │
└─────────────────────────────────────────────────────────────┘
```

## Module Structure

```
smart-hub/
├── app/                  # Application entry point & Hilt DI modules
├── core/
│   ├── model/            # Domain models (DeviceState, Profile, SystemAction)
│   ├── telemetry/        # Battery, Thermal, Memory PSI, Display, App observers
│   ├── state/            # State machine & Profile Resolver
│   ├── safety/           # Safety Governor & Veto Engine
│   ├── persistence/      # Room DB (ActionHistory) & DataStore (Preferences)
│   └── logging/          # Diagnostic logger & baseline snapshots
├── platform/
│   ├── android/          # Stock Android APIs & SystemServices
│   ├── shizuku/          # Shizuku Shell Executor & IPC
│   ├── root/             # Root Executor (libsu)
│   └── samsung/          # Samsung-specific One UI setting handlers
├── feature/
│   ├── dashboard/        # Main Compose UI dashboard
│   ├── profiles/         # Active profile viewer & manual controls
│   ├── applications/     # Protected app manager & classification editor
│   ├── battery/          # Deep battery telemetry & charging metrics
│   ├── thermal/          # Thermal HAL 2.0 telemetry & headroom view
│   ├── memory/           # PSI memory pressure & ZRAM monitor
│   ├── storage/          # Storage health & cache cleaning recommendations
│   ├── diagnostics/      # System command matrix & live log inspector
│   └── settings/         # Capability setup & Shizuku/Root toggle
```
