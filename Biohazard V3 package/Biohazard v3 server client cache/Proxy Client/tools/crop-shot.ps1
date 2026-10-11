# Crops and magnifies a region of a screenshot, so a small piece of art can be looked at
# without the rest of the frame. Nearest-neighbour on purpose: the point is to see the pixels.
#
# Usage:
#   .\crop-shot.ps1 -Image <path> -Left 380 -Top 0 -Right 600 -Bottom 60 -Zoom 6 -Out out.png

param(
    [Parameter(Mandatory = $true)][string] $Image,
    [int] $Left = 0,
    [int] $Top = 0,
    [int] $Right = 599,
    [int] $Bottom = 195,
    [int] $Zoom = 4,
    [Parameter(Mandatory = $true)][string] $Out,
    [switch] $Smooth
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$source = [System.Drawing.Bitmap]::FromFile((Resolve-Path $Image))
if ($Right -ge $source.Width) { $Right = $source.Width - 1 }
if ($Bottom -ge $source.Height) { $Bottom = $source.Height - 1 }
$w = $Right - $Left + 1
$h = $Bottom - $Top + 1

$crop = [System.Drawing.Bitmap]::new($w * $Zoom, $h * $Zoom, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$g = [System.Drawing.Graphics]::FromImage($crop)
$g.InterpolationMode = if ($Smooth) {
    [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
} else {
    [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
}
$g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
$g.DrawImage($source, [System.Drawing.Rectangle]::new(0, 0, $crop.Width, $crop.Height),
    [System.Drawing.Rectangle]::new($Left, $Top, $w, $h), [System.Drawing.GraphicsUnit]::Pixel)

# A 1px grid every 10 source pixels, so a coordinate can be read off the picture.
$pen = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(90, 0, 160, 255), 1)
for ($x = $Left; $x -le $Right; $x += 10) {
    $g.DrawLine($pen, ($x - $Left) * $Zoom, 0, ($x - $Left) * $Zoom, $crop.Height)
}
for ($y = $Top; $y -le $Bottom; $y += 10) {
    $g.DrawLine($pen, 0, ($y - $Top) * $Zoom, $crop.Width, ($y - $Top) * $Zoom)
}
$pen.Dispose()
$g.Dispose()

$dir = Split-Path -Parent $Out
if ($dir -and -not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
$crop.Save($Out, [System.Drawing.Imaging.ImageFormat]::Png)
$crop.Dispose()
$source.Dispose()
Write-Output ("crop {0},{1} -> {2},{3} of {4}x{5}, zoom {6}  =>  {7}" -f
        $Left, $Top, $Right, $Bottom, $w, $h, $Zoom, $Out)
