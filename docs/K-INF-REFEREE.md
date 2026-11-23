# The fresh-cell k_inf: an OpenMC referee says the in-house number is too high

Written 2026-11-18. This answers the question "is 1.34442 too high against the
shipped 1.2893, and what does it mean if we do it this way".

Short answer: **the shipped 1.2893 is stale, the current 1.3445 is the same code's
newer output, and BOTH are too high.** An independent continuous-energy Monte
Carlo model of the same cell lands at **1.19 ± 0.01**, i.e. roughly **9,000 to
19,000 pcm lower**. That is 170–350 equivalent manual rods of reactivity.

Everything below is reproducible: `tools/lattice/omc_referee.py`.

---

## 1. Why the two in-house numbers differ (this part is settled)

`1.2893` is not a different physical model from `1.3445`. It is the *same*
solver on an older commit. `git show 079d2fc:tools/lattice/rbmk_spectrum.py`
predates commit `4ed6660`, which fixed a missing-MT4 artifact in the inelastic
scattering. Running today's code on the same input gives:

| path | k_inf |
|---|---|
| `spec.build_fine(weight_combined, 0)` + `solve_k`, ENDF/B-VIII.0 | **1.34450** |
| same, ROSFOND-2010 | **1.33626** |
| `src/main/resources/rbmk_lattice.json` row 0 (shipped, ROSFOND) | **1.28932** |

So the "1.34442 vs 1.2893" question is not a physics question. The doc value in
`docs/ROSFOND-SWAP-MEASURED.md` was never refreshed after the MT4 fix, and the
`RodBank.java` comment quoting 1.2893 is quoting that stale figure. **The shipped
lattice table the game actually runs on is 1.2893, which is now known to be
superseded by the repo's own code.**

## 2. Why both are too high (this is the real finding)

The in-house solver is a 69-group diffusion-like solve. Its single most sensitive
input is the Bondarenko resonance self-shielding, and nothing in the repo checks
it. Measured directly, by scaling the Dancoff factor:

| Dancoff | sigma0 (b) | k_inf |
|---|---|---|
| 0.0 | 41.65 | 1.30661 |
| 0.3 | 29.16 | 1.32317 |
| **0.6 (shipped)** | **16.66** | **1.34450** |
| 0.9 | 4.17 | 1.37968 |

And with self-shielding switched off entirely: **k_inf = 0.70494**. A 64,000 pcm
swing on one unvalidated parameter. The in-house answer is therefore not a
physics result, it is a statement about `dancoff() = 0.60`.

Two candidate causes were tested and **ruled out**:

* **Group-averaging resolution.** `endf.group_average` samples 24 points/group.
  Raising it to 128 / 512 / 2048 changes k_inf by 42 / 33 / 31 pcm. Not the cause.
* **Data quality.** The cached U-238 capture resonance integral integrates to
  **274.5 b** against the textbook dilute value of **277 b**; the 6.67 eV
  resonance peak is 7,160 b in the cache. The data is correct.

## 3. The referee

`omc_referee.py` builds the cell in OpenMC 0.16.0 with ENDF/B-VIII.0, six
reflective boundaries, and reports k_inf. Two models:

* `--model homog` — homogenise the cell using **rbmk_cell's own atom densities and
  volume fractions**. Isolates the group constants from the geometry.
* `--model hetero` — the explicit 18-rod + carrier cluster in the pressure tube.

Results (15000 particles, 100 batches, half inactive; ±1 sigma statistical only):

| model | k_inf |
|---|---|
| homog (repo composition) | **1.07494** |
| hetero, ring 1.45/2.75 (tight) | **1.15093** |
| hetero, ring 1.80/3.20 (nominal) | **1.19207 ± 0.00100** |
| hetero, ring 2.00/3.70 (loose) | **1.22487** |

Three independent seeds on the nominal model: 1.19207 / 1.19162 / 1.19281. The
number is converged; the spread is the geometry assumption, not statistics.

### The dominant uncertainty is an undocumented dimension

The 18 fuel rods' **ring radii are not published** in any document we hold. The
nominal 1.80 / 3.20 cm is chosen so 18 rods plus the carrier fit the 88 mm bore
without overlap; it is not sourced. It moves k_inf across **7,400 pcm** (1.151 to
1.225), which is larger than the in-house solver's entire self-shielding
uncertainty.

**An earlier version of this section said the fix was to "hunt the ring radii in
the техническое описание". That advice was wrong on two counts, and both are worth
recording because they will recur.**

First, **we do not hold a техническое описание**. The primary documents in the
repository are INSAG-7, the 1983 operating regulation (Регламент 1Э-С-II), and a
transcribed NIKIET book excerpt on *control-rod* geometry. None of them contains
the fuel-bundle drawing. So "hunt it in the ТО" was advice to search a document
that does not exist here.

Second, **the radii are largely derivable, so they do not need a document.** A
bundle of 6 inner + 12 outer rods built with common spacers has its nearest
neighbour distances set by geometry:

- inner–inner chord (6 rods) = 2 r1 sin 30° = **r1**
- outer–outer chord (12 rods) = 2 r2 sin 15°
- inner–outer distance at a 15° offset = sqrt(r1² + r2² − 2 r1 r2 cos 15°)

Setting all three equal gives **r2 = 2 r1 cos 15° = 1.93185 r1** — a scale-free
ratio. The bore then pins the scale (outer rod must clear the wall, inner rods
must not touch). On the 88 mm bore that is a **one-parameter family**:

| constraint | value |
|---|---|
| non-overlap (inner rods clear each other) | r1 ≥ 1.360 cm |
| wall clearance (outer rod inside bore) | r1 ≤ 1.926 cm |
| uniform-spacing ratio | r2 = 1.93185 r1 |

Measured across that family:

| case | k_inf |
|---|---|
| family, tight r1 = 1.360 / r2 = 2.627 | **1.13755 ± 0.00101** |
| the arbitrary 1.80 / 3.20 guess | 1.19207 ± 0.00100 |
| family, loose r1 = 1.926 / r2 = 3.720 | **1.22687 ± 0.00101** |

**But this only narrows the bracket from 7,400 to 6,400 pcm.** The derived family
is still nearly as wide as the arbitrary one, and the 1.80/3.20 guess sits only
821 pcm above the family's midpoint. So the honest position is: the radii are
*bounded*, not *pinned*, and a derived family does **not** rescue the referee to
better than ±3,000 pcm. The 1.80/3.20 guess is not badly wrong — it is simply
unjustified, and a uniform-spacing partner (1.80 / 3.477) is at least as
defensible.

Note also that the bore itself is contested: the repo uses **88 mm** (EN Wikipedia,
"8.4 cm inner"), while INSAG-7 §11 says the channels were **"tubes 80 mm in
diameter"**. Under an 80 mm bore the loose end of the family is physically
impossible (r2 ≤ 3.32 cm), which would pull the whole family down by roughly
1,000–2,000 pcm. **This conflict should be resolved before either number is
quoted**, and it is cheaper to resolve than the rod layout.

### Two traps in the source material (found while checking this)

1. **The NIKIET quote is about control rods, not fuel.** Its "18" is *18 steel
   absorber strips (ПЭЛ) in a circle* in the lower link of a modernised РР rod
   (14 filled with Dy₂TiO₅, 4 empty) — a completely different object from the 18
   *fuel rods* in a ТВС. The two "18"s are a coincidence. Do not let a text search
   for "18" merge them.
2. **`/workspace/tools/nureg1250.pdf` is not NUREG-1250.** It is 402 bytes of
   Akamai "Access Denied" HTML, left over from a failed `curl`. Several source
   comments in the repo cite "NUREG-1250" on the strength of a file that was never
   downloaded. Those citations are not currently backed by anything on disk.

### Cross-check that did not agree — flagged, not buried

Voiding the coolant (0.75 -> 0.036 g/cm3) in the hetero model gives
k_inf 1.19207 -> 1.32468, i.e. **+0.084 dk/k per unit void**. The repo's own
recorded OpenMC value (`provenance.json`, `feedback.void_coeff.corroboration`) is
**+0.035**. Mine is 2.4x larger. Both are positive and both exceed the INSAG-7
operating-point +0.020, but the factor of 2.4 is unresolved and is probably the
same ring-geometry assumption. **Do not quote either figure as final.**

## 4. What it means if we do it this way (the implication)

`ROD_WORTH = 2.0 * 0.0065 / 24` = 0.0005 dk/k = **54.2 pcm per equivalent manual
rod**. Fresh excess reactivity:

| case | k_inf | excess pcm | equivalent rods |
|---|---|---|---|
| in-house ENDF 1.34450 | 1.34450 | 25,623 | 473 |
| shipped table 1.28932 | 1.28932 | 22,440 | 414 |
| in-house ROSFOND 1.33626 | 1.33626 | 25,164 | 464 |
| **OpenMC hetero nominal** | **1.19207** | **16,112** | **297** |
| OpenMC hetero tight | 1.15093 | 13,114 | 242 |
| OpenMC homog | 1.07494 | 6,972 | 129 |

Regulation requires **26–30** equivalent manual rods held in reserve. On the
in-house numbers the fresh core carries ~470 rods of excess — about **16x** the
required reserve. On the referee's ~300 rods it is still **10x**. So:

1. **The qualitative conclusion survives; the magnitude does not.** The fresh core
   is grossly over-reactive under every model. The accident's small ORM margin is
   real physics, not an artifact of the stale number.
2. **But 176 rods of the claimed excess is model error.** The gap between the
   in-house 1.3445 and the referee's 1.1921 is **9,511 pcm = 176 rods**. Any
   statement of the form "we are N rods outside the regulation band" inherits
   that error, so the *size* of the ORM shortfall quoted in the burnup work should
   be treated as an upper bound until the rod-bundle layout is sourced.
3. **The burnup conclusion is unaffected in direction.** Burnup lowers k_inf in
   every model tested, so "the gap is mostly burnup, not rod design" holds. Only
   the pcm figures need re-basing once the geometry is fixed.

## 5. What to do next

Ordered by value per unit effort, which is NOT the order the first version of this
doc gave:

1. **Resolve the bore: 88 mm or 80 mm?** This is a two-source conflict that can be
   settled from documents we can actually reach (regulatory/design text), it bounds
   the ring-radii family from above, and it is worth ~1,000–2,000 pcm. Cheapest
   real gain available.
2. **Find the fuel-assembly drawing (ТВС), not the техническое описание.** We do
   not hold a ТО. What is needed is one plan-view drawing of the 18-rod bundle. If
   it cannot be found, use the uniform-spacing family (r2 = 1.93185 r1) and state
   the ±3,000 pcm band honestly rather than picking 1.80/3.20 silently.
3. **Then** re-run `omc_referee.py --model hetero` and adopt its k_inf, or find why
   the in-house solver over-shields by 2.4x in the fast group.
4. Refresh `docs/ROSFOND-SWAP-MEASURED.md` and the `RodBank.java` comment; the
   1.2893 is stale and the game's lattice table is running on it.

**Do not** spend effort searching for NUREG-1250 content until a real copy is
obtained — the file on disk is a failed download, so anything "confirmed by
NUREG-1250" in this repo is currently confirmed by nothing.

## Reproducing

```
export OPENMC_CROSS_SECTIONS=/workspace/tools/openmc_data/endfb-viii.0-hdf5/cross_sections.xml
/workspace/tools/omc/env/bin/python tools/lattice/omc_referee.py --model homog
/workspace/tools/omc/env/bin/python tools/lattice/omc_referee.py --model hetero
/workspace/tools/omc/env/bin/python tools/lattice/omc_referee.py --model hetero \
    --ring1 1.45 --ring2 2.75          # and 2.00 / 3.70 for the loose bracket
/workspace/tools/omc/env/bin/python tools/lattice/omc_referee.py --model hetero \
    --water-rho 0.036                  # voided
```
