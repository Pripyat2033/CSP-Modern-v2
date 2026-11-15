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
A  normal run        (rods IN,  healthy ORM) P0= 1.435  peak= 1.440 (  +0.3%)  end= 0.513
B  26 April 1986     (rods OUT, ORM violated) P0=10.392  peak=15.631 ( +50.4%)  end=12.564
```

**The same AZ-5 gives opposite outcomes.**

- **A:** the power does not rise (+0.3 %) and the reactor **shuts down** (→ 0.51).
  This is the earlier, survivable test.
- **B:** the power **rises 50 %** as the rods go in, and the reactor **does not
  shut down** (stays at 12.6). This is the positive scram, and the accident.

The scram response separates the two nights by **50 percentage points** — and it
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
directly: at low insertion, moving the rods IN **raises** k_eff (1.030 → 1.046).
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
| **positive void coefficient** | k vs void 0.0 → 0.4 | +0.0343 (rises) |
| **positive scram** (graphite displacer) | k vs insertion 0.0 → 0.2 | +0.0197 (rises) |

The lumped `ReactorCore` *asserts* these from a hand-set curve. The unified core
*produces* them, so it can be compared against INSAG-7's measured values rather
than assumed. See `ONE-CORE-DECISION.md` §6.

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
