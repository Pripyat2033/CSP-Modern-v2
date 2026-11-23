#!/usr/bin/env python3
"""OpenMC referee for the in-house 69-group lattice k_inf.

The in-house solver (rbmk_spectrum.build_fine + solve_k) is a 69-group
diffusion-like solve. Its group constants are only as good as the resonance
self-shielding, and nothing in the repo independently checks the result: the
"shipped" k_inf of 1.2893 is the same code's output on an older commit.

This script builds the SAME cell in OpenMC (continuous-energy Monte Carlo,
ENDF/B-VIII.0) and reports k_inf, so the in-house number has a referee.

Two models, because they answer different questions:

  homog   Homogenise the cell with rbmk_cell's own atom densities and volume
          fractions, then run OpenMC on that one material. Isolates the GROUP
          CONSTANTS from the geometry: any difference here is the solver's
          spectrum/self-shielding treatment, not the model's layout.

  hetero  The explicit 18-rod + carrier cluster inside the pressure tube. Adds
          the real spatial self-shielding that a homogenised cell cannot show.

Run with the OpenMC env:
    /workspace/tools/omc/env/bin/python omc_referee.py --model hetero
    /workspace/tools/omc/env/bin/python omc_referee.py --model homog

Requires OPENMC_CROSS_SECTIONS to point at an HDF5 data library, e.g.
    export OPENMC_CROSS_SECTIONS=/workspace/tools/openmc_data/endfb-viii.0-hdf5/cross_sections.xml
"""
from __future__ import annotations

import argparse
import json
import math
import os
import subprocess
import sys
import tempfile

HERE = os.path.dirname(os.path.abspath(__file__))
DEFAULT_XS = "/workspace/tools/openmc_data/endfb-viii.0-hdf5/cross_sections.xml"

# --- cell geometry, from docs/RBMK-CORE-GEOMETRY.md -------------------------
PITCH_CM = 25.0             # 250 mm square lattice pitch
BORE_R = 11.4 / 2           # 114 mm graphite bore
TUBE_IR, TUBE_OR = 8.8 / 2, 9.6 / 2
ROD_R = 13.6 / 2 / 10       # 13.6 mm fuel rod
CLAD_T = 0.0825             # 0.825 mm wall
CLAD_IN = ROD_R - CLAD_T
PELLET_R = 11.5 / 2 / 10    # 11.5 mm pellet
HOLE_R = 0.10               # 2 mm central hole
CARRIER_R = 13.0 / 2 / 10   # 13 mm carrier rod
ACTIVE_CM = 680.0           # 6.86 m of active fuel in the 10.025 m assembly

# Ring radii are NOT in any document we hold. These values put 18 rods plus the
# carrier inside the 88 mm bore with no overlap; --ring1/--ring2 exist so the
# effect of this assumption can be measured instead of hidden.
RING1_DEF, RING2_DEF = 1.80, 3.20

# A bundle of 6 inner + 12 outer rods with EQUAL nearest-neighbour spacing has
# its two radii locked in a fixed ratio: the inner-inner chord is r1 (6 rods at
# 60 deg), the outer-outer chord is 2 r2 sin 15, and equal spacing gives
# r2 = 2 r1 cos 15. That is geometry, not a sourced value, and it lets the bore
# bound the radii to a one-parameter family instead of leaving them free.
UNIFORM_RATIO = 2.0 * math.cos(math.radians(15.0))


def ring_family(rod_r=ROD_R, bore_r=TUBE_IR, carrier_r=CARRIER_R):
    """Bounds on r1 for the uniform-spacing 6+12 layout in this bore.

    Lower bound: inner rods must not touch each other (r1 >= 2 rod radii).
    Upper bound: the outermost rod must stay inside the bore wall.
    The result is a BAND, not a value -- the layout is still under-determined,
    which is the point of exposing it.
    """
    r1_lo = 2.0 * rod_r
    r1_hi = (bore_r - rod_r) / UNIFORM_RATIO
    return r1_lo, r1_hi


def _repo_composition():
    """Atom densities and zone areas from rbmk_cell, without importing it here.

    rbmk_cell imports the repo's endf.py, which shadows OpenMC's own `endf`
    dependency, so the two cannot share a process. A subprocess writes JSON.
    """
    code = (
        "import sys, json; sys.path.insert(0, %r);"
        "import rbmk_cell as c;"
        "nd, A = c.number_densities(0.0);"
        "json.dump({'nd': nd, 'A': A,"
        " 'carbon': [[l, d] for l, d, _ in c.carbon_spec(nd['C'])]},"
        " sys.stdout)" % HERE
    )
    out = subprocess.check_output([sys.executable, "-c", code])
    return json.loads(out)


def _import_openmc():
    """Import OpenMC with this directory off sys.path.

    The repo's endf.py shadows OpenMC's own `endf` dependency, and running a
    script from HERE puts HERE first on sys.path, so `import openmc` fails with
    "No module named 'endf.data'". Drop HERE for the duration of the import.
    """
    saved = list(sys.path)
    sys.path[:] = [p for p in sys.path if os.path.abspath(p or ".") != HERE]
    try:
        import openmc
        return openmc
    finally:
        sys.path[:] = saved


def build(model, outdir, ring1, ring2, water_rho, particles, batches, seed):
    openmc = _import_openmc()

    half = PITCH_CM / 2
    h = ACTIVE_CM

    xm = openmc.XPlane(-half, boundary_type="reflective")
    xp = openmc.XPlane(+half, boundary_type="reflective")
    ym = openmc.YPlane(-half, boundary_type="reflective")
    yp = openmc.YPlane(+half, boundary_type="reflective")
    zm = openmc.ZPlane(0.0, boundary_type="reflective")
    zp = openmc.ZPlane(h, boundary_type="reflective")

    def box():
        return openmc.Intersection([+xm, -xp, +ym, -yp, +zm, -zp])

    uo2 = openmc.Material(name="UO2", temperature=600.0)
    uo2.add_element("U", 1.0, enrichment=2.0)
    uo2.add_element("O", 2.0)
    uo2.set_density("g/cm3", 10.4)

    zr = openmc.Material(name="Zr", temperature=600.0)
    zr.add_element("Zr", 0.975)
    zr.add_element("Nb", 0.025)
    zr.set_density("g/cm3", 6.56)

    graphite = openmc.Material(name="graphite", temperature=600.0)
    graphite.add_element("C", 1.0)
    graphite.set_density("g/cm3", 1.65)

    water = openmc.Material(name="water", temperature=600.0)
    water.add_element("H", 2.0)
    water.add_element("O", 1.0)
    water.set_density("g/cm3", water_rho)
    water.add_s_alpha_beta("c_H_in_H2O")

    if model == "homog":
        comp = _repo_composition()
        nd, A = comp["nd"], comp["A"]
        vols = {"fuel": A["fuel"], "clad": A["clad"], "tube": A["tube"],
                "graph": A["graphite"], "cool": A["coolant"] + A["gap"]}
        tot = sum(vols.values())
        rename = {"Zr": "Zr90", "C12": "C12", "C13": "C13", "Cnat": "C0"}
        zones = {
            "fuel": [("U235", nd["U235"]), ("U238", nd["U238"]), ("O16", nd["O16"])],
            "clad": [("Zr90", nd["Zr"] * 0.5145)],
            "tube": [("Zr90", nd["Zr"] * 0.5145)],
            "graph": [(rename.get(l, l), d) for l, d in comp["carbon"]],
            "cool": [("H1", nd["H1"]), ("O16", nd["O16_w"])],
        }
        hom = openmc.Material(name="homogenised", temperature=600.0)
        for z, pairs in zones.items():
            w = vols[z] / tot
            for label, dens in pairs:
                if dens > 0:
                    hom.add_nuclide(rename.get(label, label), dens * w)
        hom.add_s_alpha_beta("c_H_in_H2O")
        cells = [openmc.Cell(fill=hom, region=box())]
        materials = [hom]
    else:
        tube_in = openmc.ZCylinder(r=TUBE_IR)
        tube_out = openmc.ZCylinder(r=TUBE_OR)
        bore = openmc.ZCylinder(r=BORE_R)
        carrier = openmc.ZCylinder(r=CARRIER_R)

        rods = [(ring1 * math.cos(2 * math.pi * i / 6),
                 ring1 * math.sin(2 * math.pi * i / 6)) for i in range(6)]
        rods += [(ring2 * math.cos(2 * math.pi * i / 12 + math.pi / 12),
                  ring2 * math.sin(2 * math.pi * i / 12 + math.pi / 12))
                 for i in range(12)]

        cells = [openmc.Cell(fill=zr, region=(-carrier) & box())]
        rod_outers = [carrier]
        for x, y in rods:
            hole = openmc.ZCylinder(x0=x, y0=y, r=HOLE_R)
            pel = openmc.ZCylinder(x0=x, y0=y, r=PELLET_R)
            cin = openmc.ZCylinder(x0=x, y0=y, r=CLAD_IN)
            out = openmc.ZCylinder(x0=x, y0=y, r=ROD_R)
            cells += [
                openmc.Cell(fill=water, region=(-hole) & box()),
                openmc.Cell(fill=uo2, region=(+hole & -pel) & box()),
                openmc.Cell(fill=water, region=(+pel & -cin) & box()),
                openmc.Cell(fill=zr, region=(+cin & -out) & box()),
            ]
            rod_outers.append(out)
        outside_all = openmc.Intersection([+s for s in rod_outers])
        cells += [
            openmc.Cell(fill=water, region=(-tube_in & outside_all) & box()),
            openmc.Cell(fill=zr, region=(+tube_in & -tube_out) & box()),
            openmc.Cell(fill=water, region=(+tube_out & -bore) & box()),
            openmc.Cell(fill=graphite, region=(+bore) & box()),
        ]
        materials = [uo2, zr, graphite, water]

    old = os.getcwd()
    os.chdir(outdir)
    try:
        openmc.Materials(materials).export_to_xml()
        openmc.Geometry(openmc.Universe(cells=cells)).export_to_xml()
        s = openmc.Settings()
        s.batches = batches
        s.inactive = max(batches // 2, 10)
        s.particles = particles
        # `openmc -s N` names the statepoint, it does not seed the RNG; the seed
        # lives here. Without this every "different seed" run is identical.
        s.seed = seed
        # A point source at the origin sits ON the reflective z=0 plane and makes
        # OpenMC lose every particle. A point source anywhere else converges the
        # fission distribution too slowly for a small lattice cell (two runs of
        # this model differed by 4500 pcm on source placement alone), so start
        # uniform over the whole cell instead.
        s.source = openmc.IndependentSource(
            space=openmc.stats.Box((-half, -half, 0.0), (half, half, h)))
        s.export_to_xml()
    finally:
        os.chdir(old)


def run(outdir, seed):
    env = dict(os.environ)
    env.setdefault("OPENMC_CROSS_SECTIONS", DEFAULT_XS)
    log = os.path.join(outdir, "openmc.log")
    with open(log, "w") as fh:
        subprocess.run(["openmc"], cwd=outdir, env=env,
                       stdout=fh, stderr=subprocess.STDOUT, check=True)
    text = open(log).read()
    if "Maximum number of lost particles" in text:
        raise RuntimeError("OpenMC lost particles; geometry is broken")
    for line in text.splitlines():
        if "Combined k-effective" in line:
            k, unc = line.split("=")[1].split("+/-")
            return float(k), float(unc)
    raise RuntimeError("no k-effective in OpenMC output")


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--model", choices=("hetero", "homog"), default="hetero")
    ap.add_argument("--ring1", type=float, default=RING1_DEF)
    ap.add_argument("--ring2", type=float, default=RING2_DEF)
    ap.add_argument("--water-rho", type=float, default=0.75,
                    help="coolant density g/cm3; 0.036 for a voided channel")
    ap.add_argument("--particles", type=int, default=20000)
    ap.add_argument("--batches", type=int, default=120)
    ap.add_argument("--seed", type=int, default=7)
    ap.add_argument("--keep", action="store_true", help="keep the run directory")
    args = ap.parse_args(argv)

    outdir = tempfile.mkdtemp(prefix="omc_referee_")
    build(args.model, outdir, args.ring1, args.ring2, args.water_rho,
          args.particles, args.batches, args.seed)
    k, unc = run(outdir, args.seed)
    print("%-7s water=%.3f ring=%.2f/%.2f  k_inf = %.5f +/- %.5f"
          % (args.model, args.water_rho, args.ring1, args.ring2, k, unc))
    if args.keep:
        print("run directory:", outdir)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
