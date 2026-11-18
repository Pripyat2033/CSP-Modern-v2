"""GEOMETRY TEST: do the two models' geometries explain the 6x?

The deterministic cell (tools/lattice/rbmk_cell.py) uses the real RBMK channel:
  - 114 mm graphite bore, an 88/4 mm Zr tube inside it, a water annulus between
    the tube and the graphite, and the 18-rod assembly inside the tube.
The repo's OpenMC cell (openmc/rbmk_cell.py) uses:
  - an 80 mm bore, NO tube, NO gap, graphite starting at the bore.

Same physics (thermal law ON, ENDF/B-VIII.0, 293.6 K), only the geometry differs.
If the void worth jumps toward +0.09 with the deterministic geometry, then the
geometry mismatch is the cause of the 6x.

Run:  source /workspace/tools/omc/env.sh && cd openmc && python geometry_test.py
"""
from __future__ import annotations

import glob
import json
import os

import openmc

PARTICLES = 4000
BATCHES = 80
INACTIVE = 25
DENSITIES = [0.65, 0.45, 0.25, 0.0]

# deterministic-cell geometry, in cm
HOLE_R = 5.7      # 114 mm graphite bore
TUBE_IR = 4.4     # 88 mm bore
TUBE_OR = 4.8     # 4 mm wall
CELL_HALF = 12.5  # 250 mm pitch
FUEL_VF = 0.62    # inside the tube, as the repo model uses
CLAD_VF = 0.12


def _mats(coolant_density: float) -> dict:
    fuel = openmc.Material(name="UO2 2.0%")
    fuel.add_element("U", 1.0, "ao", enrichment=2.0)
    fuel.add_element("O", 2.0, "ao")
    fuel.set_density("g/cm3", 10.4)
    clad = openmc.Material(name="Zr-1Nb")
    clad.add_element("Zr", 0.99, "wo")
    clad.add_element("Nb", 0.01, "wo")
    clad.set_density("g/cm3", 6.55)
    graphite = openmc.Material(name="graphite")
    graphite.add_element("C", 1.0, "ao")
    graphite.set_density("g/cm3", 1.65)
    graphite.add_s_alpha_beta("c_Graphite")
    water = openmc.Material(name="water")
    water.add_element("H", 2.0, "ao")
    water.add_element("O", 1.0, "ao")
    water.set_density("g/cm3", coolant_density)
    return {"fuel": fuel, "clad": clad, "graphite": graphite, "water": water}


def build(coolant_density: float, deterministic_geometry: bool) -> openmc.Model:
    voided = coolant_density <= 0.0
    m = _mats(coolant_density if not voided else 0.65)

    # bore contents: homogenised fuel+clad (+water)
    if voided:
        w = FUEL_VF + CLAD_VF
        inner = openmc.Material.mix_materials([m["fuel"], m["clad"]],
                                              [FUEL_VF / w, CLAD_VF / w], "vo")
    else:
        inner = openmc.Material.mix_materials(
            [m["fuel"], m["clad"], m["water"]],
            [FUEL_VF, CLAD_VF, 1 - FUEL_VF - CLAD_VF], "vo")
        inner.add_s_alpha_beta("c_H_in_H2O")   # after mixing, not before

    h = CELL_HALF
    sq = (+openmc.XPlane(-h, boundary_type="reflective")
          & -openmc.XPlane(h, boundary_type="reflective")
          & +openmc.YPlane(-h, boundary_type="reflective")
          & -openmc.YPlane(h, boundary_type="reflective"))

    if deterministic_geometry:
        tube = openmc.ZCylinder(r=TUBE_IR)
        tube_out = openmc.ZCylinder(r=TUBE_OR)
        hole = openmc.ZCylinder(r=HOLE_R)
        # the gap coolant needs its own material: mix_materials rejects S(a,b),
        # so the bore mix gets its table added AFTER mixing (see below)
        gap_water = openmc.Material(name="gap water")
        gap_water.add_element("H", 2.0, "ao")
        gap_water.add_element("O", 1.0, "ao")
        gap_water.set_density("g/cm3", 0.65 if voided else coolant_density)
        gap_water.add_s_alpha_beta("c_H_in_H2O")
        cells = [
            openmc.Cell(name="bore", fill=inner, region=-tube),
            openmc.Cell(name="tube", fill=m["clad"], region=+tube & -tube_out),
            openmc.Cell(name="gap", fill=gap_water, region=+tube_out & -hole),
            openmc.Cell(name="graphite", fill=m["graphite"], region=+hole & sq),
        ]
        materials = [m["graphite"], gap_water, m["clad"], inner]
    else:
        bore = openmc.ZCylinder(r=4.0)
        cells = [
            openmc.Cell(name="bore", fill=inner, region=-bore),
            openmc.Cell(name="graphite", fill=m["graphite"], region=+bore & sq),
        ]
        materials = [m["graphite"], inner]

    u = openmc.Universe(cells=cells)
    s = openmc.Settings()
    s.batches = BATCHES
    s.inactive = INACTIVE
    s.particles = PARTICLES
    s.run_mode = "eigenvalue"
    s.source = openmc.IndependentSource(space=openmc.stats.Point((0, 0, 0)))
    s.temperature = {"default": 293.6}
    return openmc.Model(openmc.Geometry(u), openmc.Materials(materials), s)


def sweep(tag: str, deterministic_geometry: bool) -> dict:
    ks = {}
    for rho in DENSITIES:
        wd = f"geom_{tag}_{int(rho*100)}"
        os.makedirs(wd, exist_ok=True)
        build(rho, deterministic_geometry).export_to_model_xml(wd)
        openmc.run(cwd=wd, threads=4, output=False)
        f = sorted(glob.glob(os.path.join(wd, "statepoint.*.h5")))[-1]
        with openmc.StatePoint(f) as sp:
            ks[rho] = sp.keff.nominal_value
        print(f"  {tag} rho={rho:.2f} k={ks[rho]:.5f}", flush=True)
    return ks


if __name__ == "__main__":
    print("=== repo geometry (80 mm bore, no tube) ===")
    repo = sweep("repo", False)
    print("=== deterministic geometry (114 mm bore + tube + gap) ===")
    det = sweep("det", True)

    def slope(ks):
        return (ks[0.45] - ks[0.65]) / 0.308

    out = {"repo": {str(k): v for k, v in repo.items()},
           "det": {str(k): v for k, v in det.items()},
           "dkdvoid_repo": slope(repo), "dkdvoid_det": slope(det)}
    json.dump(out, open("geometry_test.json", "w"), indent=2)
    print()
    print(f"void worth, repo geometry          = {out['dkdvoid_repo']:+.5f}")
    print(f"void worth, deterministic geometry = {out['dkdvoid_det']:+.5f}")
    print(f"deterministic cell (its own code)  = +0.0900")
