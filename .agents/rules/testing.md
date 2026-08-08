# Testing & Verification Rules

## Verification Standards
1. **Unit Testing:** Policy resolver, hysteresis timing, safety governor veto logic, and app classification must be unit tested.
2. **On-Device ADB Verification:** Any sysfs, settings key, or shell command MUST be probed and verified on the connected physical Galaxy A25 5G before inclusion in production modules.
3. **Graceful Degradation Tests:** Test app behavior when Shizuku disconnects, root is denied, or permissions are revoked at runtime.
4. **Overhead Benchmark:** Ensure Smart Hub's CPU/Battery overhead stays below 0.5% during background monitoring.
