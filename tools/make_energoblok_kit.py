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

README = r"""# Energoblok — the plant machines as real multiblocks. LOOK, don't just run.

Ben — the turbines and pumps are no longer single textured cubes. They are real
machines now, built from their real sizes and validated. You spawn on a glass
platform; the row of specimen blocks is right in front of you, and the whole
plant (core + equipment) is to your east.

**The world opens in creative mode with cheats ON** — fly (double-tap space),
use `/gamemode creative`, `/tp`, anything you need. If it still opens in
survival for you, run `/gamemode creative` yourself (cheats are already enabled).

## What to look at first (the specimen row, ~5 blocks ahead)

In a line, so you can compare them side by side:

    turbine_cover     - the white cylinder cover. This is the thing you said must
                        be white and circular: the K-500-65/3000 has FIVE of these
                        (1 HP + 4 LP), and only they show in the hall.
    generator         - the ТВВ-500 body, on the same shaft as the covers.
    mcp_motor         - the ГЦН's 6 kV drive motor (grey, finned), above the pump.
    turbine           - the casing block the foundation/shaft are made of.
    mcp               - the pump body block.
    steam_separator   - the drum shell block (2.8 m dia x 31 m).
    *_controller      - the controller: the machine's brain. Stand on one and run
                        /rbmk machine to see it validate its own shell.

## What to look at second (the machines, to the east)

Look east from the platform: two **turbogenerators**, each five white circular
covers in a row plus the generator, on a foundation, with a condenser underneath.
Around the core are the eight **ГЦН** — each a pump body with its motor on top —
and the four steam **separator** drums (124 blocks long, 11 across).

Every one of these is a **multiblock**: a casing of the right shape with a
controller. Stand on any controller block and run:

    /rbmk machine

It reports "formed" only when the real casing is present, and "incomplete — N of
M shell blocks missing" otherwise. The world builder places exactly the cells the
validator checks, so a machine that reads "formed" really is the machine.

## What is NOT done yet (so you don't have to find it)

- The exact K-500-65/3000 cylinder lengths and cover diameter are a block
  approximation. The sources give the 39 m overall, the 5-cylinder count and the
  Ду-1200 exhausts, not each cylinder's length — see docs/RBMK-MACHINE-GEOMETRY.md.
- The machines are placed to their real size and count, but the hall's exact
  placement (600 m machine hall, bay spacing) still needs the building drawings.

## What to tell me

1. Do the five white covers read as the turbogenerator? Does the row look right?
2. Does the ГЦН read as a pump with its motor on top?
3. Does /rbmk machine read "formed" for you? (break a cover block and re-run — it
   should say "incomplete — 1 of ...")
4. fps (F3) — still the one number I cannot get.

## Install
- Minecraft Java 1.20.1 + Fabric Loader 1.20.1
- both jars from mods/ into %appdata%\.minecraft\mods\
- the world/ folder into %appdata%\.minecraft\saves\Energoblok-Machines\
- open the world, you spawn on the platform looking at the specimen row

- built by an AI agent (OpenHands) on Ben's behalf.
"""


def ensure_creative_with_cheats(world_dir):
    """Force the kit world into creative mode with cheats enabled.

    Ben must be able to fly and inspect; a survival, cheats-off world is not
    usable for that. A level.dat copied from a server-created world defaults to
    survival, so we set it here and the setting sticks for new players too.
    """
    import nbtlib

    path = os.path.join(world_dir, "level.dat")
    f = nbtlib.load(path)
    data = f["Data"]
    data["GameType"] = nbtlib.Int(1)        # 1 = creative
    data["allowCommands"] = nbtlib.Byte(1)  # cheats on
    data["Difficulty"] = nbtlib.Byte(1)     # easy (mobs are not the point)
    f.save(path)
    print("set", path, "to creative with cheats")


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
    ensure_creative_with_cheats(world_dst)
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
