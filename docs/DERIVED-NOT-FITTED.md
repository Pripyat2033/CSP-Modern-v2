# How we know the model is derived, not fitted

Written 2026-11-12, in answer to the Director's paranoia challenge:

> *"The plant ran the same experiment multiple times without exploding. So how do
> we verify the model reproduces the right differences — how do we know the
> physics is derived, not fitted?"*

This is the right question, and it is the whole reason the project exists. A model
that cannot tell the runs apart has proved nothing about what was different on the
night of 26 April. This note states the test, the result, and why the result
cannot be faked.

---

## 1. The trap: "it holds steady at power" proves nothing

Any core with a regulator will *hold*. A fitted model and a derived model both
sit at 1.0 and look identical. Steady-state agreement is not evidence.

**Discrimination is the evidence.** The question is not "does it hold?" but "does
it produce *different outcomes* for the inputs that actually differed?" — and does
it produce them from the *same code*, not from a branch that says "if accident,
explode".

## 2. The test

`src/test/.../physics/ExperimentVariantsSelfTest.java` runs the **same AZ-5
scram, through the same code path**, from the two starting states that really
differ between the historical runs. Nothing else is changed — not the rod speed,
not the solver, not a flag. Only the rod insertion at the moment of the scram, and
the poisoning of the core:

| | rods at AZ-5 | void | power | this is |
|---|---|---|---|---|
| **A** | 0.75 (healthy, part in) | 0.05 | 1.0 | a normal run — the earlier tests |
| **B** | 0.08 (nearly all OUT) | 0.10 | 0.06 | the night of 26 April — ORM violated |

## 3. The result — measured, in this repo

```
A  normal run        (rods IN,  healthy ORM) P0= 1.427  peak= 1.432 (  +0.3%)  end= 0.519
B  26 April 1986     (rods OUT, ORM violated) P0= 9.192  peak=13.571 ( +47.6%)  end=10.861
```

**The same AZ-5 gives opposite outcomes.**

- **A:** the power does not rise (+0.3 %) and the reactor **shuts down** (→ 0.52).
  This is the earlier, survivable test.
- **B:** the power **rises ~48 %** as the rods go in, and the reactor **does not
  shut down** (stays at 10.9). This is the positive scram, and the accident.

The scram response separates the two nights by **~47 percentage points** — and it
is not a flag in the test; it is the rod insertion fed to the same solver.

## 4. Why this cannot be a fitted result

The effect is the **rod's axial geometry meeting a bottom-peaked flux**, both of
which are inputs to the physics, not outputs:

- The rod is **B4C / water telescope / graphite displacer** along its length
  (`RbmkGeometry.rodMaterials`). The graphite displacer sits at the *bottom* of a
  withdrawn rod, so driving the rods in *adds* graphite to the bottom of the core
  first — before the absorber reaches it.
- When the rods are nearly all out (B), the axial flux is **bottom-peaked**. So the
  first thing the scram does is push a neutron *moderator* into the region of
  highest flux. Reactivity goes **up** before it goes down. That is the positive
  scram.
- When the rods are part-way in (A), there is no graphite left to insert at the
  bottom, so the scram only adds absorber. Reactivity goes **down** immediately.

The model does not "know" the accident happened. It knows the geometry and the
cross sections, and it computes k_eff from them. The test's third check reads that
directly: at low insertion, moving the rods IN **raises** k_eff (1.026 → 1.042).
That is a property of the *material layout*, measured by the same `balanceKeff()`
the live plant uses.

**If the model were fitted to "the answer", one of these would be impossible:**
it could not hold nominal *and* spike 50 % on a scram; it could not shut down for
one rod position and fail to shut down for another, using the same command; and it
could not show the k_eff rise from the displacer as a *computed* eigenvalue.

## 5. The two mechanisms, both derived

The same argument covers the two phenomena the accident rests on. Both are outputs
of the evaluated cross sections (ENDF/B-VIII.0, via `rbmk_lattice.json`) and the
real channel geometry — not the hand-fitted coefficients of the old lumped core:

| mechanism | test | result |
|---|---|---|
| **positive void coefficient** | k vs void 0.30 → 0.40 | +0.0222 (rises) |
| **positive scram** (graphite displacer) | k vs insertion 0.0 → 0.2 | +0.0049 (rises) |

The lumped `ReactorCore` *asserts* these from a hand-set curve. The unified core
*produces* them, so it can be compared against INSAG-7's measured values rather
than assumed. See `ONE-CORE-DECISION.md` §6.

### 5a. The void itself is now derived, not fitted (2026-11-12, option (c))

Until today the core was *told* its void by a straight line:
`void = 0.05 + 0.08*(power-1)`. That line was fitted, and its nominal value (0.05)
was **about seven times too low**. The plant already carried the sourced numbers to
do better; the core simply ignored them.

`ChannelCooling` now computes the void from the energy balance the plant actually
runs, using the sourced data in `docs/RBMK-PLANT-SYSTEMS.md` (WP §83-89):

| quantity | value | where it comes from |
|---|---|---|
| coolant mass flow | 6 × 8,000 m³/h × 750 kg/m³ = **10,000 kg/s** | sourced |
| enthalpy rise | 3,200 MWt / 10,000 kg/s = **320 kJ/kg** | derived |
| outlet steam quality | **0.156** | derived — matches the documented "~15%" |
| **core-average void** | **0.355** | derived (quality + slip S=1.5) |

The fitted 0.05 was wrong by 7×. The plant also now tells the core its **actual**
pump flow (`PlantSystem` → `UnifiedReactorCore.setCoolantFlowKgS`), so a pump trip
raises the void and the power with **no accident flag anywhere** — the coupling is
real, and it is the plant's flow that drives it:

```
flow halved:  P 1.00 -> 1.11,  void 0.355 -> 0.528
```

**The one honest calibration.** With the void derived correctly, the runaway
appeared: the derived coolant void slope (dvoid/dP ≈ 0.29) multiplied by the
derived lattice void worth (+0.086 dk/dvoid) exceeded the negative feedbacks, so
the core ran away at nominal. Measuring it showed the cause — the **lattice void
worth is 4.3× the sourced INSAG-7 value (+0.020)**. The old fitted slope had been
hiding this by being too shallow; two errors were cancelling. Per the mission rule
(a sourced number beats a computed one), the derived void *dependence* is scaled to
the source (`RbmkXsec.VOID_WORTH_SCALE = 0.233`, registered `xsec.void_worth_scale`,
`to_verify`): the shape stays the derived physics, only the magnitude is set to the
measurement. The core then holds nominal (1.2 % drift over 30 s) and keeps the
positive void coefficient.

**Still open:** the axial void profile is computed (`ChannelCooling` gives it node by
node) but applied to the neutronics as a core average. Coupling the full axial
profile makes the flux strongly bottom-peaked, which *doubles* the effective void
worth and re-introduces the runaway — a real coupling, but it needs the lattice
axial dependence reconciled before it can be switched on. Recorded, not hidden.

## 6. What this does and does not prove

**Proves:** the model discriminates the variants from the same code, by the
mechanism (rod geometry), not by a flag. That is the standard the Director asked
for, and it is met.

**Does not yet prove:** that the *magnitudes* are right. The +50 % spike is the
model's number, not INSAG-7's. Closing that gap is the verification plan: compare
the derived coefficients and the scram response against the published measured
values (INSAG-7, NIKIET/SKALA), and treat any disagreement as a defect in the
model, not in the reference. The provenance registry entry `unified.derived_feedback`
flags this as `to_verify` and says exactly what it awaits.

**The honest one-line answer:** the model is not fitted to the answer, because it
gives two different answers to the same command, and the difference is the real
geometry — the graphite displacer at the bottom of a nearly-withdrawn rod.
