@echo off
title HireGrid USB Backend Runner
echo ========================================================
echo           HireGrid USB Development Server
echo ========================================================

set ADB="C:\androidsdk\platform-tools\adb.exe"
if not exist %ADB% (
    set ADB="%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
)

if exist %ADB% (
    echo [1/2] Setting up USB reverse port forwarding (Phone localhost:8000 -^> PC:8000)...
    %ADB% reverse tcp:8000 tcp:8000
    if %ERRORLEVEL% equ 0 (
        echo [OK] USB port forwarding connected successfully!
    ) else (
        echo [!] Warning: Device not recognized by ADB or USB debugging is disabled.
    )
) else (
    echo [!] Could not locate adb.exe at %ADB%
)

echo [2/2] Starting HireGrid FastAPI Backend...
cd /d "%~dp0"
.\backend\venv\Scripts\python.exe -m uvicorn app.main:app --app-dir backend --reload --host 0.0.0.0 --port 8000
pause
