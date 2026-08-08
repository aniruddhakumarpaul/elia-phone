# Android Safety & Non-Negotiable Engineering Rules

## Core Principles
1. **Measure First:** Never apply optimizations without prior system state telemetry.
2. **Reversibility:** Every system modification must capture a baseline and provide an automatic, robust rollback mechanism.
3. **Thermal Primacy:** Thermal emergency overrides all performance boosting profiles instantly.
4. **No Fake Boosters:** Never implement aggressive RAM killing, fake "RAM cleared" metrics, or dangerous kernel tweaks.
5. **No Defeating Security:** Do not attempt to bypass Samsung thermal controls, Knox security, or SELinux policies.
6. **Graceful Degradation:** Smart Hub must remain fully functional and useful in Tier 0 (Stock Android) without Shizuku or Root.
