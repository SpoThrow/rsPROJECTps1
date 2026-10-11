<#
.SYNOPSIS
    OCRs the chatbox-creation-menu screenshots with line geometry, upscaled for the small text.

.DESCRIPTION
    The plain pass recovers the wording but not the layout, and the quantity selector in these
    screenshots is graphics rather than glyphs, so it comes back as "ooo". This pass upscales
    before recognising and prints each line's box, which is what says where the pieces sit: the
    question, the help line, the product row and the quantity row.

.EXAMPLE
    .\read-chatbox-screenshots.ps1 -Folder "C:\...\inspiration on chatbox ui..." -Scale 3 -Boxes
#>
param(
    [Parameter(Mandatory = $true)]
    [string] $Folder,

    # Upscale factor applied before recognition. Small 317-font text needs about 3.
    [int] $Scale = 3,

    # Print each line's bounding box, so the layout can be reasoned about without seeing it.
    [switch] $Boxes,

    [string] $Filter = '*.jpg'
)

$ErrorActionPreference = 'Stop'

Add-Type -AssemblyName System.Runtime.WindowsRuntime
Add-Type -AssemblyName System.Drawing

$asTaskGeneric = ([System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object {
        $_.Name -eq 'AsTask' -and
        $_.GetParameters().Count -eq 1 -and
        $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1'
    })[0]

function Await($operation, $resultType) {
    $asTask = $asTaskGeneric.MakeGenericMethod($resultType)
    $task = $asTask.Invoke($null, @($operation))
    $task.Wait(-1) | Out-Null
    $task.Result
}

[Windows.Storage.StorageFile, Windows.Storage, ContentType = WindowsRuntime] | Out-Null
[Windows.Media.Ocr.OcrEngine, Windows.Foundation, ContentType = WindowsRuntime] | Out-Null
[Windows.Graphics.Imaging.BitmapDecoder, Windows.Foundation, ContentType = WindowsRuntime] | Out-Null

$engine = [Windows.Media.Ocr.OcrEngine]::TryCreateFromUserProfileLanguages()
if ($null -eq $engine) { throw 'No OCR language pack is installed for the current user.' }

$scratch = Join-Path $env:TEMP 'chatbox-ocr'
if (-not (Test-Path -LiteralPath $scratch)) { New-Item -ItemType Directory -Path $scratch | Out-Null }

foreach ($image in Get-ChildItem -LiteralPath $Folder -Filter $Filter | Sort-Object Name) {
    # Upscale, so the 317 chatbox font is big enough for the recogniser.
    $source = [System.Drawing.Bitmap]::new($image.FullName)
    $big = [System.Drawing.Bitmap]::new(
        $source.Width * $Scale, $source.Height * $Scale,
        [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($big)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.DrawImage($source, 0, 0, $big.Width, $big.Height)
    $g.Dispose()
    $source.Dispose()

    $upscaled = Join-Path $scratch ($image.BaseName + '.png')
    $big.Save($upscaled, [System.Drawing.Imaging.ImageFormat]::Png)
    $big.Dispose()

    $file = Await ([Windows.Storage.StorageFile]::GetFileFromPathAsync($upscaled)) ([Windows.Storage.StorageFile])
    $stream = Await ($file.OpenAsync([Windows.Storage.FileAccessMode]::Read)) ([Windows.Storage.Streams.IRandomAccessStream])
    $decoder = Await ([Windows.Graphics.Imaging.BitmapDecoder]::CreateAsync($stream)) ([Windows.Graphics.Imaging.BitmapDecoder])
    $bitmap = Await ($decoder.GetSoftwareBitmapAsync()) ([Windows.Graphics.Imaging.SoftwareBitmap])
    $result = Await ($engine.RecognizeAsync($bitmap)) ([Windows.Media.Ocr.OcrResult])

    $originalWidth = [int]($decoder.PixelWidth / $Scale)
    $originalHeight = [int]($decoder.PixelHeight / $Scale)
    Write-Output ('=== ' + $image.Name + '  (original ' + $originalWidth + 'x' + $originalHeight + ') ===')
    foreach ($line in $result.Lines) {
        if ($Boxes) {
            # Word boxes read one at a time, because Measure-Object takes a property name rather
            # than a script block here. Divided back down to original pixels.
            $left = [double]::MaxValue
            $top = [double]::MaxValue
            $right = 0
            $bottom = 0
            foreach ($word in $line.Words) {
                $rect = $word.BoundingRect
                if ($rect.X -lt $left) { $left = $rect.X }
                if ($rect.Y -lt $top) { $top = $rect.Y }
                if (($rect.X + $rect.Width) -gt $right) { $right = $rect.X + $rect.Width }
                if (($rect.Y + $rect.Height) -gt $bottom) { $bottom = $rect.Y + $rect.Height }
            }
            $box = '  [' + [int]($left / $Scale) + ',' + [int]($top / $Scale) + ' -> ' +
                [int]($right / $Scale) + ',' + [int]($bottom / $Scale) + ']  '
            Write-Output ($box + $line.Text)
        } else {
            Write-Output ('  ' + $line.Text)
        }
    }
    Write-Output ''

    $stream.Dispose()
    $bitmap.Dispose()
}
