# Smart Hub — Transactional Baseline Recovery Script (PowerShell)
# Restores settings, standby buckets, and AppOps ONLY from device-baseline.json.
# Aborts if multiple devices are connected, model mismatches, or verification fails.

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
        Write-Host "[FATAL ERROR] ADB executable not found in PATH or standard location." -ForegroundColor Red
        exit 1
    }
}

# 2. Check Device Count & Serial
$devicesOutput = & $adbCmd devices | Select-String -Pattern "\tdevice$"
if ($devicesOutput.Count -eq 0) {
    Write-Host "[FATAL ERROR] No authorized ADB devices detected." -ForegroundColor Red
    exit 1
}

if ($devicesOutput.Count -gt 1 -and [string]::IsNullOrWhiteSpace($DeviceSerial)) {
    Write-Host "[FATAL ERROR] Multiple ADB devices connected ($($devicesOutput.Count)). You must specify -DeviceSerial <SERIAL>." -ForegroundColor Red
    exit 1
}

if ([string]::IsNullOrWhiteSpace($DeviceSerial)) {
    $DeviceSerial = ($devicesOutput[0].Line -split "\t")[0]
    Write-Host "[INFO] Auto-detected ADB Device Serial: $DeviceSerial" -ForegroundColor Yellow
}

# 3. Read Baseline JSON
if (-not (Test-Path $BaselineFile)) {
    Write-Host "[FATAL ERROR] Baseline snapshot file not found at $BaselineFile" -ForegroundColor Red
    exit 1
}

$baselineJson = Get-Content $BaselineFile -Raw | ConvertFrom-Json
$hasErrors = $false

# 4. Verify Model Fingerprint
$actualModel = (& $adbCmd -s $DeviceSerial shell getprop ro.product.model).Trim()
if ($baselineJson.model -and $actualModel -ne $baselineJson.model) {
    Write-Host "[FATAL ERROR] Device model mismatch! Baseline expected '$($baselineJson.model)', connected device is '$actualModel'." -ForegroundColor Red
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
        $verify = (& $adbCmd -s $DeviceSerial shell settings get secure $key).Trim()
        if ($verify -eq [string]$value) {
            Write-Host "[VERIFY SUCCESS] secure $key == $verify" -ForegroundColor Green
        } else {
            Write-Host "[VERIFY FAILURE] secure $key expected $value, got $verify" -ForegroundColor Red
            $hasErrors = $true
        }
    }
}

# 6. Restore Standby Buckets
$bucketCodeMap = @{
    "5" = "exempted"
    "10" = "active"
    "20" = "working_set"
    "30" = "frequent"
    "40" = "rare"
    "45" = "restricted"
}

if ($baselineJson.standbyBuckets) {
    $baselineJson.standbyBuckets.psobject.properties | ForEach-Object {
        $pkg = $_.Name
        $bucket = $_.Value
        Write-Host "[RESTORE] Standby Bucket $pkg -> $bucket" -ForegroundColor Yellow
        & $adbCmd -s $DeviceSerial shell am set-standby-bucket $pkg $bucket
        $rawCode = (& $adbCmd -s $DeviceSerial shell am get-standby-bucket $pkg).Trim()
        $mappedBucket = if ($bucketCodeMap.ContainsKey($rawCode)) { $bucketCodeMap[$rawCode] } else { $rawCode }
        
        if ($mappedBucket -eq [string]$bucket -or $rawCode -eq [string]$bucket) {
            Write-Host "[VERIFY SUCCESS] Standby Bucket $pkg == $mappedBucket ($rawCode)" -ForegroundColor Green
        } else {
            Write-Host "[VERIFY FAILURE] Standby Bucket $pkg expected $bucket, got $mappedBucket ($rawCode)" -ForegroundColor Red
            $hasErrors = $true
        }
    }
}

# 7. Restore AppOps
if ($baselineJson.appOps) {
    $baselineJson.appOps.psobject.properties | ForEach-Object {
        $pkg = $_.Name
        $mode = $_.Value
        Write-Host "[RESTORE] AppOps $pkg -> $mode" -ForegroundColor Yellow
        & $adbCmd -s $DeviceSerial shell cmd appops set $pkg RUN_ANY_IN_BACKGROUND $mode
        $rawOps = (& $adbCmd -s $DeviceSerial shell cmd appops get $pkg RUN_ANY_IN_BACKGROUND).Trim().ToLower()
        
        $actualMode = if ($rawOps -match "no operations" -or $rawOps -match "default") {
            "default"
        } elseif ($rawOps -match "allow") {
            "allow"
        } elseif ($rawOps -match "ignore") {
            "ignore"
        } elseif ($rawOps -match "deny") {
            "deny"
        } elseif ($rawOps -match "errored") {
            "errored"
        } elseif ($rawOps -match "foreground") {
            "foreground"
        } else {
            $rawOps
        }

        if ($actualMode -eq [string]$mode.ToLower()) {
            Write-Host "[VERIFY SUCCESS] AppOps $pkg == $actualMode" -ForegroundColor Green
        } else {
            Write-Host "[VERIFY FAILURE] AppOps $pkg expected $mode, got $actualMode" -ForegroundColor Red
            $hasErrors = $true
        }
    }
}

if ($hasErrors) {
    Write-Host "[FATAL ERROR] Baseline restoration failed verification!" -ForegroundColor Red
    exit 1
}

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host " Baseline restoration completed & verified successfully.   " -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
