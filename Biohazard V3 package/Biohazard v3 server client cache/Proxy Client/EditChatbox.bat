@echo off
rem ---------------------------------------------------------------------------
rem Creation chatbox layout editor.
rem
rem Opens a 2x drawing of the MakeX panel with a draggable box for each thing it
rem places. Dragging a box edits the pixel constants in MakeX.java - the names on
rem the boxes are the constant names - and "Save to MakeX.java" (or a drag, with
rem auto-write on) writes them back. "Compile client" then runs gradlew.
rem
rem Deliberately standalone: the editor only needs Swing and ImageIO, so it is
rem compiled into build\chatbox-editor and run on its own. Nothing in it loads the
rem client, the cache or Sprite, which is why it works while the client does not.
rem ---------------------------------------------------------------------------
setlocal
cd /d "%~dp0"

set "SRC=src\ui\MakeX.java"
set "OUT=build\chatbox-editor"
set "SPRITES=%USERPROFILE%\Biohazard.474\Sprites"

if not exist "%SRC%" (
  echo Could not find %SRC% - run this from the Proxy Client folder.
  pause
  exit /b 1
)

where javac >nul 2>nul
if errorlevel 1 (
  echo javac was not found on PATH. Install a JDK, or run this from a shell where the JDK's bin is on PATH.
  pause
  exit /b 1
)

if not exist "%OUT%" mkdir "%OUT%"

echo Compiling the editor...
javac -d "%OUT%" "tools\chatbox-editor\ChatboxEditor.java"
if errorlevel 1 (
  echo The editor did not compile.
  pause
  exit /b 1
)

echo Opening the editor...
java -cp "%OUT%" ChatboxEditor "%SRC%" "%SPRITES%"
if errorlevel 1 pause
endlocal
