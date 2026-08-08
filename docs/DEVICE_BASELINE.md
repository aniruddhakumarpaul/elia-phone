# Device Baseline Telemetry — Samsung Galaxy A25 5G

## Hardware Overview
- **Device Model:** Samsung Galaxy A25 5G (`SM-A256E` / `a25x` / `a25xdins`)
- **System-on-Chip (SoC):** Samsung **Exynos 1280** (`s5e8825`, 5nm)
- **CPU Topology:** Octa-Core (2 Cluster Layout)
  - **Efficiency Cluster (`policy0` / `cpu0-cpu5`):** 6 × ARM Cortex-A55 @ 533 MHz – 2002 MHz (16 frequency steps)
  - **Performance Cluster (`policy6` / `cpu6-cpu7`):** 2 × ARM Cortex-A78 @ 533 MHz – 2400 MHz (20 frequency steps)
  - **Governor:** `energy_aware` (Energy-Aware Scheduling - EAS)
- **GPU Architecture:** ARM **Mali-G68 MP4** (`exynos-gpu-profiler`)
- **RAM Capacity:** 8 GB LPDDR4X (7,576,320 KB physical visible to OS)
- **ZRAM Swap:** 8 GB (`8,589,345,920` bytes disksize), `lzo-rle` compression algorithm active
- **Pressure Stall Information (PSI):** Available & readable non-root at `/proc/pressure/memory`, `/proc/pressure/cpu`, `/proc/pressure/io`
- **Storage Profile:** 128 GB UFS (~94 GB used / ~12 GB free — 89% capacity utilization)
- **Display Panel:** 6.5" Super AMOLED, 1080 × 2340, 450 DPI
- **Refresh Rates:** Dual Mode — 60.0 Hz (Standard) and 120.0 Hz (Adaptive) via `settings get secure refresh_rate_mode` (0=Adaptive 120Hz, 1=Standard 60Hz)
- **Battery Hardware:** 5000 mAh Li-ion battery, 3.9V - 4.0V baseline
- **Thermal Sensors (HAL 2.0):** `AP` (SoC), `BAT` (Battery), `SKIN` (Device outer body), `USB` (Port), `PA` (Power Amplifier)

## Software Environment
- **Android Version:** Android 16 (SDK 36)
- **Build ID:** `UP1A.231005.007.A256EXXU7CXG1`
- **OEM Interface:** Samsung One UI 8.x
- **Debugging Status:** ADB Authorized (`device` state), USB Debugging Enabled
- **Installed App Footprint:** Gaming (PUBG/BGMI, Free Fire Max, Roblox, DLS 24), Productivity (Microsoft Teams, Outlook), Payments (Paytm, PhonePe, Axis, HSBC), Social (Instagram, WhatsApp, Telegram, Snapchat, Facebook), Streaming (Hotstar, YouTube, Prime Video).
