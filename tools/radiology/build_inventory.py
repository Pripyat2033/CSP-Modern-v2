#!/usr/bin/env python3
"""Build the REKORD layer-2 inventory: atoms of each nuclide at shutdown.

A FISSION YIELD IS NOT AN INVENTORY. The independent yield of Cs-137 is 0.0006%
per fission; its cumulative yield is 6.19%. The difference is real -- Cs-137 is
made almost entirely by beta decay of its precursors (I-137 -> Xe-137 -> Cs-137)
during the irradiation. So the inventory is not a yield times a fission count.
It is the solution of the decay-chain equations with fission as a source:

    dn_i/dt = Y_i*F - lambda_i*n_i + sum_j b_ji*lambda_j*n_j

Two shortcuts are wrong and are not taken:

  * cumulative yield x F x T -- overstates a short-lived nuclide (I-131, 8 d,
    over 618 d of irradiation, by lambda*T ~ 55x) and understates one fed by a
    long-lived parent (Y-90, fed by 28.8-y Sr-90, which barely decays in 618 d).
  * the equilibrium formula Y/lambda -- wrong for Sr-90 and Cs-137, whose
    half-lives (28.8 y, 30.1 y) are long against the irradiation.

METHOD. Beta decay conserves mass number, so each mass chain is an independent
linear system. For decay matrix M (M_ii = -lambda_i, M_ji = b_ji*lambda_j) and
source s = Y*F, the inventory after irradiation time T is the integral of
exp(M(T-t)) s dt, obtained exactly from one exponential of the augmented matrix
[[M, s], [0, 0]]. That is the Bateman solution; it handles stable members (zero
eigenvalue) with no special case and needs no stiff ODE integration.

THE FISSION COUNT is not typed in here. `tools/lattice/burnup.py` burns the real
RBMK cell to unit 4's 10.3 MWd/kg and reports fissions per nuclide; the cell
tally is scaled to the documented 192 t of uranium. The result is cross-checked
against the independent energy anchor (10.3 MWd/kg over 192 t at 200 MeV per
fission) and the script refuses to write if the two disagree by more than 2%.

WHAT THIS IS NOT. Atoms at shutdown. Not activity (atoms x lambda, layer 1), not
a release (layer 3, contested), not a dose. No release fraction appears here.

Usage:
    python3 tools/radiology/build_inventory.py
"""

from __future__ import annotations

import json
import os
import sys
import urllib.parse
import urllib.request


import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.abspath(os.path.join(HERE, "..", ".."))
LATTICE = os.path.join(REPO, "tools", "lattice")
RAD = os.path.join(REPO, "src", "main", "resources", "data", "csp", "radiology")
sys.path.insert(0, LATTICE)

NA = 6.02214076e23
MEV_J = 1.602176634e-13
E_FISSION_MEV = 200.0
FUEL_MASS_T_U = 192.0            # docs/RBMK-CORE-GEOMETRY.md
TARGET_BURNUP = 10.3             # MWd/kg, unit 4 at the accident

# Irradiation time, set from the lattice burn in main(). A nuclide with no
# listed decay branch is a terminus if its half-life far exceeds this.
IRRADIATION_SECONDS = 618.0 * 86400.0

YIELDS = os.path.join(RAD, "fission-yields.json")
DECAY_CACHE = os.path.join(HERE, "decay-cache.json")
OUT = os.path.join(RAD, "inventory.json")

API = "https://nds.iaea.org/relnsd/v1/data"

# Decay mode -> (delta Z, delta A) of the daughter.
DELTA = {
    "B-": (1, 0), "B+P": (0, -1), "A": (-2, -4), "P": (-1, -1), "2P": (-2, -2),
    "EC": (-1, 0), "B+": (-1, 0), "EC+B+": (-1, 0), "2B+": (-2, 0),
}

# Beta-delayed neutron emission: a beta decay whose daughter is left excited
# above the neutron separation energy. It is NOT a separate branch competing
# with beta decay -- it is the fraction OF the beta branch that also boils off
# neutrons. ENSDF reports it as its own row with the neutron-emission
# probability, e.g. I-137: B- 100%, B-N 7.14%, meaning 7.14% of decays go to
# Xe-136 and 92.86% to Xe-137. Adding it as a second 100% branch (the naive
# reading) invents a daughter that was never made.
NEUTRON_MODES = {"B-N": 1, "B-2N": 2, "B-3N": 3}

_SYMBOLS = (
    "H He Li Be B C N O F Ne Na Mg Al Si P S Cl Ar K Ca Sc Ti V Cr Mn Fe Co Ni "
    "Cu Zn Ga Ge As Se Br Kr Rb Sr Y Zr Nb Mo Tc Ru Rh Pd Ag Cd In Sn Sb Te I Xe "
    "Cs Ba La Ce Pr Nd Pm Sm Eu Gd Tb Dy Ho Er Tm Yb Lu Hf Ta W Re Os Ir Pt Au "
    "Hg Tl Pb Bi Po At Rn Fr Ra Ac Th Pa U Np Pu Am Cm Bk Cf Es Fm"
).split()


def symbol(z: int) -> str:
    return _SYMBOLS[z - 1]


def _za(key: str) -> tuple[int, int]:
    """'55-137' or '56-137m' -> (Z, A)."""
    z, a = key.rstrip("m").split("-")
    return int(z), int(a)


# --- the reactor side: how many fissions, in which nuclide -------------------

def core_fissions():
    """Run the lattice depletion; return (total fissions, split, comp, cell, days)."""
    import burnup as b

    cell = b.Cell()
    comp, _, days = cell.burn()

    # cell.hm is a number density (per cm3 of CELL), so this is a density and
    # fuel mass / density gives the fuel volume, not a count of cells.
    grams_u_per_cm3 = cell.hm * 238.05 / NA * 1e24
    volume_cm3 = FUEL_MASS_T_U * 1e6 / grams_u_per_cm3

    raw = sum(cell.fissions.values())
    split = {k: v / raw for k, v in cell.fissions.items()}
    total = raw * 1e24 * volume_cm3          # barn-cm -> fissions
    return total, split, comp, cell, days, volume_cm3


# --- the nuclear-data side: decay chains ------------------------------------

def _get(params: dict) -> str:
    url = API + "?" + urllib.parse.urlencode(params)
    req = urllib.request.Request(url, headers={"User-Agent": "csp-modern/REKORD"})
    for attempt in range(3):
        try:
            with urllib.request.urlopen(req, timeout=45) as r:
                return r.read().decode("utf-8", "replace")
        except Exception:
            if attempt == 2:
                raise
    raise RuntimeError("unreachable")


def _rows(text: str) -> list[dict]:
    lines = [l for l in text.splitlines() if l.strip()]
    if len(lines) < 2:
        return []
    head = lines[0].split(",")
    return [dict(zip(head, v.split(","))) for v in lines[1:]
            if len(v.split(",")) == len(head)]


def _bm_branches(key: str, hl_seconds: float):
    """Decay route from the mean-beta table, or None if it has nothing.

    The levels table gives a half-life with an empty mode column for a handful
    of nuclides (Cu-68, Te-127). The decay_rads bm table carries the same decay
    with the mode, the branching percentage and the daughter spelled out, so
    the route can be read instead of guessed. Rows for every level of the
    nuclide come back together, so they are matched on the parent half-life.
    """
    z, a = key.rstrip("m").split("-")
    try:
        rows = _rows(_get({"fields": "decay_rads",
                           "nuclides": f"{symbol(int(z))}{a}",
                           "rad_types": "bm"}))
    except Exception:
        return None
    best, best_d = None, float("inf")
    for r in rows:
        try:
            rhl = float((r.get("half_life_sec", "") or "").strip())
        except ValueError:
            continue
        mode = (r.get("decay", "") or "").strip()
        if not mode or abs(rhl - hl_seconds) > 0.02 * max(hl_seconds, 1e-30):
            continue
        d = abs(rhl - hl_seconds)
        if d < best_d:
            pct = (r.get("decay_%", "") or "").strip()
            best, best_d = [mode, float(pct) if pct else None], d
    return [best] if best else None


def derive_gap_routes(decay: dict, yields: dict) -> dict:
    """Route a short-lived, mode-less nuclide using the evaluated yield chain.

    A fission product with no tabulated decay mode is not necessarily unknown.
    The evaluated cumulative yields say where the mass in its mass chain ends
    up: if the daughter one Z higher carries far more cumulative yield than the
    nuclide itself, matter at this Z must move upward, and the only mode that
    raises Z at fixed A is beta-minus. This is read from the same evaluated
    file that supplies the yields, so it is a derivation, not an assumption,
    and it is recorded as such for audit.
    """
    def cum(z: int, a: int) -> float:
        total = 0.0
        for table in yields.values():
            rec = table.get("cumulative", {}).get(f"{z}-{a}")
            if rec:
                total += rec.get("cumulative", 0.0)
        return total

    for key, rec in decay.items():
        if _decay_state(key, rec) != "gap":
            continue
        z, a = (int(x) for x in key.rstrip("m").split("-"))
        here, down, up = cum(z, a), cum(z + 1, a), cum(z - 1, a)
        # A tenfold excess is far outside the yield uncertainties (which are
        # tens of percent), so it cannot be read as noise.
        if down > 10.0 * max(here, 1e-30):
            rec["branches"] = [["B-", 100.0]]
        elif up > 10.0 * max(here, 1e-30):
            rec["branches"] = [["B+", 100.0]]
        else:
            continue
        rec["provenance"] = "yield-chain-derived"
    return decay


def load_decay(keys: set[str]) -> dict:
    """Half-life and decay branches per node key ('Z-A' ground, 'Z-Am' isomer).

    Isomer keys need the excited level, not the ground state: Ba-137m lives 2.55
    min while Ba-137 is stable, and giving the isomer the ground state's
    half-life would freeze 8.7% of the fission products in place forever.
    """
    cache = {}
    if os.path.exists(DECAY_CACHE):
        with open(DECAY_CACHE) as fh:
            cache = json.load(fh)

    todo = sorted(k for k in keys if k not in cache)
    print(f"decay data: {len(cache)} cached, {len(todo)} to fetch", flush=True)
    for i, key in enumerate(todo):
        z, a = key.rstrip("m").split("-")
        metastable = key.endswith("m")
        try:
            rows = _rows(_get({"fields": "levels",
                               "nuclides": f"{symbol(int(z))}{a}"}))
        except Exception as exc:
            cache[key] = {"error": str(exc)}
            continue

        if not rows:
            # An empty response means no ENSDF dataset exists for this
            # nuclide at all -- a real limit of the evaluation, not a failed
            # fetch. Distinguishing the two matters: a failed fetch must stop
            # the build, a nuclide nobody has measured must only be reported.
            cache[key] = {"unevaluated": True}
            continue

        # The ground state is the level at energy 0; ENSDF lists it first, so
        # where several share energy 0 the first one is the ground state.
        ground_rows = [r for r in rows
                       if (r.get("energy", "") or "").strip() in ("", "0")]
        if not ground_rows:
            cache[key] = {"error": "no ground state"}
            continue
        ground = ground_rows[0]

        if metastable:
            # The isomer is the excited level that de-excites by internal
            # transition; where several do, the longest-lived is the isomer.
            #
            # Some evaluations (Nb-102, Rh-110, Sb-128, Pm-154) report the
            # ground state and the isomer BOTH at energy 0, distinguished only
            # by spin-parity. Requiring energy > 0 finds nothing there and
            # writes an error record, which the solver used to read as "stable"
            # and freeze the isomer's whole yield in place. So: take every
            # other level that has a half-life, prefer an explicit IT, then a
            # listed decay mode, then the longest-lived.
            cand = []
            for r in rows:
                if r is ground:
                    continue
                hl = (r.get("half_life_sec", "") or "").strip()
                if not hl:
                    continue
                try:
                    hlf = float(hl)
                except ValueError:
                    continue
                modes = {(r.get(f"decay_{j}", "") or "").strip() for j in (1, 2, 3, 4)}
                modes.discard("")
                cand.append((hlf, "IT" in modes, bool(modes), r))
            if not cand:
                # The yield library assigns this isomer its own independent
                # yield, but ENSDF tabulates no level for the nuclide with a
                # half-life, so the isomer's own half-life is genuinely absent.
                # Freezing those atoms is wrong (no fission-product isomer
                # survives 618 days) and inventing a half-life is a guess. The
                # bounded move is to de-excite the isomer into its own ground
                # state, which is exact at shutdown whenever the isomer is
                # short-lived against the irradiation, and to say so.
                ghl = (ground.get("half_life_sec", "") or "").strip()
                if not ghl:
                    cache[key] = {"error": "isomer level unresolved and ground "
                                           "state has no half-life"}
                    continue
                cache[key] = {"nuclide": f"{symbol(int(z))}{a}m",
                              "halfLifeSeconds": float(ghl),
                              "branches": [["IT", 100.0]],
                              "isomer": True,
                              "provenance": "isomer-level-unresolved"}
                continue
            it = [c for c in cand if c[1]]
            decaying = [c for c in cand if c[2]]
            pool = it or decaying or cand
            g = max(pool, key=lambda c: c[0])[3]
        else:
            g = ground

        hl = (g.get("half_life_sec", "") or "").strip()
        branches = []
        for j in (1, 2, 3, 4):
            mode = (g.get(f"decay_{j}", "") or "").strip()
            if not mode:
                continue
            pct = (g.get(f"decay_{j}_%", "") or "").strip()
            # A blank percentage is "not tabulated", NOT 100%. ENSDF leaves the
            # beta-delayed-neutron probability blank for many exotic nuclides,
            # and reading those blanks as 100% claims every decay emits neutrons.
            branches.append([mode, float(pct) if pct else None])
        provenance = "ensdf-levels"
        if not branches and hl:
            # The levels table gave a half-life with no mode. The mean-beta
            # table carries the same decay with mode and daughter, so read the
            # route from there rather than leaving the nuclide branchless.
            bm = _bm_branches(key, float(hl))
            if bm:
                branches, provenance = bm, "ensdf-decay_rads-bm"
        # Branches are stored exactly as the evaluation gives them, empty
        # included. What an empty list MEANS depends on the irradiation time,
        # which is a solver input, not a property of the nuclide, so that
        # decision is made in _edges and not baked into this cache.
        cache[key] = {"nuclide": f"{symbol(int(z))}{a}{'m' if metastable else ''}",
                      "halfLifeSeconds": float(hl) if hl else None,
                      "branches": branches,
                      "isomer": metastable,
                      "provenance": provenance}
        if (i + 1) % 40 == 0:
            with open(DECAY_CACHE, "w") as fh:
                json.dump(cache, fh)
            print(f"  {i + 1}/{len(todo)}", flush=True)
    with open(DECAY_CACHE, "w") as fh:
        json.dump(cache, fh)
    return cache


def _decay_state(key, rec):
    """What a node does with the atoms in it: decay, or hold them.

    Six outcomes, and the difference between the last four is the reason this
    function exists:

      * "error"    -- the fetch or parse failed. NOT the same as stable, and the
        check is first for exactly that reason: an error record has no
        half-life, so a "no half-life means stable" test silently freezes every
        atom in it. That is how 0.75% of the inventory was lost to ten isomers.
      * "unevaluated" -- no ENSDF dataset exists for the nuclide at all. Real
        atoms in an exotic corner of the yield distribution, but nobody has
        measured their decay, so they can only be reported.
      * "stable"   -- no half-life. Holds its atoms. Correct and common.
      * "terminus" -- a half-life far longer than the irradiation, and no listed
        branch. Zr-96 (2e19 y) and Ce-142 (5e16 y) are observationally stable:
        only double-beta decay, which evaluations never list as a branch. Holds.
      * "gap"      -- a short half-life and no listed branch. I-144 (0.3 us),
        I-142, Cu-68, Te-127. The evaluation is incomplete, not the physics.
        Held AND reported, so a data gap never masquerades as a result.
      * "decay"    -- a half-life and at least one branch, or an isomer (which
        defaults to IT). Decays along its branches.
    """
    if rec.get("error"):
        return "error"
    if rec.get("unevaluated"):
        return "unevaluated"
    hl = rec.get("halfLifeSeconds")
    if not hl:
        return "stable"
    if rec.get("branches"):
        return "decay"
    if key.endswith("m"):
        return "decay"                   # IT fallback inside _edges
    if float(hl) > 100.0 * IRRADIATION_SECONDS:
        return "terminus"
    return "gap"


def _edges(key, branches):
    """Daughter shares for one nuclide: ({daughter: fraction}, leaked fraction).

    All the rules below are forced by mass balance:

      * A blank percentage on an ordinary mode means "the whole decay" only if
        it is the sole mode; otherwise it means "not tabulated" and the mode is
        dropped. ENSDF leaves many beta-delayed-neutron rows blank.
      * Beta-delayed neutron emission is a sub-branch of beta decay, not a rival
        branch. I-137 with B- 100% and B-N 7.14% sends 92.86% of its decays to
        Xe-137 and 7.14% to Xe-136. A blank B-N probability is read as zero:
        that understates neutron emission slightly but never breaks the sum.
    """
    isomer = key.endswith("m")
    z, a = (int(x) for x in key.rstrip("m").split("-"))
    if not branches and isomer:
        # An isomer whose evaluation lists no mode: the physical default is an
        # internal transition to its own ground state.
        branches = [["IT", 100.0]]
    plain, neut = {}, {}
    for mode, pct in branches:
        if mode in NEUTRON_MODES:
            neut[mode] = (pct or 0.0) / 100.0
        elif mode in ("IT", "IS"):
            # Internal transition: the isomer becomes its own ground state, same
            # Z and A. This is the only route by which isomer yield enters the
            # ground state, so dropping it loses the isomer's whole share.
            plain["IT"] = plain.get("IT", 0.0) + (pct or 100.0)
            continue
        elif pct is None:
            plain.setdefault(mode, None)
        else:
            plain[mode] = plain.get(mode, 0.0) + pct

    tabulated = {m: p for m, p in plain.items() if p is not None}
    total = sum(tabulated.values())
    if total <= 0:
        tabulated = {m: 100.0 for m in plain}
        total = 100.0

    n_frac = min(sum(neut.values()), 1.0)
    shares, leak = {}, 0.0
    for mode, pct in tabulated.items():
        frac = pct / total
        if mode in ("IT", "IS"):
            d = (0, 0)
        else:
            d = DELTA.get(mode)
        if d is None:                    # fission, SF, or an unknown mode
            leak += frac
            continue
        dk = f"{z + d[0]}-{a + d[1]}"
        shares[dk] = shares.get(dk, 0.0) + frac * (1.0 - n_frac)
        for nmode, nmass in NEUTRON_MODES.items():
            f = neut.get(nmode, 0.0)
            if f > 0:
                nk = f"{z + d[0]}-{a + d[1] - nmass}"
                shares[nk] = shares.get(nk, 0.0) + frac * f
    return shares, leak


def decay_closure(seed, decay, limit=6000):
    """Every nuclide reachable from the seeds by decay, plus the seeds.

    Fission and spontaneous fission are terminal here: they produce a whole
    distribution, not a single daughter, and are handled as a loss.
    """
    nodes = set(seed)
    frontier = list(seed)
    while frontier:
        nxt = []
        for k in frontier:
            rec = decay.get(k) or {}
            if _decay_state(k, rec) != "decay":
                continue
            sh, _ = _edges(k, rec.get("branches", []))
            for dk in sh:
                if dk not in nodes:
                    nodes.add(dk)
                    nxt.append(dk)
                    if len(nodes) > limit:
                        raise SystemExit(
                            f"decay closure exceeded {limit} nodes; refusing to "
                            f"build an unbounded inventory")
        frontier = nxt
    return sorted(nodes)


def solve_component(members, shares, src, decay, T):
    """Atoms per node after irradiation, for one connected component.

    This is the matrix ODE dN/dt = M N + s, integrated to T with a stiff
    solver (Radau). The obvious alternatives all failed on this problem and the
    reasons are worth keeping:

      * expm on the whole 1112-node matrix loses ~1%, and up to 27% on
        mid-chain nuclides such as Ru-106. Half-lives here span 1e-11 s to
        1e22 s, so scaling-and-squaring needs ~60 squarings and each one
        amplifies rounding.
      * Splitting at the connected component helped but did not fix it: the
        largest component is still 440 nodes spanning the same range.
      * Slaving the fast nodes algebraically (quasi-steady-state) is wrong
        here by +0.08%, because the irradiation is only a few half-lives long
        for the slowest members and the startup transient is real.
      * expm_multiply does not terminate on this stiffness.

    Radau integrates the true transient. With rtol=1e-9 the total comes back
    at 1.00000000 of the atoms produced, and Ru-106 lands on the analytic
    Bateman value instead of 27% below it.
    """
    from scipy.integrate import solve_ivp

    m = len(members)
    pos = {k: i for i, k in enumerate(members)}
    M = np.zeros((m, m))
    for k in members:
        rec = decay.get(k) or {}
        if _decay_state(k, rec) != "decay":
            continue
        lam = np.log(2.0) / rec["halfLifeSeconds"]
        i = pos[k]
        M[i, i] -= lam
        sh, _ = shares[k]
        for dk, frac in sh.items():
            if dk in pos:
                M[pos[dk], i] += lam * frac

    s = np.array([src.get(k, 0.0) for k in members])
    sol = solve_ivp(lambda t, y: M @ y + s, (0.0, T), np.zeros(m),
                    method="Radau", rtol=1e-9, atol=1e-6, jac=M)
    if not sol.success:
        raise SystemExit(f"decay integration failed: {sol.message}")
    return {k: max(float(sol.y[pos[k], -1]), 0.0) for k in members}


def solve_all(nodes, src, decay, T):
    """Bateman solve over every node: ({node: atoms}, mass-balance residual).

    The unit of solution is the connected component of the decay graph, not the
    mass chain. Beta decay conserves A, so a per-chain solve looks right, but
    beta-delayed neutron emission moves 1.45% of fission products into the
    chain below, and a per-chain solver would silently drop them. Atoms only
    travel along decay edges, so a component is exactly self-contained.
    """
    from scipy.sparse import csr_matrix
    from scipy.sparse.csgraph import connected_components

    nodes = list(nodes)
    idx = {k: i for i, k in enumerate(nodes)}
    n = len(nodes)

    shares = {}
    rows, cols = [], []
    for k in nodes:
        rec = decay.get(k) or {}
        if _decay_state(k, rec) != "decay":
            shares[k] = ({}, 0.0)
            continue
        sh, lk = _edges(k, rec.get("branches", []))
        shares[k] = (sh, lk)
        for dk in sh:
            if dk in idx:
                rows.append(idx[dk])
                cols.append(idx[k])

    graph = csr_matrix((np.ones(len(rows)), (rows, cols)), shape=(n, n))
    _, label = connected_components(graph, directed=True, connection="weak")

    atoms = {}
    for comp in range(label.max() + 1):
        members = [k for k in nodes if label[idx[k]] == comp]
        atoms.update(solve_component(members, shares, src, decay, T))

    produced = sum(src.values()) * T
    residual = 1.0 - sum(atoms.values()) / produced if produced > 0 else 0.0
    return atoms, residual


def main() -> int:
    global IRRADIATION_SECONDS
    total, split, comp, cell, days, volume_cm3 = core_fissions()
    IRRADIATION_SECONDS = days * 86400.0

    # Independent anchor: the same burnup as pure energy, at 200 MeV/fission.
    # If the cell tally and this disagree, a unit conversion is wrong, so this
    # is a gate, not a comment.
    anchor = (TARGET_BURNUP * FUEL_MASS_T_U * 1000.0 * 86400.0 * 1e6
              / (E_FISSION_MEV * MEV_J))
    agree = 100.0 * total / anchor
    print(f"burn: {days} d to {TARGET_BURNUP} MWd/kg, fuel {volume_cm3:.3e} cm3")
    print(f"fissions from the cell : {total:.4e}")
    print(f"fissions from energy   : {anchor:.4e} ({agree:.2f}% of the cell count)")
    if not (98.0 <= agree <= 102.0):
        raise SystemExit(
            f"fission count disagrees with the energy anchor ({agree:.2f}%); "
            f"a unit conversion is wrong. Refusing to write an inventory.")
    for k, v in sorted(split.items(), key=lambda kv: -kv[1]):
        print(f"  {k}: {100 * v:.2f}% of fissions")

    T = days * 86400.0
    F = total / T

    with open(YIELDS) as fh:
        yields = json.load(fh)["yields"]

    # Independent yield of each nuclide, weighted by the fissile parents'
    # shares. Isomers are kept as their own nodes: they are 8.7% of all
    # fission products by yield (31% of chain A=133 alone), and dropping them
    # loses that mass from the chain. Each isomer decays into its own ground
    # state, which _edges routes by the IT branch.
    ind = {}
    for parent, frac in split.items():
        table = yields.get(parent)
        if not table:
            continue
        for key, rec in table["independent"].items():
            cur = ind.setdefault(key, {"z": rec["z"], "a": rec["a"], "yield": 0.0})
            cur["yield"] += frac * rec["cumulative"]

    print(f"\nnuclides with an independent yield: {len(ind)}")

    # The node set is the closure of the decay graph, not just the nuclides with
    # an independent yield. Cd-108 has no independent yield worth naming but it
    # is where 97% of Ag-108 goes, and leaving it out deletes those atoms: the
    # first attempt lost 1.3% of the inventory that way. Every reachable
    # daughter gets a node; the ones with no direct yield simply have a zero
    # source and are filled by their parents. Closure needs decay data, so it
    # is computed in passes until no new node appears.
    decay = load_decay(set(ind))
    while True:
        nodes = decay_closure(set(ind), decay)
        fresh = set(nodes) - set(decay)
        if not fresh:
            break
        decay = load_decay(set(decay) | set(nodes))
    decay = derive_gap_routes(decay, yields)
    print(f"nodes: {len(ind)} with an independent yield, {len(nodes)} after "
          f"following the decay graph")
    print(f"decay data resolved: "
          f"{sum(1 for v in decay.values() if v.get('halfLifeSeconds'))}/{len(nodes)}")
    src = {k: ind.get(k, {}).get("yield", 0.0) * F for k in nodes}
    atoms, residual = solve_all(nodes, src, decay, T)
    # Round-off alone puts the residual at ~0; a real loss (a daughter with no
    # independent yield, hence no node to land on) shows up as a positive
    # fraction. The gate is the physical bound, not an exact zero.
    if not (-1e-3 <= residual <= 0.05):
        raise SystemExit(
            f"mass balance residual {residual:.4f} outside [-1e-3, 0.05]; "
            f"the decay matrix is wrong.")
    # Atoms sitting in a node the evaluation could not route: a decay mode it
    # never lists ("gap") or a record whose fetch failed ("error"). They are
    # real atoms, just not routed onward. Naming them, separately, keeps a data
    # failure from looking like a physics result.
    def held(state):
        return {k: atoms[k] for k in nodes if atoms[k] > 0
                and _decay_state(k, decay.get(k) or {}) == state}

    unassigned = held("gap")
    errored = held("error")
    unevaluated = held("unevaluated")
    print(f"mass balance: {100 * residual:.3f}% of produced atoms left the "
          f"tabulated set")
    for label, group in (("have no listed decay mode", unassigned),
                         ("have no ENSDF evaluation at all", unevaluated),
                         ("failed to fetch (DATA ERROR)", errored)):
        if group:
            print(f"  {len(group)} nuclide(s) {label}, holding "
                  f"{sum(group.values()):.3e} atoms "
                  f"({100 * sum(group.values()) / max(sum(atoms.values()), 1):.2e}% "
                  f"of the inventory): " + ", ".join(sorted(group)))
    if errored:
        raise SystemExit(
            f"{len(errored)} nuclide(s) failed to fetch; that is a data error, "
            f"not a measurement gap. Delete those keys from decay-cache.json "
            f"and re-run.")
    # A nuclide nobody has evaluated is a limit of the source, not of the code.
    # It may only be held if it is a negligible share of the inventory; above
    # that it would distort the result and the build must stop.
    dead = sum(unassigned.values()) + sum(unevaluated.values())
    share = 100.0 * dead / max(sum(atoms.values()), 1.0)
    if share > 0.01:
        raise SystemExit(
            f"{share:.4f}% of the inventory is held in unrouted nuclides, over "
            f"the 0.01% limit; that is too much to leave out of the result.")

    out = {
        "_layer": "REKORD layer 2: core inventory at shutdown",
        "_note": ("Atoms, not becquerels. Activity = atoms x decay constant (layer 1). "
                  "Nothing here is a release."),
        "_method": ("Bateman chain solve, dn/dt = Y*F - lambda*n + sum(b*lambda*n), "
                    "constant source F over the irradiation, solved with a stiff "
                    "Radau integrator per connected decay component."),
        "_decayProvenance": {
            "counts": {p: sum(1 for v in decay.values() if v.get("provenance") == p)
                       for p in sorted({v.get("provenance") for v in decay.values()
                                        if v.get("provenance")})},
            "note": ("ensdf-levels = mode read from the ENSDF levels table; "
                     "ensdf-decay_rads-bm = levels table had a half-life but no "
                     "mode, so the mode was read from the mean-beta table; "
                     "yield-chain-derived = no mode anywhere in the evaluation, "
                     "so the route was derived from the evaluated cumulative "
                     "yields (flagged for review, not a tabulated value)."),
        },
        "_irradiationDays": days,
        "_fissionRatePerSecond": F,
        "_fissionCount": {
            "total": total,
            "energyAnchor": anchor,
            "byNuclide": split,
            "fuelMassTU": FUEL_MASS_T_U,
            "targetBurnupMWdPerKg": TARGET_BURNUP,
        },
        "_yieldSource": ("ENDF/B-VIII.0 MF=8 MT=454 independent yields, tabulated "
                         "by S. Okumura (tabfylibs)"),
        "_decaySource": "IAEA Nuclear Data Services (ENSDF), nds.iaea.org/relnsd/v1",
        "_notInThisFile": ("No activity in Bq (atoms x layer-1 lambda), no release "
                           "fraction (layer 3, contested), no dose."),
        "nuclides": {k: {"symbol": f"{symbol(_za(k)[0])}{_za(k)[1]}"
                                  f"{'m' if k.endswith('m') else ''}",
                         "z": _za(k)[0], "a": _za(k)[1], "atoms": atoms[k]}
                     for k in sorted(atoms)},
        "nuclideCount": len(atoms),
        "massBalanceResidual": round(residual, 8),
        "_unassigned": unassigned,
        "_unevaluated": unevaluated,
    }
    with open(OUT, "w") as fh:
        json.dump(out, fh, indent=1)

    print(f"\nwrote {OUT} ({len(atoms)} nuclides)")
    # Ground state only: an isomer key would otherwise overwrite its own ground
    # state and the printout would show the isomer's atoms under the plain name.
    by_name = {f"{symbol(_za(k)[0])}{_za(k)[1]}": k
               for k in atoms if not k.endswith("m")}
    for name in ("Cs137", "Sr90", "I131", "Xe135", "Ba140", "Ce144", "Ru106",
                 "Y90", "Zr95", "Nd147", "Te132", "Kr88"):
        k = by_name.get(name)
        if k:
            print(f"  {name:6s} {atoms[k]:.4e} atoms")

    return 0


if __name__ == "__main__":
    sys.exit(main())
