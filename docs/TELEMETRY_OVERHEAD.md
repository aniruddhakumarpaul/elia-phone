# Telemetry Overhead Report — Samsung Galaxy A25 5G (SM-A256E)

## Device

- Model: `SM-A256E`
- Device codename: `a25x`
- OS/API: Android 16 / API 36
- SoC: Exynos 1280

## Measurement run

This report replaces the previous unverified values. The debug APK was installed and launched on the connected device. The process was sampled for six 10-second intervals in each state on 2026-08-09:

- Screen-on active UI: 60 seconds total.
- Screen-off idle: 60 seconds total; Smart Hub uses its 10-second screen-off sampling interval.
- CPU was measured from process `/proc/<pid>/stat` user+system jiffies against `/proc/stat` aggregate jiffies.
- Memory was read from `dumpsys meminfo com.antigravity.smarthub`.
- Battery was read before and after the run; percentage resolution is too coarse to derive a trustworthy per-hour drain rate from this short sample.

## Measured results

| Metric | Screen-on active | Screen-off idle |
| :--- | ---: | ---: |
| Duration | 60 s | 60 s |
| Aggregate CPU share, mean | 0.543% | 0.112% |
| Aggregate CPU share, peak interval | 0.668% | 0.164% |
| PSS, stabilized | ~113.2 MB | ~87.2 MB |
| RSS, stabilized | ~184.7 MB | ~159.0 MB |
| Battery percentage change | — | 95% before and after |

The CPU percentage is aggregate device CPU time, not the single-core-normalized figure used by some Android profiling tools. The memory values include the Android runtime, Compose UI, libraries, and app process—not only telemetry collectors.

## What is not claimed

The previous claims of `0.21%` idle CPU, `0.38%` active CPU, `14.2 MB` RSS, and `<0.08%/hour` battery drain were not reproduced and are removed. A battery drain rate requires a substantially longer controlled run or a calibrated external measurement; this run therefore reports no invented hourly estimate.

## Sampling policy

- Default active: 2,000 ms.
- Gaming foreground: 1,000 ms.
- Screen-off: 10,000 ms.
- Thermal severe/critical: 500 ms.

The collectors use Android callbacks where available and read-only procfs/sysfs/API observations. Privileged shell mutations are not part of the routine telemetry loop.
