@echo off
chcp 65001 >nul
setlocal
cd /d "%~dp0"

echo Starting Trans Finance platform...
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\run-packaged-app.ps1"

if errorlevel 1 (
    echo.
    echo Startup failed. Please check Java, Python, MySQL, database import, and port settings.
    pause
)

endlocal
