"""Replace Soul-Trail idx0 title archive with the original 317 title from the pre-Deathly backup."""
from pathlib import Path

SOUL = Path.home() / "Soul-Trail"
BACKUP = Path.home() / "Biohazard.474.pre-deathly"
FILE_ID = 1
INDEX_ID = 1


def u3(data, offset=0):
    return (data[offset] << 16) | (data[offset + 1] << 8) | data[offset + 2]


def put_u3(value):
    return bytes([(value >> 16) & 0xFF, (value >> 8) & 0xFF, value & 0xFF])


def read_cache_file(dat, idx, file_id):
    offset = file_id * 6
    size = u3(idx, offset)
    sector = u3(idx, offset + 3)
    if size == 0 or sector == 0:
        raise RuntimeError("missing cache file %s" % file_id)
    out = bytearray()
    chunk = 0
    while len(out) < size:
        pos = sector * 520
        block = dat[pos:pos + 520]
        take = min(512, size - len(out))
        out.extend(block[8:8 + take])
        sector = u3(block, 4)
        chunk += 1
    return bytes(out)


def write_cache_file(dat_path, idx_path, file_id, index_id, payload):
    dat = bytearray(Path(dat_path).read_bytes())
    idx = bytearray(Path(idx_path).read_bytes())
    pad = (520 - (len(dat) % 520)) % 520
    if pad:
        dat.extend(b"\x00" * pad)
    sector = len(dat) // 520
    if sector < 1:
        sector = 1
        dat.extend(b"\x00" * 520)
    first = sector
    offset = 0
    chunk = 0
    size = len(payload)
    while offset < size:
        take = min(512, size - offset)
        nxt = sector + 1 if offset + take < size else 0
        block = bytearray(520)
        block[0] = (file_id >> 8) & 0xFF
        block[1] = file_id & 0xFF
        block[2] = (chunk >> 8) & 0xFF
        block[3] = chunk & 0xFF
        block[4:7] = put_u3(nxt)
        block[7] = index_id
        block[8:8 + take] = payload[offset:offset + take]
        dat.extend(block)
        sector += 1
        chunk += 1
        offset += take
    ioff = file_id * 6
    idx[ioff:ioff + 3] = put_u3(size)
    idx[ioff + 3:ioff + 6] = put_u3(first)
    Path(dat_path).write_bytes(dat)
    Path(idx_path).write_bytes(idx)
    return first, size


def main():
    src_dat = BACKUP / "main_file_cache.dat"
    src_idx = BACKUP / "main_file_cache.idx0"
    dst_dat = SOUL / "main_file_cache.dat"
    dst_idx = SOUL / "main_file_cache.idx0"
    if not src_dat.exists() or not src_idx.exists():
        raise SystemExit("pre-Deathly backup title cache not found")
    title = read_cache_file(src_dat.read_bytes(), src_idx.read_bytes(), FILE_ID)
    print("backup title bytes", len(title))
    first, size = write_cache_file(dst_dat, dst_idx, FILE_ID, INDEX_ID, title)
    check = read_cache_file(dst_dat.read_bytes(), dst_idx.read_bytes(), FILE_ID)
    print("wrote sector", first, "size", size, "reread", len(check), "match", check == title)


if __name__ == "__main__":
    main()
