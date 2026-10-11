# Finds the shapes in a screenshot: dilates the ink, groups it into connected blobs, and
# reports each one's box and how solid it is.
#
# Why: the layout questions here are "how many buttons are there, where, and how big", and the
# answer has to come out of the pixels. A text line becomes one wide short blob with a low fill
# ratio; a button becomes a blob with a ring or a filled face; a bordered panel becomes one very
# large blob. Ink is luminance below -InkMax, dilation is -Dilate.
#
# Usage:
#   .\blobs-shot.ps1 -Image <path> [-Top 0 -Bottom 195 -Left 0 -Right 599 -Dilate 2 -MinPixels 20]

param(
    [Parameter(Mandatory = $true)][string] $Image,
    [int] $Top = 0,
    [int] $Bottom = 9999,
    [int] $Left = 0,
    [int] $Right = 9999,
    [int] $InkMax = 120,
    [int] $Dilate = 1,
    [int] $MinPixels = 15,
    # dark = ink darker than the paper, light = lighter than it, both = either.
    # The reference screenshots draw product names in white, which a dark-ink pass never sees.
    [ValidateSet('dark', 'light', 'both')][string] $Mode = 'dark',
    # How far a pixel's luminance must be from the paper's to count.
    [int] $Deviation = 30
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$bitmap = [System.Drawing.Bitmap]::FromFile((Resolve-Path $Image))
if ($Bottom -ge $bitmap.Height) { $Bottom = $bitmap.Height - 1 }
if ($Right -ge $bitmap.Width) { $Right = $bitmap.Width - 1 }
$w = $Right - $Left + 1
$h = $Bottom - $Top + 1

# The paper's luminance, as the median of a sample. A fixed cutoff cannot work across these
# JPEGs - each is dithered slightly differently - so the paper is measured, then deviated from.
$samples = New-Object System.Collections.ArrayList
for ($y = 0; $y -lt $h; $y += 3) {
    for ($x = 0; $x -lt $w; $x += 3) {
        $c = $bitmap.GetPixel($Left + $x, $Top + $y)
        [void]$samples.Add([int](0.299 * $c.R + 0.587 * $c.G + 0.114 * $c.B))
    }
}
$sorted = $samples | Sort-Object
$paper = $sorted[[int]($sorted.Count / 2)]

# Ink map, then dilation, as flat byte arrays indexed y*w+x.
$ink = New-Object 'bool[]' ($w * $h)
for ($y = 0; $y -lt $h; $y++) {
    for ($x = 0; $x -lt $w; $x++) {
        $c = $bitmap.GetPixel($Left + $x, $Top + $y)
        $l = 0.299 * $c.R + 0.587 * $c.G + 0.114 * $c.B
        $dark = ($paper - $l) -gt $Deviation
        $light = ($l - $paper) -gt $Deviation
        $ink[$y * $w + $x] = switch ($Mode) {
            'dark' { $dark }
            'light' { $light }
            default { $dark -or $light }
        }
    }
}
$solid = New-Object 'bool[]' ($w * $h)
for ($y = 0; $y -lt $h; $y++) {
    for ($x = 0; $x -lt $w; $x++) {
        if (-not $ink[$y * $w + $x]) { continue }
        for ($dy = -$Dilate; $dy -le $Dilate; $dy++) {
            for ($dx = -$Dilate; $dx -le $Dilate; $dx++) {
                $nx = $x + $dx
                $ny = $y + $dy
                if ($nx -lt 0 -or $ny -lt 0 -or $nx -ge $w -or $ny -ge $h) { continue }
                $solid[$ny * $w + $nx] = $true
            }
        }
    }
}

# Connected components over the dilated map (4-connected), then each component's box.
$seen = New-Object 'bool[]' ($w * $h)
$blobs = New-Object System.Collections.ArrayList
$stack = New-Object System.Collections.Generic.Stack[int]
for ($start = 0; $start -lt $w * $h; $start++) {
    if ($seen[$start] -or -not $solid[$start]) { continue }
    $stack.Clear()
    $stack.Push($start)
    $seen[$start] = $true
    $minX = $w; $minY = $h; $maxX = -1; $maxY = -1; $count = 0; $inkCount = 0
    while ($stack.Count -gt 0) {
        $p = $stack.Pop()
        # Integer division, not [int]($p / $w): a PowerShell [int] cast ROUNDS, so for the right
        # half of each row it yields the next row down and px comes out negative.
        $py = [Math]::Floor($p / $w)
        $px = $p - $py * $w
        $count++
        if ($ink[$p]) { $inkCount++ }
        if ($px -lt $minX) { $minX = $px }
        if ($px -gt $maxX) { $maxX = $px }
        if ($py -lt $minY) { $minY = $py }
        if ($py -gt $maxY) { $maxY = $py }
        if ($px -gt 0 -and -not $seen[$p - 1] -and $solid[$p - 1]) { $seen[$p - 1] = $true; $stack.Push($p - 1) }
        if ($px -lt $w - 1 -and -not $seen[$p + 1] -and $solid[$p + 1]) { $seen[$p + 1] = $true; $stack.Push($p + 1) }
        if ($py -gt 0 -and -not $seen[$p - $w] -and $solid[$p - $w]) { $seen[$p - $w] = $true; $stack.Push($p - $w) }
        if ($py -lt $h - 1 -and -not $seen[$p + $w] -and $solid[$p + $w]) { $seen[$p + $w] = $true; $stack.Push($p + $w) }
    }
    if ($inkCount -lt $MinPixels) { continue }
    [void]$blobs.Add([pscustomobject]@{
            L    = $Left + $minX
            T    = $Top + $minY
            R    = $Left + $maxX
            B    = $Top + $maxY
            W    = $maxX - $minX + 1
            H    = $maxY - $minY + 1
            Ink  = $inkCount
            Fill = [Math]::Round($inkCount / (($maxX - $minX + 1) * ($maxY - $minY + 1)), 2)
        })
}

Write-Output ("{0}  {1}x{2}   blobs in x {3}..{4} y {5}..{6}, paper lum {7}, mode {8}, dev >{9}, dilate {10}, min {11}px" -f `
        (Split-Path -Leaf $Image), $bitmap.Width, $bitmap.Height, $Left, $Right, $Top, $Bottom,
        $paper, $Mode, $Deviation, $Dilate, $MinPixels)
Write-Output ''
Write-Output ("{0,5} {1,5} {2,5} {3,5} {4,5} {5,5} {6}" -f 'left', 'top', 'right', 'bottom', 'w', 'h', 'ink/fill')
foreach ($b in ($blobs | Sort-Object -Property T, L)) {
    Write-Output ("{0,5} {1,5} {2,5} {3,5} {4,5} {5,5} {6} ({7})" -f
            $b.L, $b.T, $b.R, $b.B, $b.W, $b.H, $b.Ink, $b.Fill)
}
Write-Output ''
Write-Output ("total blobs: {0}" -f $blobs.Count)
$bitmap.Dispose()
