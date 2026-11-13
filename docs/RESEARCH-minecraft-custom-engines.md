# Can Minecraft support custom engines running alongside or under it?

Research answer, 2026-11-12. Every claim cites a source. "SGB" = the project's
Science Grade or Better rule: no guessing.

---

## The bottom line first

**Yes — and in this project's case you are already doing the "alongside" version
and it works.** The physics engine is *already* a custom engine living inside
Minecraft: `RbmkSolver3D` is a real 3D neutron-diffusion / point-kinetics solver,
written in pure Java, ticked live on the server at ~4.4 ms/tick over 52,416 nodes
with no tick-lag warnings. Minecraft is already the *shell*; your engine runs
inside it.

So the honest answer splits into three, and you should keep them separate:

1. **"A custom engine under Minecraft, talking to it"** — *already done, here.*
   Nothing in the mod needs Minecraft's blocks to compute the reaction. The
   reactor is a separate engine; Minecraft renders and drives it. This is the
   architecture that exists today.
2. **"A custom engine *below* Minecraft — native C/C++/Rust code"** — *possible,
   done in the wild, but it does not fix the real problem.* It makes the maths
   faster (you don't need that — you have ~10× headroom) and it makes shipping
   painful (per-OS binaries, JNI crashes that kill the game, an anti-cheat smell).
   There is even an abandoned attempt of exactly this already in your repo
   (`rbmk_solver.c` + `librbmk_solver.dylib` + `REKORD*`). See §5.
3. **"A different engine with Minecraft as a front-end, or no Minecraft at all"**
   — *possible, but not needed.* The bit engine (sub-block geometry with dynamic
   shapes) already lets Minecraft itself hold the 114 mm bore and the 250 mm
   lattice; see `SCALE-DECISION.md`. A different substrate is an option only if
   you ever want the individual 13.6 mm fuel rod drawn, which the physics does not
   require. That is a *substrate* question, not an *engine* question.

**The one-sentence version:** Minecraft can absolutely run a custom engine
alongside it (you've proven it), and it can be built at true 1:1 in bit-scale
(62.5 mm bits) inside Minecraft itself — so no different engine or substrate is
needed. Engine speed is not your wall, and neither is resolution, once the build
is bit-scale.

---

## 1. How the integration actually works (the four real seams)

There are exactly four places a "custom engine" can attach to Minecraft Java:

| Seam | What it is | What it can do | What it can't |
|---|---|---|---|
| **In-process Java** | Your engine is a class the mod calls | Anything Java can do; shares memory with the game | Can't dodge the 1 m cube; runs on the tick thread unless you thread it |
| **In-process native** | C/C++/Rust via JNI or FFM | Fast native maths, use of native libraries (SIMD, CUDA via LWJGL) | Per-OS binaries; a native crash kills the whole game |
| **Mixin / ASM** | Bytecode rewriting of MC classes at load | Change what Minecraft *does* (inject, redirect, replace methods) | It rewrites *bytecode*, not the engine; it can't change voxel size or the tick model |
| **Out-of-process** | A separate program talking over sockets/shared memory | Total freedom of language and threading | Marshalling cost, a second thing to ship and start; rare in mods |

**Mixin is not "replacing the engine."** Fabric's own docs describe mixins as a
tool to "finely control the behavior" of the game where the API provides no hook,
used *instead of* events when needed — it is bytecode transformation of Minecraft's
own classes, not a plugin engine. Source: Fabric docs (bundled in the repo as
`fabric_docs_combined.txt`, lines 13133, 16721–16809) and
https://docs.fabricmc.net/develop/mixins/ . Your `csp-modern.mixins.json` is
currently empty (`"mixins": []`), so you use zero mixins today — a deliberate,
clean choice.

---

## 2. In-process native (JNI / FFM / JNA / JNR) — the "under Minecraft" option

This is the option the question is really about. It is **real and done in the
wild**, in three flavours:

- **JNI** — the classic. A Java `native` method calls a C/C++ function. Works,
  but brittle: a segfault in the native code takes down the whole JVM (the game).
- **JNA** — reflection-driven JNI wrapper; no C glue needed, slower to set up,
  what your abandoned `REKORD` code was written against.
- **FFM (Project Panama)** — the modern, *finalised* way. JEP 454 finalised the
  Foreign Function & Memory API in **JDK 22** (it was preview in JDK 19/20/21 via
  JEP 424/434/442). Source: https://openjdk.org/jeps/454 and
  https://openjdk.org/jeps/442 . It lets Java call native functions and read
  native memory "without the brittleness and danger of JNI."

**Crucially, Minecraft already ships native code in-process.** Minecraft is
written in Java but is *bound* to native libraries through **LWJGL** (Lightweight
Java Game Library) — LWJGL is the Java binding layer to OpenGL/Vulkan/OpenAL/GLFW
and more, and it is the reason Minecraft uses native code from the day it was
"rewritten to use the Lightweight Java Gaming Library" (Minecraft Wiki,
https://minecraft.wiki/w/Java_Edition ). LWJGL's whole job is exactly the thing
you're asking about: a Java program running a native engine underneath it.
Sources: https://www.lwjgl.org/ .

**So "can Minecraft run a custom engine under it" is, strictly, already answered
by Minecraft itself: yes, via LWJGL.**

---

## 3. Real-world precedents (what has actually been built)

Verified against each project's own repo (language counts from the GitHub API):

| Project | What it proves | Language / engine | Source |
|---|---|---|---|
| **MCEF** (CinemaMod) | A **full native Chromium browser** embedded in Minecraft as a mod | Java mod + native **Chromium/CEF** via java-cef/JNI, downloads per-OS binaries at runtime | https://github.com/CinemaMod/mcef |
| **pedrogpo/minecraft-mod-runtime** | A **C++ engine** modifying Minecraft live through **JNI** | C++ / JNI | https://github.com/pedrogpo/minecraft-mod-runtime |
| **Vibe Physics** | A **native Rust physics engine** for a Minecraft-based engine | **Rust** native engine | https://github.com/cosmo-jar/Vibe-Physics-Mod-for-Minecraft-on-Rust |
| **Mliybs.Minecraft.Fabric** | Writing **Fabric mods in C#** via NativeAOT + JNI | C# / NativeAOT / JNI | https://github.com/Mliybs/Mliybs.Minecraft.Fabric |
| **radioegor146/jar-to-dll** | Packing a mod jar into a native **DLL** and injecting it | C | https://github.com/radioegor146/jar-to-dll |
| **Valkyrien Skies 2** | **Custom rigid-body ship physics** inside Minecraft — and it is **pure Java** | Java + Kotlin, **no native lib** | https://github.com/ValkyrienSkies/Valkyrien-Skies-2 |
| **Physics Mod** (haubna) | Soft-body physics in Minecraft — **pure Java** | Java only | https://github.com/haubna/PhysicsMod |
| **Sodium** | A **renderer-engine replacement**, **pure Java + GLSL** | Java + GLSL | https://github.com/CaffeineMC/sodium |
| **VulkanMod** | Replaces the renderer with a **Vulkan** path (via LWJGL's Vulkan bindings) | Java | https://github.com/xCollateral/VulkanMod |
| **Immersive Portals** | Deep **render-pipeline surgery** (non-Euclidean rendering) in Java | Java | https://github.com/iPortalTeam/ImmersivePortalsMod |
| **Minestom** | A **from-scratch Minecraft server** that keeps only the client protocol | Pure Java | https://github.com/Minestom/Minestom |

**The lesson in that table is the important one for you.** The two projects that
do the *hardest* custom physics in Minecraft — Valkyrien Skies and the Physics
Mod — are **pure Java**. The projects that use native code (MCEF, pedrogpo) use it
for a *non-physics* job (a browser, live injection). So the Minecraft world's own
answer to "I need a custom engine" has, in practice, usually been **"write it in
Java and tick it yourself,"** which is exactly what CSP-Modern-v2 already does.

---

## 4. Out-of-process engines (a separate program, sockets / shared memory)

Fully possible — the mod would open a local socket or a memory-mapped file and
talk to a separate simulation process in any language, any number of threads.
It is the *cleanest* isolation (the engine crashing doesn't crash the game) and
the *only* way to get real multicore simulation. It is also **rare in mods**,
because it doubles what the user must install and start, and the marshalling
(serialising state across the boundary every tick) can cost more than the maths
saved. No mainstream Minecraft mod ships this way today. It is a legitimate
research option, not an off-the-shelf one.

---

## 5. The native engine that is *already* in your repo (and why it's dead)

The repo already contains an abandoned attempt at option 2:

- `src/main/java/com/ben/csp/rbmk/rbmk_solver.c` — 191 lines of C: a 7-point
  Laplacian stencil, cross-section lookup, a Bateman I-135/Xe-135 solver, control
  rod worth, point-kinetics, thermal-hydraulics, power density. Comments call it
  "SGB Algorithmic Heritage."
- `librbmk_solver.dylib` — the compiled macOS binary, sitting in the repo root.
- `REKORDSolverLibrary.java` / `REKORDSolverManager.java` — a JNA-style loader
  that calls `System.loadLibrary("rbmk_solver")` per OS.

**It is dead code.** `build.gradle` has no JNA dependency, and nothing calls
`REKORDSolverManager` or `REKORDSolverLibrary`. It was an idea, not a working
engine. If you ever revisit native code, **do not resurrect this as-is** — use the
FFM API (JEP 454) instead of JNA, and remember the C in that file duplicates maths
that `RbmkSolver3D` already does correctly in Java.

---

## 6. Threading reality (what can run off the tick thread)

Minecraft's game logic runs on a single **server thread** ("Server thread" in
your own logs, and `fabric_docs_combined.txt` line 41580). The tick is 50 ms
(20 TPS). Things that genuinely run off-thread in vanilla/known mods: chunk
generation, some lighting, world saving, and *parts* of rendering (the render
thread is separate from the server thread).

So a custom engine **can** tick on its own thread and feed results back — with two
rules: (a) never touch world/block state off the server thread (use a queue and
apply on-tick), and (b) beware that a background thread can still cause
*concurrent modification* if it reads state the server is editing. Your current
design (one global simulation stepped once per tick) sidesteps this entirely,
which is why it is safe and cheap.

Your own measurement already answers the capacity question: `/rbmk stress` showed
64 cores / 16.2 M blocks at 67.7 ms "overloaded, NO crash," and the real 1:1 core
is ~20–30k blocks — roughly **10× headroom**. The engine is not the bottleneck.

---

## 7. The hard limits (what cannot be done)

These are the walls. None of them are about "can an engine run" — they are about
Minecraft's representation:

1. **The 1 m block is the storage grid, not the resolution floor.** A block model
   is built from cuboids on a **16×16×16 pixel grid** (element `from`/`to`
   coordinates are limited to −16..32 in 1/16 units; Minecraft Wiki,
   https://minecraft.wiki/w/Model ). So the *smallest honest feature* is 1/16 of a
   block = **62.5 mm at 1:1** — which is exactly the **bit**. Crucially, sub-block
   mods (LittleTiles, Chisels & Bits) give a block a **dynamic `VoxelShape`**
   computed from the bits it contains, so the **collision and outline shapes ARE
   sub-block** — a 114 mm hole can be a real hole you walk into and click. The
   engine still *stores* the world on the 1 m grid; it does not force the
   *geometry* to be 1 m cubes. (Correction: an earlier draft claimed collision
   stays 1 m — that is wrong; LittleTiles-class mods make it sub-block.)
   So the plant is built from bits, as `PROJECT_NOTES.md` mandates; the 1 m block
   is only the container for 16³ of them.
2. **The tick is single-threaded and fixed at 50 ms.** You can offload maths to
   another thread, but world mutation happens on the server thread, in ticks.
3. **Mixin changes behaviour, not the engine.** It cannot resize the storage grid,
   change the tick rate, or give you true sub-block collision on its own — that
   comes from a sub-block block type (a dynamic shape), which is what the bit
   engine is.
4. **Native code cannot fix a resolution problem.** A native solver is still feeding
   a voxel world. It makes maths faster; it does not make bits smaller. (Not needed
   here anyway — the bit engine is a storage/render/interaction job, pure Java.)

---

## 8. So what should CSP-Modern-v2 actually do?

Mapped onto your situation:

- **Engine speed / "will Minecraft crash?" — answered, no.** You have ~10×
  headroom and 64 cores ran without a crash. The engine can stay pure Java.
- **Native code — not needed, and not recommended.** It would buy speed you don't
  need, at the cost of per-OS binaries and crash-the-game risk. The repo's old
  `rbmk_solver.c`/`.dylib` should be parked, not revived.
- **The build is bit-scale (1:1), per `PROJECT_NOTES.md`.** Not metre blocks —
  that is forbidden on the main site, and geometrically impossible for the core
  (88 mm channel on a 250 mm lattice). The work is the **bit engine**: sparse
  paletted storage, boxed solids, LOD rendering, and *block-click / bit-act*
  interaction. This is a *storage and interaction* job inside Minecraft, not a
  substrate change. See `SCALE-DECISION.md`.
- **The physics is unaffected.** It already runs at 1:1 in real metres; the bit
  build makes the drawing match it.

**In one line:** you can run any engine you like inside or under Minecraft — you
already run a good one — and the build is bit-scale inside Minecraft itself; no
different substrate is needed.

---

## Sources

- JEP 454 — Foreign Function & Memory API (final, JDK 22): https://openjdk.org/jeps/454
- JEP 442 — FFM API (third preview, JDK 21): https://openjdk.org/jeps/442
- LWJGL (Java↔native bindings; Minecraft's native layer): https://www.lwjgl.org/
- Minecraft Wiki — Java Edition / LWJGL: https://minecraft.wiki/w/Java_Edition
- Minecraft Wiki — Model (16×16×16 pixel grid): https://minecraft.wiki/w/Model
- Minecraft Wiki — Tick: https://minecraft.wiki/w/Tick
- Fabric docs — mixins & events (bundled: `fabric_docs_combined.txt`): https://docs.fabricmc.net/develop/mixins/
- MCEF — native Chromium in MC: https://github.com/CinemaMod/mcef
- pedrogpo/minecraft-mod-runtime — C++/JNI: https://github.com/pedrogpo/minecraft-mod-runtime
- Vibe Physics — native Rust engine: https://github.com/cosmo-jar/Vibe-Physics-Mod-for-Minecraft-on-Rust
- Mliybs.Minecraft.Fabric — C#/NativeAOT/JNI: https://github.com/Mliybs/Mliybs.Minecraft.Fabric
- radioegor146/jar-to-dll — pack jar to DLL: https://github.com/radioegor146/jar-to-dll
- Valkyrien Skies 2 — pure-Java ship physics: https://github.com/ValkyrienSkies/Valkyrien-Skies-2
- Physics Mod — pure-Java soft-body: https://github.com/haubna/PhysicsMod
- Sodium — renderer replacement (Java+GLSL): https://github.com/CaffeineMC/sodium
- VulkanMod — Vulkan renderer replacement: https://github.com/xCollateral/VulkanMod
- Immersive Portals — render-pipeline surgery: https://github.com/iPortalTeam/ImmersivePortalsMod
- LittleTiles — sub-block building: https://github.com/CreativeMD/LittleTiles
- Minestom — from-scratch Java server: https://github.com/Minestom/Minestom
