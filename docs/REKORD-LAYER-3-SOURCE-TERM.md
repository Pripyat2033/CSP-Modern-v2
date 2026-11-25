# REKORD layer 3 — source term: what left the building

## What this layer is

The fraction of each nuclide that was released to the atmosphere, and the
activity that represents. Twenty-five nuclides, quoted from a published
tabulation and divided by the layer-2 core inventory.

## What it is NOT

No dose. No transport. No deposition. No release rate per nuclide. Those are
layer 4.

And, importantly: **no single number.** Every published estimate of this release
carries an uncertainty of tens of percent at best, and the most important single
quantity in it — the fraction of the fuel itself that was ejected — has been
revised by a factor of two on measurement. A source term presented as one number
would be a fiction of precision. This layer stores the range and says which
source produced which bound.

## The source, and why it is not the preferred one

The released activities are quoted from **IAEA Pub1239 (2006), Table 3.1**,
"Revised estimates of the principal radionuclides released during the course of
the Chernobyl accident". That table is the current consolidation of UNSCEAR 2000
[ref 3.6] and Dreicer 1996 [ref 3.7], and it is what the literature converges on.

This project's standing policy is to prefer Russian and Soviet primary sources,
and the designers are the best-placed authority on their own plant. **No Russian
primary tabulation of the release was available in this environment.** The IAEA
consolidation is therefore used as a labelled second-best source, recorded as
such in `_sourcePolicy` in the data file, so it can be replaced when a Russian
source is obtained rather than being mistaken for the preferred one.

INSAG-7 was checked first and has no source-term numbers at all: it is a
document about *causes*, not about quantities released. That is worth recording,
because "INSAG-7 doesn't cover it" is not obvious until you have searched it.

## How it is built

`tools/radiology/build_source_term.py`:

1. Reads the 25 nuclides of Table 3.1 with their quoted released activities in
   PBq, keeping the source's own qualifiers (`~`, `>`) rather than flattening
   them into a bare number.
2. For each, computes the core activity from layer-2 atoms and the layer-1 decay
   constant: `A = N ln2 / T½`.
3. Divides, giving the release fraction.
4. **Refuses to emit a fraction that is impossible**, and records why.

Nothing is fitted. The released activities are observations; the fractions are
arithmetic on them.

## The range, made explicit

Three separate uncertainties are carried in the file rather than averaged away:

| quantity | value | source |
|---|---|---|
| Fuel-particle release fraction | **1.5% ± 0.5%** (0.010–0.020) | Kashparov 2003, via Pub1239 §3.1.2 |
| *Earlier* estimate of the same | **~3%** | INSAG-1 (1986) and secondary accounts |
| Daily release rate | **±50%** | Pub1239 Fig. 3.1 |
| Cs-137 deposited in former USSR | **40 PBq → 80 PBq** | Pub1239 §3.1.1 |

The fuel fraction is the one that matters most. Every refractory nuclide's
release scales with it, so the factor-of-two revision between INSAG-1 and
Kashparov propagates straight into the refractory half of the source term. The
Cs-137 revision is recorded because it is a clean worked example of the same
thing happening on measurement: the early airborne-radiometric survey gave
~40 PBq, the current figure is ~80 PBq.

## The cross-check, and the three layer-2 defects it exposed

Deriving a release fraction needs a correct core activity. Checking the derived
fractions against physics found three classes of nuclide where **layer 2 cannot
supply one**. These are recorded in `_layer2Limits` in the data file and are not
papered over with a fudge factor.

**1. Activation products are absent entirely.** Layer 2 is a fission-product
inventory. It contains **no nuclide above Z = 70** — no plutonium, no neptunium,
no americium, no curium, no cobalt. Table 3.1 lists seven actinides (Np-239,
Pu-238/239/240/241/242, Cm-242). Their released activities are quoted, and no
fraction is derived, because the denominator does not exist. Making one up would
be exactly the guess this project forbids.

**2. Isomer feeding cannot be represented.** A decay branch records a mode and a
branching ratio but **not a daughter level**. So beta decay into an excited state
is unrepresentable, and an isomer that is fed only that way is starved. Te-129m
is the clean case: it is fed by Sb-129, and the model holds 4.6e20 atoms against
a saturation value of ~3.0e24 — a shortfall of ~6600×. The quoted release
(240 PBq) is then 2182× the computed core activity, which is why that fraction is
refused.

**3. Neutron capture is not modelled.** Layer 2 solves decay chains with fission
as the source; there is no `(n,γ)` reaction anywhere in it. Cs-134 is made from
Cs-133 by capture, and Cs-136 is made by capture on Cs-135, so both are
under-produced. Their fractions come out at 752× and 1.47× — impossible and
merely implausible respectively. Both are refused.

These are **layer-2 defects that layer 3 exposed**. The correct fix is in layer 2
(add activation and isomer feeding), not a correction factor applied here. They
are left visible so the fix is not forgotten.

## What survives

Fifteen of the twenty-five fractions are derived and physically sensible, and
they order themselves the way volatility says they must:

| class | mean release fraction | comment |
|---|---|---|
| inert gases | 0.603 | Xe-133 at 95%, Kr-85 at 25% |
| volatile | 0.326 | Cs-137 at 35%, I-131 at 56% |
| intermediate | 0.049 | Sr, Ru, Ba — mostly fuel particles |
| refractory | 0.013 | Zr, Mo, Ce — fuel particles only, ~the 1.5% fuel fraction |

That ordering is the real test of the layer. It is not imposed anywhere; it
emerges from dividing published release by a core inventory computed
independently, and it lands on the fuel-particle fraction (~1.3% refractory
against a quoted 1.5% ± 0.5%) without being tuned to it. Two layers built from
different data agree on a number neither was fitted to — which is the strongest
evidence so far that the layer-2 inventory is broadly right.

## Validation

`SourceTermSelfTest` — 21 checks, 0 failures.

* **Structural**: all 25 nuclides present, no record rejected, every derived
  fraction in [0,1], and no refused entry carrying a number (the dangerous
  failure: a non-result read as a result).
* **Physical ordering**: inert > volatile > intermediate > refractory. This is
  the check that would catch the classes being assigned backwards while every
  individual number still looked plausible.
* **Published anchors**: Xe-133 at 95%, Kr-85 at 25%, Cs-137 at 35% / 85 PBq;
  fuel fraction 1.5% with bounds 1.0–2.0% and the earlier 3% retained; the
  Cs-137 40→80 PBq revision.
* **Guard on the blanks**: the seven actinides must carry a quoted activity and
  *no* fraction. This stops a later "fix" from filling in the blank.

## Known limits

* The release is quoted, not modelled. This layer cannot predict a *different*
  accident; it records what happened at this one.
* Seven actinides have a released activity and no fraction (defect 1 above).
* Three volatile fractions are refused (defects 2 and 3).
* The release time-course is a single window (26 April – 6 May 1986) with a
  ±50% daily rate. Resolving the two regimes (initial excursion, then the
  graphite fire) belongs to layer 4, which needs a rate, not a total.
* The source is IAEA, not Russian. Under this project's policy that is a
  labelled second-best and should be replaced when a Russian tabulation is found.
