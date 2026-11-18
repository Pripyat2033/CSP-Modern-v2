"""Does a control rod lower the void coefficient? (the Director's hypothesis)

The real RBMK core is about 1661 fuel channels and 211 control rods -- roughly
one rod per eight fuel channels (docs/RBMK-CORE-GEOMETRY.md). The lattice cell we
use for the void coefficient is ONE fuel channel on its own, with no rods at all.

This test asks whether that omission matters. It builds a 3x3 supercell (the
edges wrap around, so it stands for an endless tiling):
  - "norod": all nine channels are fuel;
  - "rod"  : the centre channel is a boron-carbide control rod, the other eight
             are fuel  (exactly the ~1 rod per 8 fuel the Director described).
Then it measures the void worth of the eight fuel channels in each case: how much
more reactive the lattice gets when the fuel channels' water boils to steam.

If rods lower the void worth, the Director is right in spirit: a real core's
response is gentler than a bare fuel channel's.

Run: source /workspace/tools/omc/env.sh && cd openmc && python control_rod_test.py
"""
from __future__ import annotations

import glob
import json
import os

import openmc

PARTICLES = 4000
BATCHES = 80
INACTIVE = 20
TEMP_K = 600.0
VOIDS = [0.0, 0.5]
RHO = {v: 0.75 - 0.714 * v for v in VOIDS}

PITCH = 25.0
CHANNEL_R, TUBE_R, HOLE_R = 4.4, 4.8, 5.7
FUEL_VF, CLAD_VF, WATER_VF = 0.324, 0.106, 0.570
N = 3


def materials(rho: float) -> dict:
    fuel = openmc.Material(name="UO2")
    fuel.add_element("U", 1.0, "ao", enrichment=2.0)
    fuel.add_element("O", 2.0, "ao")
    fuel.set_density("g/cm3", 10.4)
    fuel.temperature = TEMP_K
    clad = openmc.Material(name="Zr1Nb")
    clad.add_element("Zr", 0.99, "wo")
    clad.add_element("Nb", 0.01, "wo")
    clad.set_density("g/cm3", 6.55)
    clad.temperature = TEMP_K
    graphite = openmc.Material(name="graphite")
    graphite.add_element("C", 1.0, "ao")
    graphite.set_density("g/cm3", 1.65)
    graphite.temperature = TEMP_K
    graphite.add_s_alpha_beta("c_Graphite")
    water = openmc.Material(name="water")
    water.add_element("H", 2.0, "ao")
    water.add_element("O", 1.0, "ao")
    water.set_density("g/cm3", max(rho, 0.036))
    water.temperature = TEMP_K
    helium = openmc.Material(name="helium")
    helium.add_element("He", 1.0, "ao")
    helium.set_density("g/cm3", 0.0004)
    helium.temperature = TEMP_K
    b4c = openmc.Material(name="B4C absorber")
    b4c.add_element("B", 4.0, "ao")
    b4c.add_element("C", 1.0, "ao")
    b4c.set_density("g/cm3", 2.52)
    b4c.temperature = TEMP_K
    return dict(fuel=fuel, clad=clad, graphite=graphite, water=water,
                helium=helium, b4c=b4c)


def channel_universe(void: float, kind: str, m: dict, used: list,
                     rod_r: float = 2.5) -> openmc.Universe:
    half = PITCH / 2
    sq = (+openmc.XPlane(-half) & -openmc.XPlane(half)
          & +openmc.YPlane(-half) & -openmc.YPlane(half))
    chan = openmc.ZCylinder(r=CHANNEL_R)
    tube = openmc.ZCylinder(r=TUBE_R)
    hole = openmc.ZCylinder(r=HOLE_R)
    if kind == "fuel":
        if void >= 1.0:
            w = FUEL_VF + CLAD_VF
            bore = openmc.Material.mix_materials(
                [m["fuel"], m["clad"]], [FUEL_VF / w, CLAD_VF / w], "vo")
        else:
            bore = openmc.Material.mix_materials(
                [m["fuel"], m["clad"], m["water"]],
                [FUEL_VF, CLAD_VF, WATER_VF], "vo")
            bore.add_s_alpha_beta("c_H_in_H2O")
        used.append(bore)
        cells = [
            openmc.Cell(fill=bore, region=-chan),
            openmc.Cell(fill=m["clad"], region=+chan & -tube),
            openmc.Cell(fill=m["helium"], region=+tube & -hole),
            openmc.Cell(fill=m["graphite"], region=+hole & sq),
        ]
        return openmc.Universe(cells=cells)
    # Control-rod channel: a real one is a THIN absorber in a water-filled bore,
    # not the whole channel filled with B4C. rod_r sets the absorber radius.
    rod = openmc.ZCylinder(r=rod_r)
    rodclad = openmc.ZCylinder(r=rod_r + 0.4)
    cells = [
        openmc.Cell(fill=m["b4c"], region=-rod),
        openmc.Cell(fill=m["clad"], region=+rod & -rodclad),
        openmc.Cell(fill=m["water"], region=+rodclad & -chan),
        openmc.Cell(fill=m["clad"], region=+chan & -tube),
        openmc.Cell(fill=m["helium"], region=+tube & -hole),
        openmc.Cell(fill=m["graphite"], region=+hole & sq),
    ]
    return openmc.Universe(cells=cells)


def build(n_rods: int, void: float, rod_r: float = 2.5) -> openmc.Model:
    m = materials(RHO[void])
    used = [m["fuel"], m["clad"], m["graphite"], m["water"], m["helium"], m["b4c"]]
    univs = [[channel_universe(
        void, "rod" if (n_rods == 1 and i == 1 and j == 1) else "fuel", m, used,
        rod_r) for j in range(N)] for i in range(N)]
    ext = N * PITCH / 2
    lat = openmc.RectLattice()
    lat.lower_left = (-ext, -ext)
    lat.pitch = (PITCH, PITCH)
    lat.universes = univs
    xlo = openmc.XPlane(-ext, boundary_type="reflective")
    xhi = openmc.XPlane(ext, boundary_type="reflective")
    ylo = openmc.YPlane(-ext, boundary_type="reflective")
    yhi = openmc.YPlane(ext, boundary_type="reflective")
    outer = openmc.Cell(fill=lat, region=+xlo & -xhi & +ylo & -yhi)
    s = openmc.Settings()
    s.batches = BATCHES
    s.inactive = INACTIVE
    s.particles = PARTICLES
    s.run_mode = "eigenvalue"
    s.source = openmc.IndependentSource(
        space=openmc.stats.Box((-ext, -ext, -1.0), (ext, ext, 1.0)))
    s.temperature = {"default": TEMP_K}
    return openmc.Model(openmc.Geometry(openmc.Universe(cells=[outer])),
                        openmc.Materials(used), s)


def true_reactivity(k1: float, k2: float) -> float:
    """Change in reactivity between two states, in dk/k (the convention INSAG-7
    and rbmk_void_coeff.py use). NOT the plain (k2-k1)/k1, which overstates it."""
    return (k2 - k1) / (k1 * k2)


if __name__ == "__main__":
    out = {}
    cases = (("norod", 0, 2.5), ("thinrod", 1, 2.5), ("thickrod", 1, 4.4))
    for tag, n, rr in cases:
        ks = {}
        for v in VOIDS:
            wd = f"ctrl_{tag}_{int(v * 100)}"
            os.makedirs(wd, exist_ok=True)
            build(n, v, rr).export_to_model_xml(wd)
            openmc.run(cwd=wd, threads=4, output=False)
            f = sorted(glob.glob(os.path.join(wd, "statepoint.*.h5")))[-1]
            with openmc.StatePoint(f) as sp:
                ks[v] = sp.keff.nominal_value
            print(f"  {tag} void={v} k={ks[v]:.5f}", flush=True)
        out[tag] = {str(k): val for k, val in ks.items()}
        out["slope_" + tag] = true_reactivity(ks[0.0], ks[0.5]) / 0.5
    print(f"\nvoid worth in dk/k (the INSAG-7 convention):")
    print(f"  NO control rod        = {out['slope_norod']:+.5f}")
    print(f"  1 THIN rod / 8 fuel   = {out['slope_thinrod']:+.5f}")
    print(f"  1 THICK rod / 8 fuel  = {out['slope_thickrod']:+.5f}")
    json.dump(out, open("control_rod_test.json", "w"), indent=2)
