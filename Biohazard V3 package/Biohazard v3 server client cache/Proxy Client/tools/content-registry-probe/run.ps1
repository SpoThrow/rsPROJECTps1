# ---------------------------------------------------------------------------
# Content registry probe - the live-gate companion to Phase 6.5.5.
#
# WHY THIS EXISTS
# ContentRegistry declares that anims 4000, 4001 and 4002 need external frame files
# 3403 and 3353, and loads them from {cache}\667Anims\anims\. The harness can only prove
# the registry's mechanics against fixtures; it cannot touch the real cache, because no
# hermetic test supplies the real 667Anims pack or the hand-written animations.
#
# This probe closes that gap: it rebuilds the hand-written animations the way
# Animation.unpackConfig does (minus the cache read), then checks that EVERY frame of
# every declared animation resolves through the real Frames.method531 - once with the
# registry's load SUPPRESSED (the control) and once with it performed.
#
# WHY THE CONTROL MATTERS
# "All frames resolve" is only a result if they did NOT resolve beforehand. The control is
# what stops this probe from passing while doing nothing - which is precisely how the
# defect it was written for survived: nothing ever asked the question.
#
# WHY IT READS THE ANIMATIONS RATHER THAN THE DECLARATION
# The declaration is the CLAIM; the animation data is the FACT. Checking the fact is the
# whole point - a probe that re-read the declaration would only prove it agrees with itself.
#
# PHASE 6.5.6: THE CURSEPACK DECLARATIONS TOO
# The registry now also declares the CursePack's asset names and id lists, and the loaders
# build their paths from those constants. Moving a string literal is where a typo hides,
# and findRoot() would report one as "pack not found" - which reads like a missing pack
# rather than a mistyped name. So this probe also resolves EVERY declared CursePack path
# (seq.dat, spotanim.dat, each anims/{id}.gz, each models/{id}.gz) through the same private
# asset() the loader uses, with a control that an undeclared name does NOT resolve.
# Mutation-tested: renaming ASSET_MODELS to "modelz" turns this report into 9/18 with
# "FIRST MISSING: modelz\50778.gz" and exit code 1.
#
# DEPLOYING THE DATA
# The pack is deployed the same way CursePack is - copied into the cache root:
#   New-Item -ItemType Directory -Force "$env:USERPROFILE\Biohazard.474\667Anims\anims"
#   Copy-Item "<667 Data>\667 Animations\3353.gz" "$env:USERPROFILE\Biohazard.474\667Anims\anims\"
#   Copy-Item "<667 Data>\667 Animations\3403.gz" "$env:USERPROFILE\Biohazard.474\667Anims\anims\"
#
# USAGE
#   powershell -NoProfile -File tools\content-registry-probe\run.ps1
#
# Exit code: 0 = every declared frame resolves and the declaration holds, 1 = otherwise.
# ---------------------------------------------------------------------------
$ErrorActionPreference = 'Stop'

$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$clientDir = Resolve-Path (Join-Path $here '..\..')
$binDir = Join-Path $clientDir 'bin'

if (-not (Test-Path $binDir)) {
	Write-Host "Client is not compiled. Run:  gradlew.bat installBin"
	exit 1
}

$cacheRoot = Join-Path $env:USERPROFILE 'Biohazard.474'
$packRoot = Join-Path $cacheRoot '667Anims'
if (-not (Test-Path $packRoot)) {
	Write-Host "667Anims pack not found at $packRoot"
	Write-Host "Deploy it first - see the DEPLOYING THE DATA note at the top of this script."
	exit 2
}

$work = Join-Path $env:TEMP ('content-registry-probe-' + [System.Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $work -Force | Out-Null

try {
	Write-Host "Compiling probe against the client classes..."
	& javac -nowarn -cp $binDir -d $work (Join-Path $here 'ContentRegistryProbe.java')
	if ($LASTEXITCODE -ne 0) { exit 1 }

	$cp = $work + ';' + $binDir
	$lwjgl = Join-Path $clientDir 'deps\lwjgl.jar'
	if (Test-Path $lwjgl) { $cp = $cp + ';' + $lwjgl }

	Write-Host ""
	& java -cp $cp ContentRegistryProbe
	$probeExit = $LASTEXITCODE
	Write-Host ""

	if ($probeExit -eq 0) {
		Write-Host "OK: every frame of the declared animations resolves once the registry loads its sources."
	} else {
		Write-Host "FAIL: see the probe output above."
	}
	exit $probeExit
}
finally {
	Remove-Item $work -Recurse -Force -ErrorAction SilentlyContinue
}
