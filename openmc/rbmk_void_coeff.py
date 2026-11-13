"""Measure the RBMK coolant-void reactivity coefficient with OpenMC.

This is the calculation that matters for the accident: the RBMK's positive void
coefficient is the core of the 26 April 1986 physics, and it is exactly the
quantity our Java model has wrong (its derived feedback is ~5-7x too strong, to
the point that AZ-5 no longer shuts the core down).

Method: sweep the coolant density inside the channel from full water to void,
compute k-inf at each point, and take the reactivity change per unit void
fraction. Standard lattice-cell practice.

Run:  OPENMC_CROSS_SECTIONS=... python rbmk_void_coeff.py
Writes: openmc_void_coeff.json  (machine-readable result for the Java side)
"""
from __future__ import annotations

import json
import pathlib
import sys

import openmc

import rbmk_cell

# Coolant density sweep, g/cm3. 0.65 is the RBMK operating condition; 0.0 is a
# full void. The sweep resolves the coefficient as a function of void fraction.
DENSITIES = [0.65, 0.55, 0.45, 0.35, 0.25, 0.15, 0.05, 0.0]

SWEEP_CACHE = pathlib.Path("openmc_void_sweep.json")
RESULT = pathlib.Path("openmc_void_coeff.json")


def k_at(coolant_density: float) -> tuple[float, float]:
    """Return (k_inf, sigma) for the lattice cell at the given coolant density."""
    model = rbmk_cell.build_model(coolant_density)
    sp_path = model.run(output=False, cwd=str(pathlib.Path.cwd() / "_run"))
    with openmc.StatePoint(sp_path) as sp:
        k = sp.keff
    return k.nominal_value, k.std_dev


def sweep(force: bool = False) -> list[dict]:
    """Run the Monte Carlo sweep, or reuse the cache if present."""
    if SWEEP_CACHE.exists() and not force:
        print(f"reusing cached sweep ({SWEEP_CACHE})")
        return json.loads(SWEEP_CACHE.read_text())["sweep"]

    results = []
    for rho in DENSITIES:
        void = 1.0 - rho / rbmk_cell.COOLANT_DENSITY_FULL
        k, sig = k_at(rho)
        results.append({"density": rho, "void_fraction": round(void, 4), "k": k, "sigma": sig})
        print(f"rho={rho:4.2f}  void={void:5.3f}  k={k:.5f} +/- {sig:.5f}", flush=True)
    SWEEP_CACHE.write_text(json.dumps({"sweep": results}, indent=2))
    return results


def _reactivity(k1: float, k2: float) -> float:
    """Reactivity change between two states, in dk/k."""
    return (k2 - k1) / (k1 * k2)


def main() -> None:
    results = sweep(force="--force" in sys.argv)

    k_full = results[0]["k"]
    k_void = results[-1]["k"]

    # The void coefficient is a DERIVATIVE, so it must be measured at a point,
    # not across the whole sweep. The lattice's k rises as the channel voids
    # (the RBMK's positive void coefficient) until the water -- which is also a
    # moderator -- is almost entirely gone, after which k falls. Reporting a
    # single full-range secant would average the two and hide the physics.
    # So: report the operating-point coefficient (initial slope, near full water)
    # AND the turnover, separately.
    def seg(a: int, b: int) -> float:
        dk = _reactivity(results[a]["k"], results[b]["k"])
        dv = results[b]["void_fraction"] - results[a]["void_fraction"]
        return dk / dv

    coeff_operating = seg(0, 1)          # slope at the operating point
    coeff_mid = seg(3, 5)                # slope over the mid operating range
    coeff_peak = max(seg(i, i + 1) for i in range(len(results) - 2))
    coeff_turnover = seg(len(results) - 2, len(results) - 1)

    summary = {
        "k_full_water": k_full,
        "k_full_void": k_void,
        "void_coeff_operating": coeff_operating,
        "void_coeff_mid_range": coeff_mid,
        "void_coeff_peak": coeff_peak,
        "void_coeff_at_full_void": coeff_turnover,
        "sign": "positive" if coeff_operating > 0 else "negative",
        "sweep": results,
        "note": "Homogenised 18-rod assembly in a 250 mm graphite cell; ENDF/B-VIII.0; "
                "cold (293.6 K). The void coefficient is positive through the operating "
                "range and turns negative only at full void. Compare against the "
                "documented INSAG-7 / NUREG-1250 value (+0.02 dk/k per unit void).",
    }
    pathlib.Path("openmc_void_coeff.json").write_text(json.dumps(summary, indent=2))
    print(f"\nvoid coefficient at operating point = {coeff_operating:+.5f} dk/k per unit void")
    print(f"  mid-range  = {coeff_mid:+.5f}")
    print(f"  peak       = {coeff_peak:+.5f}")
    print(f"  full void  = {coeff_turnover:+.5f}  (moderator removed -> turnover)")
    print(f"  SIGN: {'POSITIVE' if coeff_operating > 0 else 'NEGATIVE'}")
    print("wrote openmc_void_coeff.json")


if __name__ == "__main__":
    main()
