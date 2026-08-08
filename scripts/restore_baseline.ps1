# Smart Hub — Transactional Baseline Recovery Script (PowerShell)
# Reads baseline data from device-baseline.json, checks device count & model fingerprint.

param (
    [string]$DeviceSerial = "",
    [string]$BaselineFile = "$PSScriptRoot\..\device-baseline.json"
)

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "      SMART HUB TRANSACTIONAL BASELINE RECOVERY SCRIPT      " -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

# 1. Resolve ADB Command
$adbCmd = Get-Command "adb" -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Source
if (-not $adbCmd) {
    if (Test-Path "C:\android-sdk\platform-tools\adb.exe") {
        $adbCmd = "C:\android-sdk\platform-tools\adb.exe"
    } else {
        Write-Host "[ERROR] ADB executable not found in PATH or standard location." -ForegroundColor Red
        exit 1
    }
}

# 2. Check Device Count & Serial
$devicesOutput = & $adbCmd devices | Select-String -Pattern "\tdevice$"
if ($devicesOutput.Count -eq 0) {
    Write-Host "[ERROR] No authorized ADB devices detected." -ForegroundColor Red
    exit 1
}

if ($devicesOutput.Count -gt 1 -and [string]::IsNullOrWhiteSpace($DeviceSerial)) {
    Write-Host "[ERROR] Multiple ADB devices connected ($($devicesOutput.Count)). You must specify -DeviceSerial <SERIAL>." -ForegroundColor Red
    exit 1
}

if ([string]::IsNullOrWhiteSpace($DeviceSerial)) {
    $DeviceSerial = ($devicesOutput[0].Line -split "\t")[0]
    Write-Host "[INFO] Auto-detected ADB Device Serial: $DeviceSerial" -ForegroundColor Yellow
}

# 3. Read Baseline JSON
if (-not (Test-Path $BaselineFile)) {
    Write-Host "[ERROR] Baseline snapshot file not found at $BaselineFile" -ForegroundColor Red
    exit 1
}

$baselineJson = Get-Content $BaselineFile -Raw | ConvertFrom-Json

# 4. Verify Model Fingerprint
$actualModel = (& $adbCmd -s $DeviceSerial shell getprop ro.product.model).Trim()
if ($baselineJson.model -and $actualModel -ne $baselineJson.model) {
    Write-Host "[ERROR] Device model mismatch! Baseline expected '$($baselineJson.model)', connected device is '$actualModel'." -ForegroundColor Red
    exit 1
}
Write-Host "[SUCCESS] Device fingerprint verified ($actualModel)." -ForegroundColor Green

# 5. Restore Secure Settings
if ($baselineJson.settings.secure) {
    $baselineJson.settings.secure.psobject.properties | ForEach-Object {
        $key = $_.Name
        $value = $_.Value
        Write-Host "[RESTORE] Setting secure $key -> $value" -ForegroundColor Yellow
        & $adbCmd -s $DeviceSerial shell settings put secure $key $value
        $verify = & $adbCmd -s $DeviceSerial shell settings get secure $key
        Write-Host "[VERIFY] secure $key is now: $verify" -ForegroundColor Green
    }
}

# 6. Restore Standby Buckets
if ($baselineJson.standbyBuckets) {
    $baselineJson.standbyBuckets.psobject.properties | ForEach-Object {
        $pkg = $_.Name
        $bucket = $_.Value
        Write-Host "[RESTORE] Standby Bucket $pkg -> $bucket" -ForegroundColor Yellow
        & $adbCmd -s $DeviceSerial shell am set-standby-bucket $pkg $bucket
    }
}

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host " Baseline restoration completed from captured snapshot.    " -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
