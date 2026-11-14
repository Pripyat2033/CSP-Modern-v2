# THE SCALE DECISION — how 1:1 is actually built (bit-scale, not metre blocks)

Written for Ben, 2026-11-13, after the Director corrected the first draft of this
document. **The first draft was wrong and is replaced.** It said "1:1 scale = 1
block = 1 metre" and proposed a "1 m block shell." That is the forbidden method
(`PROJECT_NOTES.md`: bit-scale is "the default and **mandatory** standard";
Block-Scale is "**forbidden on the main ChNPP site**"). This version is the
corrected one.

---

## 1. What "1:1" actually means here

**1:1 scale does NOT mean "1 Minecraft block = 1 metre."** It means:

> **The build matches the real ChNPP floorplans and specifications exactly —
> including the plant's 6-metre construction grid — so that the structure you walk
> through *is* the structure on the drawing.**

The metre is a unit of *measurement*, not a unit of *construction*. The unit of
construction here is the **bit**: **1/16 of a block = 62.5 mm**. A 1-metre
Minecraft block is just the container the engine groups **16 × 16 × 16 bits** into
(1,000 mm / 16 = 62.5 mm per bit).

**Why it cannot be done in 1 m blocks:** the plant's working parts are smaller than
a metre. The RBMK fuel channel is **88 mm** across and sits on a **250 mm**
lattice. A 1 m block is 1,000 mm — you cannot place a 250 mm pitch, or an 88 mm
bore, or the internal divisions of a 6 m grid, with a 1,000 mm tool. It is not a
matter of taste; it is geometrically impossible. That is exactly why bit-scale
exists.

---

## 2. The two things that were being confused

| Term | Wrong reading (first draft) | Correct meaning |
|---|---|---|
| **1:1 scale** | 1 block = 1 m; build everything from 1 m blocks | The build matches the real drawings and the 6 m grid **exactly** |
| **Bit-scale** | a "detail layer" on top of metre blocks | **The construction unit for the whole site.** Everything is bits. |

There is **one** scale (1:1, true size) and **one** construction unit (the bit).
The "metre block" is an artefact of the engine's storage grid, nothing more.

---

## 3. The arithmetic — what 1:1 in bits looks like

One bit = **62.5 mm**. The real figures then land as follows:

| Feature | Real size | In bits (1/16) | Exact? |
|---|---|---|---|
| Lattice pitch (cell) | 250 mm | **4 bits** | **exact** |
| Channel bore (ТК) | 88 mm | 1.41 bits | ≈1–2 bits (see §6) |
| Graphite block hole | 114 mm | 1.82 bits | ≈2 bits |
| **Plant construction grid** | **6 m** | **96 bits** (= 6 blocks) | **exact** |
| Active core diameter | 11.8 m | 188.8 bits (= 11.8 blocks) | exact |
| Active core height | 7 m | 112 bits (= 7 blocks) | exact |
| Graphite stack | Ø14 m × 8 m | 224 × 128 bits | exact |
| Control-rod displacer | 4.5 m | 72 bits | exact |

**So at 1:1 bit-scale the core is ~11.8 blocks across and 7 blocks tall in the
engine's storage grid — and every one of its internal features is drawn at
62.5 mm.** The 6 m grid of the building lands exactly on 6 blocks (96 bits), which
is why the plant's modular construction and the bit grid fit each other cleanly.

The current build is **1:4** (`RbmkGeometry.METRES_PER_BLOCK = 0.25`): there one
block = 250 mm, so one lattice cell = one block and the core is 47 blocks across.
That is the coarse stand-in, and it is what must be replaced by the bit build.

---

## 4. The physics question, answered (unchanged from the first draft)

Your worry — *"a wrong scale could compromise the reaction"* — is the right worry
to have, and the answer is that it is **already prevented**, because the reaction
is computed from metres and material fractions, never from block counts:

- **`RbmkGeometry`** holds the real SI numbers and derives every `*_BLOCKS` from
  them via `toBlocks()`.
- **`RbmkSolver3D`** builds its mesh from the **lattice-cell** counts
  (`CHANNEL_REGION_DIAMETER_CELLS`, `CORE_HEIGHT_CELLS`) — real metres divided by
  the real 250 mm pitch, **not** from a block count.
- **`RbmkCore`** says it outright: *"the core is data, not geometry — so its
  granularity does not depend on how many blocks the world spends on it."*
- Only **`RbmkWorldBuilder`** reads `*_BLOCKS`. The solver and core never do.

> **Corrected 2026-11-14.** This section previously claimed the physics "already
> runs at 1:1 and cannot be affected by the build". That was **not true of the
> code as it stood**: the physics mesh was built from the `*_BLOCKS` constants,
> which are derived from `METRES_PER_BLOCK`. That is correct only while
> `METRES_PER_BLOCK == LATTICE_PITCH_M` (true at 1:4 today, a coincidence), and
> §7's instruction to "set `METRES_PER_BLOCK = 1.0`" would have **silently
> collapsed the mesh 4x** — 49 cells → 12, 28 axial nodes → 7, 52,416 active nodes
> → 13,104, an effective mesh pitch of 942 mm instead of 250 mm. The physics would
> have been wrong, the opposite of what this section promised.
>
> The trap is now closed: `RbmkGeometry` carries explicit `*_CELLS` constants
> (`CORE_HEIGHT_CELLS` = 28, `CHANNEL_REGION_DIAMETER_CELLS` = 49,
> `STACK_DIAMETER_CELLS` = 56, `CORE_DIAMETER_CELLS` = 47) computed from metres /
> pitch, and the solver, `RbmkCore`, `RodWorth` and `RbmkLattice` read those. The
> numbers are identical today, so nothing changed in behaviour; but the build
> scale can now move to 1:1 without touching the physics.
> `ScaleIndependenceSelfTest` pins it: it passes at `METRES_PER_BLOCK` = 0.25
> **and** 1.0. This is exactly the class of hidden simplification the SGB rule
> exists to forbid.

---

## 5. The real problem to solve is storage, rendering and clicking

This is the honest engineering question, and it is solvable. A whole power station
at 62.5 mm resolution is a very large number of bits, so the design must be clever
about *storage* even though the *resolution* is fine:

1. **Sparse storage, not dense.** Store only bits that exist. A building is a
   **shell**, not a solid: a 100 m × 60 m × 60 m hall is its walls, floors and
   roof — surface bits — not 1600³ filled cells.
2. **Boxed / run-length solids.** A large mass of one material (a concrete wall, a
   foundation block) is stored as **one box entry**, not millions of identical
   bits. The resolution stays 62.5 mm for *measurement and rendering*, while the
   *storage* is one record. This is what makes a full-scale plant affordable.
3. **Per-block bit palettes** (like a chunk palette): each 1 m block holds a small
   palette + 4-bit indices, so a block is a few KB, not 16³ objects.
4. **Level-of-detail rendering.** Distant bits render as a merged surface; only
   near bits render individually. This is standard and keeps the frame cost down.

**Clicking — your real fear — is solved the standard way:** *click the block, act
on the bit.* The player aims at the **whole 1 m block** (a generous, normal
hitbox); Minecraft's ray-trace reports the exact point inside it, which the mod
converts to a bit coordinate. You never aim at a 62.5 mm cube. And control rooms
do not use bits for interaction at all: `CHARTER.md` §1 already decided panels are
*"a high-resolution picture with each control a named, clickable point"* — the
MAKET approach, no tiny hitboxes anywhere.

---

## 6. Honest tolerances (nothing hidden)

Per the charter rule "nothing guessed — cite the source or mark unknown":

- **The lattice (250 mm = 4 bits) is exact** at 1/16. This is the number the whole
  core hangs off, so it matters most, and it is exact.
- **The 6 m construction grid is recorded as the Director's specification.** It
  lands exactly on 96 bits / 6 blocks. *(Sourcing to a construction drawing is
  pending — Tavily is over quota — and is flagged, not asserted as verified.)*
- **The 88 mm bore (1.41 bits) rounds to 1–2 bits at 1/16.** If the bore must be
  exact, **1/32** (31.25 mm bits → 88 mm ≈ 2.8 bits) is required — heavier, and
  **not required by the physics**, which homogenises the fuel assembly inside the
  tube (`openmc/rbmk_cell.py`). Flagged as a tolerance.
- **The core-count-vs-diameter item** (1,872 channels vs Ø11.8 m at 250 mm pitch)
  stays open in `RBMK-CORE-GEOMETRY.md` §1a. That is a **sourcing** question (pin
  it from a primary core drawing), independent of the build method.

---

## 7. What this means to do next

The bit engine is the whole game, and it is a **stub** today:
`ChiseledBlockEntity` holds no bit storage, and `ChiseledBlockEntityRenderer.render()`
is empty (`// Rendering logic to follow`). The order of work:

1. **Bit storage** in `ChiseledBlockEntity` — sparse, paletted, with boxed solids
   (§5.1–5.3).
2. **Bit renderer** — LOD, so a full-scale plant stays cheap to draw.
3. **Block-click / bit-act** interaction via `ChiselItem`.
4. **Build the core at 1:1** through `RbmkWorldBuilder` — set
   `METRES_PER_BLOCK = 1.0` (true world scale, correct avatar) and lay the lattice
   in 4-bit cells.

The physics needs **no** change (it is already 1:1). The build method is the whole
of the work.

---

## 8. One line for the record

**1:1 means the build matches the real drawings and the 6 m grid exactly; the unit
of construction is the bit (62.5 mm), never the metre block. The RBMK cannot be
built from 1 m blocks — its fuel channel is 88 mm on a 250 mm lattice — so the
whole site is bit-scale, as `PROJECT_NOTES.md` already mandates. The physics
already runs at 1:1 and cannot be affected by the build; the work is the bit
engine (storage, render, click).**
