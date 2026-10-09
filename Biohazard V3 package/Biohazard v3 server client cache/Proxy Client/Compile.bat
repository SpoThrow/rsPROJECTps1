@echo off
REM ---------------------------------------------------------------------------
REM Client compile - a thin wrapper over the Gradle build (CLIENT_REFACTORING_PLAN.md, Phase 1).
REM
REM ---------------------------------------------------------------------------
REM WHY THIS IS A WRAPPER AND NOT A javac SCRIPT ANY MORE - and it is a bug this
REM file caused itself, not a matter of taste.
REM
REM It used to compile a hand-written source list: `src\*.java src\sign\*.java`.
REM Phase 3.1 moved every class into a package, so `src\*.java` has matched
REM NOTHING since that commit. The failure is indirect, which is what made it
REM survive: javac does not report "no files matched"; it treats the unexpanded
REM pattern as a filename and exits with
REM
REM     javac: file not found: src\*.java
REM
REM so the message points at a missing file rather than at a stale source list,
REM and this script then printed "Client compile failed" for the wrong reason.
REM
REM The blast radius was wider than this file, which is the part worth recording:
REM StartAll.bat calls this script and aborts the whole run on a non-zero exit,
REM so the master script died at its client step and the server was never
REM started. Run.bat's own "Run Compile.bat first" message also pointed here.
REM
REM There is no way to keep a second source list in step with the tree by
REM inspection - that is the whole reason the Gradle build exists - so this now
REM calls that build instead of duplicating it.
REM
REM `gradlew.bat installBin` compiles to build/ and then copies the classes into
REM bin/, the directory Run.bat launches from. `gradlew.bat check` additionally
REM runs the harness, the stale-class report and the collision-size freshness
REM check. This script deliberately runs ONLY the compile, so starting the game
REM never depends on the test suite passing - RunTests.bat and `check` are for
REM that, and keeping them separate is the pre-existing intent (RunTests.bat's
REM header says so).
REM
REM Two behaviours are carried over from the old script because other scripts
REM depend on them, not out of nostalgia:
REM   * the `nopause` argument, which StartAll.bat passes so that its unattended
REM     run is not left waiting on a keypress; and
REM   * :killclient, which runs BEFORE the compile. Gradle's copy cannot
REM     overwrite a .class file the running JVM has open, and "close the game
REM     window and try again" is not something a player should have to infer
REM     from a file-lock error.
REM
REM WARNING: :killclient below runs taskkill /F on the game window. Do not run
REM this as a convenience step while playing.
REM ---------------------------------------------------------------------------
cd /d "%~dp0"

call :killclient

echo Compiling client...
call gradlew.bat --console=plain installBin
if errorlevel 1 (
	echo First attempt failed - retrying after closing locked class files...
	call :killclient
	call gradlew.bat --console=plain installBin
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
