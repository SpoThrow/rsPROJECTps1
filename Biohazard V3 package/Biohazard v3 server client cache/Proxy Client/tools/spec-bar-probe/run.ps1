# ---------------------------------------------------------------------------
# Spec-bar probe - the live-gate companion to the special-attack orb fix.
#
# WHY THIS EXISTS
# The orb must send the button id the SERVER's button table listens for. Two different
# values exist in the live interface tree for the same bar: the id `specialBar` writes
# into the container (`id - 12`) and the large id the server pairs with the bar's text
# (`48023 -> 12335`). The cache loads the bar first and `Interfaces.loadInterfaces`
# patches the same container afterwards, so which one wins is a RUNTIME fact that no
# amount of reading the source settles. This probe prints the tree.
#
# USAGE
#   powershell -NoProfile -File tools\spec-bar-probe\run.ps1
#
# Exit code: the probe's own (0 = unpacked, 2 = cache missing).
# ---------------------------------------------------------------------------
$ErrorActionPreference = 'Stop'

$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$clientDir = Resolve-Path (Join-Path $here '..\..')
$binDir = Join-Path $clientDir 'bin'

if (-not (Test-Path $binDir)) {
	Write-Host "Client is not compiled. Run:  gradlew.bat installBin"
	exit 1
}

$work = Join-Path $env:TEMP ('spec-bar-probe-' + [System.Guid]::NewGuid().ToString('N'))
$outDir = Join-Path $work 'out'
New-Item -ItemType Directory -Path $outDir -Force | Out-Null

try {
	Write-Host "Compiling probe against the client classes..."
	& javac -nowarn -cp $binDir -d $outDir (Join-Path $here 'SpecBarProbe.java')
	if ($LASTEXITCODE -ne 0) { exit 1 }

	Write-Host ""
	& java -cp "$outDir;$binDir" SpecBarProbe
	$probeExit = $LASTEXITCODE
	Write-Host ""
	exit $probeExit
}
finally {
	Remove-Item $work -Recurse -Force -ErrorAction SilentlyContinue
}
