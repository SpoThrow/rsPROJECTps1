@echo off
REM Headless client tests - Phase 0.4 of CLIENT_REFACTORING_PLAN.md.
REM Compiles test\*.java against the already-built bin\ and runs the harness.
REM Deliberately does NOT rebuild the client or kill a running game client
REM (Compile.bat's :killclient would close the player's window).
cd /d "%~dp0"

if not exist bin (
	echo Client is not compiled. Run Compile.bat first.
	pause
	exit /b 1
)

set JAVAC=javac
if exist "C:\Program Files\Java\jdk1.8.0_202\bin\javac.exe" set JAVAC="C:\Program Files\Java\jdk1.8.0_202\bin\javac.exe"

set OUT=%TEMP%\soultrail-client-tests
if exist "%OUT%" rmdir /s /q "%OUT%"
mkdir "%OUT%"

echo Compiling tests against bin...
%JAVAC% -nowarn -cp bin -d "%OUT%" -sourcepath test test\ClientHarness.java
if errorlevel 1 (
	echo.
	echo Test compile failed.
	if /I not "%~1"=="nopause" pause
	exit /b 1
)

echo Running harness...
java -cp "%OUT%;bin" ClientHarness
set RC=%ERRORLEVEL%

echo.
if %RC% neq 0 echo Tests FAILED.
if %RC% equ 0 echo Tests passed.
if /I not "%~1"=="nopause" pause
exit /b %RC%
