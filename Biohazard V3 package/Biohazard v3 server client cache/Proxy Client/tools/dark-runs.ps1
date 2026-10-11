# Prints where the dark pixels are on given image rows, as x runs.
#
# Why: the pill boundaries and their captions are the layout facts here, and they are 1-2px
# strokes in a JPEG. An ASCII map of the region is readable but counting characters to find an
# x is not, so this reports the runs directly.
#
# Usage:
#   .\dark-runs.ps1 -Image <path> -Rows 13,18,51,52 [-From 360] [-Threshold 120]

param(
    [Parameter(Mandatory = $true)][string] $Image,
    # Comma-separated, taken as a string: an int[] parameter does not survive being passed from
    # a command line (the whole "13,18,19,52" arrives as one token and is cast to a single int).
    [string] $Rows = '18,51',
    [int] $From = 0,
    [int] $To = 9999,
    [int] $Threshold = 120,
    # Report the opposite: runs BRIGHTER than the threshold, for white-on-dark captions.
    [switch] $Light,
    [int] $MinRun = 1
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$bitmap = [System.Drawing.Bitmap]::FromFile((Resolve-Path $Image))
if ($To -ge $bitmap.Width) { $To = $bitmap.Width - 1 }
$rowList = $Rows.Split(',') | ForEach-Object { [int]$_.Trim() }
Write-Output ("{0}  {1}x{2}   rows {3}, x {4}..{5}, {6}, threshold {7}" -f
        (Split-Path -Leaf $Image), $bitmap.Width, $bitmap.Height, ($rowList -join ','), $From, $To,
        $(if ($Light) { 'lighter than' } else { 'darker than' }), $Threshold)

foreach ($y in $rowList) {
    if ($y -lt 0 -or $y -ge $bitmap.Height) { continue }
    $runs = @()
    $inRun = $false
    $start = 0
    for ($x = $From; $x -le $To; $x++) {
        $c = $bitmap.GetPixel($x, $y)
        $l = 0.299 * $c.R + 0.587 * $c.G + 0.114 * $c.B
        $hit = if ($Light) { $l -gt $Threshold } else { $l -lt $Threshold }
        if ($hit) {
            if (-not $inRun) { $inRun = $true; $start = $x }
        } elseif ($inRun) {
            $inRun = $false
            if (($x - $start) -ge $MinRun) { $runs += ('{0}-{1}' -f $start, ($x - 1)) }
        }
    }
    if ($inRun -and (($To - $start + 1) -ge $MinRun)) { $runs += ('{0}-{1}' -f $start, $To) }
    Write-Output ("  y={0,3}: {1}" -f $y, ($runs -join '  '))
}

$bitmap.Dispose()
