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
    [int]$Context = 6,
    # Minimum length of the common prefix required to call a pair compatible.
    # The prefix is the deterministic region: the login handshake plus the initial
    # region load. Measured at 701 units on two independent sessions six hours
    # apart, so a change anywhere real in that region - our Phase 2 hardening most
    # of all - shows up by shortening it. 600 is a floor just under the measurement
    # so ordinary server-side variation does not trip it.
    # ASSUMPTION: both sessions log in at the same place. Logging in somewhere else
    # loads different regions, which shortens the prefix for a legitimate reason.
    # The drift message says so when the prefix is short but non-trivial.
    [int]$MinPrefix = 600
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

# ---------------------------------------------------------------------------
# GUARD: never compare a capture against itself.
#
# This tool's failure mode is a VACUOUS PASS. The Gradle task picks the newest
# packet-tap-*.log, so a stale capture left in the directory - specifically, the
# very session the golden master was promoted from - is picked up again, compared
# against its own promoted copy, and reported as "IDENTICAL structure": a green
# result that looks exactly like a real one and proves nothing.
#
# Two independent sessions can never produce byte-identical captures, because the
# ISAAC keys and login seeds are per-session random (see the header). So identical
# hashes mean one file is a copy of the other and there is nothing to compare.
# Fail loudly instead of printing a false pass.
# ---------------------------------------------------------------------------
$goldenHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $Golden).Hash
$currentHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $Current).Hash
if ($goldenHash -eq $currentHash) {
    Write-Host "REFUSING TO COMPARE: the two files are byte-identical ($($goldenHash.Substring(0, 12))...)."
    Write-Host ""
    Write-Host "  golden : $Golden"
    Write-Host "  current: $Current"
    Write-Host ""
    Write-Host "A capture cannot legitimately equal the golden master: opcodes are ISAAC-encrypted with"
    Write-Host "per-session random keys and the login block embeds those seeds, so two sessions always"
    Write-Host "differ. This means the 'current' file IS the golden master (or a copy of the session it"
    Write-Host "was promoted from), and comparing it would print a meaningless IDENTICAL."
    Write-Host ""
    Write-Host "Take a fresh capture with Record.bat and re-run."
    exit 2
}

Write-Host "golden : $Golden"
Write-Host "current: $Current"
Write-Host ""

# ---------------------------------------------------------------------------
# VERDICT: common prefix + alignment.
#
# WHY THE POSITIONAL COUNT IS NO LONGER THE VERDICT.
# Record.bat records a fresh, human session, so the route is never identical to
# the golden master's. A single extra client->server packet shifts every
# following unit by one, and a positional diff then scores each of those as a
# divergence: 5,944 of them on a session that was in fact healthy, caused by one
# "W 7" inserted at unit 1569. That count is real, but it means "different route",
# not "protocol drift" - and a verdict that fails on every fresh session is one
# nobody can act on.
#
# So the verdict rests on two things that ARE comparable across sessions:
#   * the common PREFIX - the login handshake plus the initial region load, which
#     are deterministic; they matched to the unit across two sessions 6h apart;
#   * the longest common SUBSEQUENCE - which tolerates inserted/removed packets.
#
# Calibration, measured on the real captures rather than assumed:
#   same protocol, different route .... LCS/shorter = 89.1%  (incoming only 91.4%)
#   structure destroyed, same multiset . 60.1% (shuffled tail) / 62.5% (reversed)
# The floor is only that high because the stream is dominated by an R 1 / R 2
# heartbeat, so the threshold sits between the two, at 75%.
#
# KNOWN LIMIT, stated so a green is not over-trusted: a small structural change in
# the ROUTE-DEPENDENT TAIL (say three deleted units late in the session) is
# indistinguishable from a different route and will pass. What this reliably
# catches is a break in the login/load prefix, or a cascade - which is what a
# desync actually is, since one misread length corrupts every later unit.
# ---------------------------------------------------------------------------
$prefix = 0
while ($prefix -lt $goldenSig.Count -and $prefix -lt $currentSig.Count -and
       $goldenSig[$prefix] -eq $currentSig[$prefix]) { $prefix++ }

Add-Type -TypeDefinition @'
using System;
public static class CaptureAlign {
  // Length of the longest common subsequence of two unit sequences.
  public static int Lcs(string[] a, string[] b) {
    int n = a.Length, m = b.Length;
    if (n == 0 || m == 0) return 0;
    int[] prev = new int[m + 1], cur = new int[m + 1];
    for (int i = 1; i <= n; i++) {
      for (int j = 1; j <= m; j++) {
        cur[j] = (a[i - 1] == b[j - 1]) ? prev[j - 1] + 1
                                        : (prev[j] >= cur[j - 1] ? prev[j] : cur[j - 1]);
      }
      int[] t = prev; prev = cur; cur = t;
      Array.Clear(cur, 0, cur.Length);
    }
    return prev[m];
  }
}
'@

$lcs = [CaptureAlign]::Lcs([string[]]$goldenSig, [string[]]$currentSig)
$shorter = [Math]::Min($goldenSig.Count, $currentSig.Count)
$ratio = if ($shorter -gt 0) { [Math]::Round(100.0 * $lcs / $shorter, 1) } else { 0.0 }

Write-Host ("golden units  : {0}" -f $goldenSig.Count)
Write-Host ("current units : {0}" -f $currentSig.Count)
Write-Host ("common prefix : {0} units (deterministic login + region load)" -f $prefix)
Write-Host ("LCS / shorter : {0} / {1} = {2}%  (compatible if >= 75%; destroyed floor ~60%)" -f $lcs, $shorter, $ratio)
Write-Host ""

# Locate every position where the two sequences disagree. Kept even for a
# compatible pair because it pinpoints the first difference, which is what tells
# a legitimate route change apart from a fault.
$divergences = New-Object System.Collections.Generic.List[int]
$max = [Math]::Max($goldenSig.Count, $currentSig.Count)
for ($i = 0; $i -lt $max; $i++) {
    $g = if ($i -lt $goldenSig.Count) { $goldenSig[$i] } else { '<none>' }
    $c = if ($i -lt $currentSig.Count) { $currentSig[$i] } else { '<none>' }
    if ($g -ne $c) { $divergences.Add($i) }
}

if ($divergences.Count -eq 0) {
    Write-Host "VERDICT: IDENTICAL structure - the two sessions match unit for unit."
    exit 0
}

if ($prefix -ge $MinPrefix -and $ratio -ge 75) {
    Write-Host "VERDICT: COMPATIBLE - same protocol, different route."
    Write-Host ("  {0} positional differences, all consistent with a different set of player" -f $divergences.Count)
    Write-Host "  actions: one extra/absent packet shifts the alignment, which is why the raw"
    Write-Host "  count is large. The login/load prefix and the great majority of units still"
    Write-Host "  line up in order, so the client is framing packets exactly as before."
    exit 0
}

Write-Host "VERDICT: DRIFT SUSPECTED - structural divergence beyond a route difference."
if ($prefix -lt 20) {
    Write-Host ("  The common prefix is only {0} units, so even the LOGIN HANDSHAKE differs." -f $prefix)
    Write-Host "  That is the one region that is always deterministic - treat this as a real break."
} elseif ($prefix -lt $MinPrefix) {
    Write-Host ("  The prefix broke at unit {0}, inside the deterministic login/load region" -f $prefix)
    Write-Host ("  (expected at least {0}). Either a real change there, or the session logged in at a" -f $MinPrefix)
    Write-Host "  different place and loaded different regions. Compare the context below to tell"
    Write-Host "  which: a location difference diverges on map data, a protocol change on framing."
}
Write-Host ""

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
