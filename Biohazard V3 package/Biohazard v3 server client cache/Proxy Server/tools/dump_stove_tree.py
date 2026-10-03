from pathlib import Path
import gzip
import sys
import struct
import zlib

sys.path.insert(0, str(Path(__file__).resolve().parent))
from build_deathly_map_index import (  # noqa: E402
    CACHE,
    DAT,
    IDX0,
    read_cache_file,
    unpack_archive,
    get_named_file,
)

IDX1 = CACHE / "main_file_cache.idx1"


def read_string(blob, p):
    start = p
    while p < len(blob) and blob[p] != 10:
        p += 1
    s = blob[start:p].decode("latin-1", errors="replace")
    return s, p + 1


def parse_loc(blob):
    p = 0
    info = {
        "models": [],
        "model_types": None,
        "name": None,
        "w": 1,
        "h": 1,
        "sx": 128,
        "sy": 128,
        "sz": 128,
        "recol": [],
        "actions": [],
        "light": 0,
        "shade": 0,
        "children": None,
        "ops": [],
    }
    while p < len(blob):
        op = blob[p]
        p += 1
        if op == 0:
            break
        elif op == 1:
            n = blob[p]
            p += 1
            models = []
            types = []
            for _ in range(n):
                models.append((blob[p] << 8) | blob[p + 1])
                types.append(blob[p + 2])
                p += 3
            info["models"] = models
            info["model_types"] = types
        elif op == 2:
            info["name"], p = read_string(blob, p)
        elif op == 3:
            _, p = read_string(blob, p)
        elif op == 5:
            n = blob[p]
            p += 1
            models = []
            for _ in range(n):
                models.append((blob[p] << 8) | blob[p + 1])
                p += 2
            info["models"] = models
        elif op == 14:
            info["w"] = blob[p]
            p += 1
        elif op == 15:
            info["h"] = blob[p]
            p += 1
        elif op in (17, 18, 21, 22, 23, 62, 64, 73, 74):
            pass
        elif op in (19, 28, 69, 75):
            p += 1
        elif op == 29:
            info["light"] = struct.unpack("b", bytes([blob[p]]))[0]
            p += 1
        elif op == 39:
            info["shade"] = struct.unpack("b", bytes([blob[p]]))[0]
            p += 1
        elif 30 <= op <= 38:
            s, p = read_string(blob, p)
            info["actions"].append(s)
        elif op == 40:
            n = blob[p]
            p += 1
            for _ in range(n):
                a = (blob[p] << 8) | blob[p + 1]
                b = (blob[p + 2] << 8) | blob[p + 3]
                info["recol"].append((a, b))
                p += 4
        elif op in (24, 60, 65, 66, 67, 68):
            val = (blob[p] << 8) | blob[p + 1]
            if op == 65:
                info["sx"] = val
            elif op == 66:
                info["sy"] = val
            elif op == 67:
                info["sz"] = val
            p += 2
        elif op in (70, 71, 72):
            p += 2
        elif op == 77:
            vb = (blob[p] << 8) | blob[p + 1]
            va = (blob[p + 2] << 8) | blob[p + 3]
            p += 4
            n = blob[p]
            p += 1
            kids = []
            for _ in range(n + 1):
                kids.append((blob[p] << 8) | blob[p + 1])
                p += 2
            info["children"] = (vb, va, kids)
        else:
            info["unhandled"] = op
            info["unhandled_at"] = p - 1
            break
        info["ops"].append(op)
    return info


def signed_smart(data, i):
    v = data[i]
    if v < 128:
        return v - 64, i + 1
    return ((v << 8) | data[i + 1]) - 49152, i + 2


def decode_new_vertices(data, nv, nt):
    footer = len(data) - 23
    num_tex = data[footer + 4]
    l1 = data[footer + 5]
    i2 = data[footer + 6]
    j2 = data[footer + 7]
    k2 = data[footer + 8]
    l2 = data[footer + 9]
    i3 = data[footer + 10]
    j3 = (data[footer + 11] << 8) | data[footer + 12]
    k3 = (data[footer + 13] << 8) | data[footer + 14]
    l3 = (data[footer + 15] << 8) | data[footer + 16]
    i4 = (data[footer + 17] << 8) | data[footer + 18]
    j4 = (data[footer + 19] << 8) | data[footer + 20]
    k5 = num_tex
    l5 = k5
    k5 += nv
    if l1 == 1:
        k5 += nt
    k5 += nt
    if i2 == 255:
        k5 += nt
    if k2 == 1:
        k5 += nt
    if i3 == 1:
        k5 += nv
    if j2 == 1:
        k5 += nt
    k5 += i4
    if l2 == 1:
        k5 += nt * 2
    k5 += j4
    k5 += nt * 2
    k8 = k5
    k5 += j3
    l8 = k5
    k5 += k3
    i9 = k5
    xmin = ymin = zmin = 10 ** 9
    xmax = ymax = zmax = -10 ** 9
    ox, oy, oz = k8, l8, i9
    x = y = z = 0
    p = l5
    for _ in range(nv):
        flags = data[p]
        p += 1
        if flags & 1:
            dx, ox = signed_smart(data, ox)
            x += dx
        if flags & 2:
            dy, oy = signed_smart(data, oy)
            y += dy
        if flags & 4:
            dz, oz = signed_smart(data, oz)
            z += dz
        xmin = min(xmin, x)
        xmax = max(xmax, x)
        ymin = min(ymin, y)
        ymax = max(ymax, y)
        zmin = min(zmin, z)
        zmax = max(zmax, z)
    return {
        "new": True,
        "nv": nv,
        "nt": nt,
        "dx": xmax - xmin,
        "dy": ymax - ymin,
        "dz": zmax - zmin,
        "xmin": xmin,
        "xmax": xmax,
        "ymin": ymin,
        "ymax": ymax,
        "zmin": zmin,
        "zmax": zmax,
    }


def maybe_decompress(raw):
    if raw[:2] == b"\x1f\x8b":
        return gzip.decompress(raw)
    if raw[:3] == b"BZh":
        import bz2
        return bz2.decompress(raw)
    try:
        return zlib.decompress(raw)
    except Exception:
        return raw


def decode_old_vertices(data):
    # 317 old format footer
    if len(data) < 18:
        return None
    new_fmt = data[-1] == 0xFF and data[-2] == 0xFF
    footer = len(data) - (23 if new_fmt else 18)
    nv = (data[footer] << 8) | data[footer + 1]
    nt = (data[footer + 2] << 8) | data[footer + 3]
    if new_fmt:
        return decode_new_vertices(data, nv, nt)
    p = 0
    flags = data[p:p + nv]
    p += nv
    # skip triangle types
    # we don't have exact layout without full header parse; decode xyz from known 317:
    # after flags: x data, y data, z data using signed smart based on flag bits
    def signed_smart(buf, i):
        v = buf[i]
        if v < 128:
            return v - 64, i + 1
        return ((v << 8) | buf[i + 1]) - 49152, i + 2

    xs = []
    ys = []
    zs = []
    x = y = z = 0
    xp = p
    # y and z offsets need header. Parse footer more completely like method460.
    stream_off = footer
    num_tex = data[stream_off + 4]
    k = data[stream_off + 5]
    l = data[stream_off + 6]
    i1 = data[stream_off + 7]
    j1 = data[stream_off + 8]
    k1 = data[stream_off + 9]
    l1 = (data[stream_off + 10] << 8) | data[stream_off + 11]
    i2 = (data[stream_off + 12] << 8) | data[stream_off + 13]
    j2 = (data[stream_off + 14] << 8) | data[stream_off + 15]
    k2 = (data[stream_off + 16] << 8) | data[stream_off + 17]
    l2 = 0
    vert_flags = l2
    l2 += nv
    tri_type = l2
    l2 += nt
    pri = l2
    if l == 255:
        l2 += nt
    alpha = l2
    if i1 == 1:
        l2 += nt
    tpri = l2
    if k == 1:
        l2 += nt
    bones = l2
    if k1 == 1:
        l2 += nv
    tinfo = l2
    l2 += nt
    faces = l2
    l2 += k2
    colors = l2
    l2 += nt * 2
    tex = l2
    l2 += l1
    vx = l2
    l2 += i2
    vy = l2
    l2 += j2
    vz = l2

    def read_smart_at(off_ref, signed=True):
        i = off_ref[0]
        v = data[i]
        if v < 128:
            off_ref[0] = i + 1
            return v - 64 if signed else v
        val = (v << 8) | data[i + 1]
        off_ref[0] = i + 2
        return val - 49152 if signed else val - 32768

    ox = [vx]
    oy = [vy]
    oz = [vz]
    x = y = z = 0
    xmin = ymin = zmin = 10 ** 9
    xmax = ymax = zmax = -10 ** 9
    for i in range(nv):
        f = data[vert_flags + i]
        if f & 1:
            x += read_smart_at(ox)
        if f & 2:
            y += read_smart_at(oy)
        if f & 4:
            z += read_smart_at(oz)
        xmin = min(xmin, x)
        xmax = max(xmax, x)
        ymin = min(ymin, y)
        ymax = max(ymax, y)
        zmin = min(zmin, z)
        zmax = max(zmax, z)
    return {
        "new": False,
        "nv": nv,
        "nt": nt,
        "dx": xmax - xmin,
        "dy": ymax - ymin,
        "dz": zmax - zmin,
        "xmin": xmin,
        "xmax": xmax,
        "ymin": ymin,
        "ymax": ymax,
        "zmin": zmin,
        "zmax": zmax,
    }


def main():
    print("cache", CACHE)
    dat = DAT.read_bytes()
    idx0 = IDX0.read_bytes()
    config = read_cache_file(dat, idx0, 2, 1)
    data, hashes, dsz, csz, offs = unpack_archive(config)
    loc_dat = get_named_file(data, hashes, dsz, csz, offs, "loc.dat")
    loc_idx = get_named_file(data, hashes, dsz, csz, offs, "loc.idx")
    count = (loc_idx[0] << 8) | loc_idx[1]
    print("loc count", count, "loc.dat", len(loc_dat))

    ids = [114, 1276, 1278, 1315, 1316, 1318, 1319, 2728, 4172, 9085, 9086, 9087, 12264, 12269]
    offset = 2
    blobs = {}
    for i in range(count):
        size = (loc_idx[2 + i * 2] << 8) | loc_idx[3 + i * 2]
        blob = loc_dat[offset:offset + size]
        if i in ids or (114 <= i <= 120) or (12260 <= i <= 12275):
            blobs[i] = blob
        offset += size

    idx1 = IDX1.read_bytes()
    model_ids = set()
    for oid in ids:
        blob = blobs.get(oid)
        if not blob:
            print("missing loc", oid)
            continue
        info = parse_loc(blob)
        print("OBJ", oid, info)
        model_ids.update(info["models"])

    print("\nMODELS")
    for mid in sorted(model_ids):
        raw = read_cache_file(dat, idx1, mid, 2)
        if raw is None:
            print(" missing model", mid)
            continue
        payload = maybe_decompress(raw)
        try:
            bounds = decode_old_vertices(payload)
        except Exception as e:
            bounds = str(e)
        print(" model", mid, "raw", len(raw), "dec", len(payload), "head", payload[:8].hex(), "tail", payload[-8:].hex(), bounds)


if __name__ == "__main__":
    main()
