"""DECISIVE TEST 3: the REAL 18-rod cluster, explicit, in OpenMC.

The fast cell (tools/lattice) squeezes the 18 rods into a smooth annular band
and solves with 1-D diffusion theory. This builds the ACTUAL cluster -- 6 rods on
an inner ring, 12 on an outer ring, each 13.6 mm OD with 0.9 mm cladding -- inside
the 88 mm tube, in the 114 mm bore, in the 250 mm graphite cell, and runs real
Monte Carlo transport. Same water, same temperature as the fast cell.

If this gives ~+0.02, the fast cell's ring/diffusion shortcut is the culprit.
If it gives ~+0.115, the fast cell is right and something else differs.

Run: source /workspace/tools/omc/env.sh && cd openmc && python cluster_test.py
"""
from __future__ import annotations

import glob
import json
import math
import os

import openmc

PARTICLES = 6000
BATCHES = 100
INACTIVE = 30
TEMP_K = 600.0

VOIDS = [0.0, 0.3, 0.6, 1.0]
RHO = {v: 0.75 - 0.714 * v for v in VOIDS}

PITCH = 25.0
HOLE_R = 5.7
TUBE_IR, TUBE_OR = 4.4, 4.8
ROD_OD = 1.36
CLAD_T = 0.09
FUEL_R = ROD_OD / 2 - CLAD_T          # 0.59 cm
CLAD_R = ROD_OD / 2                   # 0.68 cm
INNER_RING_R = 2.0                     # 6 rods
OUTER_RING_R = 3.4                     # 12 rods


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
    if rho > 0.1:
        water.add_s_alpha_beta("c_H_in_H2O")
    return {"fuel": fuel, "clad": clad, "graphite": graphite, "water": water}


def build(void: float) -> openmc.Model:
    rho = RHO[void]
    m = _mats(rho)
    h = PITCH / 2

    sq = (+openmc.XPlane(-h, boundary_type="reflective")
          & -openmc.XPlane(h, boundary_type="reflective")
          & +openmc.YPlane(-h, boundary_type="reflective")
          & -openmc.YPlane(h, boundary_type="reflective"))
    hole = openmc.ZCylinder(r=HOLE_R)
    tube = openmc.ZCylinder(r=TUBE_IR)
    tube_out = openmc.ZCylinder(r=TUBE_OR)

    cells = [
        openmc.Cell(name="graphite", fill=m["graphite"], region=+hole & sq),
        openmc.Cell(name="gap-water", fill=m["water"], region=+tube_out & -hole),
        openmc.Cell(name="tube", fill=m["clad"], region=+tube & -tube_out),
    ]

    # 6 inner + 12 outer rods; everything inside the tube not in a rod is water
    rods = []
    for k in range(6):
        a = 2 * math.pi * k / 6
        rods.append((INNER_RING_R * math.cos(a), INNER_RING_R * math.sin(a)))
    for k in range(12):
        a = 2 * math.pi * k / 12 + math.pi / 12
        rods.append((OUTER_RING_R * math.cos(a), OUTER_RING_R * math.sin(a)))

    rod_regions = []
    for i, (x, y) in enumerate(rods):
        fcyl = openmc.ZCylinder(x0=x, y0=y, r=FUEL_R)
        ccyl = openmc.ZCylinder(x0=x, y0=y, r=CLAD_R)
        cells.append(openmc.Cell(name=f"fuel-{i}", fill=m["fuel"], region=-fcyl))
        cells.append(openmc.Cell(name=f"clad-{i}", fill=m["clad"],
                                 region=+fcyl & -ccyl))
        rod_regions.append(ccyl)
    water_region = -tube
    for rr in rod_regions:
        water_region = water_region & -rr
    cells.append(openmc.Cell(name="coolant", fill=m["water"], region=water_region))

    u = openmc.Universe(cells=cells)
    s = openmc.Settings()
    s.batches = BATCHES
    s.inactive = INACTIVE
    s.particles = PARTICLES
    s.run_mode = "eigenvalue"
    s.source = openmc.IndependentSource(
        space=openmc.stats.Box((-h, -h, -1.0), (h, h, 1.0)))
    s.temperature = {"default": TEMP_K}
    return openmc.Model(openmc.Geometry(u),
                        openmc.Materials(list(m.values())), s)


if __name__ == "__main__":
    ks = {}
    for v in VOIDS:
        wd = f"cluster_{int(v*100)}"
        os.makedirs(wd, exist_ok=True)
        build(v).export_to_model_xml(wd)
        openmc.run(cwd=wd, threads=4, output=False)
        f = sorted(glob.glob(os.path.join(wd, "statepoint.*.h5")))[-1]
        with openmc.StatePoint(f) as sp:
            ks[v] = sp.keff.nominal_value
        print(f"  void={v:.2f} k={ks[v]:.5f}", flush=True)
    slope = (ks[0.3] - ks[0.0]) / 0.3
    print(f"\nvoid worth, OpenMC REAL 18-rod cluster = {slope:+.5f}")
    print("void worth, fast cell (ring+diffusion) = +0.11500")
    print("void worth, INSAG-7 literature         = +0.02000")
    json.dump({"ks": {str(k): v for k, v in ks.items()}, "slope": slope},
              open("cluster_test.json", "w"), indent=2)
