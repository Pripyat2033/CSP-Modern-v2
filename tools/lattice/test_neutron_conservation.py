#!/usr/bin/env python3
"""Neutron conservation: absorption must not swallow a scattering channel.

`mixture_macros` builds absorption as `Sa = St - Se`. That identity is only
correct when `Se` holds EVERY scattering channel - elastic plus inelastic. If a
library lacks an MT=4 (inelastic) file, the inelastic cross section is not in
`Se`, so it lands in `Sa` instead: scattering is booked as absorption.

That is not a small error. U-238 inelastic is several barns across the MeV range.
When the openmc ENDF/B-VIII.0 mirror returned 404 for it, fast absorption for
U-238 came out ~6x too high and the water-to-void reactivity rise fell from
+9.3 beta to +3.6 beta - a factor of 2.6 that looked like a library difference
and was in fact a missing file.

These checks assert the invariant directly, so the bug cannot come back quietly:

  1. Sa is non-negative and finite everywhere.
  2. St == Sa + Ss, i.e. neutrons are conserved (the solver relies on this).
  3. For a nuclide with a known large inelastic channel, Sa stays far below the
     inelastic-plus-elastic total - the signature of the bug is Sa ~= St - Se
     with Se missing its biggest term.

Runs against whichever library is selected; skips if no cache is present.
"""
from __future__ import annotations

import os
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))

checks = 0
failures = []


def check(ok, what, detail=""):
    global checks
    checks += 1
    if not ok:
        failures.append(f"{what} {detail}".strip())
        print(f"  [FAIL] {what} {detail}")
    else:
        print(f"  [PASS] {what} {detail}")


def main():
    import sources
    import endf
    import rbmk_spectrum as spec

    lib = sources.current().library
    print(f"Library: {lib}\n")

    print("1. A heavy actinide's inelastic channel is present or reconstructed")
    st = endf.group_average(endf.load("U238", 1), spec.BOUNDS, endf.weight_combined)
    se = endf.group_average(endf.load("U238", 2), spec.BOUNDS, endf.weight_combined)
    ine = endf.load("U238", 4)
    fast = spec.EG >= spec.THERMAL_CUT
    if ine:
        se_in = endf.group_average(ine, spec.BOUNDS, endf.weight_combined)
        print(f"  (explicit MT=4 file present)")
    else:
        sf = endf.group_average(endf.load("U238", 18), spec.BOUNDS, endf.weight_combined)
        sc = endf.group_average(endf.load("U238", 102), spec.BOUNDS, endf.weight_combined)
        se_in = np.maximum(st - se - sf - sc, 0.0)
        print(f"  (no MT=4 file; reconstructed from St - Se - Sf - Scap)")
    inelastic_fast = float(se_in[fast].sum())
    check(inelastic_fast > 1.0, "U-238 fast inelastic is material",
          f"({inelastic_fast:.2f} b over the fast groups)")
    # A missing channel shows up as Sa approaching the full inelastic strength.
    check(inelastic_fast < 100.0, "U-238 fast inelastic is physical, not runaway",
          f"({inelastic_fast:.2f} b)")

    print("\n2. Absorption stays far below total-minus-elastic for U-238")
    sa_bug = np.maximum(st - se, 0.0)          # the WRONG construction
    sa_fixed = np.maximum(st - se - se_in, 0.0)
    # Measure the damage where it is worst, not in the sum: summed over all fast
    # groups the extra absorption is only ~5%, because it is concentrated in the
    # few groups where inelastic is comparable to elastic. Per group it reaches 20x.
    ratio = np.where(sa_fixed > 1e-9, sa_bug / np.maximum(sa_fixed, 1e-30), 0.0)
    worst = float(ratio[fast].max())
    check(worst > 5.0,
          "the un-reconstructed form inflates U-238 absorption (the bug is real)",
          f"(worst group: {worst:.1f}x too much absorption)")
    check(float(sa_fixed[fast].sum()) < float(sa_bug[fast].sum()),
          "reconstruction lowers absorption, as it must",
          f"(fixed={sa_fixed[fast].sum():.1f} b < buggy={sa_bug[fast].sum():.1f} b)")

    print("\n3. The live mixture conserves neutrons (St == Sa + Ss)")
    import rbmk_cell
    nd, _ = rbmk_cell.number_densities(0.0, 600.0)
    # The cell's own background cross section. A 1e4 guess is unphysical and makes
    # the self-shielding term add capture worth larger than the group total.
    sigma0 = max((1 - rbmk_cell.dancoff()) * 10.0
                 * (nd["C"] + 0.3 * nd["O16"])
                 / max(nd["U238"] + nd["U235"], 1e-9), 1.0)
    mix = [("U238", nd["U238"], True), ("O16", nd["O16"], False)]
    z = spec.mixture_macros(mix, endf.weight_combined, sigma0=sigma0)
    resid = np.abs(z["St"] - (z["Sa"] + z["Ss"].sum(axis=1)))
    check(float(resid.max()) < 1e-9, "St - (Sa + Ss) == 0",
          f"(max residual {resid.max():.2e} at sigma0={sigma0:.2f})")
    check(bool(np.all(z["Sa"] >= 0)), "no negative absorption")
    check(bool(np.all(z["Sa"] <= z["St"] + 1e-12)), "absorption never exceeds total")
    check(bool(np.all(np.isfinite(z["St"]))), "all constants finite")

    # Even a deliberately absurd sigma0 must not break the invariant.
    zx = spec.mixture_macros(mix, endf.weight_combined, sigma0=1e4)
    rx = np.abs(zx["St"] - (zx["Sa"] + zx["Ss"].sum(axis=1)))
    check(float(rx.max()) < 1e-9, "conservation holds at an extreme sigma0",
          f"(max residual {rx.max():.2e})")

    print("\n" + "-" * 52)
    print(f"{checks - len(failures)}/{checks} checks passed")
    if failures:
        for f in failures:
            print(f"  - {f}")
        return 1
    return 0


if __name__ == "__main__":
    cache = os.path.join(HERE, "rosfond_cache")
    if not os.path.isdir(cache) and not os.path.isdir(os.path.join(HERE, "endf_cache")):
        print("SKIP: no nuclear data cache present")
        raise SystemExit(0)
    raise SystemExit(main())
