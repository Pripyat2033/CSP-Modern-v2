"""CLEAN DECOMPOSITION of the void-coefficient discrepancy.

Two things were suspected: the channel VOLUME FRACTIONS and the GEOMETRY (tube +
gap). This runs the full 2x2 plus the explicit cluster, with the gap medium set
CORRECTLY to HELIUM (docs/RBMK-CORE-GEOMETRY.md: the tube-to-graphite annulus is
helium at 0.5 MPa) -- an earlier probe wrongly filled it with water, which
inflated the void worth. Everything else is identical: ENDF/B-VIII.0, 600 K,
same water, same pitch.

Configurations:
  A  repo cell,  repo VF (0.62 fuel, 0.26 water), no tube/gap   <- the baseline
  B  repo cell,  real VF (0.324 fuel, 0.570 water), no tube/gap
  C  real geom (88 mm tube + helium gap), repo VF
  D  real geom (88 mm tube + helium gap), real VF
  E  explicit 18-rod cluster, helium gap

Run: source /workspace/tools/omc/env.sh && cd openmc && python decompose_test.py
"""
from __future__ import annotations

import glob
import json
import math
import os

import openmc

PARTICLES = 5000
BATCHES = 90
INACTIVE = 25
TEMP_K = 600.0
VOIDS = [0.0, 0.3, 0.6]
RHO = {v: 0.75 - 0.714 * v for v in VOIDS}

PITCH = 25.0
HOLE_R, TUBE_IR, TUBE_OR = 5.7, 4.4, 4.8
REPO_VF = dict(fuel=0.620, clad=0.120, water=0.260)
REAL_VF = dict(fuel=0.324, clad=0.106, water=0.570)

HELIUM_RHO = 0.0004  # g/cm3, He at 0.5 MPa / 600 K


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
    helium = openmc.Material(name="helium", temperature=TEMP_K)
    helium.add_element("He", 1.0, "ao")
    helium.set_density("g/cm3", HELIUM_RHO)
    return {"fuel": fuel, "clad": clad, "graphite": graphite,
            "water": water, "helium": helium}


def _bore(vf: dict, void: float, m: dict) -> openmc.Material:
    if void >= 1.0:
        w = vf["fuel"] + vf["clad"]
        return openmc.Material.mix_materials([m["fuel"], m["clad"]],
                                             [vf["fuel"] / w, vf["clad"] / w], "vo")
    mix = openmc.Material.mix_materials(
        [m["fuel"], m["clad"], m["water"]],
        [vf["fuel"], vf["clad"], vf["water"]], "vo")
    mix.add_s_alpha_beta("c_H_in_H2O")
    return mix


def _settings(h: float) -> openmc.Settings:
    s = openmc.Settings()
    s.batches = BATCHES
    s.inactive = INACTIVE
    s.particles = PARTICLES
    s.run_mode = "eigenvalue"
    s.source = openmc.IndependentSource(
        space=openmc.stats.Box((-h, -h, -1.0), (h, h, 1.0)))
    s.temperature = {"default": TEMP_K}
    return s


def build(cfg: str, void: float) -> openmc.Model:
    m = _mats(RHO[void])
    h = PITCH / 2
    sq = (+openmc.XPlane(-h, boundary_type="reflective")
          & -openmc.XPlane(h, boundary_type="reflective")
          & +openmc.YPlane(-h, boundary_type="reflective")
          & -openmc.YPlane(h, boundary_type="reflective"))
    vf = REPO_VF if cfg in ("A", "C") else REAL_VF
    inner = _bore(vf, void, m)

    if cfg in ("A", "B"):
        # repo cell: no tube, graphite starts at the (88 mm) bore
        bore = openmc.ZCylinder(r=TUBE_IR)
        cells = [openmc.Cell(name="graphite", fill=m["graphite"], region=+bore & sq),
                 openmc.Cell(name="bore", fill=inner, region=-bore)]
        mats = [m["graphite"], inner]

    elif cfg in ("C", "D"):
        # real geometry: 88 mm tube + helium gap to the 114 mm hole
        tube = openmc.ZCylinder(r=TUBE_IR)
        tube_out = openmc.ZCylinder(r=TUBE_OR)
        hole = openmc.ZCylinder(r=HOLE_R)
        cells = [
            openmc.Cell(name="graphite", fill=m["graphite"], region=+hole & sq),
            openmc.Cell(name="gap-he", fill=m["helium"], region=+tube_out & -hole),
            openmc.Cell(name="tube", fill=m["clad"], region=+tube & -tube_out),
            openmc.Cell(name="bore", fill=inner, region=-tube)]
        mats = [m["graphite"], m["helium"], m["clad"], inner]

    else:  # E: explicit cluster, helium gap
        tube = openmc.ZCylinder(r=TUBE_IR)
        tube_out = openmc.ZCylinder(r=TUBE_OR)
        hole = openmc.ZCylinder(r=HOLE_R)
        cells = [
            openmc.Cell(name="graphite", fill=m["graphite"], region=+hole & sq),
            openmc.Cell(name="gap-he", fill=m["helium"], region=+tube_out & -hole),
            openmc.Cell(name="tube", fill=m["clad"], region=+tube & -tube_out)]
        fuel_r, clad_r = 0.59, 0.68
        rods = []
        for k in range(6):
            a = 2 * math.pi * k / 6
            rods.append((2.0 * math.cos(a), 2.0 * math.sin(a)))
        for k in range(12):
            a = 2 * math.pi * k / 12 + math.pi / 12
            rods.append((3.4 * math.cos(a), 3.4 * math.sin(a)))
        cy = []
        for i, (x, y) in enumerate(rods):
            fc = openmc.ZCylinder(x0=x, y0=y, r=fuel_r)
            cc = openmc.ZCylinder(x0=x, y0=y, r=clad_r)
            cells.append(openmc.Cell(name=f"fuel-{i}", fill=m["fuel"], region=-fc))
            cells.append(openmc.Cell(name=f"clad-{i}", fill=m["clad"],
                                     region=+fc & -cc))
            cy.append(cc)
        wr = -tube
        for c in cy:
            wr = wr & +c
        cells.append(openmc.Cell(name="coolant", fill=m["water"], region=wr))
        mats = [m["graphite"], m["helium"], m["clad"], m["fuel"], m["water"]]

    return openmc.Model(openmc.Geometry(openmc.Universe(cells=cells)),
                        openmc.Materials(mats), _settings(h))


def sweep(cfg: str) -> dict:
    ks = {}
    for v in VOIDS:
        wd = f"dec_{cfg}_{int(v*100)}"
        os.makedirs(wd, exist_ok=True)
        build(cfg, v).export_to_model_xml(wd)
        openmc.run(cwd=wd, threads=4, output=False)
        f = sorted(glob.glob(os.path.join(wd, "statepoint.*.h5")))[-1]
        with openmc.StatePoint(f) as sp:
            ks[v] = sp.keff.nominal_value
        print(f"  {cfg} void={v:.2f} k={ks[v]:.5f}", flush=True)
    return ks


if __name__ == "__main__":
    labels = {"A": "repo cell + repo VF", "B": "repo cell + REAL VF",
              "C": "real geom + repo VF", "D": "real geom + REAL VF",
              "E": "explicit cluster"}
    out = {}
    for cfg in ["A", "B", "C", "D", "E"]:
        ks = sweep(cfg)
        out[cfg] = {str(k): v for k, v in ks.items()}
        out[f"slope_{cfg}"] = (ks[0.3] - ks[0.0]) / 0.3
        print(f"  -> {cfg} ({labels[cfg]}): dk/dvoid = {out[f'slope_{cfg}']:+.5f}\n",
              flush=True)
    json.dump(out, open("decompose_test.json", "w"), indent=2)
    print("=== SUMMARY (dk/dvoid over void 0 -> 0.3) ===")
    for cfg in ["A", "B", "C", "D", "E"]:
        print(f"  {cfg}  {labels[cfg]:26s} {out[f'slope_{cfg}']:+.5f}")
