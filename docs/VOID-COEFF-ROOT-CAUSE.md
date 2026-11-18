# The void-coefficient discrepancy: what was actually wrong (2026-11-17)

## The headline

The "5-7x" was real *as a ratio between two of our own tools*, and **both tools
were wrong, in opposite directions**. With the errors fixed the two independent
physics models agree, and the truth sits between them.

## The evidence

The gold standard is an **explicit 18-rod cluster** run in OpenMC Monte Carlo (no
homogenisation, no diffusion) with the tube-to-graphite gap filled with helium, as
`docs/RBMK-CORE-GEOMETRY.md` says it should be. `openmc/decompose_test.py` compares
it against the repo's homogenised cell, one change at a time, all on ENDF/B-VIII.0
at 600 K:

| Config | Model | dk/dvoid (0 -> 0.3) |
|---|---|---|
| A | repo's OpenMC cell, as written | **+0.017** |
| B | repo cell + corrected volume fractions | +0.056 |
| C | real geometry (88 mm tube + He gap) + repo fractions | -0.014 |
| D | real geometry + corrected fractions | **+0.038** |
| E | **explicit 18-rod cluster (gold standard)** | **+0.032** |

Configs D and E — two independent methods — agree to 19%. That is the answer: the
corrected lattice void worth is about **+0.03 to +0.04**.

## What was wrong

**The repo's OpenMC cell (config A) was ~2x too low** (+0.017 vs +0.032). Two
geometry errors, both fixed in `openmc/rbmk_cell.py`:

1. The homogenised channel used **fuel 0.62 / water 0.26**, but the real 88 mm
   channel is **fuel 0.324 / clad 0.106 / water 0.570** — about twice the fuel and
   half the water. (Areas inside the 88 mm bore: tube 60.8 cm2, fuel 19.7, clad 6.5,
   water 34.7.)
2. It used an **80 mm bore**; the tube is **88 mm**. The 80 mm figure is the fuel
   *assembly* width — `docs/RBMK-CORE-GEOMETRY.md` §2, line 78.

**The deterministic 1-D cell is ~3.3x too high** (+0.115 vs the corrected OpenMC's
+0.035). Its method — 1-D diffusion on a homogenised annular ring — overstates the
void worth.

So the old "5-7x" was the ratio 0.115 / 0.017 = 6.8x: two errors pointing opposite
ways. Fixing both collapses it to a 3.3x gap that is a real (and explainable)
method difference, not a bug.

## The corroboration claim was circular — withdrawn

`provenance.json` claimed the OpenMC model "independently corroborates the INSAG-7
value of +0.02". It did not. The model only *happened* to give +0.02 because the
geometry errors pulled it down to the literature number. Two wrongs matched. The
claim is removed.

## Rules that were cleared (so they are not re-tried)

* **Missing thermal scattering law** (S(alpha,beta)) — toggling it in OpenMC moved
  the void worth the *wrong* way (+0.022 -> +0.0003). Not the cause
  (`openmc/thermal_test.py`).
* **Temperature** (600 K vs cold) and **water density** — worth ~+0.013
  (`openmc/matched_test.py`). Minor.
* **The gap medium** — early probes that filled the tube-to-graphite gap with
  *water* gave inflated numbers (+0.13, +0.16). The gap is **helium**; with helium
  the numbers fall to the table above.

## Note on conventions (important)

The repo's OpenMC script reports the coefficient as **reactivity**, dk/(k1*k2)
(`_reactivity` in `rbmk_void_coeff.py`), which is slightly smaller than the simple
dk/k slope. Shipped, the corrected cell gives:

* `void_coeff_operating` = **+0.021** (reactivity convention — the field the mod reads)
* the same data as a simple dk/k slope = **+0.035**

Both are the same physics; the factor is ~1.6. Quote whichever, but say which.

## The plant is unchanged and still safe

None of this moves the reactor. The plant's void worth is anchored to the
authoritative INSAG-7 working-point value (+0.020), and the core cannot run away
(enforced by a self-test). Only the *diagnostic* OpenMC cell was wrong, and only
its provenance note needed correcting.

## Still open (not hidden)

The corrected lattice value (~+0.035 simple slope) is ~1.75x the INSAG-7
**whole-core operating** coefficient (+0.020). The likely reason: a single-channel
infinite lattice has no control rods, no leakage, and voids only one channel, so it
measures a larger quantity than the whole-core operating coefficient. That is
plausible but **not proven here** — it is the natural next question.

## Reproduce

    source /workspace/tools/omc/env.sh
    cd openmc && python decompose_test.py       # the 2x2 + explicit cluster
    python rbmk_void_coeff.py --force           # regenerate the shipped result
    cd ../tools/lattice && python compare_curves.py
