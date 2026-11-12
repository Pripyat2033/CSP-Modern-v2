#!/usr/bin/env python3
"""Verification suite for the RBMK lattice calculation.

Each check compares the code against an independent, published value or an exact
internal identity. Run:  python3 verify.py
"""
from __future__ import annotations

import numpy as np

import endf
import rbmk_cell as cell
import rbmk_spectrum as spec
import rbmk_cell1d as c1d

PASS, FAIL = "PASS", "FAIL"
results = []


def check(name, got, want, tol, unit=""):
    ok = abs(got - want) <= tol
    results.append((ok, name, got, want, tol, unit))
    print(f"  [{PASS if ok else FAIL}] {name}: got {got:.4g}{unit}, "
          f"expected {want:.4g}+-{tol:.3g}{unit}")


def check_true(name, cond, detail=""):
    results.append((cond, name, None, None, None, detail))
    print(f"  [{PASS if cond else FAIL}] {name} {detail}")


def main():
    print("RBMK lattice verification\n")

    print("1. Evaluated nuclear data (ENDF/B-VIII.0) sanity")
    f235 = float(endf.load("U235", 18).at(0.0253))
    check("U-235 fission @ 0.0253 eV", f235, 585.0, 10.0, " b")
    c235 = float(endf.load("U235", 102).at(0.0253))
    check("U-235 capture @ 0.0253 eV", c235, 99.0, 4.0, " b")
    c12 = float(endf.load("C12", 2).at(0.0253))
    check("C-12 elastic @ 0.0253 eV", c12, 4.7, 0.4, " b")
    u = endf.load("U238", 102)
    E = np.exp(np.linspace(np.log(0.5), np.log(1e6), 6000))
    ri = np.trapezoid(u.at(E) / E, E)
    check("U-238 capture resonance integral", ri, 277.0, 15.0, " b")

    print("\n2. Geometry")
    A = cell.areas()
    check_true("cell area = 25^2 = 625 cm^2", abs(A["cell"] - 625.0) < 1e-9)
    check_true("fuel area matches 18 rods of 1.18 cm dia",
               abs(A["fuel"] - 18 * np.pi * (cell.ROD_FUEL_D / 2) ** 2) < 1e-9)

    print("\n3. 1-D multigroup operator reproduces the 0-D result (exact identity)")
    import scipy.sparse as sp
    import scipy.sparse.linalg as spla
    hom = spec.build_fine(endf.weight_combined, 0.0)
    St, Sa, Sf, nuSf, Ss = hom["St"], hom["Sa"], hom["Sf"], hom["nuSf"], hom["Ss"]
    D = 1.0 / (3.0 * np.maximum(St, 1e-8))
    G, N = spec.G, 30
    r = np.linspace(0, 10, N + 1); dr = np.diff(r); ra, rb = r[:-1], r[1:]
    V = np.pi * (rb ** 2 - ra ** 2)
    idx = lambda a, i: a * N + i
    rows, cols, vals = [], [], []
    for g in range(G):
        for i in range(N):
            row = idx(g, i); kR = kL = 0.0
            if i < N - 1:
                kR = D[g] * 2 * np.pi * rb[i] / dr[i]
                rows.append(row); cols.append(idx(g, i + 1)); vals.append(-kR)
            if i > 0:
                kL = D[g] * 2 * np.pi * ra[i] / dr[i]
                rows.append(row); cols.append(idx(g, i - 1)); vals.append(-kL)
            rows.append(row); cols.append(row)
            vals.append(kR + kL + (St[g] - Ss[g, g]) * V[i])
            for gp in range(G):
                if gp != g:
                    rows.append(row); cols.append(idx(gp, i)); vals.append(-Ss[gp, g] * V[i])
    Aop = sp.csr_matrix((vals, (rows, cols)), shape=(G * N, G * N))
    rows, cols, vals = [], [], []
    for i in range(N):
        for gp in range(G):
            if nuSf[gp] == 0:
                continue
            for g in range(G):
                rows.append(idx(g, i)); cols.append(idx(gp, i))
                vals.append(spec.watt_chi()[g] * nuSf[gp] * V[i])
    Fop = sp.csr_matrix((vals, (rows, cols)), shape=(G * N, G * N))
    phi = np.tile(spec.watt_chi(), N); phi /= phi.sum()
    lu = spla.splu(Aop.tocsc()); k = 1.0
    for _ in range(600):
        Fp = Fop @ phi; phin = lu.solve(Fp)
        kn = float((Fop @ phin).sum() / Fp.sum()); phin /= phin.sum()
        if abs(kn - k) < 1e-12:
            phi, k = phin, kn; break
        phi, k = phin, kn
    k0, _ = spec.solve_k(hom)
    check("k (1-D homogeneous) = k (0-D)", k, k0, 1e-3)

    print("\n4. Heterogeneous cell physics")
    k_w, flux_w, data_w = c1d.solve_cell(0.0)
    r, zname, St2, Sa2, Sf2, nuSf2, Ss2, V2, D2 = data_w
    th = np.where(spec.EG < spec.THERMAL_CUT)[0]
    fu = [i for i, z in enumerate(zname) if z == "fuel"]
    gr = [i for i, z in enumerate(zname) if z == "graph"]
    dis = flux_w[np.ix_(th, fu)].mean() / flux_w[np.ix_(th, gr)].mean()
    check_true("thermal disadvantage factor in [0.6, 0.9]",
               0.6 < dis < 0.9, f"(= {dis:.3f})")

    print("\n5. Void coefficient (the RBMK's defining trait)")
    k_v, _, _ = c1d.solve_cell(1.0)
    check_true("void effect is POSITIVE (water -> steam raises k)", k_v > k_w,
               f"(k_water={k_w:.4f} k_void={k_v:.4f})")
    beta = 0.0065
    rho = (k_v - k_w) / (k_w * k_v) * 1e5
    check_true("void effect within a plausible RBMK band (0.5-15 beta)",
               0.5 < rho / 1e5 / beta < 15.0, f"(= {rho/1e5/beta:+.2f} beta)")

    n_pass = sum(1 for ok, *_ in results if ok)
    print(f"\n{'-'*52}\n{n_pass}/{len(results)} checks passed")
    return 0 if n_pass == len(results) else 1


if __name__ == "__main__":
    raise SystemExit(main())
