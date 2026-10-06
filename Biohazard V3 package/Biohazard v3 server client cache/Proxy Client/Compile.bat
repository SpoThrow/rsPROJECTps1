@echo off
REM ---------------------------------------------------------------------------
REM DEPRECATED - prefer the Gradle build (CLIENT_REFACTORING_PLAN.md, Phase 1):
REM     gradlew.bat installBin     compile and copy classes into bin\
REM     gradlew.bat check          harness + stale-class + cfg-freshness checks
REM     gradlew.bat build          the lot
REM
REM Kept as a working fallback until a Gradle-built client has been confirmed on
REM a live session against the server. Two differences from Gradle:
REM   * Gradle pins --release 8; this script used -source/-target 1.7, which
REM     modern javac rejects outright and which only worked because the script
REM     pins a JDK 8 below.
REM   * Gradle reads the sources as UTF-8. This script used to set no -encoding,
REM     so it fell back to the platform default (Cp1252) while the sources are
REM     UTF-8: four player-visible strings compiled to mojibake, and any comment
REM     containing a non-ASCII character (e.g. the warning sign) failed the
REM     compile outright with "unmappable character for encoding Cp1252".
REM     This script now passes -encoding UTF-8 so BOTH builds agree. Verified:
REM     without the flag the client fails with 3 unmappable-character errors;
REM     with it, 207 classes compile and the em dash is a real U+2014.
REM
REM WARNING: :killclient below runs taskkill /F on the game window. Do not run
REM this as a convenience step while playing.
REM ---------------------------------------------------------------------------
cd /d "%~dp0"

if not exist bin mkdir bin

set JAVAC="C:\Program Files\Java\jdk1.8.0_202\bin\javac.exe"
if not exist %JAVAC% set JAVAC="C:\Program Files\Java\jdk1.7.0_80\bin\javac.exe"
if not exist %JAVAC% set JAVAC=javac

set BUILDDIR=%TEMP%\soultrail-client-build
if exist "%BUILDDIR%" rmdir /s /q "%BUILDDIR%"
mkdir "%BUILDDIR%"

call :killclient

rem Phase 7.2a: LWJGL 2 (deps\lwjgl.jar) is not used - it shadows LWJGL 3's org.lwjgl.opengl.*.
rem NOTE: this script is superseded by `gradlew.bat installBin` (see build.gradle); its source
rem list still names the pre-package src\*.java layout and no longer matches the tree.
set CP=
if exist deps\lwjgl3 set CP=-cp "deps\lwjgl3\*"

echo Compiling client...
%JAVAC% -encoding UTF-8 -source 1.7 -target 1.7 %CP% -d "%BUILDDIR%" -sourcepath src src\*.java src\sign\*.java
if errorlevel 1 (
	echo.
	echo Client compile failed.
	if /I not "%~1"=="nopause" pause
	exit /b 1
)

call :installbin
if errorlevel 1 (
	echo First copy into bin failed, retrying after closing locked class files...
	call :killclient
	call :installbin
)
if errorlevel 1 (
	echo Second copy into bin failed, retrying once more...
	call :killclient
	call :installbin
)
if errorlevel 1 (
	echo.
	echo Client compile failed.
	echo Close the Soul-Trail game window if Windows reports a locked .class file.
	if /I not "%~1"=="nopause" pause
	exit /b 1
)

echo Client compile finished.
if /I not "%~1"=="nopause" pause
exit /b 0

:killclient
echo Closing a running game client if it is locking class files...
taskkill /F /T /FI "WINDOWTITLE eq Soul-Trail Client*" >nul 2>&1
taskkill /F /T /FI "WINDOWTITLE eq Administrator: Soul-Trail Client*" >nul 2>&1
taskkill /F /FI "WINDOWTITLE eq Soul-Trail" >nul 2>&1
powershell -NoProfile -ExecutionPolicy Bypass -Command "Get-CimInstance Win32_Process | Where-Object { ($_.Name -eq 'java.exe' -or $_.Name -eq 'javaw.exe') -and $_.CommandLine -and ($_.CommandLine -match 'Loader' -or $_.CommandLine -match 'Xmx1024m') } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }; $deadline = (Get-Date).AddSeconds(8); while ((Get-Date) -lt $deadline) { $left = Get-CimInstance Win32_Process | Where-Object { ($_.Name -eq 'java.exe' -or $_.Name -eq 'javaw.exe') -and $_.CommandLine -and ($_.CommandLine -match 'Loader' -or $_.CommandLine -match 'Xmx1024m') }; if (-not $left) { break }; Start-Sleep -Milliseconds 250 }"
ping -n 2 127.0.0.1 >nul
goto :eof

:installbin
robocopy "%BUILDDIR%" bin /E /IS /IT /R:3 /W:1 /NFL /NDL /NJH /NJS /NP /NC /NS >nul
set COPYRC=%ERRORLEVEL%
if %COPYRC% GEQ 8 exit /b 1
exit /b 0
