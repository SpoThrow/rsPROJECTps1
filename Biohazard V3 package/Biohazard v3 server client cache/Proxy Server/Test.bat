@echo off
cd /d "%~dp0"

echo Running server tests...
call gradlew.bat --console=plain test
if errorlevel 1 (
	echo.
	echo Tests failed.
	if /I not "%~1"=="nopause" pause
	exit /b 1
)

echo All tests passed.
if /I not "%~1"=="nopause" pause
exit /b 0
