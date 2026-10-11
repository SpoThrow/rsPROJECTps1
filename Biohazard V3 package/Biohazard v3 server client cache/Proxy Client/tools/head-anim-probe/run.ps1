# ---------------------------------------------------------------------------
# Chat-head animation probe - the live-gate companion to the fixed chat heads.
#
# WHY THIS EXISTS
# NPC chat heads are live models carrying an animation id, and the server
# (PlayerAssistant.chatheadEmote) folds all of its 588..617 head emotes onto six
# ids in 9760..9850. Whether seq.dat defines those six is a fact about the cache,
# not about the source: if it does not, a head asked for one has no frames and
# freezes on its base model - which is what "the chat heads stopped animating"
# was. This probe reads the real seq.dat and answers the question.
#
# It checks both halves, because either alone proves nothing:
#   CONTROL - the six ids the server sends carry NO frames in this cache.
#   FIX     - the client's own emotes it translates them back onto DO.
# A translation pointing at another frameless id would look like a fix in the
# source and still animate nothing.
#
# Deliberately non-hermetic: it needs the real cache at %USERPROFILE%\Biohazard.474.
#
# USAGE
#   powershell -NoProfile -File tools\head-anim-probe\run.ps1
#
# Exit code: 0 = HEAD_ANIM_OK, 1 = cache unreadable or a check failed.
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

$work = Join-Path $env:TEMP ('head-anim-probe-' + [System.Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $work -Force | Out-Null

try {
	Write-Host "Compiling probe against the client classes..."
	& javac -nowarn -cp $classes -d $work (Join-Path $here 'HeadAnimProbe.java')
	if ($LASTEXITCODE -ne 0) { exit 1 }

	Write-Host ""
	& java -cp "$work;$classes" HeadAnimProbe
	exit $LASTEXITCODE
}
finally {
	Remove-Item $work -Recurse -Force -ErrorAction SilentlyContinue
}
