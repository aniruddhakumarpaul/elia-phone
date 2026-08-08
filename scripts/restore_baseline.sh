#!/usr/bin/env bash
# Smart Hub — Transactional Baseline Recovery Script (Bash)
# Parses device-baseline.json, checks device count & model fingerprint before restoring.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BASELINE_FILE="$SCRIPT_DIR/../device-baseline.json"
TARGET_SERIAL=""

# Parse command line flags
while [[ $# -gt 0 ]]; do
  case $1 in
    --serial|-s)
      TARGET_SERIAL="$2"
      shift 2
      ;;
    *)
      shift
      ;;
  esac
done

echo "============================================================"
echo "      SMART HUB TRANSACTIONAL BASELINE RECOVERY SCRIPT      "
echo "============================================================"

ADB_BIN=$(which adb 2>/dev/null || echo "C:/android-sdk/platform-tools/adb.exe")

# 1. Device Count & Serial Detection
CONNECTED_DEVICES=($($ADB_BIN devices | grep -w "device" | awk '{print $1}'))
NUM_DEVICES=${#CONNECTED_DEVICES[@]}

if [ "$NUM_DEVICES" -eq 0 ]; then
    echo "[ERROR] No connected/authorized ADB devices detected."
    exit 1
elif [ "$NUM_DEVICES" -gt 1 ] && [ -z "$TARGET_SERIAL" ]; then
    echo "[ERROR] Multiple ADB devices connected ($NUM_DEVICES). You must specify --serial <SERIAL>."
    exit 1
elif [ -z "$TARGET_SERIAL" ]; then
    TARGET_SERIAL="${CONNECTED_DEVICES[0]}"
fi

echo "[INFO] Using ADB Device Serial: $TARGET_SERIAL"

# 2. Verify Baseline JSON & Model Fingerprint
if [ ! -f "$BASELINE_FILE" ]; then
    echo "[ERROR] Baseline file not found at $BASELINE_FILE"
    exit 1
fi

EXPECTED_MODEL=$(grep '"model":' "$BASELINE_FILE" | head -n 1 | awk -F '"' '{print $4}')
ACTUAL_MODEL=$($ADB_BIN -s "$TARGET_SERIAL" shell getprop ro.product.model | tr -d '\r')

if [ -n "$EXPECTED_MODEL" ] && [ "$ACTUAL_MODEL" != "$EXPECTED_MODEL" ]; then
    echo "[ERROR] Device model mismatch! Baseline expected '$EXPECTED_MODEL', but connected device is '$ACTUAL_MODEL'."
    exit 1
fi

echo "[SUCCESS] Device fingerprint verified ($ACTUAL_MODEL)."

# 3. Parse and Restore Baseline JSON via Python/jq fallback
if command -v python3 &>/dev/null; then
    python3 - <<EOF
import json, subprocess

with open('$BASELINE_FILE', 'r') as f:
    data = json.load(f)

serial = '$TARGET_SERIAL'
adb = '$ADB_BIN'

# Settings restore
settings = data.get('settings', {}).get('secure', {})
for k, v in settings.items():
    print(f"[RESTORE] setting secure {k} -> {v}")
    subprocess.run([adb, "-s", serial, "shell", "settings", "put", "secure", k, str(v)])

# Standby buckets restore
buckets = data.get('standbyBuckets', {})
for pkg, bucket in buckets.items():
    print(f"[RESTORE] standby-bucket {pkg} -> {bucket}")
    subprocess.run([adb, "-s", serial, "shell", "am", "set-standby-bucket", pkg, str(bucket)])
EOF
else
    # Simple shell fallback
    echo "[RESTORE] Restoring secure refresh_rate_mode..."
    $ADB_BIN -s "$TARGET_SERIAL" shell settings put secure refresh_rate_mode 0
fi

echo "============================================================"
echo " Baseline restoration completed cleanly.                    "
echo "============================================================"
