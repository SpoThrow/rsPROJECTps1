from pathlib import Path
import gzip
import struct
import bz2
import shutil
import re

CACHE = Path.home() / "Soul-Trail"
DAT = CACHE / "main_file_cache.dat"
IDX0 = CACHE / "main_file_cache.idx0"
MAP_CONFIG = CACHE / "mapConfig.txt"
MAPS_SRC = CACHE / "Maps"
WORLD = Path(__file__).resolve().parents[1] / "Data" / "world"
MAP_DST = WORLD / "map"
MAP_INDEX_DST = WORLD / "map_index"


def u3(b, o=0):
    return (b[o] << 16) | (b[o + 1] << 8) | b[o + 2]


def u2(b, o=0):
    return (b[o] << 8) | b[o + 1]


def read_cache_file(dat, idx, file_id, index_id):
    off = file_id * 6
    if off + 6 > len(idx):
        return None
    size = u3(idx, off)
    sector = u3(idx, off + 3)
    if size <= 0 or sector <= 0:
        return None
    out = bytearray()
    chunk = 0
    while len(out) < size:
        pos = sector * 520
        block = dat[pos:pos + 520]
        if len(block) < 8:
            raise RuntimeError("short sector %s" % sector)
        fid = u2(block, 0)
        ch = u2(block, 2)
        nxt = u3(block, 4)
        iid = block[7]
        if fid != file_id or ch != chunk or iid != index_id:
            raise RuntimeError(
                "header mismatch fid=%s/%s ch=%s/%s iid=%s/%s"
                % (fid, file_id, ch, chunk, iid, index_id)
            )
        take = min(512, size - len(out))
        out.extend(block[8:8 + take])
        sector = nxt
        chunk += 1
    return bytes(out)


def jagex_hash(s):
    i = 0
    for ch in s.upper():
        i = (i * 61 + ord(ch) - 32)
        i = ((i + 2 ** 31) % 2 ** 32) - 2 ** 31
    return i


def unpack_archive(raw):
    dec_size = u3(raw, 0)
    comp_size = u3(raw, 3)
    if comp_size == 0:
        data = gzip.decompress(raw[6:])
        table_at = 0
    elif comp_size != dec_size:
        data = bz2.decompress(b"BZh1" + raw[6:])
        table_at = 0
    else:
        data = raw
        table_at = 6
    count = u2(data, table_at)
    hashes = []
    dsz = []
    csz = []
    offs = []
    p = table_at + 2
    k = p + count * 10
    for n in range(count):
        h = struct.unpack(">i", data[p:p + 4])[0]
        ds = u3(data, p + 4)
        cs = u3(data, p + 7)
        hashes.append(h)
        dsz.append(ds)
        csz.append(cs)
        offs.append(k)
        k += cs
        p += 10
    return data, hashes, dsz, csz, offs


def decompress_entry(blob, dsz, csz):
    if csz == dsz:
        return blob
    try:
        return bz2.decompress(b"BZh1" + blob)
    except Exception:
        return gzip.decompress(blob)


def get_named_file(data, hashes, dsz, csz, offs, name):
    wanted = jagex_hash(name)
    for i, h in enumerate(hashes):
        if h == wanted:
            blob = data[offs[i]:offs[i] + csz[i]]
            return decompress_entry(blob, dsz[i], csz[i])
    raise RuntimeError("missing " + name)


def parse_remaps(path):
    remaps = {}
    pattern = re.compile(r"position=(\d+)\((\d+)\)\[(\d+)\]")
    for line in path.read_text(encoding="ascii", errors="ignore").splitlines():
        m = pattern.search(line.strip())
        if m:
            remaps[int(m.group(1))] = (int(m.group(2)), int(m.group(3)))
    return remaps


def copy_maps():
    MAP_DST.mkdir(parents=True, exist_ok=True)
    copied = 0
    for src in MAPS_SRC.glob("*.gz"):
        shutil.copy2(src, MAP_DST / src.name)
        copied += 1
    return copied


def gzip_dat_overrides(needed_ids):
    made = 0
    for file_id in sorted(needed_ids):
        gz = MAP_DST / ("%s.gz" % file_id)
        dat = MAPS_SRC / ("%s.dat" % file_id)
        if gz.exists() or not dat.exists():
            continue
        with gzip.open(gz, "wb") as out:
            out.write(dat.read_bytes())
        made += 1
    return made


def main():
    dat = DAT.read_bytes()
    idx0 = IDX0.read_bytes()
    raw = read_cache_file(dat, idx0, 5, 1)
    data, hashes, dsz, csz, offs = unpack_archive(raw)
    names = [
        "map_index", "midi_index", "model_index", "anim_index",
        "midi_crc", "model_crc", "anim_crc", "map_crc",
        "midi_version", "model_version", "anim_version", "map_version",
    ]
    lookup = {jagex_hash(n): n for n in names}
    print("versionlist files:")
    for i, h in enumerate(hashes):
        print(" ", i, lookup.get(h, h), "dsz", dsz[i], "csz", csz[i])
    map_index = get_named_file(data, hashes, dsz, csz, offs, "map_index")
    print("map_index bytes", len(map_index), "mod6", len(map_index) % 6, "mod7", len(map_index) % 7)
    stride = 7 if len(map_index) % 7 == 0 else 6
    remaps = parse_remaps(MAP_CONFIG)
    print("remaps", len(remaps), "stride", stride)
    out = bytearray()
    needed = set()
    remapped = 0
    present = set()
    for i in range(0, len(map_index), stride):
        region = u2(map_index, i)
        land = u2(map_index, i + 2)
        obj = u2(map_index, i + 4)
        if region in remaps:
            land, obj = remaps[region]
            remapped += 1
        present.add(region)
        out.extend(struct.pack(">HHH", region, land, obj))
        needed.add(land)
        needed.add(obj)
    appended = 0
    for region, (land, obj) in remaps.items():
        if region in present:
            continue
        out.extend(struct.pack(">HHH", region, land, obj))
        needed.add(land)
        needed.add(obj)
        appended += 1
    if MAP_INDEX_DST.exists() and not (WORLD / "map_index.pre-deathly").exists():
        shutil.copy2(MAP_INDEX_DST, WORLD / "map_index.pre-deathly")
    MAP_INDEX_DST.write_bytes(bytes(out))
    copied = copy_maps()
    made = gzip_dat_overrides(needed)
    missing = [i for i in sorted(needed) if not (MAP_DST / ("%s.gz" % i)).exists()]
    print("wrote", MAP_INDEX_DST, "entries", len(out) // 6, "remapped", remapped, "appended", appended)
    print("copied gz", copied, "gzipped dat overrides", made, "missing ids", len(missing))
    if missing[:20]:
        print("missing sample", missing[:20])


if __name__ == "__main__":
    main()
