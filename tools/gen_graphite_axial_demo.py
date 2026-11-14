#!/usr/bin/env python3
"""A single demonstration block showing the REAL axial block lengths of the
RBMK graphite, side by side, so the size variation is visible on screen.

An RBMK graphite block is 250 x 250 mm in cross-section and 200-600 mm long
along the channel, depending on its position in the stack (WP #36). At 1:4 (one
block = 250 mm) a 250 mm block fills exactly one cell, and the longer blocks
(300-600 mm) would spill past a single cell - so the variation cannot be shown
with whole 1:4 blocks. The bit scale resolves it: at 1/16 block, one unit is
62.5 mm, so the 250 mm cross-section is 4 units and every axial length
(200-600 mm) fits inside one block's 16-unit model space.

This generator writes one block model holding six graphite columns, one per
real axial length, all on that bit scale. It is a measurement aid, not plant.
"""
import json
import os

ROOT = "src/main/resources/assets/csp-modern"
UNIT_MM = 62.5                       # 1/16 block
CROSS = round(250 / UNIT_MM)         # 4 units = 250 mm cross-section
LENGTHS = [200, 250, 300, 400, 500, 600]
GRID = [(0, 0), (5, 0), (10, 0), (0, 5), (5, 5), (10, 5)]   # 3 x 2, gap 1


def faces(tex):
    return {d: {"texture": tex} for d in
            ("down", "up", "north", "south", "east", "west")}


def box(x0, y0, z0, x1, y1, z1):
    return {"from": [x0, y0, z0], "to": [x1, y1, z1], "faces": faces("#graphite")}


def build():
    els = []
    for (gx, gz), mm in zip(GRID, LENGTHS):
        h = round(mm / UNIT_MM)
        els.append(box(gx, 0, gz, gx + CROSS, h, gz + CROSS))
    model = {
        "credit": "RBMK graphite axial-length demo (bit scale, 1 unit = 62.5 mm)",
        "textures": {
            "particle": "csp-modern:block/graphite",
            "graphite": "csp-modern:block/graphite",
        },
        "elements": els,
    }
    return model


def main():
    model = build()
    with open(f"{ROOT}/models/block/graphite_axial_demo.json", "w") as f:
        json.dump(model, f, indent=2)
    with open(f"{ROOT}/blockstates/graphite_axial_demo.json", "w") as f:
        json.dump({"variants": {"": {"model": "csp-modern:block/graphite_axial_demo"}}}, f, indent=2)
    with open(f"{ROOT}/models/item/graphite_axial_demo.json", "w") as f:
        json.dump({"parent": "csp-modern:block/graphite_axial_demo"}, f, indent=2)
    print("wrote graphite_axial_demo (lengths mm:", LENGTHS, ")")


if __name__ == "__main__":
    main()
