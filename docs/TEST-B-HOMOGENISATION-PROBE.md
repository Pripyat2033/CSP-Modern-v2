# Test B — does the homogenised core exaggerate the void worth?

Probe result, 2026-11-15. Written in plain language first, then the numbers.

## The plain version

The Director's diagnosis: *"you cannot calculate a small part of the reactor and
multiply it — a real RBMK behaved like 1,661 individual channels, each with its own
personality."* The model, in code, forces every channel to be identical
(`RbmkSolver3D.setCoreState` copies ONE void onto all 1,872 channels).

So I tested the obvious thing: **if I let the channels differ, does the
exaggerated void worth collapse?** It does **not**. With the shipped setting, a
core whose channels differ behaves *almost identically* to one whose channels are
all the same (void worth +0.0222 → +0.0220, a change of under 1%). Making the
channels individual, by itself, is **not** the fix.

**But the Director's instinct is still right — the error is real, just one layer
down.** It is not the *whole core* being averaged. It is the **single channel cell**
being collapsed: the `tools/lattice` calculation flattens one fuel channel (fuel +
graphite + water) into a single node with two-group constants, and *that* flattening
is what overstates the feedback. The project already has a second, independent
method that disagrees with it — and that disagreement is the real finding (below).

## 1. The probe — what it measured

Same yardstick as `DerivedVoidSelfTest` (the eigenvalue at uniform void 0.30 and
0.40), over four ways of building the state, **all with the same core-average void**:

| state | k(0.30) | k(0.40) | dk/dvoid | vs today |
|---|---|---|---|---|
| A UNIFORM (today) | 1.04609 | 1.04831 | **+0.0222** | 1.00× |
| B CHANNEL spread (±0.15 per channel) | 1.04599 | 1.04819 | +0.0220 | 0.99× |
| C AXIAL profile (bottom→top) | 1.04098 | 1.04340 | +0.0242 | 1.09× |
| D BOTH | 1.04114 | 1.04331 | +0.0217 | 0.98× |

(anchor ON, i.e. the shipped setting.)

**Reading:** letting the channels differ changes the whole-core void worth by
**less than 1%** (B), and the axial profile — which the model already does — is the
only one that moves it at all (+9%). So **whole-core homogenisation is not the
cause of the 4–6× exaggeration.**

A re-run with the anchor OFF (the setting that exposes the raw feedback) and the
same small spread — chosen so **no value clamps** at 0.30/0.40 — is the clean test:

| state | dk/dvoid | vs today |
|---|---|---|
| A UNIFORM | **+0.0656** | 1.00× |
| B CHANNEL spread (±0.15) | +0.0647 | **0.99×** |
| C AXIAL profile | +0.0944 | 1.44× |
| D BOTH | +0.0834 | 1.27× |

**Channel spread alone: 0.99× — a 1% change.** The conclusion holds with the anchor
off: whole-core homogenisation is **not** the cause. (A larger spread of ±0.40 was
also run but is **unreliable** — at void 0.30 the per-channel values clamp at zero,
lifting the true mean above 0.30 and biasing the comparison. Flagged, not used.)

A side probe (void one side of the core more than the other, watch the flux tilt)
gave **inconsistent answers** between the two runs — it tilted to the less-voided
side with the anchor on and to the more-voided side with it off. That probe is too
crude to trust; recorded as unreliable, not as a result.

## 2. The real finding — the project's two methods disagree by 5–7×

The project has **two independent ways** to compute the void worth, and they do not
agree:

| Method | dk/dvoid | notes |
|---|---|---|
| `tools/lattice` deterministic cell — ENDF | **+0.082** | one-node, 69-group collapse |
| `tools/lattice` deterministic cell — ROSFOND | **+0.130** | same method, Russian data |
| **OpenMC Monte Carlo** (`openmc/`) | **+0.015** | an independent method |
| INSAG-7 (the books) | +0.020 | the sourced reference |

**OpenMC agrees with the books (+0.015 vs +0.020). The deterministic cell is the
outlier** — 5–7× too high. And the OpenMC script says so in its own header:

> *"the RBMK's positive void coefficient ... is exactly the quantity our Java model
> has wrong (its derived feedback is ~5-7x too strong, to the point that AZ-5 no
> longer shuts the core down)."*

So the defect is **known and already cross-checked**: the *cell collapse* in
`tools/lattice` overstates the feedback, and Monte Carlo (a fundamentally different
method) shows the truth. The `VOID_WORTH_SCALE` anchor was blending the wrong
number toward the right one.

## 3. What this means for the fix (correcting the earlier plan)

The earlier plan was "give every channel its own state." **That is worth doing for
other reasons — the accident needs unevenness — but it will NOT fix the void worth.**
The fix is at the **cell**: either

- **(a) use the OpenMC-derived coefficient** (the independent method that already
  agrees with the books) as the source of record, and find why the deterministic
  cell disagrees; or
- **(b) find and fix the error in the deterministic cell** (the collapse, the
  self-shielding, or the leakage — the cell has no leakage term, so it reports
  k-infinity), so it agrees with OpenMC on its own.

Either way the anchor comes out honestly, and the number stands on its own.

## 4. Honest status

- **Whole-core homogenisation: NOT the cause** (measured, <1%).
- **Cell collapse: the likely cause** (the two methods disagree 5–7×, and the
  OpenMC model says so itself). **Not yet proven** — proving it means showing *why*
  the deterministic cell is high, which is the next job.
- **Nothing shipped.** The reactor is unchanged; anchor still 0.233; all physics
  self-tests pass (13/13, 0 failures).
- The probe (`HomogenisationProbe.java`) is kept as a test tool; it asserts nothing.
