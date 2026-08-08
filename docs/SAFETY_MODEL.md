# Smart Hub — Safety Model & Veto Governor

Safety is the paramount constraint of Smart Hub. The **Safety Governor** has ultimate veto authority over all requested `SystemAction` mutations.

## 1. Absolute Veto Triggers
The Safety Governor will immediately veto and revert any action if any of the following conditions are true:

1. **Thermal Headroom Exhausted:** `PowerManager.getThermalStatus() >= THERMAL_STATUS_MODERATE` or `AP Temp >= 48°C`.
2. **Protected Package Target:** The target app belongs to `NEVER_TOUCH` blacklist (Phone, SMS, System UI, Alarms, Authenticators, Banking, Teams, Outlook, Smart Hub itself).
3. **Active Voice Call or Navigation:** Phone call is active (`TelephonyManager.CALL_STATE_OFFHOOK`) or turn-by-turn navigation is running.
4. **Missing Baseline Snapshot:** No baseline setting snapshot was recorded prior to the mutation request.
5. **Verification Failure:** Post-execution readback does not match expected target state within 3000 ms.
6. **Privilege Loss:** Shizuku service disconnected or Root permission denied.

## 2. Protected Package Blacklist (`NEVER_TOUCH`)
The following package patterns are permanently protected from standby bucket demotion or AppOps restrictions:

- `com.sec.android.app.launcher` (One UI Home)
- `com.android.systemui` (System UI)
- `com.android.phone` / `com.samsung.android.incallui` (Telephony)
- `com.google.android.dialer` / `com.samsung.android.dialer` (Phone)
- `com.sec.android.app.clockpackage` (Clock & Alarms)
- `com.whatsapp`, `org.telegram.messenger` (Primary Messaging)
- `com.microsoft.teams`, `com.microsoft.office.outlook` (Work Communication)
- `net.one97.paytm`, `com.phonepe.app`, `com.axis.mobile`, `in.hsbc.hsbcindia` (Banking & Payments)
- `in.gov.uidai.facerd`, `com.digilocker.android` (Identity Services)
- `com.antigravity.smarthub` (Smart Hub Optimizer itself)

## 3. Baseline Recovery & Emergency Restorer
- Before any setting is mutated (e.g. `refresh_rate_mode`), original value is written to DataStore and logged to Room DB (`ActionHistory`).
- An **Emergency Restore** feature in the UI and standalone shell script (`scripts/restore_baseline.sh`) reads the baseline database and restores all original device settings immediately.
- Upon device reboot, Smart Hub clears temporary runtime boosts and re-verifies device state against baseline snapshots.
