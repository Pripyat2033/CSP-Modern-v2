# Is Minecraft the right substrate? — the blunt answer

Written 2026-11-15, for Ben, after: *"this whole project feels like one step
forward and 15 backward… I don't think Minecraft is the right substrate unless
you've got any better ideas."*

This is the Director's decision to make. Here is the evidence, the honest
diagnosis of the *feeling*, and the real options. No cheerleading.

---

## 1. First, the diagnosis: the "15 backward" are not backward

Look at what the last month's "backward" steps actually were:

| The "backward" step | What it really was |
|---|---|
| 7,013 signals → 7,893 | Ben challenged a **mislabel**; the model got **truer** |
| INSAG-7 anchors → ROSFOND target | Ben set the **trust hierarchy**; the model got **truer** |
| three cores → one core | The model **found its own incoherence** and collapsed it |
| AZ-5 positive-scram "failure" | It was **reproduced from geometry**, then calibrated |

**Not one of those was a regression. Every one was a correction toward the truth,
and every one was found because Ben asked the right question.** A project that
never corrects itself is the one that is actually broken. The SGB rule ("nothing
is guessed") *guarantees* you will revise — that is the method working, not the
method failing.

So the "15 backward" feeling is real but it is **misattributed**. The real cause is
different, and it is fixable:

**The project has been running depth-first research with no visible finish line.**
When every week produces *more questions* and the deliverable is always "and now we
must also…", the brain reads it as no progress. That is a **sequencing and
visibility problem, not a substrate problem.**

## 2. Is Minecraft the right substrate? — the honest ledger

The prior research already answered this (`ENGINE-DECISION.md`,
`RESEARCH-minecraft-custom-engines.md`). The key facts:

- **The hard part is already substrate-independent.** The physics
  (`RbmkSolver3D`, `UnifiedReactorCore`, the transient solver) is **pure Java, in
  real metres, and never reads a Minecraft block.** The research, the sources, the
  geometry, the enterprise design — **all portable.** The chosen architecture
  ("OpenMC computes; the front-end displays") **is a clean seam.**
- **What Minecraft gives free** is the expensive part to rebuild: the player,
  world streaming, the **building system** (and construction *is* the gameplay),
  inventory, multiplayer, save/load, the game loop.
- **The one unmeasured risk** is sub-block (bit-scale) rendering and storage at
  full-plant scale. Everything else has been measured: ~10× tick headroom, 64
  cores without a crash, the live 3D core at 4.4 ms/tick.

**Therefore: switching substrates would trade the hardest-to-rebuild asset (a
working game) to fix a problem (sub-block geometry) that has a shipped,
precedented solution (LittleTiles, Chisels & Bits).** That is a bad trade **unless
the bit engine proves infeasible** — which is the one thing nobody has measured.

## 3. The reassurance that matters most

**You are not trapped.** Because the physics and research are substrate-independent,
betting on Minecraft costs you almost nothing if you later decide to leave. The
irreplaceable work — the sources, the physics, the geometry, the design — **walks
across unchanged.** So this is not an irreversible bet; it is a reversible one.

## 4. The real options, with honest costs

| Option | Gets you | Costs you | Verdict |
|---|---|---|---|
| **Stay on Minecraft** (bit engine) | Everything you have; construction gameplay free | The bit-engine work; the unmeasured risk | **Recommended** — decide by measurement |
| **Minecraft + separate visualiser** | Keep the interactive living world *and* get exact-geometry forensic walkthroughs | A second front-end to build | **Strong hybrid** — fits the "two zones of honesty" rule |
| **Godot / Unreal** | Free-form geometry, modern rendering | Rebuild player, building, multiplayer, save/load, the whole game | Bad trade *unless* the bit engine fails |
| **Bespoke engine (e.g. Bevy/Rust)** | Total control | Rebuild *everything*, forever, alone | Worst trade for a one-person project |
| **Minestom / other MC server** | — | Loses the client; helps nothing here | No |

**The hybrid is worth naming explicitly**, because it matches the project's own
charter. `AGENTS.md` already has a **"two zones of honesty"** rule: the
disaster-relevant facility must be a real simulation; elsewhere, build real where
known. So:

- **Zone A — the living world** (Pripyat, the enterprises, the plant you build and
  run, the teaching): **stays in Minecraft.** Construction gameplay is the point,
  and it is Minecraft's home turf.
- **Zone B — the forensic core** (the 1:1 reactor internals, the exact geometry a
  person could never see at 88 mm anyway): **may use a separate high-fidelity
  visualiser** if bit-scale ever fails. The physics feeds both unchanged.

## 5. The decision that ends the argument — measure it

Build **one honest prototype**: a single reactor-hall quadrant in the 1/16 bit
engine — real storage, real renderer, real block-click/bit-act — and **measure
MSPT and FPS in-game.** This is exactly the test the physics already passed.

- **If it runs** → Minecraft is vindicated; the argument is over; the enterprise
  continues on the platform the gameplay was designed for.
- **If it does not** → you have **hard numbers** to make the switch rationally, and
  **the physics walks across unchanged.**

Either way the cost is small and the information is decisive. **Stop deciding this
by argument; decide it by measurement.**

## 6. The one-line answer

**Minecraft is the right substrate, the "backward steps" were corrections that
prove the method is working, and the way to settle the engine question is one
measured prototype — not a debate.** And if you ever do leave, the physics and the
research come with you, so the bet is reversible.

**What is actually wrong is not the substrate; it is that the finish line has never
been drawn.** Draw it: pick the next *visible deliverable* (e.g. "the reactor hall
quadrant, built and measured"), finish it, and let the feeling of forward motion
come back.
