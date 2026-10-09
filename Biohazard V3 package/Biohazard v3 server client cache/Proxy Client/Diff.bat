@echo off
title Soul-Trail Client (GL vs software A/B diff)
cd /d "%~dp0"

rem Phase 7.5a: launches the client with the A/B diff switched on, so a live session
rem answers "is the GL image the RIGHT picture?" from the log rather than by eye.
rem
rem It works because the software scene is NOT skipped - it is fully drawn into the
rem producer's buffer by the shadow stage, and the GL readback then overwrites that
rem same array in place. So at the instant before the readback there is a per-pixel
rem ground truth for exactly this frame, and GlSceneRenderer copies it.
rem
rem Run.bat is untouched: the diff is off unless -Dsoultrail.gldiff names it, so
rem normal play is unchanged and "it was on" is never an assumption.
rem
rem WHAT TO LOOK FOR. The first composited frame prints in full:
rem
rem   Renderer 'gl': GL vs SOFTWARE diff of frame #N (WxH) - compared ... VERDICT: ...
rem
rem and the VERDICT is the point of the run:
rem
rem   IDENTICAL      - GL and software agree on every pixel.
rem   SAMPLING-CLASS - differences are scattered and no 8x8 block is entirely
rem                    different. EXPECTED and benign: the software evaluates the
rem                    texture mapping every 8 pixels and interpolates between, so it
rem                    can pick the neighbouring texel on a few percent of pixels.
rem   AREA-CLASS     - some 8x8 block is ENTIRELY different. Uniform scattered noise
rem                    cannot do that, so this is NOT sampling, and it says which of
rem                    the two causes: large deltas = WRONG COLOUR came from somewhere
rem                    (texture id / atlas layer); small deltas = the right asset
rem                    shaded systematically wrong (shade block / fog fade).
rem
rem ⚠ A run with renderer=software in client_settings.properties has no GL image to
rem compare, so the diff line will not appear at all. Check that file says
rem renderer=gl before drawing a conclusion from silence.
rem
rem ⚠ And check fogStrength: at 0 the fog fade is a no-op, so a fog verdict from such
rem a run is a verdict about nothing.

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

java -Xmx1024m %NATIVES% -Dsoultrail.gldiff=true -cp "%CP%" game.Loader
pause
