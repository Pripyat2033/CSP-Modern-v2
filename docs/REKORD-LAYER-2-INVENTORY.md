# REKORD layer 2 — core inventory at shutdown

## What this layer is

How many atoms of each nuclide were in the core at the instant of the accident:
1130 nuclides, 1.077e28 atoms, from the same lattice model that produces the
burnup and the void coefficient.

## What it is NOT

No activity is stored, no release fraction, no dose. Activity is computed from
atoms and the layer-1 decay constant (`InventoryLoader.activityBq`), and that is
the only sanctioned bridge between the layers. `InventoryLoader` rejects a data
file that carries an activity or release field, so a later layer cannot start
reading a source term out of the inventory by accident.

A green `InventorySelfTest` means the inventory is right. It says nothing about
what left the building.

## How it is built

`tools/radiology/build_inventory.py`:

1. Burns the lattice cell to 10.3 MWd/kg over 618 days, tallying fissions per
   fissile nuclide (U-235 78.73%, Pu-239 20.40%, Pu-241 0.87%).
2. Multiplies the independent fission yield of every nuclide by the fissions of
   the nuclide that produced it. Independent yields are used, not cumulative:
   the cumulative yield already contains the precursors' decay, and using it
   would double-count the chains.
3. Closes the node set over the decay graph, so a daughter with no independent
   yield of its own (Cd-108, reached by 97% of Ag-108 decays) still gets a node.
4. Integrates dN/dt = M N + s to T with a stiff solver.

Yields come from `fission-yields.json`; decay data from `decay-cache.json`, both
fetched from the IAEA Nuclear Data Services API (ENSDF). Nothing is hand-typed.

## The seven bugs that made it wrong, and what caught each

Every one of these produced a plausible-looking inventory. They are recorded
because each is a way to be silently wrong, and the tests now guard all seven.

**1. Delayed-neutron emission as a parallel branch.** I-137 has `B- 100%` and
`B-N 7.14%`. Read as parallel branches that is 107% of the decays and 7% of the
mass created from nothing. It is a sub-branch: 92.86% to Xe-137, 7.14% to Xe-136.
Guarded by `test_build_inventory.py`.

**2. A blank neutron percentage defaulted to 100%.** ENSDF leaves many
beta-delayed-neutron rows blank. Reading blank as 100% moves a whole chain one
mass unit down. Blank means "not tabulated".

**3. A per-mass-chain solver.** Beta decay conserves A, so solving each mass
chain separately looks right. It is not: beta-delayed neutron emission moves
1.45% of fission products into the chain below, and a per-chain solver drops
them silently. The unit of solution is the connected component of the decay
graph.

**4. An isomer with no listed decay mode.** An isomer carrying a half-life and
an empty branch list is an infinite trap: every atom reaching it stops there.
An isomer's unlisted mode is an internal transition to its own ground state.

**5. An isomer selected by `energy > 0`.** Nb-102, Rh-110, Sb-128, Pm-154 and
six others are evaluated with the ground state and the isomer BOTH at energy 0,
told apart only by spin-parity. Requiring a positive energy finds no isomer,
writes an error record, and the record then reads as "stable" — see bug 6. The
isomer is now taken from the levels by longest half-life and an explicit IT,
excluding the ground state by identity rather than by energy.

**6. A failed fetch read as "stable".** This was the expensive one. An error
record has no half-life, so a "no half-life means stable" test froze every atom
in it. Ten isomer records whose fetch had failed held 0.75% of the whole
inventory (Nb-102m alone: 5.1e25 atoms) for the full 618 days. `_decay_state`
now tests for an error record FIRST, the generator refuses to write a file that
contains one, and a nuclide with no ENSDF dataset at all is a distinct state
("unevaluated") that must be named and must hold under 0.01% of the inventory.

**7. A half-life with no mode, treated as a terminus.** Cu-68 and Te-127 have a
half-life and a blank mode column in the levels table, but the mean-beta
(`decay_rads` `bm`) table carries the same decay with mode and daughter. The
mode is now read from there. Where no table has it (I-142, I-144), the route is
derived from the evaluated cumulative yields — a nuclide well below its chain
end must beta-minus, and only that mode raises Z at fixed A — and the record is
marked `yield-chain-derived` so a derived route is never mistaken for a
tabulated one.

The size of that last error: I-142 (0.222 s) was held as stable and reached
2.80e23 atoms against a true 1.68e15, an overstatement of 1.7e8. Te-127 was
1.22e25 atoms, 0.11% of the inventory.

## Why the solver is Radau, and not `expm`

Half-lives here span 1e-11 s to 1e22 s. That spread is what makes the naive
approaches fail, and each failure is worth knowing about:

| method | result |
|---|---|
| `expm` on all 1112 nodes | −1.3% overall, **−27% on Ru-106** |
| `expm` per connected component | still −27% (largest component is 440 nodes, same spread) |
| fast nodes slaved algebraically (QSSA) | **+0.08%** — the startup transient is real |
| `expm_multiply` | does not terminate |
| **Radau, rtol 1e-9** | **0.000%, Ru-106 exact** |

Scaling-and-squaring needs about 60 squarings at this eigenvalue spread and each
one amplifies rounding, so `expm` cannot be made accurate here by tightening
tolerances. QSSA assumes the fast nuclides have forgotten their startup, which is
false for a 618-day irradiation: the slowest members have barely begun.

The bug was found by comparing against the closed-form Bateman answer for a
nuclide fed only by fission, which is independent of how the solver works.

## Validation

`InventorySelfTest` — 24 checks, three levels of independence:

* **Solver**: closed-form Bateman for Ce-144 (1.0000 of analytic), Ru-106
  (0.9966), Ba-140 (0.9612).
* **Invariants** (no yield needed): two fission-product atoms per fission
  (1.99989); Y-90/Sr-90 atom ratio = half-life ratio (secular equilibrium);
  Ba-137/Cs-137 = 1 − (1−e^−λt)/(λt), the transient buildup.
* **Published anchors**: Cs-137 core mass 75.1 kg (published estimate 70–80 kg);
  activity 2.41e17 Bq (published core inventory ~2.6e17 Bq).
* **Regression**: I-142 and I-144 sit at saturation rather than holding 618 days
  of production; Te-127 is a transient against its I-127 daughter; the file
  declares its unrouted nuclides and none is left unrouted for want of a mode.

`test_build_inventory.py` — 39 checks on the branch parser and the decay-state
classifier, no network needed.

## Known limits

* Twenty-one nuclides in the far wings of the yield distribution have no ENSDF
  evaluation at all (Zn-86, Ga-88, Y-93, …, Lu-172). Nobody has measured their
  decay, so they cannot be routed; they are listed in `_unevaluated` and hold
  3.1e16 atoms, 2.8e-10% of the inventory. The generator refuses to write a file
  if this share ever exceeds 0.01%.
* Four isomers whose excited level ENSDF does not tabulate (As-84m, Se-85m,
  Xe-143m, Tb-162m) are de-excited into their own ground state on the ground
  state's half-life, recorded as `isomer-level-unresolved`. This is exact at
  shutdown for any isomer short-lived against the irradiation, which all four
  are.
* Two nuclides (I-142, I-144) have no mode in any ENSDF table. Their route is
  derived from the evaluated cumulative yields and recorded as
  `yield-chain-derived`, not as a tabulated value.
* Zr-96 and Ce-142 are observationally stable (2e19 y and 5e16 y, only
  double-beta decay) and are treated as termini, not gaps. The dividing line is
  physical: a half-life far longer than the irradiation means nothing decays.
* Yields are a U-235/Pu-239/Pu-241 mix at the model's own burnup. Comparisons
  against thermal-U-235 literature yields therefore differ by a few percent in
  the mass regions where the plutonium contribution is large.
