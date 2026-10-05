# ---------------------------------------------------------------------------
# Phase 7.1 GL smoke test - proves the OFFSCREEN mechanism on THIS machine.
#
# WHY THIS EXISTS
# 7.1 is a decision step, and the decision is "which GL integration, and why". Arguing that
# choice from documentation is not evidence, so this tool measures the mechanism the choice
# depends on before 7.2 is designed on top of it. It is deliberately tiny and has no client
# dependency: it does not touch AWT at all.
#
# WHAT IT PROVES (each check is falsifiable, and a failure exits non-zero)
#   1. an OpenGL context exists on a HIDDEN GLFW window - its own HWND, no AWT parent and
#      no Display.setParent (the approach LwjglPresent.java records as a Windows deadlock),
#   2. GLSL compiles and links,
#   3. an offscreen FBO with a colour AND a depth renderbuffer is FRAMEBUFFER_COMPLETE -
#      the target 7.2 would render the scene into,
#   4. glReadPixels returns what was actually drawn into that FBO,
#   5. the FBO's depth buffer discriminates - a nearer triangle wins at the same pixel.
#   Plus a CONTROL: an undrawn pixel must NOT read back as the drawn colour, so a readback
#   returning something constant cannot report success.
#
# FINDING WORTH KEEPING (found by the FIRST version of this test, which used fixed-function)
#   GLFW at version 3.3 defaults to a CORE profile here, where glMatrixMode/glBegin do not
#   exist and calling them ABORTS the JVM. So a Phase 7 port cannot be GL 2.x fixed-function;
#   it must be shaders + VBOs. That happens to match what Phase 5 prepared - 5.1's read-only
#   geometry accessors and 5.2's GpuFloatBuffer/GpuIntBuffer are exactly an upload path.
#
# RUNTIME NOTE
#   On Java 24+ loading LWJGL's natives prints restricted-method warnings; the flag that
#   silences them is --enable-native-access=ALL-UNNAMED. Not required to pass, noted here so
#   it is not a surprise in the real client (Run.bat runs plain `java`, i.e. whatever is on
#   PATH).
#
# USAGE
#   powershell -NoProfile -File tools\gl-smoke\run.ps1
#
# Exit code: 0 = GL_SMOKE_OK, 1 = any check failed.
# ---------------------------------------------------------------------------
$ErrorActionPreference = 'Stop'

$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$clientDir = Resolve-Path (Join-Path $here '..\..')
$depsDir = Join-Path $clientDir 'deps\lwjgl3'
$outDir = Join-Path $env:TEMP 'biohazard-gl-smoke'

if (-not (Test-Path $depsDir)) {
    Write-Host "LWJGL 3 not found at $depsDir"
    exit 1
}

$jars = (Get-ChildItem $depsDir -Filter *.jar | ForEach-Object { $_.FullName }) -join ';'
if (-not $jars) {
    Write-Host "No LWJGL 3 jars in $depsDir"
    exit 1
}

New-Item -ItemType Directory -Force $outDir | Out-Null

Write-Host "Compiling the GL smoke test against deps\lwjgl3..."
& javac -cp $jars -d $outDir (Join-Path $here 'GlSmokeTest.java')
if ($LASTEXITCODE -ne 0) {
    Write-Host "compile FAILED"
    exit 1
}

Write-Host ""
& java -cp "$outDir;$jars" GlSmokeTest
exit $LASTEXITCODE
