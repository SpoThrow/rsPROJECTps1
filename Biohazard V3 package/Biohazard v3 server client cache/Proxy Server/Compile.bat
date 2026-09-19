@echo off
cd /d "%~dp0"

if not exist bin mkdir bin

set JAVAC="C:\Program Files\Java\jdk1.8.0_202\bin\javac.exe"
if not exist %JAVAC% set JAVAC=javac

echo Compiling server...
%JAVAC% -classpath deps/GTLVote.jar;deps/log4j-1.2.15.jar;deps/jython.jar;deps/xstream.jar;deps/mina.jar;deps/mysql.jar;deps/RuneTopListV2.jar;deps/poi.jar;deps/slf4j.jar;deps/slf4j-nop.jar -d bin src\core\net\*.java src\core\util\*.java src\core\util\log\*.java src\server\*.java src\server\clip\*.java src\server\clip\region\*.java src\server\content\*.java src\server\content\quests\*.java src\server\content\quests\misc\*.java src\server\content\skills\*.java src\server\content\skills\misc\*.java src\server\content\music\*.java src\server\event\*.java src\server\game\content\*.java src\server\game\items\*.java src\server\game\minigames\barrows\*.java src\server\game\minigames\castlewars\*.java src\server\game\minigames\pestcontrol\*.java src\server\game\minigames\tzhaar\*.java src\server\game\minigames\bountyhunter\*.java src\server\game\minigames\crystalchest\*.java src\server\game\minigames\gliding\*.java src\server\game\minigames\roguesden\*.java src\server\game\minigames\treasuretrails\*.java src\server\game\minigames\rangersguild\*.java src\server\game\minigames\randomevents\*.java src\server\game\minigames\sailing\*.java src\server\game\minigames\trawler\*.java src\server\game\npcs\*.java src\server\game\objects\*.java src\server\game\objects\doors\*.java src\server\game\players\*.java src\server\game\players\combat\*.java src\server\game\players\packets\*.java src\server\game\shops\*.java src\server\world\*.java src\server\world\definitions\*.java
if errorlevel 1 (
	echo.
	echo Server compile failed.
	if /I not "%~1"=="nopause" pause
	exit /b 1
)

echo Server compile finished.
if /I not "%~1"=="nopause" pause
exit /b 0
