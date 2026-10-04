@echo off
title Soul-Trail Client
cd /d "%~dp0"

if not exist bin (
	echo Client is not compiled. Run Compile.bat first.
	pause
	exit /b 1
)

set CP=bin
if exist deps\lwjgl.jar set CP=bin;deps\lwjgl.jar
set NATIVES=
if exist deps\natives set NATIVES=-Dorg.lwjgl.librarypath="%~dp0deps\natives"

java -Xmx1024m %NATIVES% -cp "%CP%" Loader
pause
