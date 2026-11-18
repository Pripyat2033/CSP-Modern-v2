"""DECISIVE TEST: does the missing thermal scattering law explain the 6x?

The deterministic cell (tools/lattice) has NO S(alpha,beta) thermal scattering
law; OpenMC HAS it. This script runs OpenMC twice at the same conditions --
once with the thermal law (as the repo does it) and once without -- and measures
the coolant-void coefficient in each case.

If turning the law OFF makes OpenMC's void worth jump toward the deterministic
cell's +0.09, then the missing law explains the whole discrepancy, and the 6x is
an artefact of comparing two different physics models.

Run:  source /workspace/tools/omc/env.sh
      cd openmc && python thermal_test.py
"""
from __future__ import annotations

import json

import openmc
import rbmk_cell

PARTICLES = 4000
BATCHES = 80
INACTIVE = 25
DENSITIES = [0.65, 0.45, 0.25, 0.0]   # full water -> void


def build(use_thermal: bool, coolant_density: float) -> openmc.Model:
    """As rbmk_cell.build_model, but optionally WITHOUT the thermal law."""
    mats = rbmk_cell.materials(max(coolant_density, 1e-6))
    channel = rbmk_cell.channel_material(mats)

    if not use_thermal:
        # strip the S(alpha,beta) tables -> free-gas (no thermal scattering law)
        mats["graphite"].thermal_scattering = []
        channel.remove_s_alpha_beta()

    chan_or = openmc.ZCylinder(r=rbmk_cell.CHANNEL_R)
    h = rbmk_cell.CELL_HALF
    x_lo = openmc.XPlane(-h, boundary_type="reflective")
    x_hi = openmc.XPlane(h, boundary_type="reflective")
    y_lo = openmc.YPlane(-h, boundary_type="reflective")
    y_hi = openmc.YPlane(h, boundary_type="reflective")
    square = +x_lo & -x_hi & +y_lo & -y_hi
    universe = openmc.Universe(cells=[
        openmc.Cell(name="channel", fill=channel, region=-chan_or),
        openmc.Cell(name="graphite", fill=mats["graphite"], region=+chan_or & square),
    ])
    settings = openmc.Settings()
    settings.batches = BATCHES
    settings.inactive = INACTIVE
    settings.particles = PARTICLES
    settings.run_mode = "eigenvalue"
    settings.source = openmc.IndependentSource(space=openmc.stats.Point((0, 0, 0)))
    settings.temperature = {"default": 293.6}
    return openmc.Model(openmc.Geometry(universe),
                        openmc.Materials([mats["graphite"], channel]), settings)


def run(use_thermal: bool) -> dict:
    import os
    ks = {}
    for rho in DENSITIES:
        workdir = f"thermal_{int(use_thermal)}_{int(rho*100)}"
        os.makedirs(workdir, exist_ok=True)
        model = build(use_thermal, rho)
        model.settings.particles = PARTICLES
        model.settings.batches = BATCHES
        model.settings.inactive = INACTIVE
        model.export_to_model_xml(workdir)
        openmc.run(cwd=workdir, threads=4, output=False)
        import glob
        sfile = sorted(glob.glob(os.path.join(workdir, "statepoint.*.h5")))[-1]
        with openmc.StatePoint(sfile) as s:
            ks[rho] = s.keff.nominal_value
        print(f"  thermal={use_thermal}  density={rho:.2f}  k={ks[rho]:.5f}", flush=True)
    return ks


def slope(ks, a, b):
    return (ks[b] - ks[a]) / ((b - a) / 0.65)  # per unit void fraction


if __name__ == "__main__":
    print("=== WITH thermal scattering law (as the repo does it) ===")
    with_tsl = run(True)
    print("=== WITHOUT thermal scattering law ===")
    no_tsl = run(False)

    out = {"with_thermal": {str(k): v for k, v in with_tsl.items()},
           "without_thermal": {str(k): v for k, v in no_tsl.items()}}
    # operating-point slope, water 0.65 -> 0.45 (void fraction 0 -> 0.31)
    out["dkdvoid_with"] = slope(with_tsl, 0.65, 0.45)
    out["dkdvoid_without"] = slope(no_tsl, 0.65, 0.45)
    json.dump(out, open("thermal_test.json", "w"), indent=2)

    print()
    print(f"void worth WITH thermal law    = {out['dkdvoid_with']:+.5f}")
    print(f"void worth WITHOUT thermal law = {out['dkdvoid_without']:+.5f}")
    print(f"deterministic cell (no law)    = +0.0900")
