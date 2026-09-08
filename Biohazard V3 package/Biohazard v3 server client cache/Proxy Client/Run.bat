@echo off
title Biohazard Client
cd /d "%~dp0"

if not exist bin (
	echo Client is not compiled. Run Compile.bat first.
	pause
	exit /b 1
)

java -Xmx1024m -cp bin Loader
pause
