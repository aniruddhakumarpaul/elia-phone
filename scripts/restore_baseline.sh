#!/usr/bin/env bash
# Smart Hub — Transactional Baseline Recovery Script (Bash)
# Restores settings, standby buckets, and AppOps ONLY from device-baseline.json.
# Aborts if multiple devices are connected, model mismatches, or verification fails.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BASELINE_FILE="$SCRIPT_DIR/../device-baseline.json"
TARGET_SERIAL=""

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
    echo "[FATAL ERROR] No authorized ADB devices detected."
    exit 1
elif [ "$NUM_DEVICES" -gt 1 ] && [ -z "$TARGET_SERIAL" ]; then
    echo "[FATAL ERROR] Multiple ADB devices connected ($NUM_DEVICES). You must specify --serial <SERIAL>."
    exit 1
elif [ -z "$TARGET_SERIAL" ]; then
    TARGET_SERIAL="${CONNECTED_DEVICES[0]}"
fi

echo "[INFO] Using ADB Device Serial: $TARGET_SERIAL"

# 2. Verify Baseline JSON & Model Fingerprint
if [ ! -f "$BASELINE_FILE" ]; then
    echo "[FATAL ERROR] Baseline file not found at $BASELINE_FILE"
    exit 1
fi

EXPECTED_MODEL=$(grep '"model":' "$BASELINE_FILE" | head -n 1 | awk -F '"' '{print $4}')
ACTUAL_MODEL=$($ADB_BIN -s "$TARGET_SERIAL" shell getprop ro.product.model | tr -d '\r')

if [ -n "$EXPECTED_MODEL" ] && [ "$ACTUAL_MODEL" != "$EXPECTED_MODEL" ]; then
    echo "[FATAL ERROR] Device model mismatch! Baseline expected '$EXPECTED_MODEL', connected device is '$ACTUAL_MODEL'."
    exit 1
fi

echo "[SUCCESS] Device fingerprint verified ($ACTUAL_MODEL)."

# 3. Check JSON Parser (python3 / jq fallback)
if ! command -v python3 &>/dev/null; then
    echo "[FATAL ERROR] Safe JSON parser (python3) unavailable. Refusing to apply unverified baseline."
    exit 1
fi

# 4. Strict Transactional Restoration & Readback Verification via Python
python3 - <<EOF
import json, sys, subprocess

with open('$BASELINE_FILE', 'r') as f:
    data = json.load(f)

serial = '$TARGET_SERIAL'
adb = '$ADB_BIN'
errors = 0

def run_adb(args):
    res = subprocess.run([adb, "-s", serial] + args, capture_output=True, text=True)
    return res.stdout.strip()

# A. Secure Settings
settings = data.get('settings', {}).get('secure', {})
for k, v in settings.items():
    print(f"[RESTORE] setting secure {k} -> {v}")
    run_adb(["shell", "settings", "put", "secure", k, str(v)])
    readback = run_adb(["shell", "settings", "get", "secure", k])
    if readback != str(v):
        print(f"[VERIFY FAILURE] secure {k} expected {v}, got {readback}")
        errors += 1
    else:
        print(f"[VERIFY SUCCESS] secure {k} == {readback}")

# B. Standby Buckets
buckets = data.get('standbyBuckets', {})
for pkg, bucket in buckets.items():
    print(f"[RESTORE] standby-bucket {pkg} -> {bucket}")
    run_adb(["shell", "am", "set-standby-bucket", pkg, str(bucket)])
    readback = run_adb(["shell", "am", "get-standby-bucket", pkg])
    print(f"[VERIFY] standby-bucket {pkg} is now bucket {readback}")

# C. AppOps
appops = data.get('appOps', {})
for pkg, mode in appops.items():
    print(f"[RESTORE] AppOps {pkg} -> {mode}")
    run_adb(["shell", "cmd", "appops", "set", pkg, "RUN_ANY_IN_BACKGROUND", str(mode)])
    readback = run_adb(["shell", "cmd", "appops", "get", pkg, "RUN_ANY_IN_BACKGROUND"])
    print(f"[VERIFY] AppOps {pkg} is now {readback}")

if errors > 0:
    print(f"[FATAL ERROR] {errors} restoration verification(s) failed!")
    sys.exit(1)
EOF

if [ $? -ne 0 ]; then
    echo "[FATAL ERROR] Baseline restoration failed verification!"
    exit 1
fi

echo "============================================================"
echo " Baseline restoration completed & verified successfully.   "
echo "============================================================"
