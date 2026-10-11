<#
.SYNOPSIS
    Rebuilds the XP-drop skill icons from the skill tab's own artwork.

.DESCRIPTION
    The XP drop that floats off the orb draws an icon per skill from
    {cachedir}/Sprites/Gameframe/XP/skill N.png, indexed by the same skill number the server
    sends (Skills.skillNames: runecrafting is 20, slayer 18, farming 19). Those files are
    hand-placed and were not all filed by that index: Slayer's artwork sat in 20, so every
    runecrafting drop drew a slayer skull, Runecrafting's sat in 19, and there was no 18 at all.

    The authoritative per-index art is the set the skill tab and the hiscores panel use,
    {cachedir}/Sprites/Interfaces/skillchat/skill N.png. This script re-derives an XP icon from
    it: magenta knocked out, cropped to the artwork, scaled to 18x18 with aspect kept, centred.

    Running it over the icons that are already correct shows the shipped set was made the same way
    in spirit but not by this exact rule: the same artwork with the same hard alpha in the same
    18px box, and the art's own box within a pixel or two, but not byte-for-byte the same files. So
    the default writes only the three that are wrong or missing -- 19 and 20 are corrected and 18
    is created -- rather than re-deriving twenty-three to chase a hairline difference.

    Two caveats inherited from the source:
      * skillchat files 21 and 22 are swapped relative to the skill numbers (21.png is
        construction's art, 22.png is hunter's). HiscoresPanel.java works around the same swap,
        and this script applies it too, so the mapping is by skill number on both ends.
      * Only the skills named in -Skills are written. The rest are left exactly as they are,
        which is deliberate: they measure correctly against the same source and there is no
        reason to churn twenty files to fix three.

.EXAMPLE
    # Fix the three mis-filed icons in the default cache.
    .\rebuild-xp-skill-icons.ps1

.EXAMPLE
    # Preview where each skill's icon would come from, writing nothing.
    .\rebuild-xp-skill-icons.ps1 -WhatIf

.EXAMPLE
    # Rebuild the whole set from the tab art.
    .\rebuild-xp-skill-icons.ps1 -Skills (0..22)
#>
[CmdletBinding(SupportsShouldProcess = $true)]
param(
    # The client's cache root, the directory findcachedir() resolves to.
    [string] $CacheDir = (Join-Path $env:USERPROFILE 'Biohazard.474'),

    # Which skill numbers to rebuild. Defaults to the three that are wrong or absent.
    [int[]] $Skills = @(18, 19, 20),

    # Square edge of the written icon. The client sizes the drop from the file's own width, and
    # the shipped set is 18.
    [int] $Size = 18,

    # What counts as the transparent colour in the source art.
    [int] $MagentaTolerance = 60,

    # Copy an overwritten icon to <name>.png.bak before replacing it.
    [switch] $Backup
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$sourceDir = Join-Path $CacheDir 'Sprites/Interfaces/skillchat'
$targetDir = Join-Path $CacheDir 'Sprites/Gameframe/XP'

if (-not (Test-Path -LiteralPath $sourceDir)) {
    throw "The skill tab art is not where it was expected: $sourceDir"
}
if (-not (Test-Path -LiteralPath $targetDir)) {
    throw "The XP icon folder is not where it was expected: $targetDir"
}

# The one place the folder's numbering is known to disagree with the skill numbers.
function Get-ChatSpriteFor([int] $skill) {
    switch ($skill) {
        21 { 22 }
        22 { 21 }
        default { $skill }
    }
}

function Test-Magenta([System.Drawing.Color] $c) {
    return [Math]::Abs($c.R - 255) -lt $MagentaTolerance -and
           $c.G -lt $MagentaTolerance -and
           [Math]::Abs($c.B - 255) -lt $MagentaTolerance
}

# Magenta out, crop to the artwork, scale to fit $Size with the aspect kept, centre it.
function ConvertTo-XpIcon([string] $sourcePath) {
    $source = [System.Drawing.Bitmap]::new($sourcePath)
    try {
        $cut = [System.Drawing.Bitmap]::new(
            $source.Width, $source.Height, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
        try {
            $minX = $source.Width; $minY = $source.Height; $maxX = -1; $maxY = -1
            for ($y = 0; $y -lt $source.Height; $y++) {
                for ($x = 0; $x -lt $source.Width; $x++) {
                    $p = $source.GetPixel($x, $y)
                    if (Test-Magenta $p) {
                        $cut.SetPixel($x, $y, [System.Drawing.Color]::Transparent)
                        continue
                    }
                    $cut.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, $p.R, $p.G, $p.B))
                    if ($x -lt $minX) { $minX = $x }
                    if ($y -lt $minY) { $minY = $y }
                    if ($x -gt $maxX) { $maxX = $x }
                    if ($y -gt $maxY) { $maxY = $y }
                }
            }
            if ($maxX -lt $minX) {
                throw "$sourcePath is all transparent; there is no icon in it."
            }

            $artW = $maxX - $minX + 1
            $artH = $maxY - $minY + 1
            $scale = [Math]::Min($Size / [double] $artW, $Size / [double] $artH)
            $drawW = [Math]::Max(1, [int] [Math]::Round($artW * $scale))
            $drawH = [Math]::Max(1, [int] [Math]::Round($artH * $scale))

            $icon = [System.Drawing.Bitmap]::new(
                $Size, $Size, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
            try {
                $g = [System.Drawing.Graphics]::FromImage($icon)
                try {
                    $g.Clear([System.Drawing.Color]::Transparent)
                    # Nearest neighbour, deliberately. The shipped set is hard-edged -- every pixel
                    # is either fully opaque or fully clear, and each icon keeps to the source art's
                    # own handful of colours -- so it was made by picking source pixels rather than
                    # by averaging them. A smooth filter here would leave the three fixed icons
                    # visibly softer than the twenty they sit beside.
                    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
                    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
                    $dest = [System.Drawing.Rectangle]::new(
                        [int] (($Size - $drawW) / 2), [int] (($Size - $drawH) / 2), $drawW, $drawH)
                    $src = [System.Drawing.Rectangle]::new($minX, $minY, $artW, $artH)
                    $g.DrawImage($cut, $dest, $src, [System.Drawing.GraphicsUnit]::Pixel)
                } finally { $g.Dispose() }
                return $icon
            } catch {
                $icon.Dispose()
                throw
            }
        } finally { $cut.Dispose() }
    } finally { $source.Dispose() }
}

$changed = 0
foreach ($skill in $Skills) {
    $chat = Get-ChatSpriteFor $skill
    $sourcePath = Join-Path $sourceDir "skill $chat.png"
    $targetPath = Join-Path $targetDir "skill $skill.png"
    $action = if (Test-Path -LiteralPath $targetPath) { 'Rewrite' } else { 'Create' }

    # The source carries the swap, so say so when it bites: it is the one confusing line here.
    $via = if ($chat -eq $skill) { "skill $chat" } else { "skill $chat (the hunter/construction swap)" }
    if (-not $PSCmdlet.ShouldProcess($targetPath, "$action from $via")) {
        continue
    }
    if (-not (Test-Path -LiteralPath $sourcePath)) {
        Write-Warning "skill $skill has no source art at $sourcePath; skipped."
        continue
    }

    $icon = ConvertTo-XpIcon $sourcePath
    try {
        if ($Backup -and (Test-Path -LiteralPath $targetPath)) {
            Copy-Item -LiteralPath $targetPath -Destination "$targetPath.bak" -Force
        }
        $icon.Save($targetPath, [System.Drawing.Imaging.ImageFormat]::Png)
        $changed++
        Write-Host "$action skill $skill.png from $via"
    } finally { $icon.Dispose() }
}

Write-Host "$changed icon(s) written to $targetDir"
