"""Derive the RBMK graphite temperature coefficient of reactivity with OpenMC.

Why this exists: the Java core carried a HAND-SET graphite coefficient of
-2.0e-5 dk/k per degC -- a NEGATIVE value, i.e. graphite heating makes the core
less reactive. Two independent primary sources say the opposite: INSAG-7 Table
II-I and sec. 4.7.3 call it "the POSITIVE graphite temperature coefficient of
reactivity" (+6e-5), and NUREG-1250 sec. 2.3.2.2 states "The graphite temperature
coefficient is positive" (+3.2e-5 fresh to +6e-5 steady). A positive graphite
coefficient is a genuine RBMK destabilising property and part of the accident
physics; a negative one silently removes it.

Method: hold fuel and coolant at their operating temperature, sweep the GRAPHITE
temperature over two values the nuclear-data library actually carries, and take
the reactivity change per kelvin. The same technique used for the void worth, so
the sign and magnitude are DERIVED from the data, not dialed.

Run:  OPENMC_CROSS_SECTIONS=... python3 rbmk_graphite_coeff.py
"""
from __future__ import annotations

import json
import pathlib

import openmc

import rbmk_cell

# Graphite natural-carbon data ships at these temperatures (see the library's
# error message / cross_sections.xml). Sweep between two of them.
T_LOW_K = 600.0
T_HIGH_K = 1200.0
NON_GRAPHITE_TEMP_K = 600.0
OUT = pathlib.Path("openmc_graphite_coeff.json")


def k_at_graphite_temp(t_graphite: float, particles: int = 6000,
                       batches: int = 50, inactive: int = 20) -> float:
    model = rbmk_cell.build_model(rbmk_cell.COOLANT_DENSITY_FULL)
    for mat in model.materials:
        mat.temperature = t_graphite if "graphite" in mat.name else NON_GRAPHITE_TEMP_K
    model.settings.particles = particles
    model.settings.batches = batches
    model.settings.inactive = inactive
    sp_path = model.run(output=False, cwd=f"_graphite/{int(t_graphite)}")
    with openmc.StatePoint(sp_path) as sp:
        return sp.keff.nominal_value


def main() -> None:
    k_lo = k_at_graphite_temp(T_LOW_K)
    k_hi = k_at_graphite_temp(T_HIGH_K)
    coeff = (k_hi - k_lo) / (k_lo * k_hi) / (T_HIGH_K - T_LOW_K)

    print(f"k(graphite {T_LOW_K:.0f} K) = {k_lo:.5f}")
    print(f"k(graphite {T_HIGH_K:.0f} K) = {k_hi:.5f}")
    print(f"DERIVED graphite temperature coefficient = {coeff:+.3e} dk/k per degC")
    print("  INSAG-7 (Table II-I, sec. 4.7.3): +6e-5, POSITIVE")
    print("  NUREG-1250 (sec. 2.3.2.2, Table 2.4): +3.2e-5 fresh .. +6e-5 steady, POSITIVE")
    print(f"  sign check: derived is {'POSITIVE -- agrees with both sources' if coeff > 0 else 'NEGATIVE -- contradicts both sources'}")

    OUT.write_text(json.dumps({
        "t_low_K": T_LOW_K, "t_high_K": T_HIGH_K,
        "k_low": k_lo, "k_high": k_hi,
        "graphite_coeff_per_degC": coeff,
        "sign": "positive" if coeff > 0 else "negative",
        "sources": {
            "INSAG-7": "+6e-5 per degC (positive)",
            "NUREG-1250": "+3.2e-5 (fresh) to +6e-5 (steady) per degC (positive)",
        },
    }, indent=2))
    print(f"\nwrote {OUT}")


if __name__ == "__main__":
    main()
