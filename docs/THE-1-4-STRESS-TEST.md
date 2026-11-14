# THE 1:4 STRESS TEST — what it is for, and how you run it

Written for Ben (Director, ChSZO), 2026-11-14. This replaces the stale parts of
`TEST-1-4-RBMK.md` and states plainly what the 1:4 core is for.

---

## 1. What 1:4 is (in your words, because you are right)

1:4 is **not** a dumbed-down mod and it is **not** a compromise on fidelity. It is
a **conservative stress test** that asks exactly one question:

> **Can Minecraft cope with a science-grade reactor at all — without hanging or
> crashing?**

If the answer is yes, you give the green light for the full 1:1 core. If it is no,
we stop and decide the way forward together. The scale is the *question*, not the
*product*.

---

## 2. The one thing you were never able to do — and now can

You said it plainly: **the only test that counts is running it.** Not a benchmark,
not a claim — you at the keyboard, the reactor in front of you, watching the tick
counter. That is now possible, and the path is short.

### 2.1 Get the jar

- **Download:** `https://work-1-buxrxgsirsymladz.prod-runtime.all-hands.dev/download/csp-modern-0.0.1.jar`
- Or build it: `./gradlew build` (needs the JDK 17 that ships in the workspace).

### 2.2 Install (Fabric, Minecraft 1.20.1)

1. Install **Fabric Loader ≥ 0.15.11** for **Minecraft 1.20.1**.
2. Drop **Fabric API 0.91.0+1.20.1** (or newer for 1.20.1) into `mods/`.
3. Drop `csp-modern-0.0.1.jar` into `mods/`.
4. Launch the **1.20.1 Fabric** profile. Create a **flat, creative** world.

### 2.3 Run the test (the exact commands)

```
/rbmk build demo          → builds the 1:4 core (~231,000 blocks)
/rbmk plant build demo    → core + energoblok (~585,000 blocks total)
/rbmk plant start         → the plant runs on the server clock
/rbmk plant status        → live power, rods, pumps, drums, turbines
/rbmk status              → the tick cost (the number that matters)
```

- `demo` bypasses the enterprise gate "for a disposable test world". The gate is
  correct for the real site; the enterprises are not built yet, so `demo` is the
  way in.
- The 1:4 build places blocks **metered at 3,000/tick** so the server does not
  hitch; the build takes a few seconds.

### 2.4 What to watch

| Watch | Pass looks like | Fail looks like |
|---|---|---|
| `/rbmk status` tick cost | a few ms, steady | climbing, or "Can't keep up!" repeated |
| `/rbmk plant status` | power ~1.0, holding | power collapsing or running away |
| The world | smooth walking/flying | stutter, freezes, crash |
| F3 | fps stable | fps collapse |

A tick is **50 ms**. In-sandbox the running 1:4 plant held **~1.5 ms/tick** — about
3 % of the budget. On your PC the number will differ; that is the point of running
it there.

---

## 3. What the benchmarks already show (context, not a substitute)

Measured in-sandbox (4 GB heap, flat world):

| Build | Real blocks | MSPT after build | Verdict |
|---|---|---|---|
| 1 core at 1:4 | 253,780 | 10.1 ms | comfortable |
| 8 cores | 2,030,240 | 9.1 ms | comfortable |
| 16 cores | 4,060,480 | 15.7 ms | comfortable |
| 32 cores | 8,120,960 | 34.5 ms | comfortable |
| 64 cores | 16,241,920 | 67.7 ms | overloaded, **no crash** |
| 1 full plant | 353,292 | 2.3 ms | comfortable |

Block placement runs ~250,000 blocks/s. The realistic 1:1 RBMK-1000 core is
~20,000–30,000 blocks. Minecraft did not die at 64 cores; it got slow, which is
recoverable. These numbers say "comfortable", but **only your run settles it.**

---

## 4. The bug this test was built to catch — and it caught one

You said the test is worth it because running finds what benchmarks cannot. It did.
On 2026-11-14 the review found that the **physics mesh was silently tied to the
build scale**:

- The solver mesh and the core's axial node count were built from the `*_BLOCKS`
  constants, which derive from `METRES_PER_BLOCK` (the *drawing* scale).
- That is correct only while `METRES_PER_BLOCK == LATTICE_PITCH_M` — true at 1:4
  today, but a coincidence.
- `docs/SCALE-DECISION.md` §7 told us to set `METRES_PER_BLOCK = 1.0` for the 1:1
  build. Doing that would have collapsed the mesh 4x (49 cells → 12, 28 nodes → 7,
  52,416 → 13,104 active nodes; effective pitch 942 mm, not 250 mm) and the physics
  would have been **wrong** — while the code still looked fine.

That is precisely the hidden simplification your mandate forbids, and it was hiding
in plain sight in the doc that promised it could not happen.

**It is fixed.** `RbmkGeometry` now carries explicit lattice-**cell** constants
(`CORE_HEIGHT_CELLS` = 28, `CHANNEL_REGION_DIAMETER_CELLS` = 49,
`STACK_DIAMETER_CELLS` = 56, `CORE_DIAMETER_CELLS` = 47) computed from real metres
÷ real pitch. The solver, `RbmkCore`, `RodWorth` and `RbmkLattice` read those; the
world builder alone reads `*_BLOCKS`. The numbers are identical today, so **nothing
changed in behaviour** — but the build can now move to 1:1 without touching the
physics.

**Proof:** `src/test/java/com/ben/csp/rbmk/physics/ScaleIndependenceSelfTest.java`.
It passes at `METRES_PER_BLOCK` = 0.25 and at 1.0. Before the fix, the same test
**failed** at 1.0 (verified by running it against a modified copy).

---

## 5. What the 1:4 test proves, and what it does not

**Proves (once you run it):**
- The core builds in-world and the plant runs live, on the server clock.
- The tick cost is survivable on real hardware.
- The reactor, pumps, drums, turbines and condensers evolve together.

**Does not prove:**
- **Bit-scale (1:1) performance.** The bit engine is still a stub
  (`ChiseledBlockEntity` holds no bit storage; its renderer is empty). That remains
  the one unmeasured risk — and it is a *rendering/storage* risk, not a physics one,
  now that the mesh is decoupled.
- **Physics correctness of every constant.** Several are flagged `to_verify` in
  `PhysicsProvenance` (e.g. rod-worth scale). Running does not make them right; it
  makes them *observable*.

---

## 6. The one line

**1:4 asks "can Minecraft cope?" — nothing else. The jar is ready and the commands
are three lines; your run is the test that counts. And the test has already earned
its keep: it exposed a physics mesh silently tied to the drawing scale, which would
have corrupted the reaction the moment we scaled to 1:1. That is fixed and pinned
by a test.**
