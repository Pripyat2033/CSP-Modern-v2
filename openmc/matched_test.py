"""MATCHED TEST: run OpenMC at the fast cell's OWN conditions.

The fast cell (tools/lattice/rbmk_cell1d.py) runs HOT (600 C fuel) and with HOT,
denser water (0.75 g/cm3 liquid -> 0.036 g/cm3 steam at 7 MPa). The earlier
OpenMC runs were COLD (293.6 K) with 0.65 g/cm3 water. This runs OpenMC with:
  - temperature 600 K
  - the fast cell's water densities
  - the deterministic geometry (114 mm bore + tube + gap)
If the void worth moves from +0.022 toward the fast cell's +0.115, temperature is
the cause. If it stays near +0.02, temperature is not the cause.

Run: source /workspace/tools/omc/env.sh && cd openmc && python matched_test.py
"""
from __future__ import annotations

import glob
import json
import os

import openmc

PARTICLES = 4000
BATCHES = 80
INACTIVE = 25
TEMP_K = 600.0

# the fast cell's water: rho(void) = 0.75 - 0.714*void  (7 MPa, 284 C -> steam)
VOIDS = [0.0, 0.3, 0.6, 1.0]
RHO = {v: 0.75 - 0.714 * v for v in VOIDS}

HOLE_R, TUBE_IR, TUBE_OR, CELL_HALF = 5.7, 4.4, 4.8, 12.5
FUEL_VF, CLAD_VF = 0.62, 0.12


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
    return {"fuel": fuel, "clad": clad, "graphite": graphite, "water": water}


def build(void: float) -> openmc.Model:
    rho = RHO[void]
    m = _mats(rho)
    w = FUEL_VF + CLAD_VF
    inner = openmc.Material.mix_materials([m["fuel"], m["clad"]],
                                          [FUEL_VF / w, CLAD_VF / w], "vo")
    if void < 1.0:
        inner = openmc.Material.mix_materials(
            [m["fuel"], m["clad"], m["water"]],
            [FUEL_VF, CLAD_VF, 1 - FUEL_VF - CLAD_VF], "vo")
        inner.add_s_alpha_beta("c_H_in_H2O")

    gap_water = openmc.Material(name="gap water", temperature=TEMP_K)
    gap_water.add_element("H", 2.0, "ao")
    gap_water.add_element("O", 1.0, "ao")
    gap_water.set_density("g/cm3", max(rho, 0.036))
    if void < 1.0:
        gap_water.add_s_alpha_beta("c_H_in_H2O")

    h = CELL_HALF
    sq = (+openmc.XPlane(-h, boundary_type="reflective")
          & -openmc.XPlane(h, boundary_type="reflective")
          & +openmc.YPlane(-h, boundary_type="reflective")
          & -openmc.YPlane(h, boundary_type="reflective"))
    tube, tube_out, hole = (openmc.ZCylinder(r=TUBE_IR),
                            openmc.ZCylinder(r=TUBE_OR),
                            openmc.ZCylinder(r=HOLE_R))
    u = openmc.Universe(cells=[
        openmc.Cell(name="bore", fill=inner, region=-tube),
        openmc.Cell(name="tube", fill=m["clad"], region=+tube & -tube_out),
        openmc.Cell(name="gap", fill=gap_water, region=+tube_out & -hole),
        openmc.Cell(name="graphite", fill=m["graphite"], region=+hole & sq)])
    s = openmc.Settings()
    s.batches = BATCHES
    s.inactive = INACTIVE
    s.particles = PARTICLES
    s.run_mode = "eigenvalue"
    s.source = openmc.IndependentSource(space=openmc.stats.Point((0, 0, 0)))
    s.temperature = {"default": TEMP_K}
    return openmc.Model(openmc.Geometry(u),
                        openmc.Materials([m["graphite"], gap_water, m["clad"], inner]), s)


if __name__ == "__main__":
    ks = {}
    for v in VOIDS:
        wd = f"matched_{int(v*100)}"
        os.makedirs(wd, exist_ok=True)
        build(v).export_to_model_xml(wd)
        openmc.run(cwd=wd, threads=4, output=False)
        f = sorted(glob.glob(os.path.join(wd, "statepoint.*.h5")))[-1]
        with openmc.StatePoint(f) as sp:
            ks[v] = sp.keff.nominal_value
        print(f"  void={v:.2f} rho={RHO[v]:.3f} k={ks[v]:.5f}", flush=True)
    slope = (ks[0.3] - ks[0.0]) / 0.3
    print(f"\nvoid worth, OpenMC MATCHED to fast cell conditions = {slope:+.5f}")
    print("void worth, fast cell                            = +0.11500")
    print("void worth, OpenMC cold/repo                      = +0.02194")
    json.dump({"ks": {str(k): v for k, v in ks.items()}, "slope": slope},
              open("matched_test.json", "w"), indent=2)
