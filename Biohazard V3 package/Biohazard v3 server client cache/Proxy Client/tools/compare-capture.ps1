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
# CORRECTION (2026-10-04 late night), because the paragraph above used to be
# followed by a claim that the login/load prefix is DETERMINISTIC - it is not.
# The region carries a length-prefixed loading-progress String whose byte length
# depends on its own value and whose frequency varies several-fold per session,
# plus ordinary inserted client actions. So the prefix is compared by bounded
# ALIGNMENT rather than unit-for-unit; see the parameter block for measurements.
# Payloads are still not compared, and the frame is identified only where the
# alignment needs it - not by decoding the progress text.
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
    # GAP-TOLERANT PREFIX ALIGNMENT (replaces the old positional $MinPrefix gate).
    #
    # The old gate required the two captures to agree UNIT FOR UNIT for the first
    # 600 units. That is not a property this protocol has, and it produced false
    # positives on healthy sessions - proven by comparing captures from the SAME
    # BINARY against each other: 221412 vs 221551 scored COMPATIBLE (1,100-unit
    # prefix) while 221412 vs 221811 and 221551 vs 221811 scored DRIFT with a
    # 17-unit prefix, DESPITE SCORING HIGHER SIMILARITY (98.1% and 96.8% against
    # 97.5%). A client cannot drift from itself, so the gate was the fault.
    #
    # Two benign, per-session-varying things sit in that region:
    #   * a loading-progress String frame, length-prefixed, whose byte length
    #     depends on its own value - "98%\n" is 6 bytes, "100%\n" is 7 - and which
    #     appears a wildly different number of times per session (3, 7 and 31
    #     measured). It changes BOTH the R 2 length prefix and the R 6/R 7 payload,
    #     so it breaks a positional match at two units.
    #   * ordinary inserted client actions: 221811 carries an extra W 1 at unit 520
    #     where 221412 carries R 1, shifting everything by one.
    # Neither is a desync. A desync is a CASCADE - one misread length makes every
    # later unit garbage - and that is what the alignment below actually measures.
    #
    # So the gate asks: can the golden's first $PrefixWindow units be matched, in
    # order, into the current's first ($PrefixWindow + $PrefixSlack) units, missing
    # at most $PrefixTolerance of them?
    #
    # Measured, and this is what sets the defaults. Note the TWO healthy baselines
    # score differently, which is why the tolerance cannot be tighter:
    #   healthy, golden vs a fresh session .. 600 / 594 / 594 / 593
    #     (213845, 221412, 221551, 221811 - the ~6-unit cost is benign: golden's
    #      session interleaves the SAME variable-length frames in a different
    #      order, e.g. `R 2 ffff` + `R 6 00566401c763` early where 221412 carries
    #      them later. Both sessions hold ~163 such frames, so no data is lost.)
    #   healthy, same binary vs itself .... 600 / 599 / 599
    #   cascade breaking at unit 18 ....... 351
    #   cascade at 60 / 120 / 300 ......... 372 / 392 / 470
    #   cascade at 440 / 480 / 500 ........ 536 / 559 / 568
    #   cascade at 520 / 540 / 560 ........ 573 / 580 / 589
    #   cascade at 580 .................... 597
    # So 590 (600 - 10) keeps every healthy measurement clear with margin, and still
    # catches every cascade that begins at or before unit 560.
    #
    # KNOWN LIMIT, stated so a green is not over-trusted: a cascade beginning in the
    # LAST ~35 UNITS of the window is not distinguishable from benign drift - at unit
    # 580 it scores 597, above a healthy 593 - because the R 1 / R 2 / R 3 heartbeat
    # keeps matching by coincidence however the tail is reordered. The limit already
    # accepted on the route-dependent tail is the same phenomenon.
    [int]$PrefixWindow = 600,
    [int]$PrefixSlack = 64,
    [int]$PrefixTolerance = 10
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
# VERDICT: prefix ALIGNMENT + whole-stream similarity.
#
# WHY THE POSITIONAL COUNT IS NOT THE VERDICT.
# Record.bat records a fresh, human session, so the route is never identical to
# the golden master's. A single extra client->server packet shifts every
# following unit by one, and a positional diff then scores each of those as a
# divergence: 5,944 of them on a session that was in fact healthy, caused by one
# "W 7" inserted at unit 1569. That count is real, but it means "different route",
# not "protocol drift" - and a verdict that fails on every fresh session is one
# nobody can act on.
#
# The verdict rests on two things that ARE comparable across sessions:
#   * the PREFIX - the login handshake plus the initial region load. It is NOT
#     deterministic (see the parameter block: a variable-length loading-progress
#     frame and ordinary inserted client actions both live there), so it is
#     compared by bounded ALIGNMENT - how many of the golden's first N units can
#     be matched, in order, inside a small slack. Measured: healthy sessions score
#     593-600 of 600; every cascade scores 351-589 until it starts past unit 560;
#   * the longest common SUBSEQUENCE over the whole stream - which tolerates
#     inserted/removed packets.
#
# Calibration, measured on the real captures rather than assumed:
#   same protocol, different route .... LCS/shorter = 89.1% .. 98.1%
#   structure destroyed, same multiset . 60.1% (shuffled tail) / 62.5% (reversed)
# The floor is only that high because the stream is dominated by an R 1 / R 2
# heartbeat, so the threshold sits between the two, at 75%.
#
# KNOWN LIMIT, stated so a green is not over-trusted: a small structural change in
# the ROUTE-DEPENDENT TAIL (say three deleted units late in the session) is
# indistinguishable from a different route and will pass; likewise a cascade
# confined to the last few units of the prefix window scores like benign drift.
# What this reliably catches is a break in the login/load prefix, or a cascade
# anywhere before the end of that window - which is what a desync actually is,
# since one misread length corrupts every later unit.
# ---------------------------------------------------------------------------
# Exact positional prefix: kept ONLY as a diagnostic. It is expected to be SHORT
# on healthy sessions - 221412 vs 221811 scores 17 while being a same-binary pair -
# so it must never drive the verdict again. That mistake is what produced the
# false DRIFT verdicts this tool was repaired for.
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
  // Bounded prefix alignment: how many of the golden's first `window` units can
  // be matched, in order, into the current's first `window + slack` units. This
  // tolerates the benign per-session variation (the loading-progress frame
  // changing its length, one inserted client action) while a cascade - where
  // every unit past the break is drawn from the wrong offset - cannot reach the
  // required count.
  public static int PrefixAlign(string[] golden, string[] current, int window, int slack) {
    int n = Math.Min(window, golden.Length);
    if (n == 0) return 0;
    int m = Math.Min(window + slack, current.Length);
    if (m == 0) return 0;
    string[] g = new string[n]; Array.Copy(golden, g, n);
    string[] c = new string[m]; Array.Copy(current, c, m);
    return Lcs(g, c);
  }
}
'@

$lcs = [CaptureAlign]::Lcs([string[]]$goldenSig, [string[]]$currentSig)
$shorter = [Math]::Min($goldenSig.Count, $currentSig.Count)
$ratio = if ($shorter -gt 0) { [Math]::Round(100.0 * $lcs / $shorter, 1) } else { 0.0 }

$window = [Math]::Min($PrefixWindow, $goldenSig.Count)
$prefixMatch = [CaptureAlign]::PrefixAlign([string[]]$goldenSig, [string[]]$currentSig, $PrefixWindow, $PrefixSlack)
$prefixRequired = $window - $PrefixTolerance
$prefixOk = $prefixMatch -ge $prefixRequired

Write-Host ("golden units  : {0}" -f $goldenSig.Count)
Write-Host ("current units : {0}" -f $currentSig.Count)
Write-Host ("prefix align  : {0} / {1} units matched  (need >= {2}; window {3}, slack {4})" -f $prefixMatch, $window, $prefixRequired, $PrefixWindow, $PrefixSlack)
Write-Host ("LCS / shorter : {0} / {1} = {2}%  (compatible if >= 75%; destroyed floor ~60%)" -f $lcs, $shorter, $ratio)
Write-Host ("exact prefix  : {0} units (diagnostic only - NOT a gate; healthy pairs also score ~17 here)" -f $prefix)
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

if ($prefixOk -and $ratio -ge 75) {
    Write-Host "VERDICT: COMPATIBLE - same protocol, different route."
    Write-Host ("  {0} positional differences, all consistent with a different set of player" -f $divergences.Count)
    Write-Host "  actions: one extra/absent packet shifts the alignment, which is why the raw"
    Write-Host "  count is large. The login/load prefix still aligns almost unit for unit, and"
    Write-Host "  the great majority of units line up in order, so the client is framing"
    Write-Host "  packets exactly as before."
    exit 0
}

Write-Host "VERDICT: DRIFT SUSPECTED - structural divergence beyond a route difference."
if (-not $prefixOk) {
    Write-Host ("  The login/load prefix failed to align: only {0} of the golden's first {1} units could" -f $prefixMatch, $window)
    Write-Host ("  be matched in order (need >= {0})." -f $prefixRequired)
    Write-Host "  That region does vary between sessions - a length-prefixed loading-progress frame"
    Write-Host "  whose byte length depends on its own value, plus ordinary inserted client actions -"
    Write-Host "  but only by a few units: healthy sessions measure 593-600 here. A score this low is"
    Write-Host "  a CASCADE, which is what a misread length produces, since every later unit is then"
    Write-Host "  drawn from the wrong offset. Treat it as a real break."
} else {
    Write-Host ("  The prefix aligned ({0} / {1}), so the login/load region itself is intact, but" -f $prefixMatch, $window)
    Write-Host ("  whole-stream similarity is only {0}% (need >= 75%). The two streams do not stay in" -f $ratio)
    Write-Host "  order beyond that, which points at a framing fault rather than a different route."
}
Write-Host ""

$first = $divergences[0]
Write-Host ("First positional divergence at unit {0} (1-based {1}):" -f $first, ($first + 1))
Write-Host "  Positional, so a benign inserted action or a progress frame produces one too -"
Write-Host "  read it together with the prefix alignment above, never on its own."
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
