#!/usr/bin/env python3
"""69-group spectrum solve of the RBMK lattice cell -> 2-group reactor constants.

Ties together geometry (rbmk_cell) and evaluated nuclear data (endf) into the
few-group constants the core runs on, and — the reason this exists — computes the
VOID COEFFICIENT from first principles:

    water -> steam  =>  less moderation + less absorption  =>  harder spectrum
                    =>  k_inf rises   (the RBMK's defining, dangerous trait)

Nothing here is a typed-in feedback scale. The void coefficient is an OUTPUT.

Method (the standard lattice route):
  1. Fine-group (69) macroscopic cross sections, flux-weighted, with U-238
     resonance self-shielding applied in the resonance range.
  2. Elastic scattering matrix from the isotropic-CM energy-transfer kernel,
     per nuclide mass; scattering never upscatters elastically (A > 1).
  3. A fission spectrum (Watt, U-235) and per-nuclide nubar.
  4. k_inf and the flux from the multigroup balance (B^2 = 0, infinite lattice)
     by power iteration.
  5. Collapse flux -> 2 groups (fast > 0.625 eV, thermal below) and report the
     two-group constants: D, Sigma_a, nuSigma_f, removal, Sigma_s(1->2), k_inf.

Honest limitations (stated, not hidden): no thermal up-scattering (would need
S(alpha,beta) thermal scattering law), no explicit spatial transport inside the
cluster (homogenised cell), 69 groups. Each biases k slightly but the void
SIGN and MAGNITUDE are robust because they are set by the moderator-to-fuel ratio
and the U-238 resonance absorption, both of which are modelled from real data.
"""
from __future__ import annotations

import numpy as np

import endf
import rbmk_cell as cell

THERMAL_CUT = cell.THERMAL_CUT
BOUNDS = cell.BOUNDS
G = len(BOUNDS) - 1
# The arrays from endf.group_average are stored FAST-FIRST: index 0 is the highest
# energy group. EG/EU give the energy bounds of array index g.
EG = BOUNDS[::-1][:G]        # EG[g] = lower energy bound of array group g
EU = BOUNDS[::-1][1:]        # EU[g] = upper energy bound of array group g

# per-nuclide average neutrons per fission (evaluated thermal/fast values)
NUBAR = {"U235": 2.4367, "U238": 2.4930, "Pu239": 2.8766, "Pu241": 2.9345}
# fissioning labels
FISSILE = ("U235", "U238", "Pu239", "Pu241")


def watt_chi():
    """Watt fission spectrum for U-235 (a=0.988 MeV, b=2.249 /MeV), on the groups."""
    a, b = 0.988e6, 2.249e-6
    E = np.exp(np.linspace(np.log(BOUNDS[0]), np.log(BOUNDS[-1]), 4000))
    chi_E = np.exp(-E / a) * np.sinh(np.sqrt(b * E))
    chi = np.zeros(G)
    for g in range(G):
        m = (E >= BOUNDS[g]) & (E <= BOUNDS[g + 1])
        chi[g] = np.trapezoid(chi_E[m], E[m]) if m.sum() > 1 else 0.0
    return (chi / chi.sum())[::-1]        # ascending -> fast-first


def elastic_transfer(A, weight):
    """Flux-weighted elastic transfer matrix P[g'][g] (isotropic in CM).

    E_out lies in [alpha*E', E'], alpha = ((A-1)/(A+1))^2. The probability of a
    scatter from group g' landing in group g is the flux-weighted overlap of that
    interval with the group.

    Built in ascending-energy order (index 0 = lowest energy, matching BOUNDS) and
    returned FAST-FIRST (index 0 = highest energy) to match the macroscopic arrays.
    """
    alpha = ((A - 1.0) / (A + 1.0)) ** 2
    P = np.zeros((G, G))
    for gp in range(G):
        lo, hi = BOUNDS[gp], BOUNDS[gp + 1]
        Ep = np.exp(np.linspace(np.log(lo), np.log(hi), 24))
        w = weight(Ep)
        denom = np.trapezoid(w, np.log(Ep))
        if denom <= 0:
            P[gp, gp] = 1.0
            continue
        for g in range(G):
            glo, ghi = BOUNDS[g], BOUNDS[g + 1]
            out = np.maximum(0.0, np.minimum(ghi, Ep) - np.maximum(glo, alpha * Ep))
            Pg = out / ((1 - alpha) * Ep)
            P[gp, g] = np.trapezoid(Pg * w, np.log(Ep)) / denom
        s = P[gp].sum()
        if s > 0:
            P[gp] /= s
    return P[::-1, ::-1]        # ascending -> fast-first


def elastic_transfer_full(A, weight, T=600.0):
    """Elastic transfer matrix with thermal up-scatter, rows summing to 1.

    Start from the down-scattering kernel (isotropic CM), then add up-scatter for
    the thermal groups by DETAILED BALANCE against the Maxwellian equilibrium flux
    phi(E) ~ E exp(-E/T):

        P[j -> i] * phi_j = P[i -> j] * phi_i        (i higher energy than j)

    and finally renormalise each row to 1 so the matrix is a probability
    distribution and the scattering rate out of every group equals its total
    scattering cross section (neutron balance is preserved exactly).
    """
    P = elastic_transfer(A, weight)        # fast-first, rows sum to 1, down only
    th = np.where(EG < 1.0)[0]
    if len(th) >= 2:
        Emid = 0.5 * (EG[th] + EU[th])
        phig = Emid * np.exp(-Emid / T)    # equilibrium flux weight per group
        for a, i in enumerate(th):
            for b, j in enumerate(th):
                if j > i:                  # j is lower energy -> up-scatter j->i
                    P[j, i] = P[i, j] * phig[a] / phig[b]
        P = P / P.sum(axis=1, keepdims=True)
    return P


def build_zones(flux_weight, void_fraction, fuel_temp_c=600.0):
    """Per-zone fine-group macroscopic constants for the cell.

    Returns (zone_out, vols, meta) where zone_out[zone] has St, Sa, Sf, nuSf, Ss.
    Scattering is total-minus-absorption so neutrons are conserved."""
    nd, A = cell.number_densities(void_fraction, fuel_temp_c)
    sig_pot = 10.0
    nd_u = nd["U238"] + nd["U235"]
    sigma0 = max((1 - cell.dancoff()) * sig_pot * (nd["C12"] + 0.3 * nd["O16"]) / max(nd_u, 1e-9), 1.0)
    res_lo, res_hi = 0.625, 1.0e5
    res_groups = np.array([g for g in range(G)
                           if EG[g] >= res_lo and EU[g] <= res_hi * 1.01])

    def shield(xs_cap):
        base = endf.group_average(xs_cap, BOUNDS, flux_weight)
        eff = endf.effective_capture(xs_cap, BOUNDS, endf.weight_1_over_e, sigma0)
        out = base.copy()
        out[res_groups] = eff[res_groups]
        return out

    zones = {
        "fuel":  [("U235", nd["U235"], True), ("U238", nd["U238"], True), ("O16", nd["O16"], False)],
        "clad":  [("Zr90", nd["Zr"] * 0.5145, False)],
        "tube":  [("Zr90", nd["Zr"] * 0.5145, False)],
        "graph": [("C12", nd["C12"], False), ("C13", nd["C13"], False)],
        "cool":  [("H1", nd["H1"], False), ("O16", nd["O16_w"], False)],
    }
    MASS = {"U235": 235.0, "U238": 238.0, "O16": 16.0, "Zr90": 90.0,
            "C12": 12.0, "C13": 13.0, "H1": 1.0, "Pu239": 239.0, "Pu241": 241.0}

    zone_out = {}
    for zname, spec_ in zones.items():
        St = np.zeros(G); Sa = np.zeros(G); Sf = np.zeros(G); nuSf = np.zeros(G)
        Ss = np.zeros((G, G))
        for label, dens, shield_cap in spec_:
            if dens <= 0:
                continue
            xs_t = endf.load(label, 1)
            xs_c = endf.load(label, 102)
            xs_f = endf.load(label, 18)
            st = endf.group_average(xs_t, BOUNDS, flux_weight)
            sc = shield(xs_c) if shield_cap else endf.group_average(xs_c, BOUNDS, flux_weight)
            sf = endf.group_average(xs_f, BOUNDS, flux_weight) if xs_f else np.zeros(G)
            ss = np.maximum(st - sc - sf, 0.0)
            St += dens * st
            Sa += dens * (sc + sf)
            Sf += dens * sf
            nuSf += dens * sf * NUBAR.get(label, 0.0)
            P = elastic_transfer_full(MASS[label], flux_weight)
            Ss += dens * (ss[:, None] * P)
        zone_out[zname] = dict(St=St, Sa=Sa, Sf=Sf, nuSf=nuSf, Ss=Ss)

    vols = {"fuel": A["fuel"], "clad": A["clad"], "tube": A["tube"],
            "graph": A["graphite"], "cool": A["coolant"] + A["gap"]}
    return zone_out, vols, dict(sigma0=sigma0, areas=A)


def build_fine(flux_weight, void_fraction, fuel_temp_c=600.0):
    """Homogenised fine-group macroscopic constants for the cell."""
    zone_out, vols, meta = build_zones(flux_weight, void_fraction, fuel_temp_c)
    tot = sum(vols.values())
    hom = dict(St=np.zeros(G), Sa=np.zeros(G), Sf=np.zeros(G),
               nuSf=np.zeros(G), Ss=np.zeros((G, G)))
    for z, v in vols.items():
        w = v / tot
        for k in ("St", "Sa", "Sf", "nuSf", "Ss"):
            hom[k] += zone_out[z][k] * w
    hom["chi"] = watt_chi()
    hom["sigma0"] = meta["sigma0"]
    return hom


def solve_k(hom, B2=0.0):
    """Multigroup k_inf and flux by power iteration (B^2 = 0 infinite lattice)."""
    St, Ss = hom["St"], hom["Ss"]
    nuSf, chi = hom["nuSf"], hom["chi"]

    # L[g,g] = St[g] (+ leakage D*B2), L[g,g'] = -Ss[g',g]
    L = -Ss.T.copy()
    L[np.diag_indices(G)] += St
    if B2 > 0:
        D = 1.0 / (3.0 * np.maximum(St, 1e-6))
        L[np.diag_indices(G)] += D * B2

    phi = chi.copy()
    k = 1.0
    for _ in range(300):
        F = float(np.dot(nuSf, phi))
        rhs = chi * F
        phi_new = np.linalg.solve(L, rhs)
        F_new = float(np.dot(nuSf, phi_new))
        k_new = F_new / max(F, 1e-30)
        phi = phi_new / phi_new.sum()
        if abs(k_new - k) < 1e-9:
            k = k_new
            break
        k = k_new
    return k, phi


def collapse_2group(hom, phi):
    """Collapse fine-group flux and constants to two groups (1 fast, 2 thermal)."""
    fast = EG >= THERMAL_CUT          # array indices of fast groups
    grp = np.where(fast, 0, 1)
    out = {}
    for name in ("St", "Sa", "Sf", "nuSf"):
        arr = hom[name]
        out[name] = np.array([np.dot(arr[grp == Gg], phi[grp == Gg]) /
                              max(phi[grp == Gg].sum(), 1e-30) for Gg in (0, 1)])
    # scattering: total out-scatter from group 1, and 1->2 transfer
    Ss = hom["Ss"]  # Ss[g',g]
    phi1 = phi[grp == 0].sum()
    s12 = float((Ss[grp == 0][:, grp == 1] * phi[grp == 0][:, None]).sum() / max(phi1, 1e-30))
    s_out1 = float((Ss[grp == 0].sum(axis=1) * phi[grp == 0]).sum() / max(phi1, 1e-30))
    out["Ss12"] = s12
    out["Ss_out1"] = s_out1
    # diffusion coefficients from transport cross section
    D = 1.0 / (3.0 * np.maximum(out["St"], 1e-6))
    out["D"] = D
    return out


def reactivity(k0, k1):
    """Reactivity change in pcm."""
    return (k1 - k0) / (k0 * k1) * 1e5


def main():
    print("RBMK-1000 lattice cell — 69-group spectrum solve")
    print(f"  groups={G}  thermal cut={THERMAL_CUT} eV")
    weight = endf.weight_combined

    results = {}
    for vf in (0.0, 1.0):
        hom = build_fine(weight, vf)
        k, phi = solve_k(hom)
        two = collapse_2group(hom, phi)
        results[vf] = (k, two, hom)
        tag = "WATER" if vf == 0 else "VOID "
        print(f"\n  [{tag}]  k_inf = {k:.5f}")
        print(f"          Sigma_a  = {two['Sa'][0]:.5f} (fast) {two['Sa'][1]:.5f} (therm) /cm")
        print(f"          nuSigma_f= {two['nuSf'][0]:.5f} (fast) {two['nuSf'][1]:.5f} (therm)")
        print(f"          D        = {two['D'][0]:.4f} (fast) {two['D'][1]:.4f} (therm) cm")
        print(f"          Sigma_s(1->2) = {two['Ss12']:.5f} /cm")

    k_w = results[0.0][0]
    k_v = results[1.0][0]
    dk = k_v - k_w
    print(f"\n  void coefficient:  dk/k = {dk/max(k_w,1e-9):+.5f}")
    print(f"                     d(rho) = {reactivity(k_w, k_v):+.1f} pcm")
    beta = 0.0065
    print(f"                     = {(reactivity(k_w,k_v)/1e5)/beta:+.2f} beta  (beta={beta})")
    print(f"\n  self-shielding background sigma0 = {results[0.0][2]['sigma0']:.2f} b")


if __name__ == "__main__":
    main()
