<#
    Regenerates Proxy Client/src/ObjectCollisionSizes.java from the server's
    Data/objectSize.cfg.

    Why this exists: the client builds its own collision from loc.dat object sizes
    (Class11.method212 / method216), and loc.dat carries no size for most scenery, so
    the client and the server disagree about ~2229 objects once the server switches to
    the authoritative table. Embedding the table in the client keeps the two in step
    without asking every player to drop a config file into their cache directory.

    Run it after editing Data/objectSize.cfg, then rebuild the client:

        powershell -ExecutionPolicy Bypass -File tools/generate-object-collision-sizes.ps1
#>

$ErrorActionPreference = 'Stop'

$toolsDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$clientDir = Split-Path -Parent $toolsDir
$repoRoot = Split-Path -Parent $clientDir
$cfgPath = Join-Path $repoRoot 'Proxy Server\Data\objectSize.cfg'
$outPath = Join-Path $clientDir 'src\ObjectCollisionSizes.java'

if (-not (Test-Path $cfgPath)) {
    throw "Cannot find the object size table at $cfgPath"
}

Write-Host "Reading $cfgPath"

# id -> packed byte, where 0 means "no entry" and anything else is (width << 4) | height.
# Widths top out at 8 and heights at 7 in the current table, so four bits each is plenty;
# the generator fails loudly rather than silently truncating if that ever stops holding.
$sizes = @{}
$maxId = 0
foreach ($line in Get-Content $cfgPath) {
    if ($line -notmatch '^objectId = (\d+)') { continue }
    $id = [int]$Matches[1]
    $sizeText = $null
    foreach ($field in ($line -split "`t")) {
        $f = $field.Trim()
        if ($f -match '^\d+x\d+$') { $sizeText = $f; break }
    }
    if (-not $sizeText) { continue }
    $parts = $sizeText -split 'x'
    $w = [int]$parts[0]
    $h = [int]$parts[1]
    if ($w -lt 1 -or $h -lt 1 -or $w -gt 15 -or $h -gt 15) {
        throw "Object $id has footprint $sizeText, which does not fit in four bits per axis"
    }
    $sizes[$id] = [byte](($w -shl 4) -bor $h)
    if ($id -gt $maxId) { $maxId = $id }
}

Write-Host ("Parsed {0} footprints, highest object id {1}" -f $sizes.Count, $maxId)

$table = New-Object byte[] ($maxId + 1)
foreach ($id in $sizes.Keys) { $table[$id] = $sizes[$id] }

$hex = New-Object System.Text.StringBuilder ($table.Length * 2)
foreach ($b in $table) { [void]$hex.Append($b.ToString('x2')) }
$packed = $hex.ToString()

# Split into chunks: keeps every string literal well under the 65535-byte constant pool limit.
$chunkSize = 4000
$chunks = New-Object System.Collections.Generic.List[string]
for ($i = 0; $i -lt $packed.Length; $i += $chunkSize) {
    $len = [Math]::Min($chunkSize, $packed.Length - $i)
    $chunks.Add($packed.Substring($i, $len))
}

# Hash of the cfg this file was generated from. The client build's
# verifyObjectCollisionSizes task recomputes it and fails on a mismatch, so a stale
# generated file cannot pass unnoticed. Nothing else about a generated file reveals
# its age, and the client and server must not drift apart on scenery footprints.
$cfgHash = (Get-FileHash -LiteralPath $cfgPath -Algorithm SHA256).Hash.ToLower()

$builder = New-Object System.Text.StringBuilder
[void]$builder.AppendLine('/*')
[void]$builder.AppendLine(' * GENERATED FILE - do not hand-edit.')
[void]$builder.AppendLine(' *')
[void]$builder.AppendLine(' * Collision footprints for scenery, mirrored from the server''s Data/objectSize.cfg.')
[void]$builder.AppendLine(' * The client normally sizes objects from loc.dat via ObjectDef.anInt744/anInt761, but that')
[void]$builder.AppendLine(' * data has no size for most scenery, so the client would disagree with the server about')
[void]$builder.AppendLine(' * ~2229 objects. Class11 builds collision from whichever numbers it is handed, so the')
[void]$builder.AppendLine(' * collision call sites apply these footprints instead - while ObjectDef.anInt744/anInt761')
[void]$builder.AppendLine(' * keep their cache values, so model centring is untouched.')
[void]$builder.AppendLine(' *')
[void]$builder.AppendLine(' * Regenerate after editing Data/objectSize.cfg:')
[void]$builder.AppendLine(' *     powershell -ExecutionPolicy Bypass -File tools/generate-object-collision-sizes.ps1')
[void]$builder.AppendLine(' */')
[void]$builder.AppendLine('public final class ObjectCollisionSizes')
[void]$builder.AppendLine('{')
[void]$builder.AppendLine('')
[void]$builder.AppendLine('	/** Highest object id in the table. */')
[void]$builder.AppendLine('	private static final int MAX_ID = ' + $maxId + ';')
[void]$builder.AppendLine('')
[void]$builder.AppendLine('	/**')
[void]$builder.AppendLine('	 * SHA-256 of the server Data/objectSize.cfg this file was generated from.')
[void]$builder.AppendLine('	 * Read only by the build (verifyObjectCollisionSizes), which recomputes it and fails')
[void]$builder.AppendLine('	 * if the two disagree: the client and server must not drift on scenery footprints,')
[void]$builder.AppendLine('	 * and a generated file gives no other clue that it is stale.')
[void]$builder.AppendLine('	 */')
[void]$builder.AppendLine('	static final String SOURCE_CFG_SHA256 = "' + $cfgHash + '";')
[void]$builder.AppendLine('')
[void]$builder.AppendLine('	/**')
[void]$builder.AppendLine('	 * One hex byte per object id, indexed by id. 0 means "the table has no entry";')
[void]$builder.AppendLine('	 * otherwise the byte is (width << 4) | height, both at least 1. Hex rather than Base64')
[void]$builder.AppendLine('	 * because the client targets Java 7, where java.util.Base64 does not exist.')
[void]$builder.AppendLine('	 */')
[void]$builder.AppendLine('	private static final String PACKED = ""')
foreach ($chunk in $chunks) {
    [void]$builder.AppendLine('		+ "' + $chunk + '"')
}
[void]$builder.AppendLine('		;')
[void]$builder.AppendLine('')
[void]$builder.AppendLine('	/** Decoded lazily on first collision lookup; one byte per id. */')
[void]$builder.AppendLine('	private static byte[] table;')
[void]$builder.AppendLine('')
[void]$builder.AppendLine('	private ObjectCollisionSizes()')
[void]$builder.AppendLine('	{')
[void]$builder.AppendLine('	}')
[void]$builder.AppendLine('')
[void]$builder.AppendLine('	private static synchronized byte[] table()')
[void]$builder.AppendLine('	{')
[void]$builder.AppendLine('		if (table != null)')
[void]$builder.AppendLine('			return table;')
[void]$builder.AppendLine('		byte[] decoded = new byte[MAX_ID + 1];')
[void]$builder.AppendLine('		int count = PACKED.length() / 2;')
[void]$builder.AppendLine('		for (int i = 0; i < count && i < decoded.length; i++)')
[void]$builder.AppendLine('		{')
[void]$builder.AppendLine('			int high = Character.digit(PACKED.charAt(i * 2), 16);')
[void]$builder.AppendLine('			int low = Character.digit(PACKED.charAt(i * 2 + 1), 16);')
[void]$builder.AppendLine('			decoded[i] = (byte) ((high << 4) | low);')
[void]$builder.AppendLine('		}')
[void]$builder.AppendLine('		table = decoded;')
[void]$builder.AppendLine('		return table;')
[void]$builder.AppendLine('	}')
[void]$builder.AppendLine('')
[void]$builder.AppendLine('	/** The packed byte for an id, or 0 when the table has no entry. */')
[void]$builder.AppendLine('	private static int packed(int objectId)')
[void]$builder.AppendLine('	{')
[void]$builder.AppendLine('		byte[] t = table();')
[void]$builder.AppendLine('		if (objectId < 0 || objectId >= t.length)')
[void]$builder.AppendLine('			return 0;')
[void]$builder.AppendLine('		return t[objectId] & 0xff;')
[void]$builder.AppendLine('	}')
[void]$builder.AppendLine('')
[void]$builder.AppendLine('	/** True when the table carries an explicit footprint for this object. */')
[void]$builder.AppendLine('	public static boolean has(int objectId)')
[void]$builder.AppendLine('	{')
[void]$builder.AppendLine('		return packed(objectId) != 0;')
[void]$builder.AppendLine('	}')
[void]$builder.AppendLine('')
[void]$builder.AppendLine('	/** Footprint width for the collision map, or the caller''s loc.dat size when absent. */')
[void]$builder.AppendLine('	public static int width(int objectId, int fallback)')
[void]$builder.AppendLine('	{')
[void]$builder.AppendLine('		int p = packed(objectId);')
[void]$builder.AppendLine('		return p == 0 ? fallback : (p >> 4);')
[void]$builder.AppendLine('	}')
[void]$builder.AppendLine('')
[void]$builder.AppendLine('	/** Footprint height for the collision map, or the caller''s loc.dat size when absent. */')
[void]$builder.AppendLine('	public static int height(int objectId, int fallback)')
[void]$builder.AppendLine('	{')
[void]$builder.AppendLine('		int p = packed(objectId);')
[void]$builder.AppendLine('		return p == 0 ? fallback : (p & 0xf);')
[void]$builder.AppendLine('	}')
[void]$builder.AppendLine('}')

[System.IO.File]::WriteAllText($outPath, $builder.ToString(), (New-Object System.Text.UTF8Encoding($false)))
Write-Host ("Wrote {0} ({1} ids, {2} with footprints, {3} KB of hex)" -f $outPath, ($maxId + 1), $sizes.Count, [Math]::Round($packed.Length / 1024))
