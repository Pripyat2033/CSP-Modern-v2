# RBMK-1000 CORE GEOMETRY — pre-1986 (Unit 4 generation)

**Status: research note, for the Director's review. Not yet a build spec.**
Every figure below is either cited or explicitly marked **UNKNOWN / TO VERIFY**,
per the charter rule "nothing guessed — cite the source or mark unknown".

This note answers the Director's question: the real width, height and length of the
fuel channels (ТК), the control-rod channels (СУЗ) and the coolant path — so we
understand the core's actual shape **before** any multiblock is designed.

---

## 0. Terms

| Term | Russian | Meaning here |
|---|---|---|
| TK | **ТК**, технологический канал | technological (fuel) channel — the pressure tube carrying one fuel assembly and the boiling coolant |
| TVS | **ТВС**, тепловыделяющая сборка | fuel assembly (the thing loaded into a TK) |
| SUZ | **СУЗ**, система управления и защиты | control and protection system; the control-rod channels |
| CPS | — | English for СУЗ |
| Lattice | — | the square grid of channels through the graphite |
| CPS channel | — | the separate channel type that takes a control rod |

---

## 1. The lattice — the single most important number

The whole core is a **square lattice at 250 mm (25 cm) pitch**. The graphite is
built from square-section blocks laid on that grid, each block pierced by one
vertical hole.

- **Graphite moderator block**: **250 × 250 mm** cross-section (the plane
  perpendicular to the channels); longitudinal lengths **200–600 mm** depending on
  position in the stack. *(EN Wikipedia, RBMK — "The moderator blocks ... are 25 by
  25 centimetres ... with several longitudinal dimensions of between 20 centimetres
  and 60 centimetres")*
- **Hole through each block**: **114 mm (11.4 cm) diameter** through the block's
  longitudinal axis, for the fuel and control channels. *(EN Wikipedia — "holes of
  11.4 centimetres diameter")*
- **Coolant/reflector annulus**: the pressure tube sits in that hole with a small
  clearance, held by alternating split graphite rings (see §2).

> **THE BUILDABILITY WALL — and how the bit engine clears it.** One lattice cell is
> **250 mm**. A 1 m block is 1,000 mm, so a cell cannot be placed with a 1 m tool,
> and the ТК/СУЗ channels are **not** 1 m³ blocks — this is the crux of the
> Director's objection, and he is right. The answer is the **bit**: at 1/16 block =
> **62.5 mm**, one cell is **4 bits** (exact), the 88 mm bore is ~1.4 bits, and the
> plant's **6 m grid** is 96 bits. The whole site is therefore built bit-scale
> (as `PROJECT_NOTES.md` mandates); the 1 m block is only the engine's storage
> container for 16³ bits. See `SCALE-DECISION.md`.

### 1a. The lattice arithmetic — an OPEN ITEM (marked, not hidden)

Second-generation cores (ChNPP 3 & 4) are stated as **1,661 fuel channels + 211
control channels = 1,872 channels**. At a clean 250 mm pitch:

| figure | value |
|---|---|
| area of 1,872 cells @ 0.25 m | 1,872 × 0.0625 = **117.0 m²** |
| circle diameter that needs | **≈ 12.2 m** |
| stated active core diameter | **11.8 m** → 109.4 m² → **1,750 cells @ 0.25 m** |

So **1,872 channels do not fit a Ø11.8 m circle at 250 mm pitch** — the implied
effective pitch is **≈ 242 mm**, or the active diameter is really ~12.2 m.
**TO VERIFY** against a primary core drawing (the Unit 4 longitudinal and transverse
sections in the design documentation / NIKIET material). Until then: treat
"250 mm block" and "Ø11.8 m active zone" as the two given figures and the exact
in-circle channel count as **UNKNOWN**. Do **not** silently pick a pitch to make the
count work.

---

## 2. The fuel channel (ТК) — the pressure tube

The "technological channel" is the pressure tube that carries one fuel assembly and
the boiling water. It is a **vertical tube through the whole stack**, not a block.

- **Inner diameter**: **88 mm (8.4 cm)** — the tube the fuel assembly sits in.
  *(EN Wikipedia — "each fuel assembly is enclosed in an individual 8.4 cm (inner)
  diameter pipe (called a 'technological channel')" and "... 8.4 cm in inner diameter
  with 4 mm thick walls")*
- **Wall**: **≈ 4 mm**.
- **Material**: the **central (in-core) section is zirconium–niobium alloy
  Zr + 2.5 % Nb**; the **upper and lower sections are stainless steel**; the two are
  joined by welded transition pieces. *(RU Wikipedia, РБМК — "Центральная часть ...
  изготовлена из сплава циркония с ниобием (Zr + 2,5 % Nb) ... верхние и нижние
  части ... из нержавеющей стали ... соединены сварными переходниками")*
- **Length**: the tube runs the full height of the stack and out both ends into the
  upper and lower biological shields / standpipes — i.e. **many metres, not 7 m**.
  The *active* (fuelled) length it must span is **7 m** (the core height). The exact
  tube length is **TO VERIFY**; it is governed by the stack (8 m) plus the UBS/LBS
  standpipe penetrations (see §5).
- **Held in the stack** by two alternating types of **20 mm-high split graphite
  rings**: one touching the tube with **1.5 mm clearance** to the graphite, the other
  touching the graphite with **1.3 mm clearance** to the tube. *(EN Wikipedia)*
- **Coolant**: boiling light water, single circuit (КМПЦ). Feedwater → **22 group
  distribution headers**, each feeding **38–41 pressure channels**. Saturated steam
  ~**284 °C** to the drum separators. *(EN + RU Wikipedia)*

### 2a. The fuel assembly (ТВС) inside the channel

- **Two sub-assemblies** per channel — a **lower and an upper** — joined by a central
  cylinder. *(RU Wikipedia — "кассета, составленная из двух тепловыделяющих сборок
  (ТВС) — нижней и верхней")*
- **18 fuel rods (твэл) + 1 carrier rod** per sub-assembly. *(EN Wikipedia)*
- **Total assembly length 10.025 m**, of which **6.862 m is the active region**
  (i.e. the two sub-assemblies together give the ~7 m active core height).
  *(EN Wikipedia)*
- **Fuel rod**: **Ø13.6 mm outer, 0.825 mm wall**, **zircaloy + 1 % niobium**;
  **3.64 m long**, **3.4 m active**; **3.5 kg of pellets** each; **10 stainless-steel
  spacers at 360 mm**; filled with **helium at 0.5 MPa**. *(EN Wikipedia)*
- **Carrier rod**: **Ø13 mm outer**. *(EN Wikipedia)*
- **Pellet**: **UO₂, Ø11.5 mm × 15 mm**, with a **2 mm central hole**; hemispherical
  ends. **Enrichment 2 %** (1980), **0.4 %** for the end pellets; **Er₂O₃**
  burnable poison introduced later. *(EN Wikipedia)*
- **Total uranium in an assembly: 114.7 kg.** Total fuel under stationary
  conditions: **192 t**. *(EN Wikipedia)*

**So, dimensionally, the fuel assembly is a ~10 m long, ~80 mm wide bundle — a
pencil, not a cube.** In plan it is ~0.08 m across, sitting in a 0.25 m lattice cell.

---

## 3. The control-rod channel (СУЗ) and the rod

The control channels are the **same lattice geometry** (250 mm cell, Ø114 mm block
hole), but they take a rod instead of fuel. This is why a control rod can "control"
the TKs around it.

- **Count**: **211 movable control rods** (2nd generation; 170 in the 1st).
  *(EN Wikipedia — "The RCPS system consists of 211 movable control rods"; "Second
  generation reactor cores ... have 1661 fuel channels and 211 control rod channels")*
- **Insertion direction**: **the majority from above**; **24 shortened rods from
  below** (УСП, укороченные стержни-поглотители) for axial power shaping.
- **Rod composition (the tip effect)**: **4.5 m graphite displacer** + **1.25 m
  telescope** (a water-filled gap) + a **boron-carbide (B₄C) absorber section**.
  *(EN Wikipedia — "the control rods have a 4.5 m long graphite section at the end,
  separated by a 1.25 m long telescope ... and a boron carbide neutron absorber
  section")*
- **When fully retracted**: the graphite displacer sits in the **middle of the core
  height, with 1.25 m of water at each end**. *(EN Wikipedia)*
- **Speed**: **≈ 0.4 m/s**, i.e. **18–21 s** for full travel. *(EN Wikipedia)*

> **This is the "positive scram" mechanism, and it is pure geometry**: withdrawing a
> rod pushes its graphite displacer up through the bottom of the core, **displacing
> water (a weak absorber) with graphite (almost none)** — a local *rise* in
> reactivity before the B₄C arrives. A core model that does not carry these three
> axial sections at the right lengths **cannot reproduce the accident**.

- **SUZ rod census** (2nd gen, RU Wikipedia — useful for the drive/renderer):
  АЗ emergency 24; РР manual 119; УСП shortened 24; АР automatic 12 (later 8);
  ЛАР local automatic 12; ПК-АЗ over-compensation 24.

---

## 4. Static absorbers

- **~240 additional boron-based static absorbers** are inserted at initial core
  loading and **gradually removed with burnup**. *(EN Wikipedia)*
- Consequence: the **void coefficient runs from negative (all absorbers in) to
  positive (all removed)** — the accident happened with the core near the positive
  end. *(EN Wikipedia)*

---

## 5. Core envelope and surrounding structures

| Structure | Dimension | Source |
|---|---|---|
| Active core | **Ø 11.8 m × H 7 m** | EN + RU Wikipedia |
| Graphite stack (cylinder) | **Ø 14 m × H 8 m**, ~**1,700 t** graphite | EN Wikipedia |
| Reactor vessel | annular steel cylinder, **inner Ø 14.5 m × H 9.7 m**, wall 16 mm, N₂-filled | EN Wikipedia |
| Annular water tank (outer) | wall 30 mm, **inner Ø 16.6 m, outer Ø 19 m**, 16 vertical compartments | EN Wikipedia |
| Upper biological shield (UBS, "Schema E") | **Ø 17 m × 3 m**, ~2,000 t, serpentinite concrete + steel-graphite plugs, one plug per channel | EN Wikipedia |
| Lower biological shield (LBS) | **Ø 14.5 m × 2 m** | EN Wikipedia |
| Thermal power (Unit) | **3,200 MWt / 1,000 MWe** — *widely cited; verify against IAEA PRIS before use* | **TO VERIFY** |

---

## 6. Channel census (pre-1986, Unit 4 generation)

| | 1st generation | **2nd generation (ChNPP 3 & 4)** |
|---|---|---|
| Fuel channels (ТК) | 1,693 | **1,661** |
| Control-rod channels (СУЗ) | 170 | **211** |
| Total lattice cells (if all filled) | 1,863 | **1,872** |

*(EN Wikipedia — "There are 1693 fuel channels and 170 control rod channels in the
first generation ... Second generation ... have 1661 fuel channels and 211 control
rod channels")*

> **Director's note "one rod per 10 TKs":** 211 rods over 1,661 fuel channels is
> **≈ 1 rod per 8 fuel channels**, close to the recalled figure; the physical basis
> is the rod's sphere of influence across the 250 mm lattice. The precise rod
> pattern (which channels are control channels, and the AZ/РР/АР/УСП layout) is
> **TO VERIFY** from a core map — it matters for the spatial kinetics.

---

## 7. Scale table — real mm to Minecraft, correctly stated

Because the channels are sub-block, the **construction unit** must be chosen once
and applied everywhere (core, cooling, drives, renderer).

**The correction (Director, 2026-11-13).** "1:1" does **not** mean "1 block = 1
metre." A 1 m block is the engine's *storage* container only; the *construction*
unit is the **bit** = 1/16 block = **62.5 mm**. At 1:1 the build matches the real
drawings and the plant's **6 m construction grid** exactly. The table below is the
corrected one: the middle column is the true 1:1 build, expressed in bits.

| Feature | Real size | @1:1 bit-scale (1 bit = 62.5 mm) | @1:4 (coarse stand-in, today) |
|---|---|---|---|
| Lattice pitch | 250 mm | **4 bits** (exact) | 1 block |
| Channel bore (ТК) | 88 mm | ~1.4 bits (→1–2) | 0.35 block |
| Graphite block hole | 114 mm | ~1.8 bits (→2) | 0.46 block |
| **Plant construction grid** | **6 m** | **96 bits = 6 blocks** (exact) | 24 blocks |
| Active core diameter | 11.8 m | 188.8 bits ≈ 11.8 blocks | 47 blocks |
| Active core height | 7 m | 112 bits = 7 blocks | 28 blocks |
| Fuel assembly length | 10.025 m | 160 bits ≈ 10 blocks | 40 blocks |
| Graphite stack | Ø14 × 8 m | 224 × 128 bits | 56 × 32 |
| Graphite displacer | 4.5 m | 72 bits | 18 blocks |
| Telescope gap | 1.25 m | 20 bits | 5 blocks |

**Reading the table:** at **1:1 bit-scale** one lattice cell is **4 bits**, one 1 m
block holds a 4×4 grid of cells (16 channels), and the active core is **~11.8
blocks across × 7 tall**. Every internal feature is drawn at 62.5 mm, so the build
can be measured against the real drawing. The 1:4 column is the current coarse
stand-in (one cell = one whole block), which is **not** verifiable at true scale
and is to be replaced by the bit build.

> The 88 mm bore is the one feature that rounds (1.4 bits → 1–2). If it must be
> exact, use 1/32 (31.25 mm bits). The physics does not require it — it homogenises
> the assembly inside the tube. See `SCALE-DECISION.md` §6.

---

## 8. What this means for the Director's question

1. **The channels are not cubes.** TK = Ø88 mm tube; СУЗ = same lattice, rod inside;
   fuel = a 10 m × 80 mm pencil. At 1:1 that is a 4-bit cell with a ~1.4-bit tube —
   why the build must be bit-scale, not metre blocks.
2. **The core is big in one axis and small in the other.** Active region is a
   **wide, shallow cylinder** (11.8 m across, 7 m tall) — unlike a PWR's squat vessel.
   This shape is forced by the channel refuelling design and is why the stack is
   8 m while the fuel assembly is 10 m.
3. **The subsystems are coupled through geometry, exactly as the Director says:**
   the lattice pitch fixes the renderer (bit-scale), the logic (per-channel
   state), the physics (spatial coupling across the lattice) and the coolant circuit
   (22 headers × 38–41 channels) all at once. Changing the pitch changes all four.
4. **The correct build is 1:1 bit-scale**: the real 250 mm lattice as 4-bit cells,
   the whole site on the 6 m grid, verifiable against the design drawings. It is
   **not** built from 1 m blocks (forbidden on the main site). The physics is
   already 1:1 and unaffected by the build.

---

## 9. Sources

- **English Wikipedia, "RBMK"** — the detailed design section (lattice, blocks, tube,
  fuel assembly, control rods, shielding, channel counts). Retrieved 2026-11-09.
- **Russian Wikipedia, "Реактор большой мощности канальный"** — graphite stack
  Ø/H, pressure-tube materials, two sub-assemblies per channel, СУЗ rod census.
  Retrieved 2026-11-09.
- **Open / TO VERIFY**: IAEA PRIS and NIKIET design documentation for the exact
  active-zone diameter vs channel count, the pressure-tube full length, and the
  thermal power. These are the figures to pin from primary sources before any
  build spec is frozen.
