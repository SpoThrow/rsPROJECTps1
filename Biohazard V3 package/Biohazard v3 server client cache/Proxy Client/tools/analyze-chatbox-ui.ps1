# Reports where content actually sits in a chatbox screenshot: row bands the eye would see,
# and the colour clusters inside a band. Written because OCR returns the wording but the
# quantity selector is drawn art, and its position/size is what the layout has to match.
#
# Usage:
#   .\analyze-chatbox-ui.ps1 -Image <path> [-BandTop 55 -BandBottom 165 -Scale 1]

param(
    [Parameter(Mandatory = $true)][string] $Image,
    [int] $BandTop = 55,
    [int] $BandBottom = 165,
    [int] $Scale = 4,
    [int] $ColourTolerance = 24,
    [switch] $Ascii
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$bitmap = [System.Drawing.Bitmap]::FromFile((Resolve-Path $Image))
Write-Output ("image {0}  {1}x{2}" -f (Split-Path -Leaf $Image), $bitmap.Width, $bitmap.Height)

# --- the panel: every pixel's colour counted, so the dominant one is the paper ---------
$counts = @{}
for ($y = 0; $y -lt $bitmap.Height; $y++) {
    for ($x = 0; $x -lt $bitmap.Width; $x++) {
        $c = $bitmap.GetPixel($x, $y).ToArgb() -band 0xFFFFFF
        if ($counts.ContainsKey($c)) { $counts[$c]++ } else { $counts[$c] = 1 }
    }
}
$top = $counts.GetEnumerator() | Sort-Object -Property Value -Descending | Select-Object -First 8
Write-Output 'dominant colours:'
foreach ($c in $top) {
    Write-Output ("  #{0:X6}  {1} px  ({2:P1})" -f $c.Key, $c.Value, ($c.Value / ($bitmap.Width * $bitmap.Height)))
}

# --- row profile: how many pixels on each line are NOT the paper, per band -------------
$paper = $top[0].Key
Write-Output ''
Write-Output ("row profile (pixels differing from paper #{0:X6})" -f $paper)
$runs = @()
$inRun = $false
for ($y = 0; $y -lt $bitmap.Height; $y++) {
    $n = 0
    for ($x = 0; $x -lt $bitmap.Width; $x++) {
        $c = $bitmap.GetPixel($x, $y).ToArgb() -band 0xFFFFFF
        $dr = [Math]::Abs((($c -shr 16) -band 0xFF) - (($paper -shr 16) -band 0xFF))
        $dg = [Math]::Abs((($c -shr 8) -band 0xFF) - (($paper -shr 8) -band 0xFF))
        $db = [Math]::Abs(($c -band 0xFF) - ($paper -band 0xFF))
        if (($dr + $dg + $db) -gt $ColourTolerance) { $n++ }
    }
    if ($n -gt 2) {
        if (-not $inRun) { $inRun = $true; $start = $y; $peak = 0 }
        if ($n -gt $peak) { $peak = $n }
    } elseif ($inRun) {
        $inRun = $false
        $runs += [pscustomobject]@{ Top = $start; Bottom = $y - 1; Height = $y - $start; Peak = $peak }
    }
}
if ($inRun) { $runs += [pscustomobject]@{ Top = $start; Bottom = $bitmap.Height - 1; Height = $bitmap.Height - $start; Peak = $peak } }
foreach ($r in $runs) {
    Write-Output ("  y {0,4}..{1,-4} h={2,-4} widest row {3}" -f $r.Top, $r.Bottom, $r.Height, $r.Peak)
}

# --- colour clusters inside one band, with boxes and an optional ASCII map -------------
$label = [char[]]'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#$%&*+=<>?/'
$clusters = @{}
$pixelCluster = @{}
for ($y = $BandTop; $y -le $BandBottom -and $y -lt $bitmap.Height; $y++) {
    for ($x = 0; $x -lt $bitmap.Width; $x++) {
        $c = $bitmap.GetPixel($x, $y).ToArgb() -band 0xFFFFFF
        $key = '#{0:X6}' -f ((($c -shr 16) -band 0xF8) -shl 16 -bor ((($c -shr 8) -band 0xF8) -shl 8) -bor ($c -band 0xF8))
        if (-not $clusters.ContainsKey($key)) {
            $clusters[$key] = [pscustomobject]@{ Key = $key; Count = 0; L = $bitmap.Width; T = $bitmap.Height; R = -1; B = -1 }
        }
        $cl = $clusters[$key]
        $cl.Count++
        if ($x -lt $cl.L) { $cl.L = $x }
        if ($x -gt $cl.R) { $cl.R = $x }
        if ($y -lt $cl.T) { $cl.T = $y }
        if ($y -gt $cl.B) { $cl.B = $y }
        $pixelCluster["$x,$y"] = $key
    }
}

Write-Output ''
Write-Output ("colour clusters in band y {0}..{1} (quantised to 5 bits per channel)" -f $BandTop, $BandBottom)
$index = 0
$order = @{}
foreach ($c in ($clusters.Values | Sort-Object -Property Count -Descending | Select-Object -First 18)) {
    $ch = if ($index -lt $label.Count) { $label[$index] } else { '?' }
    $order[$c.Key] = $ch
    Write-Output ("  {0}  {1}  {2,7} px  box [{3},{4} -> {5},{6}]" -f $ch, $c.Key, $c.Count, $c.L, $c.T, $c.R, $c.B)
    $index++
}

if ($Ascii) {
    Write-Output ''
    Write-Output ("ascii map, 1 char = {0}x{0} px, top-left of band at ({1},{2})" -f $Scale, 0, $BandTop)
    for ($y = $BandTop; $y -le $BandBottom -and $y -lt $bitmap.Height; $y += $Scale) {
        $sb = New-Object System.Text.StringBuilder
        for ($x = 0; $x -lt $bitmap.Width; $x += $Scale) {
            $c = $bitmap.GetPixel($x, $y).ToArgb() -band 0xFFFFFF
            $key = '#{0:X6}' -f ((($c -shr 16) -band 0xF8) -shl 16 -bor ((($c -shr 8) -band 0xF8) -shl 8) -bor ($c -band 0xF8))
            if ($order.ContainsKey($key)) { [void]$sb.Append($order[$key]) } else { [void]$sb.Append('.') }
        }
        Write-Output ("{0,4}|{1}" -f $y, $sb.ToString())
    }
}

$bitmap.Dispose()
