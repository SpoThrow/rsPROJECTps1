@echo off
title Biohazard - Master Compile and Run
cd /d "%~dp0"

echo ========================================
echo Biohazard Master Compile and Run
echo ========================================
echo.

echo [1/4] Compiling Server...
cd "Proxy Server"
call Compile.bat
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
call Compile.bat
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
start "Biohazard Server" cmd /k Run.bat
cd ..
echo Server started in new window.
echo.

echo [4/4] Starting Client...
cd "Proxy Client"
start "Biohazard Client" cmd /k Run.bat
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
