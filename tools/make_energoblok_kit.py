#!/usr/bin/env python3
"""Assemble the disposable client test kit: one Energoblok at 1:4, running live.

Takes an already-built world (see RUN below) and the mod jar, and produces a zip
with the world, both mod jars and a README, so Ben can drop it into a Fabric
1.20.1 client and report the client framerate - the one number the sandbox cannot
measure.

To rebuild the WORLD itself (not just the zip), on a server with the mod loaded:

    /gamerule doDaylightCycle false
    /time set noon
    /execute positioned 0 1 0 run rbmk plant build     # metered, no tick hitch
    /rbmk plant start
    /fill -8 69 138 8 69 152 minecraft:glass           # a viewing platform
    /setworldspawn 0 70 145
    /save-all flush

then stop the server and run this script.
"""
import os
import shutil
import sys
import zipfile

MOD_JAR = "build/libs/csp-modern-0.0.1.jar"
FABRIC_JAR_NAME = "fabric-api-0.92.12+1.20.1.jar"
OUT_DIR = "/workspace/serve_energoblok"
ZIP_NAME = "Energoblok-Test-Kit.zip"

README = r"""# Energoblok — corrected core, with the bores. LOOK, don't just run.

Ben — this is the corrected core you told me to fix. Three things changed, and
you can see all three. You spawn on a glass platform; **the specimen is right in
front of you**, and the core is beyond it.

## What to look at first (the specimen, ~15 blocks ahead)

Directly ahead of the platform is a 7x7 patch of graphite and, in a row:

    graphite      - the block WITH ITS BORE: look straight down into it. You
                    should see a real 114 mm hole and the lip on top, not a
                    solid cube. This is the thing you said was missing.
    fuel_channel  - the same graphite block, with the pressure tube and fuel
                    sitting IN the bore. Graphite all around it.
    control_rod   - the same graphite block, with the control rod in the bore.

If the bore reads as a solid block, tell me — that is the whole point of this
build and I need to know if it does not come across.

## What to look at second (the core, ~36 blocks north)

The core is no longer a bare lump of graphite. It is the graphite stack INSIDE
its sections:
  - the upper biological shield (Схема Е) - the big disc over the top
  - the lower biological shield - the disc under it
  - the vessel (кожух) - the steel ring around the stack
  - the side water tank - the wide ring of water (the gamma shield)

## What to look at third (the control rods)

Climb up and look down at the top. The control channels (СУЗ) are **spread
through the whole core**, reaching the centre — not a ring on the outside. The
nearest rod to the centre is at the centre. Every channel is within about 3
blocks of a rod.

## What is NOT done yet (so you don't have to find it)

- The turbines and separators are still single blocks with textures. You said
  they must be multiblocks with a controller. Not done yet — you told me the
  bore block first, so this is next.
- The exact control-rod cartogram (which cells are AZ / РР / АР / УСП) is still
  a regular pattern. The distribution is right; the exact layout needs a
  primary drawing.

## What to tell me

1. Does the bore read as a real hole? Does the channel read as a tube in it?
2. Do the sections read as the sections (shield discs, vessel, water tank)?
3. Does the control distribution look right to you now?
4. fps (F3) — still the one number I cannot get.

## Install
- Minecraft Java 1.20.1 + Fabric Loader 1.20.1
- both jars from mods/ into %appdata%\.minecraft\mods\
- the world/ folder into %appdata%\.minecraft\saves\Energoblok\
- open the world, you spawn on the platform looking at the specimen

- built by an AI agent (OpenHands) on Ben's behalf.
"""


def main():
    repo = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    world_src = sys.argv[1] if len(sys.argv) > 1 else "/workspace/mcserver/Energoblok-Test"
    fabric_src = sys.argv[2] if len(sys.argv) > 2 else \
        "/workspace/mcserver/mods/" + FABRIC_JAR_NAME
    mod_jar = os.path.join(repo, MOD_JAR)

    for p in (world_src, fabric_src, mod_jar):
        if not os.path.exists(p):
            sys.exit("missing: " + p)

    world_dst = os.path.join(OUT_DIR, "world")
    mods_dst = os.path.join(OUT_DIR, "mods")
    for d in (world_dst, mods_dst):
        shutil.rmtree(d, ignore_errors=True)
    shutil.copytree(world_src, world_dst)
    lock = os.path.join(world_dst, "session.lock")
    if os.path.exists(lock):
        os.remove(lock)
    os.makedirs(mods_dst, exist_ok=True)
    shutil.copy(mod_jar, mods_dst)
    shutil.copy(fabric_src, mods_dst)

    with open(os.path.join(OUT_DIR, "START-HERE.md"), "w") as f:
        f.write(README)

    zip_path = os.path.join(OUT_DIR, ZIP_NAME)
    if os.path.exists(zip_path):
        os.remove(zip_path)
    with zipfile.ZipFile(zip_path, "w", zipfile.ZIP_DEFLATED, compresslevel=6) as z:
        for root, _dirs, files in os.walk(OUT_DIR):
            for fn in files:
                p = os.path.join(root, fn)
                if p == zip_path:
                    continue
                z.write(p, os.path.relpath(p, OUT_DIR))
    print("wrote", zip_path, os.path.getsize(zip_path) // 1024, "KB")


if __name__ == "__main__":
    main()
