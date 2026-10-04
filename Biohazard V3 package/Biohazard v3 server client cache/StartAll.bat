@echo off
title Soul-Trail - Master Compile and Run
cd /d "%~dp0"

echo ========================================
echo Soul-Trail Master Compile and Run
echo ========================================
echo.

echo Closing previous Soul-Trail windows so class files are not locked...
taskkill /F /T /FI "WINDOWTITLE eq Soul-Trail Server*" >nul 2>&1
taskkill /F /T /FI "WINDOWTITLE eq Administrator: Soul-Trail Server*" >nul 2>&1
taskkill /F /T /FI "WINDOWTITLE eq Soul-Trail Client*" >nul 2>&1
taskkill /F /T /FI "WINDOWTITLE eq Administrator: Soul-Trail Client*" >nul 2>&1
taskkill /F /FI "WINDOWTITLE eq Soul-Trail" >nul 2>&1
powershell -NoProfile -ExecutionPolicy Bypass -Command "Get-CimInstance Win32_Process | Where-Object { ($_.Name -eq 'java.exe' -or $_.Name -eq 'javaw.exe') -and $_.CommandLine -and ($_.CommandLine -match 'server\.Server' -or $_.CommandLine -match 'Xmx4000m' -or $_.CommandLine -match 'Loader' -or $_.CommandLine -match 'Xmx1024m') } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }"
ping -n 3 127.0.0.1 >nul
echo.

echo [1/4] Compiling Server...
cd "Proxy Server"
call Compile.bat nopause
if errorlevel 1 (
    echo.
    echo Server compile failed. Aborting.
    pause
    exit /b 1
)
cd ..
echo Server compiled successfully.
echo.

echo [2/4] Compiling Client...
cd "Proxy Client"
call Compile.bat nopause
if errorlevel 1 (
    echo.
    echo Client compile failed. Aborting.
    pause
    exit /b 1
)
cd ..
echo Client compiled successfully.
echo.

echo [3/4] Starting Server...
cd "Proxy Server"
start "Soul-Trail Server" cmd /k Run.bat
cd ..
echo Server started in new window.
echo.

echo [4/4] Starting Client...
cd "Proxy Client"
start "Soul-Trail Client" cmd /k Run.bat
cd ..
echo Client started in new window.
echo.

echo ========================================
echo All systems started successfully!
echo ========================================
echo.
echo Server and client are running in separate windows.
echo Close this window to keep them running.
pause
exit /b 0
