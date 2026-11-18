"""CONFIRMING TEST: the homogenisation volume fractions are the bug.

The repo's OpenMC cell (openmc/rbmk_cell.py) homogenises the 18-rod assembly with
FUEL_VF=0.62, CLAD_VF=0.12, water=0.26. The REAL channel is fuel 0.324, clad 0.106,
water 0.570 -- i.e. the repo has ~2x too much fuel and ~half the water.

This runs the SAME homogenised cell twice -- once with the repo's volume fractions,
once with the real ones -- in the real 88 mm bore, at the same 600 K / same water.
If the real fractions push the void worth from ~+0.02 toward the explicit cluster's
+0.16, the volume fractions ARE the bug.

Run: source /workspace/tools/omc/env.sh && cd openmc && python vf_test.py
"""
from __future__ import annotations

import glob
import json
import os

import openmc

PARTICLES = 6000
BATCHES = 100
INACTIVE = 30
TEMP_K = 600.0
VOIDS = [0.0, 0.3, 0.6]
RHO = {v: 0.75 - 0.714 * v for v in VOIDS}

PITCH = 25.0
TUBE_IR = 4.4
# real channel volume fractions inside the 88 mm bore (computed in vf_test output)
REAL = dict(fuel=0.324, clad=0.106, water=0.570)
REPO = dict(fuel=0.620, clad=0.120, water=0.260)


def _mats(rho: float) -> dict:
    fuel = openmc.Material(name="UO2 2.0%", temperature=TEMP_K)
    fuel.add_element("U", 1.0, "ao", enrichment=2.0)
    fuel.add_element("O", 2.0, "ao")
    fuel.set_density("g/cm3", 10.4)
    clad = openmc.Material(name="Zr-1Nb", temperature=TEMP_K)
    clad.add_element("Zr", 0.99, "wo")
    clad.add_element("Nb", 0.01, "wo")
    clad.set_density("g/cm3", 6.55)
    graphite = openmc.Material(name="graphite", temperature=TEMP_K)
    graphite.add_element("C", 1.0, "ao")
    graphite.set_density("g/cm3", 1.65)
    graphite.add_s_alpha_beta("c_Graphite")
    water = openmc.Material(name="water", temperature=TEMP_K)
    water.add_element("H", 2.0, "ao")
    water.add_element("O", 1.0, "ao")
    water.set_density("g/cm3", max(rho, 0.036))
    # NOTE: no add_s_alpha_beta here -- this material is fed to mix_materials,
    # which rejects materials carrying S(a,b). The mix gets the table after.
    return {"fuel": fuel, "clad": clad, "graphite": graphite, "water": water}


def build(void: float, vf: dict) -> openmc.Model:
    rho = RHO[void]
    m = _mats(rho)
    h = PITCH / 2
    sq = (+openmc.XPlane(-h, boundary_type="reflective")
          & -openmc.XPlane(h, boundary_type="reflective")
          & +openmc.YPlane(-h, boundary_type="reflective")
          & -openmc.YPlane(h, boundary_type="reflective"))
    hole = openmc.ZCylinder(r=5.7)
    tube = openmc.ZCylinder(r=TUBE_IR)
    tube_out = openmc.ZCylinder(r=4.8)

    if rho <= 0.1:
        # near-full void: fuel + clad only (steam is negligible)
        w = vf["fuel"] + vf["clad"]
        inner = openmc.Material.mix_materials([m["fuel"], m["clad"]],
                                              [vf["fuel"] / w, vf["clad"] / w], "vo")
    else:
        inner = openmc.Material.mix_materials(
            [m["fuel"], m["clad"], m["water"]],
            [vf["fuel"], vf["clad"], vf["water"]], "vo")
        inner.add_s_alpha_beta("c_H_in_H2O")

    gap_water = openmc.Material(name="gap water", temperature=TEMP_K)
    gap_water.add_element("H", 2.0, "ao")
    gap_water.add_element("O", 1.0, "ao")
    gap_water.set_density("g/cm3", max(rho, 0.036))
    if rho > 0.1:
        gap_water.add_s_alpha_beta("c_H_in_H2O")

    cells = [
        openmc.Cell(name="graphite", fill=m["graphite"], region=+hole & sq),
        openmc.Cell(name="gap", fill=gap_water, region=+tube_out & -hole),
        openmc.Cell(name="tube", fill=m["clad"], region=+tube & -tube_out),
        openmc.Cell(name="bore", fill=inner, region=-tube),
    ]
    s = openmc.Settings()
    s.batches = BATCHES
    s.inactive = INACTIVE
    s.particles = PARTICLES
    s.run_mode = "eigenvalue"
    s.source = openmc.IndependentSource(
        space=openmc.stats.Box((-h, -h, -1.0), (h, h, 1.0)))
    s.temperature = {"default": TEMP_K}
    return openmc.Model(openmc.Geometry(openmc.Universe(cells=cells)),
                        openmc.Materials([m["graphite"], gap_water, m["clad"], inner]), s)


def sweep(tag: str, vf: dict) -> dict:
    ks = {}
    for v in VOIDS:
        wd = f"vf_{tag}_{int(v*100)}"
        os.makedirs(wd, exist_ok=True)
        build(v, vf).export_to_model_xml(wd)
        openmc.run(cwd=wd, threads=4, output=False)
        f = sorted(glob.glob(os.path.join(wd, "statepoint.*.h5")))[-1]
        with openmc.StatePoint(f) as sp:
            ks[v] = sp.keff.nominal_value
        print(f"  {tag} void={v:.2f} k={ks[v]:.5f}", flush=True)
    return ks


if __name__ == "__main__":
    repo = sweep("repo", REPO)
    real = sweep("real", REAL)
    def slope(ks):
        return (ks[0.3] - ks[0.0]) / 0.3
    print(f"\nvoid worth, repo VF (0.62 fuel)  = {slope(repo):+.5f}")
    print(f"void worth, real VF (0.32 fuel)  = {slope(real):+.5f}")
    print("explicit 18-rod cluster          = +0.15769")
    json.dump({"repo": {str(k): v for k, v in repo.items()},
               "real": {str(k): v for k, v in real.items()},
               "slope_repo": slope(repo), "slope_real": slope(real)},
              open("vf_test.json", "w"), indent=2)
