# Draws the placeholder art for the quantity-first creation chatbox.
#
# Three files, and they are the only art that chatbox owns:
#
#   Sprites/Interfaces/MakeX/QUANTITY 0.png   idle amount pill    28x31
#   Sprites/Interfaces/MakeX/QUANTITY 1.png   armed amount pill   28x31
#   Sprites/Interfaces/MakeX/SLOT.png         product slot frame  64x82
#
# Replacing them is a drop-in: MakeX loads them by those names and falls back to the same shapes
# filled in code, so nothing else has to change. The colours below are the ones MakeX's fallback
# uses, so a missing file and a generated one look identical.
#
# Written to both the repo's Sprites folder (so a re-skin has somewhere to work) and the live cache
# folder the client actually reads, because the client resolves Sprites/ under
# signlink.findcachedir().
#
# Usage:  .\make-quantity-sprites.ps1

param(
    [string[]]$Destinations = @()
)

Add-Type -AssemblyName System.Drawing

# The three faces, per state. The idle pill is LIGHT because its caption is drawn in black; the
# armed one is DARK because its caption is drawn in white. Getting that the wrong way round makes
# one of the two states unreadable, which is what the probe's luminance checks are for.
$PILL_IDLE = @{ Border = 0x6E5A3C; Highlight = 0xE6DAB8; Face = 0xC8B591; Shade = 0xA08B6A }
$PILL_ARMED = @{ Border = 0x3A2A10; Highlight = 0xF2D070; Face = 0x7A5416; Shade = 0x54380C }
$SLOT = @{ Border = 0x6E5A3C; Highlight = 0xE6DAB8; Face = 0xC8B591; Shade = 0xA08B6A }

function New-Bevelled {
    param(
        [string]$Path,
        [int]$Width,
        [int]$Height,
        [int]$Inset,
        [hashtable]$Palette
    )

    $bitmap = New-Object System.Drawing.Bitmap($Width, $Height)
    for ($y = 0; $y -lt $Height; $y++) {
        for ($x = 0; $x -lt $Width; $x++) {
            $outer = $x -lt $Inset -or $y -lt $Inset -or $x -ge $Width - $Inset -or $y -ge $Height - $Inset
            $nearEdge = $x -lt $Inset + 1 -or $y -lt $Inset + 1 -or
                $x -ge $Width - $Inset - 1 -or $y -ge $Height - $Inset - 1
            if ($outer) {
                $argb = 0xFF000000 -bor $Palette.Border
            } elseif ($nearEdge) {
                if ($x -lt $Inset + 1 -or $y -lt $Inset + 1) {
                    $argb = 0xFF000000 -bor $Palette.Highlight
                } else {
                    $argb = 0xFF000000 -bor $Palette.Shade
                }
            } else {
                $argb = 0xFF000000 -bor $Palette.Face
            }
            $bitmap.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($argb))
        }
    }
    # Corners knocked out, matching MakeX.bevelled, so the art does not read as a hard rectangle.
    $clear = [System.Drawing.Color]::FromArgb(0, 0, 0, 0)
    $bitmap.SetPixel(0, 0, $clear)
    $bitmap.SetPixel($Width - 1, 0, $clear)
    $bitmap.SetPixel(0, $Height - 1, $clear)
    $bitmap.SetPixel($Width - 1, $Height - 1, $clear)

    $directory = Split-Path -Parent $Path
    if (-not (Test-Path $directory)) {
        New-Item -ItemType Directory -Path $directory -Force | Out-Null
    }
    $bitmap.Save($Path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bitmap.Dispose()
    Write-Output "wrote $Path"
}

if ($Destinations.Count -eq 0) {
    $repoSprites = (Join-Path $PSScriptRoot "..\Sprites\Interfaces\MakeX").TrimEnd('\')
    $Destinations = @($repoSprites, (Join-Path $env:USERPROFILE "Biohazard.474\Sprites\Interfaces\MakeX"))
}

foreach ($destination in $Destinations) {
	# 28x31 with a 1px border: MakeX.QUANT_W x QUANT_H.
	New-Bevelled -Path (Join-Path $destination "QUANTITY 0.png") -Width 28 -Height 31 -Inset 1 -Palette $PILL_IDLE
	New-Bevelled -Path (Join-Path $destination "QUANTITY 1.png") -Width 28 -Height 31 -Inset 1 -Palette $PILL_ARMED
    # 64x82 with a 2px border: MakeX.SLOT_W x SLOT_H.
    New-Bevelled -Path (Join-Path $destination "SLOT.png") -Width 64 -Height 82 -Inset 2 -Palette $SLOT
}
