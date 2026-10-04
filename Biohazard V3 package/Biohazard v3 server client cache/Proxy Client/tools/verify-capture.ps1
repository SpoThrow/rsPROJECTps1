# ---------------------------------------------------------------------------
# Capture sanity check - Phase 0.3 tooling, supporting Phase 0.5.
#
# WHY THIS EXISTS
# ---------------
# Record.bat used to end by printing "Packet log written to: <path>" whether or
# not anything had been written. If the JVM never started (java not on PATH),
# the classpath was wrong, or the client was closed before it ever connected,
# the run produced NO file - and the banner said the same thing it says on
# success. That is a silent failure on the one artefact the whole plan's
# regression oracle depends on.
#
# It also catches the opposite failure, which is worse because it looks green:
# a capture that IS the golden master (or the stale file the golden master was
# promoted from) compares as "IDENTICAL structure" and proves nothing. The tap
# writes a two-line header on class load, so "the file exists" is not enough -
# it has to contain at least one real protocol unit, and it must not hash the
# same as the golden master.
#
# PacketTap.java opens its file and writes the header as soon as the class is
# initialised, and the class is initialised on the first RSSocket.read(). So:
#
#   no file          -> the client never got as far as a socket read
#   header only      -> the client ran but exchanged no packets
#   content, unique  -> a usable capture
#
# Usage:
#   verify-capture.ps1 -Capture <packet-tap log> [-GoldenDir <dir>]
#
# Exit codes:  0 usable capture   3 not usable (reason printed)
# ---------------------------------------------------------------------------
param(
    [Parameter(Mandatory = $true)][string]$Capture,
    [string]$GoldenDir = 'golden-master'
)

$ErrorActionPreference = 'Stop'

function Fail([string]$headline, [string]$detail) {
    Write-Host ""
    Write-Host "  *** CAPTURE NOT USABLE ***"
    Write-Host "  $headline"
    Write-Host ""
    Write-Host "  $detail"
    Write-Host ""
    exit 3
}

# --- 1. does the file exist at all ----------------------------------------
if (-not (Test-Path -LiteralPath $Capture)) {
    Fail "No capture file was created: $Capture" @"
PacketTap only opens its log once the client initialises the class, which
happens on the first socket read. No file therefore means the client either
never launched (check that `java` is on PATH and that the window really
appeared) or never connected to the server.
"@
}

# --- 2. does it contain any protocol units --------------------------------
$dirCounts = @{}
$units = 0
$lines = [System.IO.File]::ReadAllLines($Capture)
foreach ($line in $lines) {
    $t = $line.Trim()
    if ($t.Length -eq 0 -or $t.StartsWith('#')) { continue }
    $p = $t -split '\s+'
    if ($p.Count -lt 2) { continue }
    if ($p[0] -ne 'R' -and $p[0] -ne 'W') { continue }
    if ($p[1] -notmatch '^\d+$') { continue }
    $units++
    if ($dirCounts.ContainsKey($p[0])) { $dirCounts[$p[0]]++ } else { $dirCounts[$p[0]] = 1 }
}

if ($units -eq 0) {
    Fail "The capture exists but holds no protocol units: $Capture" @"
The tap wrote its header, so the client did start and initialise PacketTap -
but no packets were logged. That is usually a session that was closed before
logging in, or a launch with the tap property missing.
"@
}

# --- 3. is it actually the golden master? ---------------------------------
$golden = $null
if (Test-Path -LiteralPath $GoldenDir) {
    $golden = Get-ChildItem -LiteralPath $GoldenDir -File -ErrorAction SilentlyContinue |
        Where-Object { $_.Name.EndsWith('.log') } |
        Sort-Object Name |
        Select-Object -First 1
}

$capHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $Capture).Hash
if ($golden -ne $null) {
    $goldHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $golden.FullName).Hash
    if ($goldHash -eq $capHash) {
        Fail "This capture is byte-identical to the golden master ($($capHash.Substring(0,12))...)." @"
Comparing it would print a meaningless IDENTICAL, so it is refused here rather
than after a game session has been spent on it.

  capture: $Capture
  golden : $($golden.FullName)

Either this IS the golden master (or the session it was promoted from), or the
recording run overwrote nothing and the old file is being read back. Take a new
capture with Record.bat.
"@
    }
}

# --- usable ---------------------------------------------------------------
$r = if ($dirCounts.ContainsKey('R')) { $dirCounts['R'] } else { 0 }
$w = if ($dirCounts.ContainsKey('W')) { $dirCounts['W'] } else { 0 }
$bytes = (Get-Item -LiteralPath $Capture).Length

Write-Host ""
Write-Host "  capture usable: $Capture"
Write-Host ("  units: {0}  ({1} received / {2} sent)   bytes: {3}" -f $units, $r, $w, $bytes)
if ($golden -ne $null) {
    Write-Host ("  distinct from golden master ({0}) - safe to compare." -f $golden.Name)
} else {
    Write-Host "  NOTE: no golden master found to compare against."
}
Write-Host ""
exit 0
