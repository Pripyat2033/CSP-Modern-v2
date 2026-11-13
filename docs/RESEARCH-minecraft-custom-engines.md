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
   — *possible, and it is the only thing that breaks the 1 m cube limit.* If the
   forensic mesh must hold a 114 mm bore, Minecraft's 1 m cube is a hard ceiling
   (§7), and no amount of native code changes that. That is a *substrate*
   question, not an *engine* question.

**The one-sentence version:** Minecraft can absolutely run a custom engine
alongside it (you've proven it); it can even run native code underneath it; but
neither of those can make a 1-metre cube hold a 114-millimetre hole. Engine
speed is not your wall. Voxel resolution is.

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

1. **The 1 m cube is the voxel floor.** A Minecraft block model is built from
   cuboids on a **16×16×16 pixel grid** (element `from`/`to` coordinates are
   limited to −16..32 in 1/16 units; Minecraft Wiki, https://minecraft.wiki/w/Model ).
   So the *smallest honest feature* is 1/16 of a block. At 1:4 (1 block = 0.25 m),
   1/16 block = **15.6 mm** — fine for detail. But a **1 m block cannot be a
   114 mm fuel-channel bore**; that is 0.114 of a block. Sub-block *models*
   (LittleTiles-style microblocks, https://github.com/CreativeMD/LittleTiles )
   and sub-block *rendering* (Immersive Portals-style render surgery) can draw
   smaller geometry, but the **collision and the block grid stay 1 m**. You can
   *draw* a 114 mm hole; you cannot make the world *be* one.
2. **The tick is single-threaded and fixed at 50 ms.** You can offload maths to
   another thread, but world mutation happens on the server thread, in ticks.
3. **Mixin changes behaviour, not the engine.** It cannot resize the voxel grid,
   change the tick rate, or give you true sub-block collision.
4. **Native code cannot fix the voxel problem.** A native solver is still feeding
   a 1 m voxel world. It makes maths faster; it does not make blocks smaller.

---

## 8. So what should CSP-Modern-v2 actually do?

Mapped onto your situation:

- **Engine speed / "will Minecraft crash?" — answered, no.** You have ~10×
  headroom and 64 cores ran without a crash. The engine can stay pure Java.
- **Native code — not needed, and not recommended.** It would buy speed you don't
  need, at the cost of per-OS binaries and crash-the-game risk. The repo's old
  `rbmk_solver.c`/`.dylib` should be deleted or explicitly parked, not revived.
- **The real wall is the 1 m cube for core *internals*.** This is a *representation*
  decision, and it is exactly the fork the last agent message put to you:
  - **(a) The model is the artefact** (physics is real-size and validated; the
    Minecraft build is a labelled *view*). Fully achievable today, in Java, no
    engine change. Recommended if the deliverable to your team is the forensic
    *simulation* of the accident programme.
  - **(b) The visual mesh is the artefact** (a real-size reconstruction). Then
    Minecraft's 1 m cube is the wrong substrate for the *core internals*, and the
    options are a custom sub-block renderer/collision system on top of MC
    (LittleTiles-class work), or a different engine with sub-metre cells
    (Minestom is still 1 m; you'd need a fine-voxel engine). This is the "sit
    down and choose" moment, and it is a *substrate* choice, not an *engine* one.

**In one line:** you can run any engine you like inside or under Minecraft — you
already run a good one — but the 1 m cube is Minecraft's floor, and only a
different substrate (or a custom sub-block layer) can go below it.

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
