@echo off
title Soul-Trail Client
cd /d "%~dp0"

if not exist bin (
	echo Client is not compiled. Run Compile.bat first.
	pause
	exit /b 1
)

set CP=bin
rem Phase 7.2a: deps\lwjgl.jar (LWJGL 2) is deliberately NOT on the classpath. Both it and
rem LWJGL 3 declare org.lwjgl.opengl.*, and LWJGL 2 was ahead, so its GL30 shadowed LWJGL
rem 3's and GlScene would not even compile. Nothing imports LWJGL 2 any more - LwjglPresent
rem is an inert stub - so the jar is dropped rather than disambiguated.
rem Phase 7.2a: LWJGL 3, used only when the 'gl' renderer is selected. Absent jars are
rem harmless - GlScene records why and the client stays on the software path.
if exist deps\lwjgl3 set CP=%CP%;deps\lwjgl3\*
rem No -Dorg.lwjgl.librarypath here: deps\natives holds LWJGL 2's DLLs (lwjgl.dll/64).
rem LWJGL 3 ships its own natives inside lwjgl*-natives-windows.jar and extracts them.
set NATIVES=

java -Xmx1024m %NATIVES% -cp "%CP%" game.Loader
pause
