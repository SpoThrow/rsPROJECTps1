@echo off
cd /d "%~dp0"

if not exist bin mkdir bin

set JAVAC="C:\Program Files\Java\jdk1.8.0_202\bin\javac.exe"
if not exist %JAVAC% set JAVAC=javac

set BUILDDIR=%TEMP%\soultrail-server-build
if exist "%BUILDDIR%" rmdir /s /q "%BUILDDIR%"
mkdir "%BUILDDIR%"

call :killserver

echo Compiling server...
set SRCS=src\core\net\*.java src\core\util\*.java src\core\util\log\*.java src\server\*.java src\server\clip\*.java src\server\clip\region\*.java src\server\content\*.java src\server\content\quests\*.java src\server\content\quests\misc\*.java src\server\content\skills\*.java src\server\content\skills\misc\*.java src\server\content\music\*.java src\server\event\*.java src\server\game\content\*.java src\server\game\items\*.java src\server\game\minigames\barrows\*.java src\server\game\minigames\castlewars\*.java src\server\game\minigames\pestcontrol\*.java src\server\game\minigames\tzhaar\*.java src\server\game\minigames\bountyhunter\*.java src\server\game\minigames\crystalchest\*.java src\server\game\minigames\gliding\*.java src\server\game\minigames\roguesden\*.java src\server\game\minigames\treasuretrails\*.java src\server\game\minigames\rangersguild\*.java src\server\game\minigames\randomevents\*.java src\server\game\minigames\sailing\*.java src\server\game\minigames\trawler\*.java src\server\game\npcs\*.java src\server\game\objects\*.java src\server\game\objects\doors\*.java src\server\game\players\*.java src\server\game\players\combat\*.java src\server\game\players\packets\*.java src\server\game\shops\*.java src\server\world\*.java src\server\world\definitions\*.java
set CP=deps/GTLVote.jar;deps/log4j-1.2.15.jar;deps/jython.jar;deps/xstream.jar;deps/mina.jar;deps/mysql.jar;deps/RuneTopListV2.jar;deps/poi.jar;deps/slf4j.jar;deps/slf4j-nop.jar
%JAVAC% -classpath %CP% -d "%BUILDDIR%" %SRCS%
if errorlevel 1 (
	echo.
	echo Server compile failed.
	if /I not "%~1"=="nopause" pause
	exit /b 1
)

call :installbin
if errorlevel 1 (
	echo First copy into bin failed, retrying after closing locked class files...
	call :killserver
	call :installbin
)
if errorlevel 1 (
	echo Second copy into bin failed, retrying once more...
	call :killserver
	call :installbin
)
if errorlevel 1 (
	echo.
	echo Server compile failed.
	echo Close the Soul-Trail Server window if Windows reports a locked .class file.
	if /I not "%~1"=="nopause" pause
	exit /b 1
)

echo Server compile finished.
if /I not "%~1"=="nopause" pause
exit /b 0

:killserver
echo Closing a running game server if it is locking class files...
taskkill /F /T /FI "WINDOWTITLE eq Soul-Trail Server*" >nul 2>&1
taskkill /F /T /FI "WINDOWTITLE eq Administrator: Soul-Trail Server*" >nul 2>&1
powershell -NoProfile -ExecutionPolicy Bypass -Command "Get-CimInstance Win32_Process | Where-Object { ($_.Name -eq 'java.exe' -or $_.Name -eq 'javaw.exe') -and $_.CommandLine -and ($_.CommandLine -match 'server\.Server' -or $_.CommandLine -match 'Xmx4000m') } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }; $deadline = (Get-Date).AddSeconds(8); while ((Get-Date) -lt $deadline) { $left = Get-CimInstance Win32_Process | Where-Object { ($_.Name -eq 'java.exe' -or $_.Name -eq 'javaw.exe') -and $_.CommandLine -and ($_.CommandLine -match 'server\.Server' -or $_.CommandLine -match 'Xmx4000m') }; if (-not $left) { break }; Start-Sleep -Milliseconds 250 }"
ping -n 2 127.0.0.1 >nul
goto :eof

:installbin
robocopy "%BUILDDIR%" bin /E /IS /IT /R:3 /W:1 /NFL /NDL /NJH /NJS /NP /NC /NS >nul
set COPYRC=%ERRORLEVEL%
if %COPYRC% GEQ 8 exit /b 1
exit /b 0
