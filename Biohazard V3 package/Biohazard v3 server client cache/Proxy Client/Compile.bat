@echo off
title Biohazard Client - Compile
cd /d "%~dp0"

if not exist bin mkdir bin

set JAVAC="C:\Program Files\Java\jdk1.8.0_202\bin\javac.exe"
if not exist %JAVAC% set JAVAC="C:\Program Files\Java\jdk1.7.0_80\bin\javac.exe"
if not exist %JAVAC% set JAVAC=javac

echo Compiling client...
%JAVAC% -source 1.7 -target 1.7 -d bin -sourcepath src src\*.java src\sign\*.java
if errorlevel 1 (
	echo.
	echo Compile failed.
	pause
	exit /b 1
)

echo.
echo Compile finished.
pause
