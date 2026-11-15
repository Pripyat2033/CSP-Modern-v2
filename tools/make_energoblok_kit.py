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

README = r"""# Energoblok — the RBMK-1000 plant, built and RUNNING. LOOK, don't just run.

Ben — this world has the whole energoblok in it: the reactor core (stack + UBS +
LBS + vessel), the seven sealed compartments and five islands, 4 steam separator
drams, 2 turbogenerators, 2 condensers, 8 ГЦН pumps, 4 deaerators and the САОР
accumulators — **585,307 blocks**, placed to the real counts.

You spawn on a **glass viewing platform**. The plant is to your **north** (turn
around / look for it, or `/tp 0 70 0`).

**The world opens in creative mode with cheats ON** — fly (double-tap space),
`/gamemode creative`, `/tp`, anything you need.

## Run it (this is the point)

    /rbmk plant start        # boot the core: time-dependent spatial flux, feedback CLOSED
    /rbmk plant status       # watch it hold — power, k_eff, void, drums, turbines

With the fix in this jar the core **holds**: expect `k_eff ~ 1.0000`, power ~0.99,
void ~0.63, ~4 ms/step. That is the physics change (self-consistent axial seed);
it is a *number* you read, not a thing you see.

    /rbmk plant scram        # AZ-5: watch the scram
    /rbmk machine            # stand on a *_controller block to validate a machine's shell
    /rbmk instrument         # stand on a valve / flow meter: reads the RUNNING plant

## What is NOT done yet (so you don't have to find it)

- The exact K-500-65/3000 cylinder lengths and cover diameter are a block
  approximation (docs/RBMK-MACHINE-GEOMETRY.md).
- The hall's exact placement (600 m machine hall, bay spacing) still needs the
  building drawings.
- The pipework, per-channel valves and meters are placed but the full pipe routing
  (Phase D1) is not built.
- The equipment register (docs/EQUIPMENT-REGISTER.md) is the honest list of what is
  still only *named* rather than modelled — read it before deciding what is missing.

## What to tell me

1. Does the plant start and HOLD for you? (paste `/rbmk plant status`)
2. Do the machines read "formed" from their controllers?
3. fps (F3) — still the one number I cannot get.
4. The first three things that are wrong.

## Install
- Minecraft Java 1.20.1 + Fabric Loader 1.20.1
- both jars from mods/ into %appdata%\.minecraft\mods\
- the world/ folder into %appdata%\.minecraft\saves\Energoblok-Test\
- open the world, you spawn on the platform

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
