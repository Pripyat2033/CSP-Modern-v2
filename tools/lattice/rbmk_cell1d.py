#!/usr/bin/env python3
"""1-D cylindrical multigroup diffusion solve of the RBMK lattice cell.

A homogenised cell cannot see the thermal flux depression inside the fuel — the
"disadvantage factor" — and without it k_inf comes out far too high and the void
coefficient is meaningless. So the cell is solved properly, in space:

    -div(D_g grad phi_g) + Sa_g phi_g + sum_{g'!=g} Ss[g->g'] phi_g
        = (1/k) chi_g sum_g' nuSf_g' phi_g' + sum_{g'!=g} Ss[g'->g] phi_g'

on a radial mesh from the cell axis to the equivalent cell radius, with reflective
boundary conditions at both ends (an infinite lattice). G = 69 energy groups.

Self-scatter is kept on the diagonal (total removal minus self-transfer), which is
what makes the operator conservative. From the converged spatial flux the code
homogenises the cell and collapses to two groups, and reports k_inf and the VOID
COEFFICIENT — all from geometry and evaluated data, nothing typed in.

Units: cm, barns, eV.
"""
from __future__ import annotations

import numpy as np
import scipy.sparse as sp
import scipy.sparse.linalg as spla

import endf
import rbmk_cell as cell
import rbmk_spectrum as spec

G = spec.G
R_CELL = cell.PITCH / np.sqrt(np.pi)      # equivalent radius of the 250 mm square
CHI = spec.watt_chi()


def zone_radii():
    """Radii from the cell axis outward, with the 18-rod cluster modelled as an
    annular fuel band of the SAME area and SAME mean radius as the real ring of
    rods — the standard cylindrical-cell treatment. Smearing the cluster into one
    central lump (as a naive cell would) over-depresses the thermal flux and
    wrecks k_inf."""
    A = cell.areas()
    # rod ring: 18 rods on a circle; ring radius ~3.5 cm puts them inside the
    # 88 mm tube (circumference 18*1.18 = 21.2 cm => r >= 3.37 cm)
    ring_r = 3.50
    a_fuel = A["fuel"]
    w = a_fuel / (2 * np.pi * ring_r)        # band width, area = 2*pi*ring_r*w
    r1 = ring_r - w / 2
    r2 = ring_r + w / 2
    r_clad = r2 + 0.09                       # thin clad outside the band
    r_ti = cell.TUBE_ID / 2
    r_to = cell.TUBE_OD / 2
    r_h = cell.HOLE_D / 2
    return [0.0, r1, r2, r_clad, r_ti, r_to, r_h, R_CELL], \
           ["cool", "fuel", "clad", "cool", "tube", "cool", "graph"]


def build_mesh(n_per_cm=8.0):
    edges, names = zone_radii()
    rs = [0.0]
    for a, b in zip(edges[:-1], edges[1:]):
        n = max(3, int(round((b - a) * n_per_cm)))
        rs += list(np.linspace(a, b, n + 1)[1:])
    r = np.array(rs)
    rc = 0.5 * (r[:-1] + r[1:])
    zid = np.searchsorted(edges[1:-1], rc, side="right")
    zname = [names[i] for i in zid]
    return r, zname


def _material_arrays(void_fraction, fuel_temp_c):
    zone_out, vols, meta = spec.build_zones(endf.weight_combined, void_fraction, fuel_temp_c)
    r, zname = build_mesh()
    N = len(zname)
    St = np.zeros((G, N)); Sa = np.zeros((G, N))
    Sf = np.zeros((G, N)); nuSf = np.zeros((G, N))
    Ss = np.zeros((G, G, N)); D = np.zeros((G, N))
    for i, zn in enumerate(zname):
        z = zone_out[zn]
        St[:, i] = z["St"]; Sa[:, i] = z["Sa"]
        Sf[:, i] = z["Sf"]; nuSf[:, i] = z["nuSf"]
        Ss[:, :, i] = z["Ss"]
        D[:, i] = 1.0 / (3.0 * np.maximum(z["St"], 1e-8))
    return r, zname, St, Sa, Sf, nuSf, Ss, D


def solve_cell(void_fraction, fuel_temp_c=600.0, tol=1e-10, max_it=400):
    """Return k_inf, the (G,N) spatial flux, and the material data."""
    r, zname, St, Sa, Sf, nuSf, Ss, D = _material_arrays(void_fraction, fuel_temp_c)
    N = len(zname)
    dr = np.diff(r)
    ra, rb = r[:-1], r[1:]
    V = np.pi * (rb ** 2 - ra ** 2)

    idx = lambda g, i: g * N + i
    rows, cols, vals = [], [], []

    def add(row, col, v):
        rows.append(row); cols.append(col); vals.append(v)

    def face_D(g, i):                       # face between cell i-1 and i
        d1, d2 = D[g, i - 1], D[g, i]
        return 2 * d1 * d2 / (d1 + d2) if (d1 + d2) > 0 else 0.0

    for g in range(G):
        for i in range(N):
            row = idx(g, i)
            kR = kL = 0.0
            if i < N - 1:
                kR = face_D(g, i + 1) * 2 * np.pi * rb[i] / dr[i]
                add(row, idx(g, i + 1), -kR)
            if i > 0:
                kL = face_D(g, i) * 2 * np.pi * ra[i] / dr[i]
                add(row, idx(g, i - 1), -kL)
            add(row, row, kR + kL + (St[g, i] - Ss[g, g, i]) * V[i])
            for gp in range(G):
                if gp != g:
                    add(row, idx(gp, i), -Ss[gp, g, i] * V[i])
    A = sp.csr_matrix((vals, (rows, cols)), shape=(G * N, G * N))

    rows, cols, vals = [], [], []
    for i in range(N):
        for gp in range(G):
            if nuSf[gp, i] == 0:
                continue
            for g in range(G):
                rows.append(idx(g, i)); cols.append(idx(gp, i))
                vals.append(CHI[g] * nuSf[gp, i] * V[i])
    F = sp.csr_matrix((vals, (rows, cols)), shape=(G * N, G * N))

    lu = spla.splu(A.tocsc())
    phi = np.tile(CHI, N)
    phi /= phi.sum()
    k = 1.0
    for _ in range(max_it):
        Fphi = F @ phi
        phi_new = lu.solve(Fphi)          # phi_new = A^-1 F phi
        # k_new is the Rayleigh quotient of phi_new: sum(F phi_new)/sum(A phi_new)
        # and A phi_new = F phi, so the denominator is sum(F phi).
        k_new = float((F @ phi_new).sum() / Fphi.sum())
        phi_new = phi_new / phi_new.sum()
        if abs(k_new - k) < tol:
            phi, k = phi_new, k_new
            break
        phi, k = phi_new, k_new
    return k, phi.reshape(G, N), (r, zname, St, Sa, Sf, nuSf, Ss, V, D)


def homogenise_2group(flux, data):
    """Collapse to 2 groups and homogenise. Each coarse-group constant is the
    flux- and volume-weighted average, <Sigma phi V>/<phi V>, over that group."""
    r, zname, St, Sa, Sf, nuSf, Ss, V, D = data
    G, N = flux.shape
    grp = np.where(spec.EG >= spec.THERMAL_CUT, 0, 1)
    out = {}
    for Gg in (0, 1):
        sel = grp == Gg
        phiv = np.sum(flux[sel] * V[None, :])
        for name, arr in (("St", St), ("Sa", Sa), ("Sf", Sf), ("nuSf", nuSf)):
            out.setdefault(name, np.zeros(2))[Gg] = \
                np.sum(arr[sel] * flux[sel] * V[None, :]) / max(phiv, 1e-30)
        out.setdefault("phi2", np.zeros(2))[Gg] = phiv
    s12 = 0.0
    for gp in np.where(grp == 0)[0]:
        for g in np.where(grp == 1)[0]:
            s12 += np.sum(Ss[gp, g, :] * flux[gp, :] * V)
    out["Ss12"] = s12 / max(np.sum(flux[grp == 0] * V[None, :]), 1e-30)
    return out


def main():
    res = {}
    for vf in (0.0, 1.0):
        k, flux, data = solve_cell(vf)
        two = homogenise_2group(flux, data)
        res[vf] = (k, two)
        tag = "WATER" if vf == 0 else "VOID "
        print(f"[{tag}] k_inf = {k:.5f}")
        print(f"        Sa  = {two['Sa'][0]:.5f} {two['Sa'][1]:.5f} /cm")
        print(f"        nuSf= {two['nuSf'][0]:.5f} {two['nuSf'][1]:.5f} /cm")
        print(f"        Ss12= {two['Ss12']:.5f} /cm   flux {two['phi2']/two['phi2'].sum()}")
    k0, k1 = res[0.0][0], res[1.0][0]
    beta = 0.0065
    drho = (k1 - k0) / (k0 * k1) * 1e5
    print(f"\nvoid: k_water={k0:.5f} k_void={k1:.5f} dk/k={k1-k0:+.5f} "
          f"{drho:+.0f} pcm = {drho/1e5/beta:+.2f} beta")


if __name__ == "__main__":
    main()
