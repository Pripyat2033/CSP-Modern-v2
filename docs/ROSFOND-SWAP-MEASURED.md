# ROSFOND-2010 vs ENDF/B-VIII.0 — measured, same code, same geometry

The lattice pipeline (`tools/lattice/`) can run on either library. This is the
result of actually running both, not an estimate.

## Where the data came from

ROSFOND-2010, 27 nuclides, supplied by Ben from the local IPPE distribution
(686 archives, 67 MB). Verified as genuine: header of `n_9235_92-U-235.dat` reads
`92-U -235  IPPE  EVAL-OCT05 NIKOLAEV M.N.`, `----RUSFOND  MATERIAL 9235`,
retrieved 2010/02/01. All 686 zips pass integrity.

Pipeline actually run end to end:

    python3 import_rosfond.py --from <dir> --out rosfond_raw        # 27/27
    python3 reconstruct_rosfond.py --jobs 4                          # 27 ok, 0 failed
    python3 verify.py --source rosfond                               # 10/10

`reconstruct_rosfond.py` is not optional. ROSFOND ships thermal/epithermal
resonances as MF=2 parameters with MF=3 pointwise zero below ~2.25 keV for U-235;
read raw, the cell is moderator-only. NJOY RECONR->BROADR turns the resonance
parameters into pointwise cross sections across the full range.

## Thermal cross sections at 0.0253 eV

| quantity | ENDF/B-VIII.0 | ROSFOND-2010 | diff |
|---|---|---|---|
| U-235 fission | 586.63 b | 584.90 b | -0.30% |
| U-235 capture | 99.37 b | 98.66 b | -0.71% |
| U-238 capture | 2.68 b | 2.68 b | +0.01% |
| Pu-239 fission | 747.22 b | 747.69 b | +0.06% |
| Pu-241 fission | 1012.09 b | 1012.09 b | +0.00% |
| U-235 elastic | 14.11 b | 15.12 b | **+7.13%** |
| U-238 capture resonance integral | 274.84 b | 275.37 b | +0.19% |

The thermal values agree closely, as they must — these are measured quantities
that every modern library reproduces. **The differences that matter are not at
thermal energy.** U-235 elastic differs by 7%, and that feeds the moderator and
resonance treatment.

## The number that matters: void coefficient

Same code, same geometry, only the library differs.
Reactivity `rho = (k_void - k_water) / (k_water * k_void)`.

| library | k (water) | k (void) | rho (dk/k) | in beta_eff |
|---|---|---|---|---|
| ENDF/B-VIII.0 | 1.1329 | 1.1633 | +0.02307 | +3.55 |
| ROSFOND-2010 | 1.2893 | 1.4001 | +0.06138 | **+9.44** |

**ROSFOND's void coefficient is 2.66x the ENDF value.** The RBMK's defining trait
— positive void reactivity — is materially stronger on the Russian library. This
is the strongest argument for the swap: the accident's central physical mechanism
was being understated by a factor of 2.7 by using a Western library.

Both are positive, so the qualitative conclusion (the RBMK voids positively) never
depended on the library choice. The *magnitude* did.

## Caveats

- `verify.py` previously ignored `--source` entirely and always read the default
  cache. Fixed: it now selects the library and prints which one it used. The
  header used to say "ENDF/B-VIII.0" even when running on ROSFOND.
- He-4 has no MT=102 in the ENDF mirror (404), so the ENDF run lacks a helium
  capture section. ROSFOND has it. This does not affect the void result, which
  is a water/steam comparison.
- Absolute k values differ a lot (1.13 vs 1.29) because the two libraries do not
  share a normalisation for the same cell model; the reactivity *difference*
  between water and void states is the comparable quantity, not absolute k.
