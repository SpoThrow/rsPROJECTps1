# ---------------------------------------------------------------------------
# MakeX probe - builds the five quantity-first creation chatboxes out of the
# REAL cache and checks them, so the layout is argued from the interface
# definitions rather than from MakeX's own tables.
#
# WHY THIS EXISTS
# MakeX reparents each of interfaces 8938 / 4429 / 8880 / 1743 / 2400: their
# product models, their four invisible make buttons and their title, then adds a
# quantity row and a label per product. Which of that interface's children is
# which was read off the interface bytes, and a mistyped frame id does not
# throw - it draws an item where nothing is and leaves an invisible hitbox
# somewhere else. This is the only place that gets checked.
#
# Deliberately non-hermetic: it needs the real cache at
# %USERPROFILE%\Biohazard.474, so it is a gate tool that must be RUN.
#
# USAGE
#   powershell -NoProfile -File tools\makex-probe\run.ps1
#
# Classes are taken from the Gradle build output, not from bin/, so that what is
# checked is what was just compiled. bin/ is what Run.bat plays and can lag it.
#
# Exit code: the probe's own (0 = MAKEX_OK, 1 = cache unreadable or a check failed).
# ---------------------------------------------------------------------------
$ErrorActionPreference = 'Stop'

$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$clientDir = Resolve-Path (Join-Path $here '..\..')
$buildDir = Join-Path $clientDir 'build\classes\java\main'
$binDir = Join-Path $clientDir 'bin'

$classes = if (Test-Path $buildDir) { $buildDir } else { $binDir }
if (-not (Test-Path $classes)) {
	Write-Host "Client is not compiled. Run:  gradlew.bat classes"
	exit 1
}
Write-Host "client classes: $classes"

$work = Join-Path $env:TEMP ('makex-probe-' + [System.Guid]::NewGuid().ToString('N'))
$outDir = Join-Path $work 'out'
$previewDir = Join-Path $here 'out'
New-Item -ItemType Directory -Path $outDir -Force | Out-Null

try {
	Write-Host "Compiling probe against the client classes..."
	& javac -nowarn -cp $classes -d $outDir (Join-Path $here 'MakeXProbe.java') (Join-Path $here 'MakeXPreview.java')
	if ($LASTEXITCODE -ne 0) { exit 1 }

	Write-Host ""
	& java -cp "$outDir;$classes" MakeXProbe --preview $previewDir
	exit $LASTEXITCODE
}
finally {
	Remove-Item $work -Recurse -Force -ErrorAction SilentlyContinue
}
