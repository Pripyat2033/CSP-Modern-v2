"""HEAD-TO-HEAD: the deterministic 1-D cell vs OpenMC, on the SAME void points.

Both use ENDF/B-VIII.0. This solves the fast cell (tools/lattice) at the exact
void fractions of the OpenMC sweep (openmc/openmc_void_coeff.json) so the two
k_inf curves can be compared point by point, and reports the operating slope of
each. The void fraction is defined identically: void = 1 - rho/0.65.

Run:  cd tools/lattice && python compare_curves.py
"""
from __future__ import annotations

import json
import pathlib

import rbmk_cell1d as c1d

OOMC = pathlib.Path(__file__).resolve().parents[2] / "openmc" / "openmc_void_coeff.json"


def _slope(d, a, b):
    return (d[b] - d[a]) / (b - a)


if __name__ == "__main__":
    sweep = json.loads(OOMC.read_text())["sweep"]
    voids = [p["void_fraction"] for p in sweep]
    omc = {p["void_fraction"]: p["k"] for p in sweep}

    print(f"{'void':>6} {'k_fast':>10} {'k_OpenMC':>10} {'diff':>10}")
    fast = {}
    for v in voids:
        k, _, _ = c1d.solve_cell(v)
        fast[v] = k
        print(f"{v:6.3f} {k:10.5f} {omc[v]:10.5f} {k - omc[v]:+10.5f}", flush=True)

    v_op = voids[1]
    v_mid = voids[2]

    print()
    print(f"baseline k_inf (zero void): fast={fast[0.0]:.4f}  OpenMC={omc[0.0]:.4f}"
          f"  ratio={fast[0.0] / omc[0.0]:.3f}")
    for tag, b in (("operating 0->%.3f" % v_op, v_op),
                   ("matched   0->%.3f" % v_mid, v_mid)):
        sf, so = _slope(fast, 0.0, b), _slope(omc, 0.0, b)
        print(f"slope {tag}: fast={sf:+.5f}  OpenMC={so:+.5f}  ratio={sf / so:.2f}x")

    json.dump({"fast": {str(k): v for k, v in fast.items()},
               "openmc": {str(k): v for k, v in omc.items()}},
              open("compare_curves.json", "w"), indent=2)
