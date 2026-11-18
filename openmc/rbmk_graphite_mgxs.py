"""How the homogenised 2-group cell constants move with GRAPHITE temperature.

The unified core computes a graphite temperature but never feeds it to the
neutronics, so the RBMK's positive graphite temperature coefficient -- a real
destabilising property, +6e-5 dk/k/degC in INSAG-7 -- is absent. This script
measures WHERE the effect lives in the two-group constants, so it can be wired in
through the cross sections (the project's rule: feedback enters the neutron
balance, not a fitted readout term).

Method: fix fuel and coolant at their operating temperature, sweep the graphite
temperature over two values the library carries (600 K, 1200 K), and report each
homogenised constant and its fractional change per kelvin. Same cell and same
MGXS tallies as rbmk_mgxs.py, so the numbers are directly comparable.

Run:  OPENMC_CROSS_SECTIONS=... python3 rbmk_graphite_mgxs.py
"""
from __future__ import annotations

import json
import pathlib

import openmc

import rbmk_cell
import rbmk_mgxs

# The S(alpha,beta) tables top out at 800 K (water) and 800 K (graphite), so the
# operating graphite temperature (~873 K) cannot be measured directly. Measure the
# SLOPE over 600->800 K (both on the grids), and anchor the Java scaling factor at
# the core's nominal graphite temperature (600 degC = 873.15 K), so the correction
# is a perturbation about the nominal state (factor = 1 there).
T_LOW_K = 600.0
T_HIGH_K = 800.0
NON_GRAPHITE_K = 600.0
NOMINAL_GRAPHITE_K = 873.15   # 600 degC, the core's reference graphite temperature
OUT = pathlib.Path("openmc_graphite_mgxs.json")

# The constants the Java reader consumes, in table order.
KEYS = ("d1", "sr1", "nsf1", "d2", "sa2", "nsf2", "ss12", "ss21")


def constants_at(t_graphite: float, particles: int, batches: int, inactive: int) -> dict:
    model = rbmk_cell.build_model(rbmk_cell.COOLANT_DENSITY_FULL)
    # The operating temperatures (600 degC graphite) are not on the library grid,
    # so let OpenMC interpolate between the tabulated temperatures.
    model.settings.temperature = {"default": NON_GRAPHITE_K, "method": "interpolation"}
    for mat in model.materials:
        mat.temperature = t_graphite if "graphite" in mat.name else NON_GRAPHITE_K
    mesh = rbmk_mgxs._cell_mesh()
    for x in rbmk_mgxs._mgxs_set(mesh):
        for tally in x.tallies.values():
            model.tallies.append(tally, merge=True)
    sp = rbmk_mgxs._run(model, particles, batches, inactive,
                        cwd=f"_graphite_mgxs/{int(t_graphite)}")
    row = rbmk_mgxs._extract(sp, mesh)
    sp.close()
    return row


def main() -> None:
    import argparse
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--particles", type=int, default=20000)
    ap.add_argument("--batches", type=int, default=200)
    ap.add_argument("--inactive", type=int, default=50)
    args = ap.parse_args()

    lo = constants_at(T_LOW_K, args.particles, args.batches, args.inactive)
    hi = constants_at(T_HIGH_K, args.particles, args.batches, args.inactive)
    dT = T_HIGH_K - T_LOW_K

    print(f"\nhomogenised constants, graphite {T_LOW_K:.0f} K -> {T_HIGH_K:.0f} K")
    print(f"{'const':>6} {T_LOW_K:>12.0f} {T_HIGH_K:>12.0f} {'frac/k':>12}")
    frac = {}
    for k in KEYS:
        f = (hi[k] - lo[k]) / lo[k] / dT if lo[k] != 0 else 0.0
        frac[k] = f
        print(f"{k:>6} {lo[k]:12.6f} {hi[k]:12.6f} {f:+12.3e}")

    k_lo, k_hi = lo["kInf"], hi["kInf"]
    coeff = (k_hi - k_lo) / (k_lo * k_hi) / dT
    print(f"\nk_inf  {k_lo:.5f} -> {k_hi:.5f}")
    print(f"DERIVED graphite temperature coefficient = {coeff:+.3e} dk/k per degC")
    print(f"  INSAG-7 / NUREG-1250: POSITIVE (+3.2e-5 fresh .. +6e-5 steady)")

    OUT.write_text(json.dumps({
        "source": "openmc (corrected RBMK channel cell, MGXS)",
        "library": "ENDF/B-VIII.0",
        "measuredLowK": T_LOW_K, "measuredHighK": T_HIGH_K,
        "nominalGraphiteK": NOMINAL_GRAPHITE_K,
        "kLow": k_lo, "kHigh": k_hi,
        "slopeCoeffPerC": coeff,
        "fracPerK": frac,
    }, indent=2))
    print(f"\nslope measured over {T_LOW_K:.0f}-{T_HIGH_K:.0f} K; "
          f"scaling factor anchored at {NOMINAL_GRAPHITE_K:.2f} K "
          f"({NOMINAL_GRAPHITE_K - 273.15:.1f} degC)")
    print(f"wrote {OUT}")


if __name__ == "__main__":
    main()
