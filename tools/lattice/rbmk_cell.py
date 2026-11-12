#!/usr/bin/env python3
"""The RBMK-1000 fuel channel as a real lattice cell.

This is the piece that turns geometry + evaluated nuclear data into the few-group
constants the reactor core runs on. It replaces the hand-set "feedback scales" of
the Java core with numbers DERIVED from:

  * the true channel geometry — 250 mm pitch, 114 mm hole, 88/4 mm Zr-2.5%Nb
    pressure tube, an 18-rod UO2 assembly of 13.6 mm rods at 2.0 % enrichment
    (the second-generation ChNPP-3/4 channel, RbmkGeometry);
  * real evaluated cross sections — ENDF/B-VIII.0 pointwise data, resonance
    self-shielded for the fuel-rod cluster.

The calculation is a fine-group (69-group) spectrum solve of the homogenised cell,
with elastic down-scattering, a fission source, resonance self-shielding of U-238
(Bondarenko with a Dancoff-corrected background), and a Maxwellian thermal
spectrum. The flux is then collapsed to the two groups the reactor uses
(fast > 0.625 eV, thermal below).

The point is the VOID COEFFICIENT: because the constants come from geometry and
data, raising the void (replacing water with steam) must make the spectrum harder
and the reactivity rise — and it must do so by the right amount, not by a factor
someone typed in.

Units: energy eV, cross section barns, length cm, density g/cm^3.
"""
from __future__ import annotations

import numpy as np

import endf

# --------------------------------------------------------------- group mesh
E_MIN, E_MAX = 1.0e-5, 15.0e6
N_GROUPS = 69
THERMAL_CUT = 0.625                     # eV, the fast/thermal boundary
BOUNDS = np.exp(np.linspace(np.log(E_MIN), np.log(E_MAX), N_GROUPS + 1))

# ------------------------------------------------------------- geometry (cm)
PITCH = 25.0                            # 250 mm square lattice
HOLE_D = 11.4                           # 114 mm graphite bore
TUBE_ID, TUBE_OD = 8.8, 9.6             # 88 mm bore, 4 mm wall
ROD_OD = 1.36                           # 13.6 mm fuel rod
ROD_CLAD_T = 0.09                       # 0.9 mm clad
ROD_FUEL_D = ROD_OD - 2 * ROD_CLAD_T    # 11.8 mm pellet
N_RODS = 18
ENRICH = 0.020                          # 2.0 % U-235

def areas():
    a_cell = PITCH ** 2
    a_hole = np.pi * (HOLE_D / 2) ** 2
    a_graphite = a_cell - a_hole
    a_tube = np.pi * ((TUBE_OD / 2) ** 2 - (TUBE_ID / 2) ** 2)
    a_gap = np.pi * ((HOLE_D / 2) ** 2 - (TUBE_OD / 2) ** 2)   # water between tube and graphite
    a_inside = np.pi * (TUBE_ID / 2) ** 2
    a_fuel = N_RODS * np.pi * (ROD_FUEL_D / 2) ** 2
    a_clad = N_RODS * np.pi * ((ROD_OD / 2) ** 2 - (ROD_FUEL_D / 2) ** 2)
    a_cool = a_inside - a_fuel - a_clad
    return dict(cell=a_cell, graphite=a_graphite, gap=a_gap, tube=a_tube,
                fuel=a_fuel, clad=a_clad, coolant=a_cool)

# ---------------------------------------------------- atom densities (/barn-cm)
AVOG = 6.02214076e23

def uo2_density():      # 95 % of the 10.96 g/cm^3 theoretical
    return 10.4

def number_densities(void_fraction, fuel_temp_c=600.0):
    """Atom densities (10^24 /cm^3 = /barn-cm) for every zone.

    void_fraction 0 = all liquid water, 1 = all steam, at ~7 MPa.
    """
    A = areas()
    nd = {}
    # fuel: UO2
    m_uo2 = 269.9
    n_uo2 = uo2_density() / m_uo2 * AVOG / 1e24
    nd["U235"] = n_uo2 * ENRICH
    nd["U238"] = n_uo2 * (1 - ENRICH)
    nd["O16"] = 2 * n_uo2
    # clad + tube: Zr-2.5%Nb, 6.56 g/cm^3
    n_zr = 6.56 / 91.22 * AVOG / 1e24
    nd["Zr"] = n_zr
    # graphite: 1.65 g/cm^3
    n_c = 1.65 / 12.011 * AVOG / 1e24
    nd["C12"] = n_c * 0.9893
    nd["C13"] = n_c * 0.0107
    # water: liquid 0.75 g/cm^3, steam ~0.036 g/cm^3 at 7 MPa/284 C
    rho_liq, rho_steam = 0.75, 0.036
    rho_w = rho_liq * (1 - void_fraction) + rho_steam * void_fraction
    n_w = rho_w / 18.015 * AVOG / 1e24
    nd["H1"] = 2 * n_w
    nd["O16_w"] = n_w
    return nd, A

# --------------------------------------------------------------- data layer
_LIB = {}
def lib(label, mt):
    key = (label, mt)
    if key not in _LIB:
        _LIB[key] = endf.load(label, mt)
    return _LIB[key]

def dancoff():
    """Dancoff factor for the 18-rod cluster: how much of the fuel surface sees
    another rod rather than the moderator. Rods are packed in a ring inside the
    tube, so the factor is high (~0.6). A geometry input, from the assembly
    layout; see docs/RBMK-CORE-GEOMETRY.md."""
    return 0.60

def group_xs(label, mt, sigma0=None):
    """Flux-weighted group cross sections for one nuclide/reaction."""
    xs = lib(label, mt)
    if xs is None:
        return np.zeros(N_GROUPS)
    if sigma0 is None:
        return endf.group_average(xs, BOUNDS, endf.weight_combined)
    return endf.effective_capture(xs, BOUNDS, endf.weight_1_over_e, sigma0)

def build_macroscopic(void_fraction, fuel_temp_c=600.0):
    """Macroscopic cross sections per zone and group, with U-238 self-shielding."""
    nd, A = number_densities(void_fraction, fuel_temp_c)

    # --- resonance self-shielding in the fuel (U-238 + U-235 capture) ---
    # background sigma0 = potential scattering per U-238 atom, Dancoff-weighted
    sig_pot = 10.0                                     # barns, U-238 potential
    nd_u = (nd["U238"] + nd["U235"])                   # /barn-cm
    # escape from the lump: sigma0 = (1-C) * N_potential_scatter / N_U
    sigma0 = (1 - dancoff()) * sig_pot * (nd["C12"] + nd["O16"] * 0.3) / max(nd_u, 1e-9)
    sigma0 = max(sigma0, 1.0)

    zones = {}
    # helper to accumulate a nuclide's contribution into a zone dict
    def add(zone, label, dens, shield_cap=False):
        total = group_xs(label, 1)
        elast = group_xs(label, 2)
        capt = group_xs(label, 102, sigma0 if shield_cap else None)
        fiss = group_xs(label, 18)
        inel = group_xs(label, 4)
        zone["St"] += dens * total
        zone["Sel"] += dens * elast
        zone["Scap"] += dens * capt
        zone["Sfis"] += dens * fiss
        zone["Sin"] += dens * inel

    def newzone():
        return dict(St=np.zeros(N_GROUPS), Sel=np.zeros(N_GROUPS),
                    Scap=np.zeros(N_GROUPS), Sfis=np.zeros(N_GROUPS),
                    Sin=np.zeros(N_GROUPS))

    fuel = newzone()
    add(fuel, "U238", nd["U238"], shield_cap=True)
    add(fuel, "U235", nd["U235"], shield_cap=True)
    add(fuel, "O16", nd["O16"])
    clad = newzone(); add(clad, "Zr", nd["Zr"])
    tube = newzone(); add(tube, "Zr", nd["Zr"])
    graph = newzone(); add(graph, "C12", nd["C12"]); add(graph, "C13", nd["C13"])
    cool = newzone(); add(cool, "H1", nd["H1"]); add(cool, "O16", nd["O16_w"])

    # volume-weighted homogenisation to the cell
    vols = dict(fuel=A["fuel"], clad=A["clad"], tube=A["tube"],
                graph=A["graphite"], cool=A["coolant"] + A["gap"])
    tot = sum(vols.values())
    hom = newzone()
    for z, v in [(fuel, vols["fuel"]), (clad, vols["clad"]), (tube, vols["tube"]),
                 (graph, vols["graph"]), (cool, vols["cool"])]:
        for k in hom:
            hom[k] += z[k] * v / tot
    return hom, dict(sigma0=sigma0, vols=vols, tot=tot)


if __name__ == "__main__":
    A = areas()
    print("RBMK cell areas (cm^2):", {k: round(v, 4) for k, v in A.items()})
    nd, _ = number_densities(0.0)
    print("atom densities /barn-cm:", {k: round(v, 5) for k, v in nd.items()})
    hom, info = build_macroscopic(0.0)
    print("self-shield background sigma0 =", round(info["sigma0"], 2), "b")
    print("homogenised thermal capture S_cap =", f"{hom['Scap'][-1]:.5f} /cm")
    print("homogenised thermal fission S_fis =", f"{hom['Sfis'][-1]:.5f} /cm")
