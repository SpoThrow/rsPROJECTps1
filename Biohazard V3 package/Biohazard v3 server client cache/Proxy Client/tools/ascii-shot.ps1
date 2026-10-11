# Renders a screenshot region as a luminance ASCII map.
#
# Why: these are JPEGs of dithered parchment, so a colour-cluster reading returns dozens of
# near-identical "clusters" that are all the same paper. Luminance is what separates the things
# a layout cares about - the dark text/borders, the mid-tone button faces and the paper - so
# this quantises to a fixed ramp rather than clustering.
#
# Usage:
#   .\ascii-shot.ps1 -Image <path> -Top 0 -Bottom 195 -Scale 3

param(
    [Parameter(Mandatory = $true)][string] $Image,
    [int] $Top = 0,
    [int] $Bottom = 9999,
    [int] $Left = 0,
    [int] $Right = 9999,
    [int] $Scale = 3,
    # Below this luminance the pixel is drawn as text/ink; above, it is paper or a face.
    [int] $InkMax = 110
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$bitmap = [System.Drawing.Bitmap]::FromFile((Resolve-Path $Image))
if ($Bottom -ge $bitmap.Height) { $Bottom = $bitmap.Height - 1 }
if ($Right -ge $bitmap.Width) { $Right = $bitmap.Width - 1 }
Write-Output ("{0}  {1}x{2}   map x {3}..{4} y {5}..{6} scale {7}" -f `
        (Split-Path -Leaf $Image), $bitmap.Width, $bitmap.Height, $Left, $Right, $Top, $Bottom, $Scale)

# The ramp, darkest first. Ink is '#'/'@', paper is '.'/' '.
$ramp = '@#%&*+=:-. '
$levels = $ramp.Length

Write-Output ("columns 0..{0}, each char = {1}px" -f $Right, $Scale)
$header = '     '
for ($x = $Left; $x -le $Right; $x += $Scale * 10) {
    $header += ('{0,-10}' -f $x)
}
Write-Output $header

for ($y = $Top; $y -le $Bottom; $y += $Scale) {
    $sb = New-Object System.Text.StringBuilder
    for ($x = $Left; $x -le $Right; $x += $Scale) {
        # Average the block so a single dark pixel in the block still shows.
        $sum = 0
        $n = 0
        $darkest = 255
        for ($dy = 0; $dy -lt $Scale; $dy++) {
            for ($dx = 0; $dx -lt $Scale; $dx++) {
                $px = $x + $dx
                $py = $y + $dy
                if ($px -gt $Right -or $py -gt $Bottom) { continue }
                $c = $bitmap.GetPixel($px, $py)
                $l = [int](0.299 * $c.R + 0.587 * $c.G + 0.114 * $c.B)
                $sum += $l
                $n++
                if ($l -lt $darkest) { $darkest = $l }
            }
        }
        $avg = if ($n -gt 0) { $sum / $n } else { 255 }
        # Weight towards the darkest pixel, so thin dark strokes are not averaged away.
        $value = [Math]::Min($avg, ($darkest + 40))
        $idx = [int](($value / 255) * ($levels - 1))
        if ($idx -lt 0) { $idx = 0 }
        if ($idx -ge $levels) { $idx = $levels - 1 }
        [void]$sb.Append($ramp[$idx])
    }
    Write-Output ("{0,4}|{1}" -f $y, $sb.ToString())
}

$bitmap.Dispose()
