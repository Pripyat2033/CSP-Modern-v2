#!/usr/bin/env python3
"""Emit the two-group reactor constants the Java core runs on, from the lattice.

Runs the 1-D cell solve over a grid of void fractions and writes the two-group
constants (D, Sigma_r, nu*Sigma_f, Sigma_a, Sigma_s(1->2)) to JSON. The Java core
loads this file instead of using hand-set cross sections, so its feedback is
derived from evaluated nuclear data and the real channel geometry.

Output: ../../src/main/resources/rbmk_lattice.json
"""
from __future__ import annotations

import json
import os

import numpy as np

import endf
import rbmk_cell1d as c1d
import rbmk_spectrum as spectrum

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                   "..", "..", "src", "main", "resources", "rbmk_lattice.json")


def constants_at(void_fraction):
    k, flux, data = c1d.solve_cell(void_fraction)
    two = c1d.homogenise_2group(flux, data)
    # fast removal = fast absorption + down-scatter out of the fast group
    sr1 = float(two["Sa"][0] + two["Ss12"])
    return {
        "kInf": float(k),
        "d1": float(two["D"][0]) if "D" in two else float(1.0 / (3 * two["St"][0])),
        "sr1": sr1,
        "nsf1": float(two["nuSf"][0]),
        "d2": float(1.0 / (3 * two["St"][1])),
        "sa2": float(two["Sa"][1]),
        "nsf2": float(two["nuSf"][1]),
        "ss12": float(two["Ss12"]),
        "ss21": float(two["Ss21"]),
    }


def control_constants(flux, geom):
    """Two-group constants for the control-rod materials (graphite, B4C).

    Both are derived the same way as the cell: evaluated ENDF data homogenised
    over the cell's own flux. ``geom`` is the fuel-cell data tuple, used only for
    its radial node volumes so the mixture collapses on the same mesh.
    Returns dict of material -> two-group dict.
    """
    n_c = 1.65 / 12.011 * 6.022e23 / 1e24          # graphite, 1.65 g/cm^3
    n_b4c = 2.52 / (4 * 10.811 + 12.011) * 6.022e23 / 1e24   # B4C, 2.52 g/cm^3
    mixtures = {
        "graphite": [("C12", n_c, False)],
        "b4c": [("B10", 0.199 * 4 * n_b4c, False),
                ("B11", 0.801 * 4 * n_b4c, False),
                ("C12", n_b4c, False)],
    }
    r, _, _, _, _, _, _, V, _ = geom
    n_nodes = len(V)
    out = {}
    for name, mixture in mixtures.items():
        m = spectrum.mixture_macros(mixture, endf.weight_combined)
        # one homogeneous material: repeat each fine-group constant over the nodes
        tile = lambda a: np.repeat(np.asarray(a)[:, None], n_nodes, axis=1)
        data = (r, [name] * n_nodes, tile(m["St"]), tile(m["Sa"]), tile(m["Sf"]),
                tile(m["nuSf"]), np.repeat(np.asarray(m["Ss"])[:, :, None], n_nodes, axis=2),
                V, None)
        two = c1d.homogenise_2group(flux, data)
        out[name] = {
            "d1": float(1.0 / (3 * two["St"][0])),
            "sr1": float(two["Sa"][0] + two["Ss12"]),
            "nsf1": float(two["nuSf"][0]),
            "d2": float(1.0 / (3 * two["St"][1])),
            "sa2": float(two["Sa"][1]),
            "nsf2": float(two["nuSf"][1]),
            "ss12": float(two["Ss12"]),
            "ss21": float(two["Ss21"]),
        }
    return out


def main():
    voids = [round(v, 3) for v in np.linspace(0.0, 1.0, 11)]
    table = []
    for vf in voids:
        c = constants_at(vf)
        c["void"] = vf
        table.append(c)
        print(f"  void={vf:.2f}  k_inf={c['kInf']:.5f}  "
              f"D1={c['d1']:.4f} Sr1={c['sr1']:.5f} nSf1={c['nsf1']:.5f}  "
              f"D2={c['d2']:.4f} Sa2={c['sa2']:.5f} nSf2={c['nsf2']:.5f} Ss12={c['ss12']:.5f}")

    # control materials at the reference (cold) flux
    _, flux, geom = c1d.solve_cell(0.0)
    controls = control_constants(flux, geom)
    for name, m in controls.items():
        print(f"  control {name}: D1={m['d1']:.4f} Sr1={m['sr1']:.5f} "
              f"D2={m['d2']:.4f} Sa2={m['sa2']:.5f} Ss12={m['ss12']:.5f}")

    doc = {
        "source": "tools/lattice (ENDF/B-VIII.0 + RBMK channel geometry)",
        "groups": 2,
        "thermalCutEV": c1d.spec.THERMAL_CUT,
        "betaEff": 0.0065,
        "table": table,
        "controls": controls,
    }
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w") as f:
        json.dump(doc, f, indent=2)
    print(f"\nwrote {os.path.relpath(OUT)}")


if __name__ == "__main__":
    main()
