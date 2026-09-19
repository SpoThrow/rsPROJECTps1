@echo off
cd /d "%~dp0"

if not exist bin mkdir bin

set JAVAC="C:\Program Files\Java\jdk1.8.0_202\bin\javac.exe"
if not exist %JAVAC% set JAVAC="C:\Program Files\Java\jdk1.7.0_80\bin\javac.exe"
if not exist %JAVAC% set JAVAC=javac

echo Closing a running game client if it is locking class files...
for /f "tokens=2 delims=," %%P in ('tasklist /v /fo csv ^| findstr /i /c:"java.exe" ^| findstr /i /c:"Loader"') do (
	taskkill /PID %%~P /F >nul 2>&1
)

echo Compiling client...
%JAVAC% -source 1.7 -target 1.7 -d bin -sourcepath src src\*.java src\sign\*.java
if errorlevel 1 (
	echo First compile pass failed, retrying...
	timeout /t 2 /nobreak >nul
	%JAVAC% -source 1.7 -target 1.7 -d bin -sourcepath src src\*.java src\sign\*.java
)
if errorlevel 1 (
	echo.
	echo Client compile failed.
	echo Close the running client window if Windows reports a locked .class file.
	if /I not "%~1"=="nopause" pause
	exit /b 1
)

echo Client compile finished.
if /I not "%~1"=="nopause" pause
exit /b 0
