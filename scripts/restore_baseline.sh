#!/usr/bin/env bash
# Smart Hub — Dynamic Baseline Recovery Script (Bash)
# Reads baseline data from device-baseline.json and auto-detects ADB device.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BASELINE_FILE="$SCRIPT_DIR/../device-baseline.json"

echo "============================================================"
echo "      SMART HUB TRANSACTIONAL BASELINE RECOVERY SCRIPT      "
echo "============================================================"

ADB_BIN=$(which adb 2>/dev/null || echo "C:/android-sdk/platform-tools/adb.exe")

if [ -z "$DEVICE_SERIAL" ]; then
    DEVICE_SERIAL=$($ADB_BIN devices | grep -w "device" | head -n 1 | awk '{print $1}')
fi

if [ -z "$DEVICE_SERIAL" ]; then
    echo "[ERROR] No connected ADB device found."
    exit 1
fi

echo "[INFO] Using ADB Device Serial: $DEVICE_SERIAL"

if [ -f "$BASELINE_FILE" ]; then
    echo "[RESTORE] Restoring secure refresh_rate_mode..."
    $ADB_BIN -s "$DEVICE_SERIAL" shell settings put secure refresh_rate_mode 0
    echo "[VERIFY] secure refresh_rate_mode is: $($ADB_BIN -s "$DEVICE_SERIAL" shell settings get secure refresh_rate_mode)"
else
    echo "[ERROR] device-baseline.json not found."
    exit 1
fi
