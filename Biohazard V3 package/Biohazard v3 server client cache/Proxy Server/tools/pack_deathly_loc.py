from pathlib import Path
import gzip
import shutil
import struct
import sys

sys.path.insert(0, str(Path(__file__).resolve().parent))
from build_deathly_map_index import (  # noqa: E402
    CACHE,
    DAT,
    IDX0,
    MAPS_SRC,
    MAP_DST,
    get_named_file,
    read_cache_file,
    unpack_archive,
)

OBJ_DIR = Path(__file__).resolve().parents[1] / "Data" / "world" / "object"
LOC_DAT = OBJ_DIR / "loc.dat"
LOC_IDX = OBJ_DIR / "loc.idx"


def pack_memory_archive(classic_dat, classic_idx):
    count = (classic_idx[0] << 8) | classic_idx[1]
    offset = 2
    packed = bytearray()
    index = bytearray()
    file_off = 0
    for i in range(count):
        size = (classic_idx[2 + i * 2] << 8) | classic_idx[3 + i * 2]
        blob = classic_dat[offset:offset + size]
        if len(blob) != size:
            raise RuntimeError("loc.dat truncated at object %s" % i)
        index += struct.pack(">Q", file_off)
        index += struct.pack(">i", size)
        packed += blob
        file_off += size
        offset += size
    return bytes(packed), bytes(index), count


def parse_classic_loc(classic_dat, classic_idx):
    """Sanity-check Deathly loc opcodes/strings before packing."""
    count = (classic_idx[0] << 8) | classic_idx[1]
    offset = 2
    walk_through = 0
    named = 0
    for i in range(count):
        size = (classic_idx[2 + i * 2] << 8) | classic_idx[3 + i * 2]
        blob = classic_dat[offset:offset + size]
        p = 0
        solid = True
        while p < len(blob):
            op = blob[p]
            p += 1
            if op == 0:
                break
            elif op == 1:
                n = blob[p]
                p += 1 + n * 3
            elif op == 2 or op == 3 or (30 <= op <= 38):
                while p < len(blob) and blob[p] != 10:
                    p += 1
                p += 1
                if op == 2:
                    named += 1
            elif op == 5:
                n = blob[p]
                p += 1 + n * 2
            elif op in (14, 15, 19, 28, 29, 39, 69, 75):
                p += 1
            elif op in (17,):
                solid = False
            elif op in (18, 21, 22, 23, 62, 64, 73, 74):
                pass
            elif op in (24, 60, 65, 66, 67, 68, 70, 71, 72):
                p += 2
            elif op == 40:
                n = blob[p]
                p += 1 + n * 4
            elif op == 77:
                p += 4
                n = blob[p]
                p += 1 + (n + 1) * 2
            else:
                raise RuntimeError("unhandled loc opcode %s on object %s" % (op, i))
        if p != len(blob) and not (p == len(blob) and blob[-1] == 0):
            # leftover bytes after opcode 0 are unused padding
            pass
        if not solid:
            walk_through += 1
        offset += size
    return count, named, walk_through


def overlay_dat_maps():
    if not MAPS_SRC.exists():
        print("no Maps dir at", MAPS_SRC)
        return 0
    MAP_DST.mkdir(parents=True, exist_ok=True)
    written = 0
    for dat_path in sorted(MAPS_SRC.glob("*.dat")):
        raw = dat_path.read_bytes()
        if len(raw) < 10:
            continue
        if raw[:2] == b"\x1f\x8b":
            payload = raw
        else:
            payload = gzip.compress(raw)
        dest = MAP_DST / (dat_path.stem + ".gz")
        dest.write_bytes(payload)
        written += 1
    return written


def main():
    dat = DAT.read_bytes()
    idx0 = IDX0.read_bytes()
    config = read_cache_file(dat, idx0, 2, 1)
    if config is None:
        raise RuntimeError("cache idx0 file 2 (config) missing")
    data, hashes, dsz, csz, offs = unpack_archive(config)
    loc_dat = get_named_file(data, hashes, dsz, csz, offs, "loc.dat")
    loc_idx = get_named_file(data, hashes, dsz, csz, offs, "loc.idx")
    packed_dat, packed_idx, packed_count = pack_memory_archive(loc_dat, loc_idx)
    OBJ_DIR.mkdir(parents=True, exist_ok=True)
    if LOC_DAT.exists() and not (OBJ_DIR / "loc.dat.pre-deathly-clip").exists():
        shutil.copy2(LOC_DAT, OBJ_DIR / "loc.dat.pre-deathly-clip")
        shutil.copy2(LOC_IDX, OBJ_DIR / "loc.idx.pre-deathly-clip")
    LOC_DAT.write_bytes(packed_dat)
    LOC_IDX.write_bytes(packed_idx)
    overlays = overlay_dat_maps()
    print("packed loc objects:", packed_count)
    print("classic loc.dat/idx:", len(loc_dat), len(loc_idx))
    print("loc.dat bytes:", len(packed_dat), "loc.idx bytes:", len(packed_idx))
    print("map .dat overlays gzipped:", overlays)
    print("cache:", CACHE)


if __name__ == "__main__":
    main()
