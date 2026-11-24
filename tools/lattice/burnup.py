#!/usr/bin/env python3
"""Burnup to unit 4's 10.3 MWd/kg and the reactivity balance it produces.

This answers one question: does fuel depletion, done honestly on the repo's own
cross sections, account for the operating reactivity margin the regulation
demands (26-30 equivalent manual rods)?

WHAT IS MODELLED, AND WHY

Modelled from evaluated data, through the repo's 69-group solver:

  * U-235 fission and capture, with rates collapsed over the cell's own
    spectrum (not a hand-set one-group value).
  * U-238 capture -> Pu-239 -> Pu-240 -> Pu-241, capture rates Bondarenko
    self-shielded at the lattice's own dilution cross section, so the burnup
    and the eigenvalue use the same physics.
  * Fission products as ONE lumped pseudo-nuclide, sigma_c = 50 b at 0.0253 eV
    falling as 1/v (Lamarsh, Introduction to Nuclear Engineering, Table 3.2;
    DOE HDBK-1019 Vol. 1). One lumped atom is produced per fission.

Deliberately NOT modelled, with the reason:

  * Individual fission-product chains. Routing each mass chain's cumulative
    yield to its ENDF terminal nuclide over-poisons the lattice badly, because
    the terminals are frequently short-lived (Nd-147 is 11 d) and never reach
    inventory. Resolving ~1000 chains needs a real depletion code. The lumped
    50 b absorber is the standard, citable stand-in.
  * Xe-135 and Sm-149 in the burn loop: their inventory is a production /
    decay / burnout balance, not a cumulative sum. They are added afterwards at
    equilibrium, where they belong.

THREE BUGS THIS FILE EXISTS TO NOT REPEAT

  1. Power density over the whole graphite stack instead of the fuel. 3200 MWt
     into 192 t of uranium is 152.9 W/cm3, not the 4.23 W/cm3 core average;
     the core average understates flux by 36x and leaves U-235 undepleted.
  2. Pu-240 production from Pu-239 TOTAL absorption (fission included) rather
     than capture only. That manufactures Pu-240 about 3x too fast, drives the
     Pu vector to ~65% Pu-240, and flips the void coefficient negative - the
     opposite of the RBMK's defining behaviour.
  3. A flat (energy-independent) lumped absorber. Real FP capture is 1/v; a
     flat 50 b adds a large fake fast absorption and corrupts the void
     coefficient by an order of magnitude.

Usage:
    python3 burnup.py            # full report
    python3 burnup.py --quick    # coarser steps, same physics
"""
from __future__ import annotations

import json
import os
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

import endf
import rbmk_cell as cell
import rbmk_spectrum as spec

NA = 6.02214076e23
G = spec.G

# --- plant constants -------------------------------------------------------
P_FUEL = 152.9            # W/cm3 of FUEL: 3200 MWt over 192 t U at 9.173 g/cm3
E_FISSION = 200.0 * 1.6e-13   # J
TARGET_BURNUP = 10.3      # MWd/kg, unit 4 at the accident (INSAG-7)
# One fully inserted manual (РР) rod, dk/k. Calibrated from the one rod-worth
# figure the unit 4 panel states outright - 24 АЗ rods = -2 beta_eff
# (provenance rod.worth_group) - and agreed with RodBank.unitWorth().
ROD_WORTH = 2.0 * 0.0065 / 24.0
# Regulation band, equivalent manual rods (Регламент 1Э-С-II, ГЛАВА 9).
ORM_BAND = (26.0, 30.0)

# Bare-cylinder geometric buckling, from the core the repo already documents
# (active diameter 11.8 m, active height 7.0 m; docs/RBMK-CORE-GEOMETRY.md).
CORE_RADIUS_CM = 11.8 / 2.0 * 100.0
CORE_HEIGHT_CM = 7.0 * 100.0
B2 = (2.4048 / CORE_RADIUS_CM) ** 2 + (np.pi / CORE_HEIGHT_CM) ** 2

# Bondarenko dilution cross section, from the cell's own geometry.
SIGMA0 = 16.7
RESONANCE = np.array([g for g in range(G)
                      if spec.EG[g] >= 0.625 and spec.EU[g] <= 1.0e5 * 1.01])

# Equilibrium poison data. Yields are cumulative chain yields per fission of
# U-235 (ENDF/B-VIII.0 MF=8 MT=459); cross sections and half-lives are the
# standard thermal values.
XE_YIELD = 0.0639 + 0.00237      # I-135 + Xe-135 direct
XE_LAMBDA = 2.09e-5              # 1/s
XE_SIGMA_A = 2.6e6 * 1e-24       # cm2
SM_YIELD = 0.0107 + 0.0003       # Pm-149 + Nd-149
SM_SIGMA_A = 40140.0 * 1e-24     # cm2


def _lumped_fp(sigma_thermal=50.0):
    """Write the lumped FP pseudo-nuclide into the cache, 1/v in energy."""
    E = np.logspace(np.log10(1e-5), np.log10(2e7), 1200)
    xs = (sigma_thermal * np.sqrt(0.0253 / np.clip(E, 0.0253, None))).tolist()
    for mt in (endf.MT_TOTAL, endf.MT_ELASTIC, endf.MT_CAPTURE):
        path = os.path.join(endf.cache_dir(), f"FPlump_{mt}.json")
        with open(path, "w") as fh:
            json.dump({"energy": E.tolist(),
                       "cross section": [0.0] * len(E) if mt == endf.MT_ELASTIC else xs},
                      fh)
    return path


def prepare(label, shield):
    """Multi-group St, Sa, Sf, nuSf, Ss for one nuclide. Independent of burnup."""
    st = endf.group_average(endf.load(label, 1), spec.BOUNDS, endf.weight_combined)
    se = endf.group_average(endf.load(label, 2), spec.BOUNDS, endf.weight_combined)
    ine = endf.load(label, 4)
    if ine:
        se = se + endf.group_average(ine, spec.BOUNDS, endf.weight_combined)
    else:
        # Sa is built as St - Se, so every scattering channel must be in Se.
        # A missing MT=4 books inelastic scattering as absorption; see
        # test_neutron_conservation.py.
        sf_x, sc_x = endf.load(label, 18), endf.load(label, 102)
        sf_g = (endf.group_average(sf_x, spec.BOUNDS, endf.weight_combined)
                if sf_x else np.zeros(G))
        sc_g = (endf.group_average(sc_x, spec.BOUNDS, endf.weight_combined)
                if sc_x else np.zeros(G))
        se = se + np.maximum(st - se - sf_g - sc_g, 0.0)
    sa = np.maximum(st - se, 0.0)
    if shield:
        cap = endf.group_average(endf.load(label, 102), spec.BOUNDS,
                                 endf.weight_combined)
        eff = endf.effective_capture(endf.load(label, 102), spec.BOUNDS,
                                     endf.weight_1_over_e, SIGMA0)
        sa[RESONANCE] += eff[RESONANCE] - cap[RESONANCE]
    sa = np.minimum(np.maximum(sa, 0.0), st)
    sf_x = endf.load(label, 18)
    sf = (endf.group_average(sf_x, spec.BOUNDS, endf.weight_combined)
          if sf_x else np.zeros(G))
    ss = np.maximum(st - sa, 0.0)
    digits = "".join(c for c in label if c.isdigit())
    spec.MASS.setdefault(label, float(digits) if digits else 100.0)
    return dict(St=st, Sa=sa, Sf=sf, nuSf=sf * spec.NUBAR.get(label, 0.0),
                Ss=ss[:, None] * spec.elastic_transfer_full(spec.MASS[label],
                                                            endf.weight_combined))


class Cell:
    """A burnable cell: geometry, zone volumes and per-nuclide group data."""

    ACTINIDES = ("U235", "U238", "Pu239", "Pu240", "Pu241")
    SHIELDED = {"U235", "U238", "Pu239", "Pu240", "Pu241"}

    def __init__(self):
        _lumped_fp()
        self.nd, self.areas = cell.number_densities(0.0)
        self.n5 = self.nd["U235"]
        self.n8 = self.nd["U238"]
        self.hm = self.n5 + self.n8
        grams_u = self.hm * 238.05 / NA * 1e24
        self.burn_rate = P_FUEL / grams_u * 1e-3      # MWd/kg/day

        self.data = {l: prepare(l, l in self.SHIELDED)
                     for l in list(self.ACTINIDES) + ["FPlump", "O16", "Zr90",
                                                      "H1", "C12", "C13"]}
        nd = self.nd
        self.zones = {
            "clad": [("Zr90", nd["Zr"] * 0.5145)],
            "tube": [("Zr90", nd["Zr"] * 0.5145)],
            "graph": [(l, d) for l, d, _ in cell.carbon_spec(nd["C"])],
            "cool": [("H1", nd["H1"]), ("O16", nd["O16_w"])],
        }
        self.vols = {"fuel": self.areas["fuel"], "clad": self.areas["clad"],
                     "tube": self.areas["tube"], "graph": self.areas["graphite"],
                     "cool": self.areas["coolant"] + self.areas["gap"]}
        self.total = sum(self.vols.values())

        # Fission tally per nuclide, accumulated over the burn. Reset by burn().
        self.fissions = {}

    def fresh(self):
        comp = {l: 0.0 for l in self.data}
        comp["U235"] = self.n5
        comp["U238"] = self.n8
        comp["O16"] = self.nd["O16"]
        return comp

    def macros(self, comp):
        hom = {k: (np.zeros((G, G)) if k == "Ss" else np.zeros(G))
               for k in ("St", "Sa", "Sf", "nuSf", "Ss")}
        for lab, dens in comp.items():
            if dens <= 0 or lab not in self.data:
                continue
            w = dens * self.vols["fuel"] / self.total
            for k in hom:
                hom[k] = hom[k] + self.data[lab][k] * w
        for name, mix in self.zones.items():
            w = self.vols[name] / self.total
            for lab, dens in mix:
                if dens <= 0 or lab not in self.data:
                    continue
                for k in hom:
                    hom[k] = hom[k] + self.data[lab][k] * (dens * w)
        hom["chi"] = spec.watt_chi()
        return hom

    def solve(self, comp):
        """k_inf, k_eff, migration area and the group flux."""
        hom = self.macros(comp)
        k, phi = spec.solve_k(hom)
        diffusion = 1.0 / (3.0 * max(hom["St"][0], 1e-9))
        migration = diffusion / max(hom["Sa"][0], 1e-9)
        return k, k / (1.0 + migration * B2), migration, np.asarray(phi).ravel()

    def collapse(self, phi, label, mt):
        xx = endf.load(label, mt)
        if xx is None:
            return 0.0
        g = endf.group_average(xx, spec.BOUNDS, endf.weight_combined)
        return float((np.asarray(g) * phi).sum() / phi.sum())

    def collapse_capture(self, phi, label):
        """Self-shielded one-group capture - the same values the solve uses."""
        xx = endf.load(label, 102)
        if xx is None:
            return 0.0
        g = endf.effective_capture(xx, spec.BOUNDS, endf.weight_1_over_e, SIGMA0)
        return float((np.asarray(g) * phi).sum() / phi.sum())

    def step(self, comp, days):
        """One depletion step. Returns the thermal flux that drove it."""
        _, _, _, phi = self.solve(comp)
        f5 = self.collapse(phi, "U235", 18)
        c5 = self.collapse_capture(phi, "U235")
        c8 = self.collapse_capture(phi, "U238")
        f9 = self.collapse(phi, "Pu239", 18)
        c9 = self.collapse_capture(phi, "Pu239")
        c0 = self.collapse_capture(phi, "Pu240")
        f1 = self.collapse(phi, "Pu241", 18)
        c1 = self.collapse_capture(phi, "Pu241")
        a1 = f1 + c1

        denom = f5 * comp["U235"] + f9 * comp["Pu239"]
        if denom <= 0:
            raise RuntimeError("no fissile material left")
        # denom is 1/cm; the flux that sustains P_FUEL is P/Ef/denom. Reaction
        # counts come out per cm3, so divide by 1e24 to return to barn-cm.
        phi_th = P_FUEL / E_FISSION / denom
        dt = days * 86400.0 / 1e24

        f5r = comp["U235"] * f5 * phi_th * dt
        c5r = comp["U235"] * c5 * phi_th * dt
        c8r = comp["U238"] * c8 * phi_th * dt
        f9r = comp["Pu239"] * f9 * phi_th * dt
        a9r = comp["Pu239"] * (f9 + c9) * phi_th * dt
        c9r = comp["Pu239"] * c9 * phi_th * dt
        c0r = comp["Pu240"] * c0 * phi_th * dt
        f1r = comp["Pu241"] * f1 * phi_th * dt
        a1r = comp["Pu241"] * a1 * phi_th * dt

        # Fissions per nuclide, in the same barn-cm units as comp. The inventory
        # needs the split, not just the total: each fissile nuclide has its own
        # yield curve, and Pu-239 makes more Cs-137 per fission than U-235 does.
        self.fissions["U235"] = self.fissions.get("U235", 0.0) + f5r
        self.fissions["Pu239"] = self.fissions.get("Pu239", 0.0) + f9r
        self.fissions["Pu241"] = self.fissions.get("Pu241", 0.0) + f1r

        comp["U235"] -= f5r + c5r
        comp["U238"] -= c8r
        comp["Pu239"] += c8r - a9r
        comp["Pu240"] += c9r - c0r        # capture only, never fission
        comp["Pu241"] += c0r - a1r
        comp["FPlump"] += f5r + f9r + a1r
        return phi_th

    def burn(self, target=TARGET_BURNUP, step_days=2):
        comp = self.fresh()
        self.fissions = {}
        flux = 0.0
        day = 0
        while day * self.burn_rate < target:
            flux = self.step(comp, step_days)
            day += step_days
        return comp, flux, day

    def with_poisons(self, comp, flux):
        """Equilibrium Xe-135 and Sm-149 on top of the burned composition."""
        for label, mass in (("Xe135", 135.0), ("Sm149", 149.0)):
            if label not in self.data:
                spec.MASS.setdefault(label, mass)
                self.data[label] = prepare(label, False)
        _, _, _, phi = self.solve(comp)
        sigf = self.collapse(phi, "U235", 18)
        fission_rate = comp["U235"] * sigf * 1e-24
        n_xe = XE_YIELD * fission_rate * flux / (XE_LAMBDA + XE_SIGMA_A * flux)
        n_sm = SM_YIELD * fission_rate / SM_SIGMA_A
        out = dict(comp)
        out["Xe135"] = n_xe
        out["Sm149"] = n_sm
        return out, n_xe, n_sm


def pcm(k):
    return (k - 1.0) / k * 1e5


def rods(k):
    return pcm(k) / (ROD_WORTH * 1e5)


def report(quick=False):
    c = Cell()
    step = 4 if quick else 2
    print("fuel power density %.1f W/cm3 -> burnup rate %.4f MWd/kg/day"
          % (P_FUEL, c.burn_rate))
    print("%.1f MWd/kg needs %.0f full-power days\n"
          % (TARGET_BURNUP, TARGET_BURNUP / c.burn_rate))

    fresh = c.fresh()
    kf, kef, m2f, _ = c.solve(fresh)
    print("FRESH FUEL")
    print("  k_inf %.5f  (%+7.0f pcm)   k_eff %.5f  (%+7.0f pcm, %.0f rods)"
          % (kf, pcm(kf), kef, pcm(kef), rods(kef)))

    comp, flux, days = c.burn(step_days=step)
    k1, ke1, m2b, _ = c.solve(comp)
    print("\nAFTER %.2f MWd/kg (%d full-power days, flux %.2e n/cm2/s)"
          % (days * c.burn_rate, days, flux))
    print("  U-235 %.2f%%   Pu/HM %.3f%%   FP/HM %.3f%%"
          % (100 * comp["U235"] / c.hm,
             100 * (comp["Pu239"] + comp["Pu240"] + comp["Pu241"]) / c.hm,
             100 * comp["FPlump"] / c.hm))
    print("  k_inf %.5f  (%+7.0f pcm)   k_eff %.5f  (%+7.0f pcm, %.0f rods)"
          % (k1, pcm(k1), ke1, pcm(ke1), rods(ke1)))

    poisoned, n_xe, n_sm = c.with_poisons(comp, flux)
    k2, ke2, _, _ = c.solve(poisoned)
    print("\n  + equilibrium Xe-135 %.2e, Sm-149 %.2e" % (n_xe, n_sm))
    print("  k_inf %.5f  (%+7.0f pcm)   k_eff %.5f  (%+7.0f pcm, %.0f rods)"
          % (k2, pcm(k2), ke2, pcm(ke2), rods(ke2)))

    lo, hi = ORM_BAND
    print("\nREGULATION (Регламент 1Э-С-II, ГЛАВА 9): ORM >= %.0f-%.0f rods"
          " = %+.0f..%+.0f pcm" % (lo, hi, lo * ROD_WORTH * 1e5, hi * ROD_WORTH * 1e5))
    print("MODEL at %.1f MWd/kg: %.0f rods (%+.0f pcm)"
          % (TARGET_BURNUP, rods(ke2), pcm(ke2)))
    print("GAP: %+.0f pcm = %.0f rods" % (pcm(ke2) - hi * ROD_WORTH * 1e5,
                                          rods(ke2) - hi))
    return dict(fresh=kef, burned=ke1, poisoned=ke2, comp=comp, cell=c)


if __name__ == "__main__":
    report(quick="--quick" in sys.argv)
