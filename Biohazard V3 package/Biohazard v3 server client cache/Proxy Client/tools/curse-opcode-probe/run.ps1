# ---------------------------------------------------------------------------
# CursePack opcode probe - the live-gate companion to Phase 6.5.3.
#
# WHY THIS EXISTS
# 6.5.3's claim is that the CursePack's 667 definitions are read correctly by the 474
# readers (Animation.readValues / SpotAnim.readValues). The harness can only pin the
# CursePack's skip tables against those readers; it cannot touch the real pack, because
# no hermetic test supplies %USERPROFILE%\Biohazard.474\CursePack.
#
# This probe closes that gap: it walks the REAL seq.dat and spotanim.dat twice - once
# with the pack's own private skipper, once with the 474 reader - and requires the cursor
# to land identically after every entry, and both to reach exact EOF.
#
# WHY THAT MATTERS
# CurseData667 reads WANTED entries with the readers and walks UNWANTED ones with
# hand-written skippers. If the two disagree about how many bytes an opcode takes, the
# walk desynchronises - and CurseData667's catch blocks are EMPTY, so it would not throw.
# It would silently corrupt every definition after the desync point. Agreement on the real
# data is therefore the property the live gate actually rests on.
#
# The opcode histogram is read from the boundaries the REAL skipper produced (so this
# probe adds no third opcode table of its own and cannot itself drift from the code).
#
# USAGE
#   powershell -NoProfile -File tools\curse-opcode-probe\run.ps1
#
# Exit code: 0 = both paths agree and both reach EOF, 1 = they disagree.
# ---------------------------------------------------------------------------
$ErrorActionPreference = 'Stop'

$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$clientDir = Resolve-Path (Join-Path $here '..\..')
$binDir = Join-Path $clientDir 'bin'

if (-not (Test-Path $binDir)) {
	Write-Host "Client is not compiled. Run:  gradlew.bat installBin"
	exit 1
}

$work = Join-Path $env:TEMP ('curse-opcode-probe-' + [System.Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $work -Force | Out-Null

try {
	Write-Host "Compiling probe against the client classes..."
	& javac -nowarn -cp $binDir -d $work (Join-Path $here 'CurseOpcodeProbe.java')
	if ($LASTEXITCODE -ne 0) { exit 1 }

	$cp = $work + ';' + $binDir
	$lwjgl = Join-Path $clientDir 'deps\lwjgl3'
	if (Test-Path $lwjgl) { $cp = $cp + ';' + (Join-Path $lwjgl '*') }

	Write-Host ""
	& java -cp $cp CurseOpcodeProbe
	$probeExit = $LASTEXITCODE
	Write-Host ""

	if ($probeExit -eq 0) {
		Write-Host "OK: the CursePack skipper and the 474 readers agree entry by entry, and both reach exact EOF."
	} elseif ($probeExit -eq 2) {
		Write-Host "CursePack not present under the cache root - this probe needs the real pack."
	} else {
		Write-Host "FAIL: the CursePack skipper and the 474 reader disagree - a definition walk is desynchronising."
	}
	exit $probeExit
}
finally {
	Remove-Item $work -Recurse -Force -ErrorAction SilentlyContinue
}
