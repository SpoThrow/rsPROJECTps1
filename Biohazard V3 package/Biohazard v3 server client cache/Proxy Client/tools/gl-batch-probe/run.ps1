# ---------------------------------------------------------------------------
# GL batch probe - the verification for Phase 7.2b-1.
#
# WHY THIS EXISTS
# 7.2b-1 added ui\GlBatcher: the GPU triangle sink (accumulate pixel-space
# triangles -> VBO -> draw into GlScene's FBO with depth -> read back into the
# client's int[] pixel format). Three of the four things it does can be wrong in a
# way that still LOOKS like a picture, which is exactly the class of bug this plan
# keeps trying to make impossible:
#
#   * BYTE ORDER - a packed ARGB int reaches GL as bytes (b,g,r,a). Drop the shader
#     swizzle and red and blue SWAP. Plausible-looking, wrong.
#   * Y FLIP     - glReadPixels is bottom-up, the client is top-down. Miss it and the
#     whole scene is upside down.
#   * ALPHA      - the client writes 0x00RRGGBB. Inventing 0xFF changes every pixel
#     the 4.1b framebuffer hash covers.
#   * DEPTH      - the nearer triangle must win, not the last one drawn.
#
# So this probe drives the PRODUCTION class (not a copy) on a real GPU and asserts
# each one, plus a CONTROL that an undrawn pixel is the clear colour - because a
# readback returning one constant would pass a naive "is it drawn?" check.
#
# It renders into GlScene's own hidden-window FBO, so it also re-proves the 7.1/7.2a
# context on every run.
#
# USAGE
#   powershell -NoProfile -File tools\gl-batch-probe\run.ps1
#   powershell -NoProfile -File tools\gl-batch-probe\run.ps1 -Mutate
#   powershell -NoProfile -File tools\gl-batch-probe\run.ps1 -Mutate -Mutation swizzle
#
# -Mutate recompiles a MUTATED copy of ui\GlBatcher.java into an override directory
# placed FIRST on the classpath, so it shadows the real class. The probe MUST then
# report GL_BATCH_PROBE_FAIL; if it still passes, the probe is vacuous. Mutations:
#   flip     - drop the readback Y flip          (expect the top/bottom checks to fail)
#   alpha    - write 0xFF into the alpha byte     (expect the alpha checks to fail)
#   swizzle  - remove aCol.bgra from the shader   (expect red/blue to swap)
#
# Exit code: 0 when the expectation holds (including "mutation was caught"), else 1.
# ---------------------------------------------------------------------------
param(
	[switch]$Mutate,
	[ValidateSet('flip', 'alpha', 'swizzle')]
	[string]$Mutation = 'flip'
)

$ErrorActionPreference = 'Stop'

$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$clientDir = Resolve-Path (Join-Path $here '..\..')
$binDir = Join-Path $clientDir 'bin'
$lwjgl3 = Join-Path $clientDir 'deps\lwjgl3'

if (-not (Test-Path $binDir)) {
	Write-Host "Client is not compiled. Run:  gradlew.bat installBin"
	exit 1
}
if (-not (Test-Path $lwjgl3)) {
	Write-Host "deps\lwjgl3 is missing - Phase 7.2a vendored LWJGL 3 there."
	exit 1
}

$work = Join-Path $env:TEMP ('gl-batch-probe-' + [System.Guid]::NewGuid().ToString('N'))
$outDir = Join-Path $work 'classes'
New-Item -ItemType Directory -Path $outDir -Force | Out-Null

$jars = Join-Path $lwjgl3 '*'
$override = $null

if ($Mutate) {
	# Build a mutated ui\GlBatcher and put it AHEAD of bin, so the probe exercises the
	# mutation through the SAME call path the real client would use.
	$override = Join-Path $work 'override'
	New-Item -ItemType Directory -Path $override -Force | Out-Null

	$src = [System.IO.File]::ReadAllText((Join-Path $clientDir 'src\ui\GlBatcher.java'))
	$before = $src

	switch ($Mutation) {
		'flip' {
			# Remove the Y flip: rows are written straight through, bottom-up.
			$src = $src.Replace('int srcRow = (frameHeight - 1 - y) * frameWidth * 4;',
				'int srcRow = y * frameWidth * 4;')
		}
		'alpha' {
			# Invent an opaque alpha byte.
			$src = $src.Replace('dest[dstRow + x] = (r << 16) | (g << 8) | b;',
				'dest[dstRow + x] = 0xff000000 | (r << 16) | (g << 8) | b;')
		}
		'swizzle' {
			# Drop the byte-order swizzle, so (b,g,r,a) is used as if it were RGBA.
			$src = $src.Replace('vCol = aCol.bgra;', 'vCol = aCol;')
		}
	}

	if ($src -eq $before) {
		Write-Host "MUTATION '$Mutation' DID NOT APPLY - the source anchor moved. Fix the probe,"
		Write-Host "because a mutation that never runs proves nothing."
		Remove-Item -Recurse -Force $work
		exit 1
	}
	Write-Host "MUTATION: $Mutation applied to a copy of ui\GlBatcher; expecting GL_BATCH_PROBE_FAIL."
	Write-Host ""

	# Compile ONLY the mutated class; it links against bin for its siblings.
	$mutSrcDir = Join-Path $work 'mutsrc\ui'
	New-Item -ItemType Directory -Path $mutSrcDir -Force | Out-Null
	[System.IO.File]::WriteAllText((Join-Path $mutSrcDir 'GlBatcher.java'), $src)
	& javac -nowarn -cp "$binDir;$jars" -d $override (Join-Path $mutSrcDir 'GlBatcher.java')
	if ($LASTEXITCODE -ne 0) {
		Write-Host "Mutated GlBatcher failed to compile."
		Remove-Item -Recurse -Force $work
		exit 1
	}
	Write-Host ""
}

Write-Host "Compiling the probe against the client classes..."
& javac -nowarn -cp "$binDir;$jars" -d $outDir (Join-Path $here 'GlBatchProbe.java')
if ($LASTEXITCODE -ne 0) {
	Write-Host "Probe failed to compile."
	Remove-Item -Recurse -Force $work
	exit 1
}

if ($override) {
	$cp = "$outDir;$override;$binDir;$jars"
} else {
	$cp = "$outDir;$binDir;$jars"
}

Write-Host "Running the probe on the production GlBatcher:"
Write-Host ""
& java -cp $cp tools.glbatchprobe.GlBatchProbe
$probeExit = $LASTEXITCODE

Remove-Item -Recurse -Force $work -ErrorAction SilentlyContinue

Write-Host ""
if ($Mutate) {
	if ($probeExit -eq 1) {
		Write-Host "MUTATION CAUGHT ($Mutation): the probe fails on the mutated class - it is not vacuous."
		exit 0
	}
	Write-Host "MUTATION MISSED ($Mutation): the probe passed a mutated GlBatcher. It proves nothing."
	exit 1
}

if ($probeExit -eq 0) {
	Write-Host "OK: GlBatcher draws and reads back correctly - colours, Y orientation,"
	Write-Host "    the 0x00RRGGBB alpha contract, depth order, and the undrawn control."
} else {
	Write-Host "FAIL: GlBatcher's draw/readback contract is broken."
}
exit $probeExit
