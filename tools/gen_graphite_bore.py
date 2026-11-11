#!/usr/bin/env python3
"""The graphite block with its bore - the foundation of the RBMK lattice.

An RBMK graphite block is not solid: it is a 250 mm brick pierced by a 114 mm
vertical hole, with a lip at the top corner where the blocks interlock. The
pressure tube (the fuel channel) passes THROUGH that hole; the graphite does not
disappear where a channel is. This generator writes the three models that make
that real at 1:4 (1 block = 1 graphite block):

  graphite       - the block with its bore and top lip
  fuel_channel   - the same block, with the pressure tube + fuel in the bore
  control_rod    - the same block, with the control rod in the bore

The bore is a genuine hole: the block is modelled as a frame of walls around an
open shaft, so looking down the core you see the bores, and the channel in them.
"""
import json
import os

ROOT = "src/main/resources/assets/csp-modern"
BORE0, BORE1 = 4, 12          # the 114 mm hole -> 8/16 of the block (see note)
TUBE0, TUBE1 = 5, 11          # the pressure tube sitting in the hole
CORE0, CORE1 = 6, 10          # the fuel / rod inside the tube


def face(tex, cull=None):
    f = {"texture": tex}
    if cull:
        f["cullface"] = cull
    return f


def cuboid(x0, y0, z0, x1, y1, z1, tex, cull=None):
    """A cuboid; `cull` is a dict dir->True for faces that sit on a block face."""
    cull = cull or {}
    faces = {
        "down": face(tex, "down" if cull.get("down") else None),
        "up": face(tex, "up" if cull.get("up") else None),
        "north": face(tex, "north" if cull.get("north") else None),
        "south": face(tex, "south" if cull.get("south") else None),
        "east": face(tex, "east" if cull.get("east") else None),
        "west": face(tex, "west" if cull.get("west") else None),
    }
    for k in list(faces):
        if faces[k].get("cullface") is None:
            faces[k].pop("cullface", None)
    return {"from": [x0, y0, z0], "to": [x1, y1, z1], "faces": faces}


def graphite_frame(tex="#graphite"):
    """The four walls around the bore, plus the lip where the blocks interlock.

    Everything stays inside the valid 0..16 model space: the body is 15 px tall
    and the lip is a rim raised on top and inset 1 px from the block edge, so the
    step that couples one block to the next is real and visible.
    """
    return [
        # the four walls of the bore (body)
        cuboid(0, 0, 0, BORE0, 15, 16, tex, {"west": True}),
        cuboid(BORE1, 0, 0, 16, 15, 16, tex, {"east": True}),
        cuboid(BORE0, 0, 0, BORE1, 15, BORE0, tex, {"north": True}),
        cuboid(BORE0, 0, BORE1, BORE1, 15, 16, tex, {"south": True}),
        # the lip: a rim on top, inset 1 px from the edge (the interlock step)
        cuboid(1, 15, 1, BORE0, 16, 15, tex),
        cuboid(BORE1, 15, 1, 15, 16, 15, tex),
        cuboid(BORE0, 15, 1, BORE1, 16, BORE0, tex),
        cuboid(BORE0, 15, BORE1, BORE1, 16, 15, tex),
    ]


def model(textures, elements):
    tex = {"particle": textures.get("graphite", list(textures.values())[0])}
    tex.update(textures)
    return {"credit": "RBMK-1000 graphite block with bore", "textures": tex,
            "elements": elements}


def write(name, data):
    path = f"{ROOT}/models/block/{name}.json"
    with open(path, "w") as f:
        json.dump(data, f, indent=2)
        f.write("\n")
    print("wrote", path)


def main():
    # 1. the graphite block: bore + lip, nothing in the hole
    write("graphite", model({"graphite": "csp-modern:block/graphite"},
                            graphite_frame()))

    # 2. the fuel channel: the same block, with the pressure tube and fuel in the
    #    bore - the graphite is still there around it
    els = graphite_frame()
    els.append(cuboid(TUBE0, 0, TUBE0, TUBE1, 16, TUBE1, "#channel"))
    write("fuel_channel", model({"graphite": "csp-modern:block/graphite",
                                 "channel": "csp-modern:block/fuel_channel"}, els))

    # 3. the control-rod channel: the same block, with the rod in the bore
    els = graphite_frame()
    els.append(cuboid(TUBE0, 0, TUBE0, TUBE1, 16, TUBE1, "#rod"))
    write("control_rod", model({"graphite": "csp-modern:block/graphite",
                                "rod": "csp-modern:block/control_rod"}, els))


if __name__ == "__main__":
    os.makedirs(f"{ROOT}/models/block", exist_ok=True)
    main()
