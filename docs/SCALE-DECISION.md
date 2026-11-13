# THE SCALE DECISION — can Minecraft hold a true 1:1 RBMK-1000?

Written for Ben, 2026-11-13. Plain language. Answers the question you asked in
your own words: *"we have to use the original dimensions ... if we don't we are
faking the data and it could even compromise the reaction in the core ... I don't
know if we can do that with the actual RBMK-1000 ... my gut feeling says there's
a solution."*

**Your gut is right. There is a solution, it is already half-built, and — the
important part — the block scale cannot compromise the reaction, for a reason I
can show you in the code.**

---

## 1. First, the fear, taken seriously

You are worried about two different things and they have two different answers:

1. *"If we don't use the real dimensions we are faking the data, and it could
   compromise the reaction."* → **True in general, and already prevented here.**
2. *"I don't know if we can do 1:1 with the RBMK, and the blocks are too hard to
   click on."* → **A real, solvable display problem — not a physics problem.**

Keep those separate and the whole thing becomes easy.

---

## 2. Why the block scale CANNOT compromise the reaction (this is the key fact)

The reaction is computed from **metres and material fractions**, never from block
counts. This is not a hope; it is how the code is written:

- **`RbmkGeometry`** holds the real SI numbers — `LATTICE_PITCH_M = 0.250`,
  `PRESSURE_TUBE_ID_M = 0.088`, `CORE_HEIGHT_M = 7.0`. Every `*_BLOCKS` constant
  is *derived* from metres by one function, `toBlocks()`.
- **`RbmkSolver3D`** builds its mesh from `LATTICE_PITCH_M` (converted to cm),
  **not** from any block count. The mesh pitch is the real 250 mm.
- **`RbmkCore`** says it outright in its own header:
  *"the core is **data**, not geometry — so its granularity does not depend on how
  many blocks the world spends on it ... the tick cost is the cost of stepping
  THIS array, and that is independent of block scale."*
- I grepped the whole `rbmk` package: the only code that reads a `*_BLOCKS`
  constant is **`RbmkWorldBuilder`** — the thing that *draws* the world. The
  solver and the core never touch it.

**So the physics has always run at 1:1, in real metres, and the world could be
drawn at any size at all without changing a single number in the reaction.**
Changing the display from 1:4 to 1:1 changes **nothing** in the physics. The
"compromise the reaction" worry is real in general but **provably not applicable
to this codebase**.

> **The honest scale statement for the record:** the *simulation* is 1:1. The
> *drawing* is currently 1:4. Those are different layers, and only the second one
> is "wrong."

---

## 3. What is ACTUALLY broken (and it is not the reaction)

Two real problems, both about the *build*, not the physics:

1. **The build is not verifiable at true scale.** At 1:4 (1 block = 250 mm) you
   cannot put a real drawing against the built core and read off millimetres. The
   mission's gate — *"mis-built is refused and flagged"* — needs the built thing
   to *be* the real thing, at real size, so it can be measured.
2. **The avatar is 4× too small.** At 1:4 a player 1.8 m tall is 7 blocks tall in
   a core that is 47 blocks across. Everything feels like a model because it *is*
   a model — this is the "kids' art piece" feeling you recorded.

Neither is a physics failure. Both are fixed by drawing at 1:1.

---

## 4. The solution: 1:1 scale, and the core built in 1/16 bits

Here is the thing that makes it click. **"1:1 scale" and "bit-scale" are two
different ideas, and you need both, for different parts of the plant:**

| Idea | Meaning | Where it applies |
|---|---|---|
| **1:1 scale** | 1 Minecraft block = 1 real metre | **Buildings, halls, rooms** — a 60 m hall is 60 blocks tall. Correct and easy. |
| **Bit-scale** | subdivide one block into 16³ = **62.5 mm** bits | **The core internals**, whose features are *smaller than a metre* (the 250 mm lattice). |

The buildings you have already got right — 1 m blocks at 1:1 is the only sane way
to build a power-station hall, exactly as you said. The *core* is the only place
that needs to go finer, because its natural unit is 250 mm.

**And at 1/16 (62.5 mm bits), the lattice is EXACT:**

| Feature | Real size | At 1/16 bit scale |
|---|---|---|
| Lattice pitch (cell) | 250 mm | **4 bits — exact** |
| Graphite block hole | 114 mm | ~2 bits |
| Pressure-tube bore (ТК) | 88 mm | ~1 bit (see the honest note in §7) |
| Active core diameter | 11.8 m | 188 bits = **11.75 blocks ≈ 12 blocks** |
| Active core height | 7 m | 112 bits = **7 blocks** |

**So the 1:1 bit-scale core is ~12 blocks across and 7 blocks tall.** Read that
again, because it is the opposite of what you'd expect: **going from 1:4 to 1:1
bit-scale makes the core SMALLER in block count, not bigger.**

| | 1:4 (today) | 1:1 with 1/16 bits |
|---|---|---|
| Core footprint | 47 blocks across | **12 blocks across** |
| Core height | 28 blocks | **7 blocks** |
| Blocks to build the core | ~48,600 | **~765** |
| Detail carried by | the blocks themselves | the **bits inside** each block |

At 1:4, 1 block = 1 lattice cell, so you need 47×47×28 blocks. At 1:1, 1 block =
1 m = 16 bits = **4 lattice cells across**, so each block holds 16 channels — and
you need only ~12×12×7 blocks. The detail moved *inside* the blocks, and the block
count dropped ~60×.

**This is the solution your gut was pointing at:** you can have true 1:1 scale AND
the real 250 mm lattice, and it is *cheaper* in world cost than what you have now.

---

## 5. The clicking problem — your real fear, and how it is actually solved

You are right that 1/16 bits would be unclickable if every bit were its own target:
a 62.5 mm cube is smaller than the crosshair. **But that is not how sub-block
building works.** The standard solution — used by LittleTiles, Chisels & Bits, and
by your own MAKET-panel idea in `CHARTER.md` §1 — is:

> **Click the block, act on the bit.**

- The **hitbox the player aims at is the whole 1 m block** — generous, easy to hit,
  exactly like a normal block. Minecraft's ray-trace gives you the block and the
  *exact point inside it*; the mod converts that point to a bit coordinate and acts
  on **that bit**. You never aim at a 62.5 mm box.
- **Interaction is mode-based.** The `ChiselItem` already in the repo switches the
  player between "place/break a bit" and "use the machine." So you do not click a
  bit to press a button; you click the **block** (or a named control point) to use
  the machine.
- **Control rooms don't use bits at all.** `CHARTER.md` §1 already decided this:
  panels are *"a high-resolution panel picture with each control a named, clickable
  point."* A control is a **hot-point on a picture**, not a tiny block. This is the
  MAKET approach and it sidesteps the tiny-hitbox problem completely.

So the fear — "bit-scale blocks get very hard to click on" — is real for a naive
implementation and **already solved by the design the project has chosen.**

---

## 6. The three options, side by side

| | A — keep 1:4 display | **B — 1:1 + 1/16-bit core** (recommended) | C — finer/different engine |
|---|---|---|---|
| Physics at 1:1 | yes (already) | **yes** | yes |
| Build verifiable against a real drawing | **no** | **yes** | yes |
| Avatar correct size | no (4× small) | **yes** | yes |
| Block cost of the core | ~48,600 | **~765** | — |
| Work to get there | none | implement the bit engine | huge |
| Clicking | fine | **fine (block-click, bit-act)** | — |

**Option C is not needed.** The only feature finer than 62.5 mm is the individual
13.6 mm fuel rod — and the physics *homogenises the fuel assembly inside the
pressure tube anyway* (see `openmc/rbmk_cell.py`: the 18 rods are homogenised into
the bore). So you do **not** need individually-drawn rods for the build to be true.
C is the answer only if you wanted to see single fuel rods, which the physics does
not require.

**Recommend Option B.** It is already the project's stated standard
(`PROJECT_NOTES.md` — *"Bit-Scale ... the default and mandatory standard"*), it is
*cheaper* than the current 1:4 build, and it is the only one that makes the build
measurable against the design — which is the whole point of the field.

---

## 7. The honest tolerances (nothing hidden)

Per the charter rule "nothing guessed — cite the source or mark unknown":

- **The lattice is exact** at 1/16 (4 bits = 250 mm). This is the number the whole
  core hangs off, so this matters most, and it is exact.
- **The tube bore (88 mm = 1.4 bits) is approximate** — it rounds to 1–2 bits. If
  you want the 88 mm bore exact, you need **1/32** (31.25 mm bits → 88 mm ≈ 3 bits),
  at 32³ = 32,768 bits per block — heavier, and not required by the physics (which
  homogenises the bore). **Flagged as a tolerance, not hidden.**
- **The core diameter vs channel count** is the one open item already recorded in
  `RBMK-CORE-GEOMETRY.md` §1a: 1,872 channels do not quite fit a Ø11.8 m circle at
  250 mm pitch. That is a *sourcing* question (pin it from a primary core drawing),
  and it is independent of the display scale.

---

## 8. What it costs to do (so this is a plan, not a wish)

The bit engine is the one real piece of new work. It is **not** a rewrite:

1. **The core's truth already exists** — `RbmkCore` is the data, at full
   per-channel/per-node resolution, independent of blocks. Nothing there changes.
2. **Implement `ChiseledBlockEntity`** to hold a 16³ bit grid (a palette + 4-bit
   indices ≈ 8 KB per block, or sparse). The class and its block/renderer stubs
   already exist; the renderer is empty (`// Rendering logic to follow`).
3. **Map the lattice to bits**: 1 lattice cell = 4×4 bits; 16 channels per block.
4. **Block-click, bit-act** interaction (the `ChiselItem` is the hook).
5. **Rebuild the core** through `RbmkWorldBuilder` at 1:1 instead of 1:4 — this is
   the `METRES_PER_BLOCK` change from 0.25 to 1.0, plus the bit layer.

Steps 1, 3 and 5 are small. Step 2 is the real work. Step 4 is the clicking fix.

---

## 9. One line for the record

**Minecraft can hold a true 1:1 RBMK-1000: build the plant in 1 m blocks, build the
core in 1/16 bits (4 bits = the real 250 mm lattice), click the block and act on the
bit. The physics already runs at 1:1 and cannot be affected by the display — so the
reaction was never at risk; only the drawing was, and this fixes it.**
