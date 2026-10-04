"""Force-sync Soul-Trail Maps/*.dat into Proxy Server Data/world/map/*.gz.

build_deathly_map_index previously skipped .dat when a stale .gz already
existed, leaving old Grand Exchange landscape/objects on the server clip
matrix while the client showed the Deathly/open map.
"""
from pathlib import Path
import gzip
import sys

ROOT = Path(__file__).resolve().parents[1]
MAP_DST = ROOT / "Data" / "world" / "map"
MAPS_SRC = Path.home() / "Soul-Trail" / "Maps"


def main():
    if not MAPS_SRC.is_dir():
        print("missing", MAPS_SRC)
        return 1
    MAP_DST.mkdir(parents=True, exist_ok=True)
    written = 0
    for dat in sorted(MAPS_SRC.glob("*.dat")):
        gz = MAP_DST / (dat.stem + ".gz")
        payload = dat.read_bytes()
        with gzip.open(gz, "wb", compresslevel=6) as out:
            out.write(payload)
        written += 1
        if written % 500 == 0:
            print("wrote", written, "...")
    print("synced", written, "maps from", MAPS_SRC, "->", MAP_DST)
    # quick GE sanity
    import hashlib
    for i in (266, 267):
        sg = gzip.decompress((MAP_DST / ("%s.gz" % i)).read_bytes())
        sd = (MAPS_SRC / ("%s.dat" % i)).read_bytes()
        print("verify", i, "match" if sg == sd else "MISMATCH", "len", len(sg))
    return 0


if __name__ == "__main__":
    sys.exit(main())
