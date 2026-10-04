@echo off
cd /d "%~dp0"

if not exist bin mkdir bin

set JAVAC="C:\Program Files\Java\jdk1.8.0_202\bin\javac.exe"
if not exist %JAVAC% set JAVAC="C:\Program Files\Java\jdk1.7.0_80\bin\javac.exe"
if not exist %JAVAC% set JAVAC=javac

set BUILDDIR=%TEMP%\soultrail-client-build
if exist "%BUILDDIR%" rmdir /s /q "%BUILDDIR%"
mkdir "%BUILDDIR%"

call :killclient

set CP=
if exist deps\lwjgl.jar set CP=-cp deps\lwjgl.jar

echo Compiling client...
%JAVAC% -source 1.7 -target 1.7 %CP% -d "%BUILDDIR%" -sourcepath src src\*.java src\sign\*.java
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
