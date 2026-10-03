@echo off
cd /d "%~dp0"

call :killserver

echo Compiling server...
call gradlew.bat --console=plain compileJava
if errorlevel 1 (
	echo.
	echo Server compile failed.
	if /I not "%~1"=="nopause" pause
	exit /b 1
)

echo Server compile finished.
echo Classes are in build\classes\java\main - use Run.bat to start the server.
if /I not "%~1"=="nopause" pause
exit /b 0

:killserver
echo Closing a running game server if it is locking class files...
taskkill /F /T /FI "WINDOWTITLE eq Soul-Trail Server*" >nul 2>&1
taskkill /F /T /FI "WINDOWTITLE eq Administrator: Soul-Trail Server*" >nul 2>&1
powershell -NoProfile -ExecutionPolicy Bypass -Command "Get-CimInstance Win32_Process | Where-Object { ($_.Name -eq 'java.exe' -or $_.Name -eq 'javaw.exe') -and $_.CommandLine -and ($_.CommandLine -match 'server\.Server' -or $_.CommandLine -match 'Xmx4000m') } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }; $deadline = (Get-Date).AddSeconds(8); while ((Get-Date) -lt $deadline) { $left = Get-CimInstance Win32_Process | Where-Object { ($_.Name -eq 'java.exe' -or $_.Name -eq 'javaw.exe') -and $_.CommandLine -and ($_.CommandLine -match 'server\.Server' -or $_.CommandLine -match 'Xmx4000m') }; if (-not $left) { break }; Start-Sleep -Milliseconds 250 }"
ping -n 2 127.0.0.1 >nul
goto :eof
