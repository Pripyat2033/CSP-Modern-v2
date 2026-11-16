# The "5–7× discrepancy" is not yet proven — the two models are not comparable

Investigation, 2026-11-15. Read the plain version first.

## Plain version

Earlier I told the Director that the project's two methods disagree by 5–7× and that
the deterministic cell is "the outlier". **That was over-stated.** On investigation,
the two models are **not measuring the same thing** — they differ in at least three
ways that are large enough to explain the whole gap. So the discrepancy is **not yet
established as a defect in the physics.** It may be an apples-to-oranges comparison.

The biggest of the three differences is the **thermal scattering law** — the physics
of how neutrons behave when they are already slow and bouncing off water and graphite
atoms. The deterministic cell **does not have it** (its own header admits this); the
OpenMC model **does**. Measured here: turning that one piece of physics off changes
the void worth by **more than the entire 6× discrepancy**. A lever that big cannot be
ignored when comparing the two numbers.

**Nothing is shipped, nothing is broken.** This is a finding about *how to compare*,
not a new fault.

## 1. The two numbers

| Method | dk/dvoid at the operating point | source |
|---|---|---|
| deterministic 1-D cell (`tools/lattice`) | **+0.090** | measured today |
| OpenMC Monte Carlo (`openmc/`) | **+0.015** | `openmc_void_coeff.json` |
| ratio | 6.0× | |

Both curves have the **same shape** — k rises as void increases, then turns over near
full void. So the physics agrees qualitatively; only the *magnitude* differs.

## 2. The three differences that make the comparison invalid

| | deterministic 1-D cell | OpenMC Monte Carlo |
|---|---|---|
| **thermal scattering law S(α,β)** | **absent** (header: "no thermal up-scattering (would need S(alpha,beta)...)") | **present** — `add_s_alpha_beta` for water AND graphite |
| **temperature** | 600 °C fuel | **293.6 K** (cold) |
| **geometry** | 57% water in the tube bore + a water-filled tube/graphite gap; Zr tube explicit | 26% water in the bore; no tube; graphite starts at the bore |

The models disagree at void = 0 already (deterministic k∞ = 1.133 at 600 °C; OpenMC
k = 1.311 cold). Two models that disagree about the *baseline* cannot be trusted to
agree about a *derivative* taken from that baseline.

## 3. The thermal-scattering lever, measured

Ran the 1-D cell with and without its thermal up-scattering term:

| variant | dk/dvoid (0.3–0.4) |
|---|---|
| with up-scattering (the code's model) | +0.0900 |
| without (pure down-scatter) | +0.2594 |

The void worth moves by **−0.169** when the thermal treatment changes — **1.9× the
whole discrepancy, and 11× the OpenMC value.** The thermal treatment is the dominant
physics of the void coefficient, and the two models handle it completely differently.

(Note: the deterministic code's "up-scattering" is a *detailed-balance approximation*,
not a real S(α,β) law. So even the "with" case is not the same physics OpenMC runs.)

## 4. What this means

- **The 6× is not yet a proven defect.** It is the difference between two models that
  differ in thermal scattering, temperature and geometry. Any of those could account
  for it, and the thermal lever alone is big enough.
- **The honest next step is to make the models comparable** — same geometry, same
  temperature, and the deterministic cell given a real S(α,β) treatment — and only
  then compare. If they still disagree after that, the discrepancy is real.
- **The earlier "outlier" claim is withdrawn.** I should not have called it a defect
  before checking that the two models were the same calculation. That is the lesson
  this project keeps teaching: measure before concluding.
- The `VOID_WORTH_SCALE` anchor is still a calibration, and the plan to remove it
  still stands — but the justification is now "make the models comparable", not
  "the deterministic cell is 6× wrong".

## 5. Status

- Nothing shipped. All source reverted; `git status` clean; 13/13 physics tests pass.
- OpenMC is not installed in this sandbox, so the decisive test (OpenMC with and
  without S(α,β)) could not be run here. It should be run where OpenMC is available.
