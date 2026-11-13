# ENGINE & RESOLUTION DECISION — 1/16 bits, stay on Minecraft, prove it by measurement

Written for Ben, 2026-11-13, in answer to two questions: *"why not 1:32?"* and
*"should we abandon Minecraft for Godot/Unreal — do you believe it is possible in
Minecraft?"* This is the Director's decision to make; this note gives the evidence
and the recommendation.

---

## 1. Why not 1:32 — the decisive fact is not visibility, it is native support

**Minecraft's block-model system is defined on a 16×16×16 grid.** Every model JSON
carries `from`/`to` coordinates in **1/16-block units**. That means:

- **1/16 is the finest resolution the engine supports *natively*.** A 1/16 build
  can use the **normal model system** — fast, well-supported, cached, meshable.
- **1/32 has no native support.** Every block would need **fully custom
  tessellation** (raw vertices, bypassing the model pipeline). That is the
  expensive, fragile path — exactly where LittleTiles-class mods earn their
  complexity.

**Cost of 1/32:** 8× the storage (32³ = 32,768 bits/block vs 16³ = 4,096) **and**
the loss of the native model system.

**What 1/32 buys:** almost nothing the player can perceive. The lattice is **already
exact at 1/16** (250 mm = exactly 4 bits). The only feature that improves is the
88 mm bore: 1.4 bits at 1/16 → 2.8 bits at 1/32 — and 2.8 still rounds to 3, so
**88 mm is never exact at any power-of-two bit scale** (it would need 22 mm bits =
1/45.5). You would pay 8× for ~5 mm of accuracy on one feature.

**Therefore: 1/16 is the right bit scale.** Solve the bore (and any other precision
feature) with a **custom mesh** — a small cylinder model — placed on top of the
1/16 bit grid. One does **not** choose a single resolution for the whole world:
bits for the bulk, custom meshes for the few sub-bit features. That is how the good
sub-block mods are built.

| | 1/16 (recommended) | 1/32 |
|---|---|---|
| Bits per block | 4,096 | 32,768 (8×) |
| Native model support | **yes** | no (custom tessellation) |
| Lattice 250 mm | **exact (4 bits)** | exact (8 bits) |
| Bore 88 mm | 1.4 bits (→1–2) | 2.8 bits (→3) |
| Bore still exact? | no | **still no** |
| Right use | the whole build | *(not recommended)* |

---

## 2. "Would the reactor internals be invisible to the player at that scale?"

**Yes — and that is correct.** The real fuel channel is **88 mm**. A human standing
at the top of the reactor **cannot see into a fuel channel with the naked eye.** That
is the truth of the plant, not a failure of the build.

The operator does not experience an RBMK core by looking at it; they experience it
through **instruments** — the control room, the meters, the SKALA computer. The
project already knows this: `CHARTER.md` §1 makes panels *"a high-resolution picture
with each control a named, clickable point"* (the MAKET approach). The fidelity that
matters **to the player** lives at the **equipment and instrument level** — a
100 mm pipe, a 600 mm panel, a valve, a gauge — and **all of those are sub-metre**,
so **bit-scale serves them**. The core internals being too small to see is the plant
being honest, not the build being wrong.

---

## 3. Minecraft vs Godot/Unreal — the honest account

**The hard part of this project is already engine-independent.**

- The neutron physics (`RbmkSolver3D`, `RbmkCore`, point-kinetics, xenon, the
  positive-scram) is **pure Java, in real metres, and never reads a Minecraft
  block.** (Verified: the solver mesh comes from `LATTICE_PITCH_M`; only
  `RbmkWorldBuilder` reads block counts.)
- The SGB research, the sources, the geometry, the enterprise/workforce design —
  **all portable.**
- The chosen architecture — *"OpenMC computes the physics; the front-end displays
  it"* — **is exactly a clean seam.** The front-end is a **display** choice.

**What a switch would cost:** not the physics, not the research, not the design —
but the **presentation layer**, which is precisely what Minecraft gives for free
and what is most expensive to rebuild. In Godot/Unreal you get free-form geometry
and modern rendering, but you rebuild **the player, world streaming, the building
system, inventory, multiplayer, save/load, the game loop**. And the project's
**core gameplay is construction**, which is **Minecraft's native mode**. A switch
trades the hardest-to-rebuild asset (a working game) to fix a problem
(sub-block geometry) that has a **shipped, precedented solution** (LittleTiles,
Chisels & Bits).

**Verdict: a switch is a bad trade *unless* the bit engine proves infeasible at
full-plant scale.** That is the one thing we have not measured.

---

## 4. Do I believe it is possible in Minecraft?

**Yes.** Evidence: LittleTiles and Chisels & Bits ship sub-block building with
**dynamic `VoxelShape` collision** (an 88 mm hole is a real hole) at scale;
Valkyrien Skies does real rigid-body physics; the engine measured **~10× tick
headroom**; the physics is done and decoupled; construction is Minecraft's home
turf.

**But it is not yet proven, and the honest gap is exact:** we measured *block-count*
performance (64 cores, 16.2 M blocks, no crash) and we have **never measured a
bit-scale build in-game.** Sub-block geometry performance at full-plant scale is the
**single unmeasured risk**, and it is the thing the Director's instinct is circling.

---

## 5. The proposal — decide the engine question by measurement, not argument

Build **one honest prototype**: a single reactor hall, or one core quadrant, in the
1/16 bit engine — **real storage, real renderer, real block-click / bit-act** — and
**measure MSPT and FPS in-game.** This is the "does the engine turn over" test for
the presentation layer, exactly as the reactor spike was for the physics, and it
matches the project's rule to **measure, not assume**.

- **If it runs** → Minecraft is vindicated; the enterprise continues on the platform
  the gameplay was designed for.
- **If it does not** → we will have **hard numbers** to make the Godot/Unreal call
  rationally, and **the physics walks across unchanged**.

Either way the cost of the experiment is small, and the information is decisive.

**The reframe that matters:** the Director's instinct — *"we just need to get the
mathematics correct"* — is **already satisfied**. The mathematics is correct and it
is done. What remains is a **storage-and-rendering engineering problem**, not a
physics one. The wall is the front-end, and the front-end is measurable.

---

## 6. Recommendation, in one line

**Stay on Minecraft; build at 1/16 bits (the finest natively supported), use custom
meshes for sub-bit features; and let a measured in-game prototype — not a debate —
decide whether the platform holds.** The physics is safe either way.
