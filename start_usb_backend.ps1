# HireGrid USB Backend Runner for PowerShell
$possibleAdbPaths = @(
    "C:\androidsdk\platform-tools\adb.exe",
    "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
)

$adbPath = $possibleAdbPaths | Where-Object { Test-Path $_ } | Select-Object -First 1

Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "           HireGrid USB Development Server             " -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan

if ($adbPath) {
    Write-Host "[1/2] Found adb at: $adbPath" -ForegroundColor Gray
    Write-Host "[1/2] Setting up USB port forwarding (Phone localhost:8000 -> PC:8000)..." -ForegroundColor Yellow
    & $adbPath reverse tcp:8000 tcp:8000
    if ($LASTEXITCODE -eq 0) {
        Write-Host "[OK] USB port forwarding active! Your phone can now reach the server." -ForegroundColor Green
    } else {
        Write-Host "[!] Note: Check that USB Debugging is ON and your phone is unlocked." -ForegroundColor Yellow
    }
} else {
    Write-Host "[!] Warning: adb.exe not found at C:\androidsdk\platform-tools\adb.exe" -ForegroundColor Red
}

Write-Host "[2/2] Starting HireGrid FastAPI Backend on 0.0.0.0:8000..." -ForegroundColor Cyan
Set-Location "$PSScriptRoot"
& ".\backend\venv\Scripts\python.exe" -m uvicorn app.main:app --app-dir backend --reload --host 0.0.0.0 --port 8000
