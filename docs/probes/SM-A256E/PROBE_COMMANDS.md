# Read-Only Probe Command Log & Documentation

Below are the exact, sanitized read-only ADB shell commands used to extract the baseline telemetry records for Samsung Galaxy A25 5G (`SM-A256E`):

```bash
# 1. Build & Device Properties
adb shell getprop ro.product.model
adb shell getprop ro.build.display.id
adb shell getprop ro.board.platform

# 2. CPU Topology & Frequencies
adb shell cat /sys/devices/system/cpu/possible
adb shell cat /sys/devices/system/cpu/cpufreq/policy0/scaling_available_frequencies
adb shell cat /sys/devices/system/cpu/cpufreq/policy6/scaling_available_frequencies
adb shell cat /sys/devices/system/cpu/cpufreq/policy0/scaling_governor

# 3. Display & Samsung Refresh Rate
adb shell dumpsys display | grep -E 'supportedModes|mBaseDisplayInfo'
adb shell settings get secure refresh_rate_mode

# 4. Thermal & Battery Telemetry
adb shell dumpsys thermalservice
adb shell dumpsys battery

# 5. Pressure Stall Information (PSI)
adb shell cat /proc/pressure/memory
adb shell cat /proc/pressure/cpu
adb shell cat /proc/pressure/io
```
