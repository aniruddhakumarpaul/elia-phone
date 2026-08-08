#!/usr/bin/env bash
# Smart Hub — Emergency Baseline Restoration Script (Bash)
# Target Device: Samsung Galaxy A25 5G (SM-A256E)

SERIAL="RZCX10THBZL"
ADB_BIN="adb"

echo "============================================================"
echo "      SMART HUB EMERGENCY BASELINE RESTORATION SCRIPT       "
echo "============================================================"

if ! command -v $ADB_BIN &> /dev/null; then
    ADB_BIN="C:/android-sdk/platform-tools/adb.exe"
fi

echo "[1/3] Restoring Samsung Refresh Rate Setting (Adaptive 120Hz Baseline)..."
$ADB_BIN -s $SERIAL shell settings put secure refresh_rate_mode 0
CURRENT_REFRESH=$($ADB_BIN -s $SERIAL shell settings get secure refresh_rate_mode)
echo "[VERIFY] secure refresh_rate_mode set to: $CURRENT_REFRESH"

echo "[2/3] Restoring Standby Buckets..."
$ADB_BIN -s $SERIAL shell am set-standby-bucket com.sec.android.app.launcher active

echo "[3/3] Baseline Restoration Completed Successfully."
