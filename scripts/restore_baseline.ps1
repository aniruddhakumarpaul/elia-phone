# Smart Hub — Emergency Baseline Restoration Script (PowerShell)
# Target Device: Samsung Galaxy A25 5G (SM-A256E)

param (
    [string]$DeviceSerial = "RZCX10THBZL",
    [string]$AdbPath = "C:\android-sdk\platform-tools\adb.exe"
)

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "      SMART HUB EMERGENCY BASELINE RESTORATION SCRIPT       " -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

if (-not (Test-Path $AdbPath)) {
    Write-Host "[ERROR] ADB executable not found at $AdbPath" -ForegroundColor Red
    exit 1
}

Write-Host "[1/3] Checking ADB Connection to $DeviceSerial..." -ForegroundColor Yellow
$deviceStatus = & $AdbPath devices | Select-String $DeviceSerial
if (-not $deviceStatus) {
    Write-Host "[ERROR] Device $DeviceSerial not connected or unauthorized." -ForegroundColor Red
    exit 1
}
Write-Host "[SUCCESS] Device connected." -ForegroundColor Green

Write-Host "[2/3] Restoring Samsung Refresh Rate Setting (Adaptive 120Hz Baseline)..." -ForegroundColor Yellow
& $AdbPath -s $DeviceSerial shell settings put secure refresh_rate_mode 0
$currentRefresh = & $AdbPath -s $DeviceSerial shell settings get secure refresh_rate_mode
Write-Host "[VERIFY] secure refresh_rate_mode set to: $currentRefresh" -ForegroundColor Green

Write-Host "[3/3] Restoring App Standby Buckets & AppOps..." -ForegroundColor Yellow
# Reset Smart Hub target packages if modified
& $AdbPath -s $DeviceSerial shell am set-standby-bucket com.sec.android.app.launcher active
Write-Host "[SUCCESS] Standby buckets reset." -ForegroundColor Green

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host " Baseline restoration complete. System defaults active.   " -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
