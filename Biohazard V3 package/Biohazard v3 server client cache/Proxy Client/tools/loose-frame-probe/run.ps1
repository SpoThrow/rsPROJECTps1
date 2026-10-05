# ---------------------------------------------------------------------------
# Loose-frame probe - the live-gate companion to Phase 6.5.2.
#
# WHY THIS EXISTS
# 6.5.2 moved the frame-slot budget into one owner (model.FrameSlots) and wired
# Frames.loadLooseFrameFiles to FrameSlots.isLooseSlot. That wiring is the ONE
# part of 6.5.2 no unit test can reach, because the method reads the REAL cache
# directory and no hermetic test supplies one. Five mutations were tried and
# four were caught by the harness; "make the predicate never fire" was NOT.
#
# This script closes that gap. It runs the real Frames.loadFrames() against the
# real cache root and asserts the three loose archives actually landed - so the
# wiring is measured rather than assumed, and does not depend on someone
# noticing a missing animation by eye.
#
# USAGE
#   powershell -NoProfile -File tools\loose-frame-probe\run.ps1
#   powershell -NoProfile -File tools\loose-frame-probe\run.ps1 -Mutate
#
# The -Mutate switch re-runs the probe against a copy of FrameSlots whose
# isLooseSlot always returns false. It MUST report LOOSE_FAIL; if it reports
# LOOSE_OK the probe is vacuous and proves nothing. This is the mutation that
# the harness misses, made catchable.
#
# Exit code: the probe's own (0 = LOOSE_OK, 1 = LOOSE_FAIL).
# ---------------------------------------------------------------------------
param(
	[switch]$Mutate
)

$ErrorActionPreference = 'Stop'

$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$clientDir = Resolve-Path (Join-Path $here '..\..')
$binDir = Join-Path $clientDir 'bin'
$srcModel = Join-Path $clientDir 'src\model'

if (-not (Test-Path $binDir)) {
	Write-Host "Client is not compiled. Run:  gradlew.bat installBin"
	exit 1
}

$work = Join-Path $env:TEMP ('loose-frame-probe-' + [System.Guid]::NewGuid().ToString('N'))
$outDir = Join-Path $work 'out'
New-Item -ItemType Directory -Path $outDir -Force | Out-Null

try {
	$cp = $binDir
	$lwjgl = Join-Path $clientDir 'deps\lwjgl.jar'
	if (Test-Path $lwjgl) { $cp = "$binDir;$lwjgl" }

	Write-Host "Compiling probe against the client classes..."
	& javac -nowarn -cp $binDir -d $outDir (Join-Path $here 'LooseFrameProbe.java')
	if ($LASTEXITCODE -ne 0) { exit 1 }

	$runCp = "$outDir;$cp"

	if ($Mutate) {
		# Reproduce the recorded mutation: the shared predicate never fires.
		$mutSrc = Join-Path $work 'mut\model'
		New-Item -ItemType Directory -Path $mutSrc -Force | Out-Null
		Copy-Item (Join-Path $srcModel 'Frames.java') $mutSrc -Force
		Copy-Item (Join-Path $srcModel 'FrameSlots.java') $mutSrc -Force

		$fsPath = Join-Path $mutSrc 'FrameSlots.java'
		$text = [System.IO.File]::ReadAllText($fsPath)
		$needle = 'return file >= LOOSE_MIN_ID;'
		if (-not $text.Contains($needle)) {
			Write-Host "ERROR: could not find '$needle' in FrameSlots.java - the probe needs updating."
			exit 2
		}
		$text = $text.Replace($needle, 'return false; // MUTATION: predicate that never fires')
		[System.IO.File]::WriteAllText($fsPath, $text)

		$mutOut = Join-Path $work 'mutout'
		New-Item -ItemType Directory -Path $mutOut -Force | Out-Null
		Write-Host "Compiling MUTATED FrameSlots (isLooseSlot -> false) into a shadow dir..."
		& javac -nowarn -cp $binDir -d $mutOut (Join-Path $mutSrc 'Frames.java') (Join-Path $mutSrc 'FrameSlots.java')
		if ($LASTEXITCODE -ne 0) { exit 1 }

		# Shadow dir FIRST so the mutated classes win over bin/.
		$runCp = "$mutOut;$runCp"
		Write-Host "Running probe against the MUTANT (expect LOOSE_FAIL / exit 1)..."
	}
	else {
		Write-Host "Running probe against the real build (expect LOOSE_OK / exit 0)..."
	}

	Write-Host ""
	& java -cp $runCp LooseFrameProbe
	$probeExit = $LASTEXITCODE
	Write-Host ""

	if ($Mutate) {
		if ($probeExit -eq 0) {
			Write-Host "PROBLEM: the mutant also passed, so this probe is VACUOUS."
			exit 1
		}
		Write-Host "OK: mutant correctly rejected (exit $probeExit)."
		exit 0
	}

	if ($probeExit -eq 0) {
		Write-Host "OK: the loose frames (1777, 2160, 3502) loaded through FrameSlots.isLooseSlot."
	}
	else {
		Write-Host "FAIL: a loose archive did not load - check FrameSlots.isLooseSlot."
	}
	exit $probeExit
}
finally {
	Remove-Item $work -Recurse -Force -ErrorAction SilentlyContinue
}
