@echo off
title Soul-Trail Server
cd /d "%~dp0"

rem Compiles if needed, then starts the server on the Java 21 toolchain.
rem Heap size and main class are set in build.gradle.
call gradlew.bat --console=plain run
pause
