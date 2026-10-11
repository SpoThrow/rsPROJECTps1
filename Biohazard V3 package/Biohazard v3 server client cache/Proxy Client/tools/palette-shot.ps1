<#
.SYNOPSIS
    Renders a screenshot region as letters, one letter per quantised colour, so the layout of a
    piece of interface art can be read as text.

.DESCRIPTION
    A luminance ramp is useless here: the chatbox art is all parchment browns within a narrow
    brightness band, so everything collapses to one character. Quantising to colour FIRST and then
    giving each of the largest colour clusters its own letter separates the panel, its border, the
    item icons and the button faces from each other.

    Printed alongside is each cluster's share and bounding box, which is what actually locates the
    pieces without needing to see the image.

.EXAMPLE
    .\palette-shot.ps1 -Image shot.jpg -RowTop 0 -RowBottom 145 -Cols 170
#>
param(
	[Parameter(Mandatory = $true)][string] $Image,
	[int] $Cols = 160,
	[int] $RowTop = 0,
	[int] $RowBottom = -1,
	[int] $ColLeft = 0,
	[int] $ColRight = -1,
	[int] $Colours = 10
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$bmp = [System.Drawing.Bitmap]::new($Image)
$l = $ColLeft; $r = if ($ColRight -ge 0) { $ColRight } else { $bmp.Width }
$t = $RowTop; $b = if ($RowBottom -ge 0) { $RowBottom } else { $bmp.Height }
$cw = $r - $l; $ch = $b - $t
$rows = [int]([math]::Max(1, $ch / $cw * $Cols * 0.5))

# Cluster the crop's colours by quantising to 32s, then keep the biggest clusters.
$hist = @{}
for ($y = $t; $y -lt $b; $y++) {
	for ($x = $l; $x -lt $r; $x++) {
		$c = $bmp.GetPixel($x, $y)
		$key = (([int]($c.R / 32)) * 64) + (([int]($c.G / 32)) * 8) + ([int]($c.B / 32))
		if ($hist.ContainsKey($key)) { $hist[$key]++ } else { $hist[$key] = 1 }
	}
}
$total = $cw * $ch
$top = $hist.GetEnumerator() | Sort-Object -Property Value -Descending | Select-Object -First $Colours

$ramp = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789'
$map = @{}
$legend = @()
$i = 0
foreach ($e in $top) {
	$key = [int]$e.Key
	$red = [int]($key / 64) * 32 + 16
	$green = ([int]($key / 8) % 8) * 32 + 16
	$blue = ($key % 8) * 32 + 16
	$map[$key] = $ramp[$i]
	$legend += ('  {0} = #{1:X2}{2:X2}{3:X2}   {4,6:P2}' -f $ramp[$i], $red, $green, $blue, ($e.Value / $total))
	$i++
}

Write-Output ("image {0}x{1}  crop x[{2}..{3}] y[{4}..{5}]  render {6}x{7}" -f $bmp.Width, $bmp.Height, $l, $r, $t, $b, $Cols, $rows)
Write-Output "colours (32-step quantised), largest first:"
$legend | ForEach-Object { Write-Output $_ }

# Bounding box of each cluster, so the pieces can be located in original pixels.
Write-Output "bounding boxes:"
foreach ($e in $top) {
	$key = [int]$e.Key
	$minX = [int]::MaxValue; $minY = [int]::MaxValue; $maxX = 0; $maxY = 0
	for ($y = $t; $y -lt $b; $y++) {
		for ($x = $l; $x -lt $r; $x++) {
			$c = $bmp.GetPixel($x, $y)
			$k = (([int]($c.R / 32)) * 64) + (([int]($c.G / 32)) * 8) + ([int]($c.B / 32))
			if ($k -ne $key) { continue }
			if ($x -lt $minX) { $minX = $x }
			if ($x -gt $maxX) { $maxX = $x }
			if ($y -lt $minY) { $minY = $y }
			if ($y -gt $maxY) { $maxY = $y }
		}
	}
	Write-Output ('  {0}: {1,4},{2,4} -> {3,4},{4,4}' -f $map[$key], $minX, $minY, $maxX, $maxY)
}
Write-Output ""

for ($ry = 0; $ry -lt $rows; $ry++) {
	$sb = [System.Text.StringBuilder]::new()
	for ($rx = 0; $rx -lt $Cols; $rx++) {
		$x0 = $l + [int]($rx * $cw / $Cols)
		$y0 = $t + [int]($ry * $ch / $rows)
		$c = $bmp.GetPixel($x0, $y0)
		$key = (([int]($c.R / 32)) * 64) + (([int]($c.G / 32)) * 8) + ([int]($c.B / 32))
		[void]$sb.Append($(if ($map.ContainsKey($key)) { $map[$key] } else { '.' }))
	}
	Write-Output $sb.ToString()
}
Write-Output ""

# A ruler so columns can be read back as original pixel positions.
$ruler = [System.Text.StringBuilder]::new()
for ($rx = 0; $rx -lt $Cols; $rx++) {
	$x = $l + [int]($rx * $cw / $Cols)
	[void]$ruler.Append($(if ($x % 100 -eq 0) { '|' } elseif ($x % 20 -eq 0) { "'" } else { '-' }))
}
Write-Output ("0         1         2         3         4         5         6         7")
Write-Output $ruler.ToString()
Write-Output ("x = {0} .. {1}   y = {2} .. {3}" -f $l, $r, $t, $b)

$bmp.Dispose()
