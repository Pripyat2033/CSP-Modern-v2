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

README = """# Energoblok-1 test world - "can Minecraft actually draw it?"

Ben - this is the one number only you can get. Everything else is measured; the
client framerate is not. This kit gives you one complete RBMK-1000 Energoblok at
1:4, built from real blocks, with the real physics running live, in a flat
disposable world, so the plant is the only thing your GPU has to draw.

## What is in the box
    world/        one Energoblok, already built (262,368 blocks), plant running
    mods/         csp-modern-0.0.1.jar + fabric-api (required)
    START-HERE.md this file

## What you need
- Minecraft Java Edition 1.20.1
- Fabric Loader for 1.20.1  (https://fabricmc.net/use/installer/)
- Java 17 (the launcher's bundled runtime is fine)

## Singleplayer test (recommended - this is the FPS number)
1. Install Fabric Loader 1.20.1.
2. Copy BOTH jars from mods/ into  %appdata%\\.minecraft\\mods\\
3. Copy the world folder into  %appdata%\\.minecraft\\saves\\Energoblok-Test\\
4. Launch the Fabric 1.20.1 profile, open the world Energoblok-Test.
5. You spawn on a glass platform looking at the plant. Wait for chunks. Press F3.
6. Report: fps still, fps walking, fps looking straight at the core.

## Commands (in-game)
- /rbmk plant status   live readout: power, rods, drum level, pumps, rotameters
- /rbmk status         server tick cost (ms/tick) and the verdict
- /rbmk plant scram    drop the rods, watch the power fall
- /rbmk test           the 3D neutron solver cost

## What to tell me
1. fps still / walking / looking at the core.
2. Your GPU and CPU.
3. Whether the blocks render correctly (machines should look distinct).
4. Anything that looks wrong.

## What I already know (server side, measured)
- Whole plant built metered = zero tick hitch (262,368 blocks over ~4 s).
- Plant running live = 2.45 ms/tick; the server's own profiler: 20.03 TPS.
- 16 complete Energobloks running = 0.16 ms/tick of simulation.

The server is not the risk. The client draw is, and it is yours to answer.

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
