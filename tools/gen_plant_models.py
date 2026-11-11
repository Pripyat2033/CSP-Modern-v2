#!/usr/bin/env python3
"""Write blockstate + block model + item model JSON for the plant equipment."""
import json
import os

ROOT = "src/main/resources/assets/csp-modern"
NAMES = ["steam_separator", "turbine", "condenser", "mcp", "feedwater",
         "eccs_accumulator", "plant_pipe", "control_rod",
         "ubs", "lbs", "vessel", "water_tank"]

os.makedirs(f"{ROOT}/blockstates", exist_ok=True)
os.makedirs(f"{ROOT}/models/block", exist_ok=True)
os.makedirs(f"{ROOT}/models/item", exist_ok=True)

for n in NAMES:
    blockstate = {"variants": {"": {"model": f"csp-modern:block/{n}"}}}
    model = {"parent": "minecraft:block/cube_all",
             "textures": {"all": f"csp-modern:block/{n}"}}
    item = {"parent": f"csp-modern:block/{n}"}
    for path, data in [(f"{ROOT}/blockstates/{n}.json", blockstate),
                       (f"{ROOT}/models/block/{n}.json", model),
                       (f"{ROOT}/models/item/{n}.json", item)]:
        with open(path, "w") as f:
            json.dump(data, f, indent=2)
            f.write("\n")
    print("wrote assets for", n)
