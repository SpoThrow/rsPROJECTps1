@echo off
REM ---------------------------------------------------------------------------
REM Golden-master capture - Phase 0.3 of CLIENT_REFACTORING_PLAN.md.
REM
REM Launches the client EXACTLY as Run.bat does, but with the passive packet tap
REM switched on via -Dsoultrail.packettap. The tap only observes; it does not
REM change a single byte the client sends or receives, so a capture taken with it
REM is a valid baseline for the unmodified client.
REM
REM Usage:  Record.bat [output-file]
REM         default: packet-tap-<timestamp>.log, next to this script.
REM
REM Re-run it after a phase and diff the two logs to prove the protocol did not
REM drift. The logs have no timestamps precisely so they can be diffed directly.
REM
REM When the client exits, this script VERIFIES the capture (tools/verify-capture.ps1)
REM before trusting it - a run where `java` never started, or where nothing was
REM logged, used to look identical to a good one and silently wasted the session.
REM If the check passes it runs the structural comparison for you.
REM ---------------------------------------------------------------------------
cd /d "%~dp0"

if not exist bin (
	echo Client is not compiled. Run:  gradlew.bat installBin
	if /I not "%~1"=="nopause" pause
	exit /b 1
)

REM Default the output name WITHOUT a parenthesised block: `%STAMP%` inside the
REM block below would be expanded when the whole block is parsed, before `set
REM STAMP` has run, producing "packet-tap-.log". Branching avoids needing delayed
REM expansion (which would also make `!` in a path special).
set OUT=%~1
if not "%OUT%"=="" goto :namedout
for /f %%i in ('powershell -NoProfile -Command "Get-Date -Format yyyyMMdd-HHmmss"') do set STAMP=%%i
REM Fallback, and it matters: if the clock lookup returns nothing the name below
REM collapses to "packet-tap-.log" - the exact name of the stale pre-fix capture
REM sitting in this folder. That collision is how a vacuous "IDENTICAL" gets
REM started, so never let the stamp come out empty.
if "%STAMP%"=="" set STAMP=manual-%RANDOM%
set OUT=packet-tap-%STAMP%.log
:namedout

REM Display path: a relative OUT lives next to this script, an absolute one is
REM already complete. Without this the banner would print e.g. "C:\client\C:\tmp\x.log".
set DISPLAY=%OUT%
echo "%OUT%" | findstr /r /c:":" >nul
if errorlevel 1 set DISPLAY=%CD%\%OUT%

set CP=bin
if exist deps\lwjgl.jar set CP=bin;deps\lwjgl.jar
set NATIVES=
if exist deps\natives set NATIVES=-Dorg.lwjgl.librarypath="%~dp0deps\natives"

REM Prove the output path is writable BEFORE the session, not after. The tap only
REM opens its file on the first packet, so a bad path or a read-only folder would
REM otherwise only surface once the capture was already lost. Probe then delete, so
REM no misleading empty log is left behind.
> "%OUT%.probe" echo probe 2>nul
if not exist "%OUT%.probe" (
	echo.
	echo   ERROR: cannot write to "%DISPLAY%"
	echo   Check the folder exists and is writable, or pass a different path:
	echo       Record.bat C:\some\writable\folder\capture.log
	echo.
	if /I not "%~1"=="nopause" pause
	exit /b 1
)
del "%OUT%.probe" >nul 2>&1

echo.
echo   ================================================================
echo    GOLDEN MASTER CAPTURE SESSION
echo   ================================================================
echo    Packet log : %DISPLAY%
echo    Client     : unchanged (tap is passive, off by default)
echo.
echo    Please perform, in order:
echo      1. log in
echo      2. walk a short distance
echo      3. attack something once
echo      4. pick one item up off the ground
echo    and while recording, also visit the 667 content:
echo      5. walk past Nex, and past any curse / Blood reaver content
echo      6. the Rock Crab area (where the noted-item crash happened)
echo.
echo    Record the screen too, then close the client normally.
echo   ================================================================
echo.

java -Xmx1024m %NATIVES% -Dsoultrail.packettap="%OUT%" -cp "%CP%" game.Loader
set GAMEEXIT=%ERRORLEVEL%
echo.
echo Client exited (code %GAMEEXIT%).

REM Do NOT report success just because the banner above was printed. The old
REM version of this file ended with "Packet log written to:" unconditionally,
REM so a run where the JVM never started looked exactly like a good one. Verify
REM the capture is real (exists, holds units, is not the golden master) before
REM claiming anything, and only then spend time comparing it.
echo Verifying capture: %DISPLAY%
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\verify-capture.ps1" -Capture "%DISPLAY%" -GoldenDir "%~dp0golden-master"
set VERIFY=%ERRORLEVEL%
if not "%VERIFY%"=="0" goto :unusable

if not exist gradlew.bat goto :nogradle
echo Comparing against the golden master (structure only)...
call gradlew.bat compareCapture -Pcapture="%DISPLAY%" --console=plain
echo.
echo   Comparison exit code: %ERRORLEVEL%   (0 = identical structure, 1 = see the diff above)
goto :done

:nogradle
echo   gradlew.bat not found, so the comparison was not run. Run it yourself:
echo       gradlew.bat compareCapture -Pcapture="%DISPLAY%"
goto :done

:unusable
echo.
echo   No usable capture was produced, so there is nothing to compare. The packet
echo   log above never got written - see the reason printed by the verifier.

:done
if /I not "%~1"=="nopause" pause
exit /b 0
