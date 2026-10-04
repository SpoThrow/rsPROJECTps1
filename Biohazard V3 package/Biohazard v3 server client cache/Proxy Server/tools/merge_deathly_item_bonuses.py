import os

st_path = r"C:\Users\llrbi\Documents\GitHub\rsPROJECTps\Biohazard V3 package\Biohazard v3 server client cache\Proxy Server\Data\cfg\item.cfg"
dy_path = r"C:\Users\llrbi\Desktop\Deathly Source\Data\cfg\item.cfg"
out_path = st_path

def parse_items(path):
    header = []
    footer = []
    items = {}
    order = []
    in_items = False
    with open(path, "r") as f:
        for raw in f:
            line = raw.rstrip("\n")
            stripped = line.strip()
            if stripped.startswith("item ="):
                in_items = True
                parts = stripped.split("=", 1)[1].strip().split("\t")
                try:
                    item_id = int(parts[0])
                except (ValueError, IndexError):
                    continue
                items[item_id] = parts
                order.append(item_id)
            else:
                if not in_items:
                    header.append(line)
                else:
                    footer.append(line)
    return header, items, order, footer

st_header, st_items, st_order, st_footer = parse_items(st_path)
_, dy_items, dy_order, _ = parse_items(dy_path)

merged_order = list(st_order)
seen = set(st_order)
appended = 0
updated = 0
for item_id, parts in dy_items.items():
    if item_id in st_items:
        st_parts = st_items[item_id]
        if len(parts) >= 18 and len(st_parts) >= 6:
            bonuses = parts[6:18]
            while len(st_parts) < 6:
                st_parts.append("0")
            st_items[item_id] = st_parts[:6] + bonuses
            updated += 1
    else:
        st_items[item_id] = parts
        merged_order.append(item_id)
        seen.add(item_id)
        appended += 1

def normalize_parts(parts):
    while len(parts) > 3 and parts[2] == "" and not _is_int(parts[3]):
        parts.pop(2)
    for i in range(len(parts)):
        if parts[i] == "":
            parts[i] = "0" if i >= 3 else "-"
    while len(parts) < 18:
        parts.append("0")
    return parts[:18]

def _is_int(value):
    try:
        int(value)
        return True
    except (TypeError, ValueError):
        return False

max_id = max(st_items.keys()) if st_items else 0
with open(out_path, "w") as f:
    for line in st_header:
        f.write(line + "\n")
    for item_id in merged_order:
        parts = normalize_parts(list(st_items[item_id]))
        f.write("item = " + "\t".join(parts) + "\n")
    for line in st_footer:
        f.write(line + "\n")

print("updated %d existing, appended %d new, max_id %d, total %d" % (updated, appended, max_id, len(st_items)))
