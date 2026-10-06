# ---------------------------------------------------------------------------
# GL classpath probe - the live-gate companion to Phase 7.2a.
#
# WHY THIS EXISTS
# 7.2a removed deps\lwjgl.jar (LWJGL 2) from every classpath, because it and
# deps\lwjgl3\ BOTH declare org.lwjgl.opengl.* and LWJGL 2 was winning the
# lookup - its GL30 has no GL14 ancestor, so GL30.GL_DEPTH_COMPONENT24 did not
# resolve and src\ui\GlScene.java would not compile.
#
# Nothing else can see this at runtime. The live capture runs renderer=software,
# so it never touches org.lwjgl and would pass identically if the collision came
# back; and a green compile only proves the constant resolves for javac, not
# which JAR the running JVM picks. The runtime failure is the worse one: LWJGL 2
# static GL entry points route through a context GLFW never initialises.
#
# So this probe loads the classes on the EXACT runtime classpath (bin +
# deps\lwjgl3\*, the same string Run.bat/Record.bat build) and asserts the LWJGL
# 3 arrangement specifically: GL30 must reach GL14 by INHERITANCE and the
# constant must read back. Under LWJGL 2 that lookup throws.
#
# USAGE
#   powershell -NoProfile -File tools\gl-classpath-probe\run.ps1
#   powershell -NoProfile -File tools\gl-classpath-probe\run.ps1 -Mutate
#
# The -Mutate switch prepends deps\lwjgl.jar to the classpath - recreating the
# pre-7.2a ordering - and the probe MUST then report CLASSPATH_PROBE_FAIL. If it
# reports OK, the probe is vacuous and proves nothing. That is the regression
# this probe exists to catch, made catchable on demand.
#
# Exit code: the probe's own (0 = CLASSPATH_PROBE_OK, 1 = CLASSPATH_PROBE_FAIL).
# ---------------------------------------------------------------------------
param(
	[switch]$Mutate
)

$ErrorActionPreference = 'Stop'

$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$clientDir = Resolve-Path (Join-Path $here '..\..')
$binDir = Join-Path $clientDir 'bin'
$lwjgl3 = Join-Path $clientDir 'deps\lwjgl3'
$lwjgl2 = Join-Path $clientDir 'deps\lwjgl.jar'

if (-not (Test-Path $binDir)) {
	Write-Host "Client is not compiled. Run:  gradlew.bat installBin"
	exit 1
}
if (-not (Test-Path $lwjgl3)) {
	Write-Host "deps\lwjgl3 is missing - Phase 7.2a vendored LWJGL 3 there."
	exit 1
}

$work = Join-Path $env:TEMP ('gl-classpath-probe-' + [System.Guid]::NewGuid().ToString('N'))
$outDir = Join-Path $work 'out'
New-Item -ItemType Directory -Path $outDir -Force | Out-Null

# The runtime ordering, exactly as the launchers build it: bin, then LWJGL 3.
# Under -Mutate, LWJGL 2 goes FIRST, which is what used to happen and is the
# whole point of the probe.
if ($Mutate) {
	if (-not (Test-Path $lwjgl2)) {
		Write-Host "Cannot mutate: deps\lwjgl.jar is gone (deleted after 7.2a)."
		Write-Host "The mutation is kept for as long as the jar is, matching Phase 8.1."
		Remove-Item -Recurse -Force $work
		exit 1
	}
	$cp = "$binDir;$lwjgl2;" + (Join-Path $lwjgl3 '*')
	Write-Host "MUTATION: deps\lwjgl.jar (LWJGL 2) placed AHEAD of deps\lwjgl3 (LWJGL 3)."
	Write-Host "          Expecting CLASSPATH_PROBE_FAIL. OK here would mean the probe is vacuous."
} else {
	$cp = "$binDir;" + (Join-Path $lwjgl3 '*')
}
Write-Host ""

$src = Join-Path $here 'ClasspathProbe.java'
$outClasses = Join-Path $work 'classes'
New-Item -ItemType Directory -Path $outClasses -Force | Out-Null

Write-Host "Compiling the probe (no display and no GL context needed)..."
& javac -nowarn -d $outClasses $src
if ($LASTEXITCODE -ne 0) {
	Write-Host "Probe failed to compile."
	Remove-Item -Recurse -Force $work
	exit 1
}

Write-Host "Running on the runtime classpath:"
# A Java 25 JVM prints restricted-method warnings while LWJGL loads (System::load,
# sun.misc.Unsafe::objectFieldOffset). They are expected and harmless; the flag that
# silences them is --enable-native-access=ALL-UNNAMED. Not required to pass, and the
# probe deliberately does not pass it - the same choice tools\gl-smoke makes - so the
# probe stays runnable on an older JVM that does not recognise the flag.
& java -cp "$cp;$outClasses" tools.glclasspathprobe.ClasspathProbe
$probeExit = $LASTEXITCODE

Remove-Item -Recurse -Force $work -ErrorAction SilentlyContinue

Write-Host ""
if ($Mutate) {
	if ($probeExit -eq 1) {
		Write-Host "MUTATION CAUGHT: the probe fails when LWJGL 2 shadows LWJGL 3 - it is not vacuous."
		exit 0
	}
	Write-Host "MUTATION MISSED: the probe reported OK with LWJGL 2 ahead. It proves nothing."
	exit 1
}

if ($probeExit -eq 0) {
	Write-Host "OK: LWJGL 3 owns org.lwjgl.opengl.* on the real runtime classpath, and"
	Write-Host "    GL30 inherits GL_DEPTH_COMPONENT24 from GL14 (LWJGL 2 cannot do this)."
} else {
	Write-Host "FAIL: the LWJGL 2 / LWJGL 3 collision is back on the runtime classpath."
}
exit $probeExit
