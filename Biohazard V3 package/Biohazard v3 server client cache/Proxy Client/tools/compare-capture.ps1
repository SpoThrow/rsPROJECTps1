# ---------------------------------------------------------------------------
# Golden-master capture comparison - Phase 0.5 of CLIENT_REFACTORING_PLAN.md.
#
# Reduces a packet-tap log to a structural signature and diffs two signatures.
#
# WHY A SIGNATURE AND NOT A BYTE DIFF
# -----------------------------------
# The obvious oracle - diff the two logs byte for byte - DOES NOT WORK on this
# protocol, and it is worth stating why so nobody tries it later:
#
#   * Every packet OPCODE after login is ISAAC-encrypted with keys the client
#     seeds from Math.random() (client.java:10426) plus the server's 8 random
#     bytes, so opcodes differ on every single session.
#   * The login block itself embeds those random seeds, so even the handshake
#     is not reproducible.
#
# What IS reproducible is the STRUCTURE: for a fixed sequence of actions the
# client sends and receives the same number of protocol units, in the same
# order, with the same lengths. That is exactly what a refactor breaks - reading
# a packet in the wrong order changes how many bytes are consumed, so a desync
# shows up here as a length/order divergence.
#
# Verified against the real capture: the first 12 units are the login handshake
# exactly as client.java:10409-10461 performs it (8 discarded reads, 1 response
# code, an 8-byte seed flush, then the 171-byte login block).
#
# WHAT THIS DOES NOT CATCH
# ------------------------
# It does not compare packet CONTENTS. A packet whose payload changed while its
# length stayed the same is invisible here. Payload comparison is impossible for
# the same encryption reason, so content regressions are covered by the screen
# recording half of 0.3 and by the live check, not by this tool.
#
# Usage:
#   compare-capture.ps1 -Golden <capture> -Current <capture>
#
# Both arguments may be a raw tap log or a pre-reduced signature - the parser
# reads the first two fields of each line either way, so no conversion step is
# needed and a stored signature is just a log with the payload column removed.
# ---------------------------------------------------------------------------
param(
    [Parameter(Mandatory = $true)][string]$Golden,
    [Parameter(Mandatory = $true)][string]$Current,
    [int]$Context = 6
)

$ErrorActionPreference = 'Stop'

# A .log line is "R 1 0a"; a .sig line is "R 1". Taking the first two
# whitespace-separated fields parses both, so callers can compare raw logs
# directly without a separate conversion step.
function Read-Signature([string]$path) {
    if (-not (Test-Path -LiteralPath $path)) {
        throw "Cannot find $path"
    }
    $sig = New-Object System.Collections.Generic.List[string]
    foreach ($line in [System.IO.File]::ReadAllLines($path)) {
        $t = $line.Trim()
        if ($t.Length -eq 0 -or $t.StartsWith('#')) { continue }
        $p = $t -split '\s+'
        if ($p.Count -lt 2) { continue }
        $dir = $p[0]
        $len = $p[1]
        if ($dir -ne 'R' -and $dir -ne 'W') { continue }
        if ($len -notmatch '^\d+$') { continue }
        $sig.Add("$dir $len")
    }
    return $sig
}

$goldenSig = Read-Signature $Golden
$currentSig = Read-Signature $Current

Write-Host "golden : $Golden"
Write-Host "current: $Current"
Write-Host ""

# Locate every position where the two sequences disagree. Reporting the count as
# well as the first divergence matters: one late divergence is usually innocent
# (a wandering NPC), whereas many starting early is a protocol change.
$divergences = New-Object System.Collections.Generic.List[int]
$max = [Math]::Max($goldenSig.Count, $currentSig.Count)
for ($i = 0; $i -lt $max; $i++) {
    $g = if ($i -lt $goldenSig.Count) { $goldenSig[$i] } else { '<none>' }
    $c = if ($i -lt $currentSig.Count) { $currentSig[$i] } else { '<none>' }
    if ($g -ne $c) { $divergences.Add($i) }
}

Write-Host ("golden units : {0}" -f $goldenSig.Count)
Write-Host ("current units: {0}" -f $currentSig.Count)
Write-Host ("divergences  : {0}" -f $divergences.Count)
Write-Host ""

if ($divergences.Count -eq 0) {
    Write-Host "IDENTICAL structure."
    exit 0
}

$first = $divergences[0]
Write-Host ("First divergence at unit {0} (1-based {1}):" -f $first, ($first + 1))
Write-Host ""

$from = [Math]::Max(0, $first - $Context)
Write-Host ("  {0,-6} {1,-10} {2}" -f 'unit', 'golden', 'current')
for ($i = $from; $i -le $first; $i++) {
    $g = if ($i -lt $goldenSig.Count) { $goldenSig[$i] } else { '<none>' }
    $c = if ($i -lt $currentSig.Count) { $currentSig[$i] } else { '<none>' }
    $mark = if ($i -eq $first) { '  <-- first difference' } else { '' }
    Write-Host ("  {0,-6} {1,-10} {2}{3}" -f ($i + 1), $g, $c, $mark)
}
Write-Host ""

if ($divergences.Count -le 20) {
    Write-Host ("All divergence positions (1-based): {0}" -f (($divergences | ForEach-Object { $_ + 1 }) -join ', '))
} else {
    $head = ($divergences | Select-Object -First 20 | ForEach-Object { $_ + 1 }) -join ', '
    Write-Host ("First 20 divergence positions (1-based): {0} ..." -f $head)
}
Write-Host ""
Write-Host "NOTE: this compares structure (direction + length), not payloads. See the header."
exit 1
