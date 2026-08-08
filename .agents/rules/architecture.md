# Smart Hub Architectural Guidelines

## Technology Stack
- **Language:** Kotlin (100% Native)
- **UI:** Jetpack Compose + Material 3 Design
- **Target SDK:** Android 16 (API 36), compileSdk 36, minSdk 31
- **Architecture:** Unidirectional Data Flow (UDF), Clean Architecture (Data, Domain, UI)
- **DI & Persistence:** Hilt, Room, DataStore
- **Concurrency:** Kotlin Coroutines & Flow (Event-driven, zero polling loops)

## Layer Responsibilities
1. **Telemetry & Observers:** Collect state asynchronously (Battery, Thermal, Memory, Display, App context).
2. **Safety Governor:** Evaluates baseline conditions and enforces absolute thermal/battery/app protection vetoes.
3. **Policy Engine & Resolver:** Determines target system state based on priority hierarchy.
4. **Execution Backend:** Dispatches actions through `StockBackend`, `ShizukuBackend`, or `RootBackend`.
5. **Verification & Feedback:** Rereads system state to confirm successful mutation or executes immediate rollback.
