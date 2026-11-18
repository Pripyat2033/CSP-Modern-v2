"""Emit the Java core's two-group lattice table from the CORRECTED OpenMC cell.

This is the missing link the plan needs: until now the live table
(`src/main/resources/rbmk_lattice.json`) was built by `tools/lattice/` from a
1-D cell that overstates the coolant-void worth by ~3.5x, and a hand-set dial
(`RbmkXsec.VOID_WORTH_SCALE = 0.233`) dragged it back to the INSAG-7 value. This
script rebuilds the table from the validated Monte Carlo cell (`rbmk_cell.py`),
so the raw, undialed void worth should land near +0.020 on its own.

Method (standard few-group homogenisation):
  - one RBMK lattice cell, reflective boundaries, at each coolant density;
  - two-group macroscopic cross sections from OpenMC MGXS tallies, homogenised
    over the WHOLE cell on a single-bin mesh (the cell's flux weighting);
  - the same schema as `tools/lattice/emit_constants.py` writes, so the Java
    reader (`LatticeTable.java`) is unchanged.

Group boundary 0.625 eV (fast above, thermal below) matches the shipped table's
`thermalCutEV`.

Run:  OPENMC_CROSS_SECTIONS=... python3 rbmk_mgxs.py [--particles N] [--batches N]
Writes: rbmk_lattice_openmc.json  (compare, do not overwrite the shipped file)
"""
from __future__ import annotations

import argparse
import json
import pathlib

import numpy as np
import openmc
import openmc.mgxs as mgxs

import rbmk_cell

THERMAL_CUT_EV = 0.625
FAST_TOP_EV = 2.0e7
VOIDS = [round(v, 3) for v in np.linspace(0.0, 1.0, 11)]
OUT = pathlib.Path("rbmk_lattice_openmc.json")


def _groups() -> mgxs.EnergyGroups:
    return mgxs.EnergyGroups([1.0e-5, THERMAL_CUT_EV, FAST_TOP_EV])


def _cell_mesh() -> openmc.RegularMesh:
    """One bin covering the whole lattice cell cross-section (homogenisation)."""
    mesh = openmc.RegularMesh()
    mesh.dimension = [1, 1, 1]
    mesh.lower_left = [-rbmk_cell.CELL_HALF, -rbmk_cell.CELL_HALF, -1.0]
    mesh.upper_right = [rbmk_cell.CELL_HALF, rbmk_cell.CELL_HALF, 1.0]
    return mesh


def _mgxs_set(mesh: openmc.RegularMesh) -> list:
    groups = _groups()
    kw = dict(energy_groups=groups, domain=mesh, domain_type="mesh")
    return [
        mgxs.TotalXS(**kw),
        mgxs.AbsorptionXS(**kw),
        mgxs.FissionXS(nu=True, **kw),
        mgxs.DiffusionCoefficient(**kw),
        mgxs.ScatterMatrixXS(**kw),
    ]


def _run(model: openmc.Model, particles: int, batches: int, inactive: int,
         cwd: str) -> openmc.StatePoint:
    model.settings.particles = particles
    model.settings.batches = batches
    model.settings.inactive = inactive
    sp_path = model.run(output=False, cwd=cwd)
    return openmc.StatePoint(sp_path)


def _extract(sp: openmc.StatePoint, mesh: openmc.RegularMesh) -> dict:
    """Homogenised two-group constants for the cell at this coolant density."""
    groups = _groups()
    kw = dict(energy_groups=groups, domain=mesh, domain_type="mesh")

    total = mgxs.TotalXS(**kw)
    absorb = mgxs.AbsorptionXS(**kw)
    nufis = mgxs.FissionXS(nu=True, **kw)
    diff = mgxs.DiffusionCoefficient(**kw)
    smat = mgxs.ScatterMatrixXS(**kw)
    for x in (total, absorb, nufis, diff, smat):
        x.load_from_statepoint(sp)

    tr = total.get_xs()
    sa = absorb.get_xs()
    nsf = nufis.get_xs()
    d = diff.get_xs()
    # ScatterMatrixXS: [g_in][g_out]; index 0 = fast, 1 = thermal.
    m = smat.get_xs()
    ss12 = float(m[0, 1])   # down-scatter fast -> thermal
    ss21 = float(m[1, 0])   # up-scatter thermal -> fast
    sr1 = float(sa[0] + ss12)   # fast removal = fast absorption + down-scatter out
    return {
        "kInf": float(sp.keff.nominal_value),
        "kSigma": float(sp.keff.std_dev),
        "d1": float(d[0]),
        "sr1": sr1,
        "nsf1": float(nsf[0]),
        "d2": float(d[1]),
        "sa2": float(sa[1]),
        "nsf2": float(nsf[1]),
        "ss12": ss12,
        "ss21": ss21,
        "_transport": float(tr[0]),
    }


def build_table(particles: int, batches: int, inactive: int) -> list[dict]:
    table = []
    for void in VOIDS:
        density = rbmk_cell.COOLANT_DENSITY_FULL * (1.0 - void)
        model = rbmk_cell.build_model(density)
        mesh = _cell_mesh()
        for x in _mgxs_set(mesh):
            for tally in x.tallies.values():
                model.tallies.append(tally, merge=True)
        sp = _run(model, particles, batches, inactive, cwd=f"_mgxs/void_{int(void*100):03d}")
        row = _extract(sp, mesh)
        row["void"] = void
        table.append(row)
        print(f"  void={void:.2f}  k_inf={row['kInf']:.5f}+/-{row['kSigma']:.5f}  "
              f"D1={row['d1']:.4f} Sr1={row['sr1']:.5f} nSf1={row['nsf1']:.5f}  "
              f"D2={row['d2']:.4f} Sa2={row['sa2']:.5f} nSf2={row['nsf2']:.5f} "
              f"Ss12={row['ss12']:.5f}", flush=True)
        sp.close()
    return table


def raw_void_worth(table: list[dict]) -> float:
    """Whole-core-scale secant void worth, dk/(k1*k2) per unit void, near operating.

    Measured on the homogenised cell k_inf between void 0.0 and 0.4 -- the same
    span the Java diagnostic (`/rbmk dial`) uses, so the two are comparable.
    """
    k0 = next(r["kInf"] for r in table if r["void"] == 0.0)
    k4 = next(r["kInf"] for r in table if r["void"] == 0.4)
    return (k4 - k0) / (k0 * k4) / 0.4


def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--particles", type=int, default=20000)
    ap.add_argument("--batches", type=int, default=200)
    ap.add_argument("--inactive", type=int, default=50)
    args = ap.parse_args()

    print(f"RBMK lattice cell -> two-group table (OpenMC, {args.particles} x "
          f"{args.batches} batches)")
    table = build_table(args.particles, args.batches, args.inactive)

    worth = raw_void_worth(table)
    print(f"\nRAW (undialed) cell void worth, void 0.0->0.4 = {worth:+.4f} dk/k per unit void")
    print("  INSAG-7 / NUREG-1250 whole-core operating value = +0.0200")
    print(f"  ratio to source = {worth/0.020:.2f}x")

    doc = {
        "source": "openmc (corrected RBMK channel cell, Monte Carlo MGXS)",
        "library": "ENDF/B-VIII.0",
        "libraryStatus": "to_verify",
        "groups": 2,
        "thermalCutEV": THERMAL_CUT_EV,
        "betaEff": 0.0065,
        "table": table,
        "rawVoidWorth": worth,
    }
    OUT.write_text(json.dumps(doc, indent=2))
    print(f"\nwrote {OUT}")


if __name__ == "__main__":
    main()
