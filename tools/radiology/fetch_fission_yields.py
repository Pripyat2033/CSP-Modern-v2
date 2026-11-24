#!/usr/bin/env python3
"""Fetch cumulative fission-product yields for REKORD layer 2.

REKORD layer 1 gave the decay data: what each nuclide emits and how fast it
decays. Layer 2 is the inventory: how many atoms of each nuclide a fissioning
core has made. The bridge between them is the fission yield -- the number of
atoms of a given mass chain produced per fission.

Nothing here is typed by hand. The yields are read from the evaluated
ENDF/B-VIII.0 fission-yield sublibraries (MF=8), tabulated by Shin Okumura
(JAEA) from the ENDF/B-VIII.0 files themselves:

    https://github.com/shinokumura/tabfylibs
    FY/n/<Nuclide>/endfb8.0/tables/FY/

Two files per nuclide are used, because they answer different questions:

  * MT=459 (cumulative chain yield, %/fission, one line per mass number). This
    is the mass distribution. It sums to 200% because fission makes two
    fragments -- that sum is the check that the file is complete and that the
    columns have been read in the right order.

  * MT=459 per-nuclide table (cumulative yield of each individual nuclide).
    This is what an inventory needs: Cs-137's cumulative yield is 6.19%, and it
    already includes the decay of the iodine and xenon precursors.

COLUMN ORDER TRAP. The per-nuclide file's header claims the columns are
"A Z ISO Yield dYield". They are not: the data is Z then A. Read as A-then-Z,
row 1 becomes A=23 Z=66, which is not a nuclide. Reading it the wrong way still
produces plausible-looking small numbers, so the sum-to-200% check on the mass
yield file is the guard.

WHAT IS DELIBERATELY NOT HERE

  * No inventory in becquerels. An inventory needs the number of fissions, which
    depends on burnup and on the reactor, not on nuclear data. That is the
    layer-2 calculation (Inventory.java), which consumes this file.
  * No release fraction. That is layer 3 and it is the contested part.

Usage:
    python3 tools/radiology/fetch_fission_yields.py
    python3 tools/radiology/fetch_fission_yields.py U235 Pu239
"""

from __future__ import annotations

import json
import os
import sys
import urllib.request

BASE = ("https://raw.githubusercontent.com/shinokumura/tabfylibs/master/"
        "FY/n/{nuclide}/endfb8.0/tables/FY/")
THERMAL = "E2.53E-08"

# The fissile nuclides that matter in an RBMK core at 10.3 MWd/kg: U-235, the
# plutonium the U-238 bred, and U-238 fast fission (a small but real term).
#
# U-238 has NO thermal yield file, and that is physics, not a gap in the data:
# U-238 fission has a ~1 MeV threshold, so ENDF tabulates its yields only at
# 0.5 and 14 MeV. The 0.5 MeV file is used, which is the lowest energy ENDF
# evaluates for it; an RBMK's U-238 fissions are driven by the fast part of the
# spectrum above that threshold. The substitution is recorded per nuclide in the
# output so the approximation travels with the number.
DEFAULT = ["U235", "Pu239", "Pu241", "U238", "Pu240"]
ENERGY = {"U238": ("E0000.500", 0.5)}

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "..", "src", "main", "resources", "data", "csp",
                   "radiology", "fission-yields.json")


def _get(url: str) -> str:
    with urllib.request.urlopen(url, timeout=60) as r:
        return r.read().decode("utf-8", "replace")


def _mass_yields(text: str) -> dict[int, float]:
    """MT=459 chain yields: one line per mass number."""
    out = {}
    for line in text.splitlines():
        if line.startswith("#") or not line.strip():
            continue
        p = line.split()
        if len(p) < 2:
            continue
        out[int(p[0])] = float(p[1])
    return out


def _nuclide_yields(text: str) -> dict[str, dict]:
    """Per-nuclide cumulative yields. Columns are Z, A, ISO, yield, dYield.

    Isomer rows (ISO=1) are recorded separately and never merged into the ground
    state. They are distinct nuclides with their own half-life: Ba-137m is not
    Ba-137. Merging them would also be numerically wrong, because the ground
    state's cumulative yield is the larger of the two (it includes the isomer's
    feeding), so an isomer row overwriting it silently lowers the answer -- for
    A=137 the ground state is 6.1885% and the isomer 5.8543%.
    """
    ground, isomers = {}, {}
    for line in text.splitlines():
        if line.startswith("#") or not line.strip():
            continue
        p = line.split()
        if len(p) < 5:
            continue
        z, a, iso = int(p[0]), int(p[1]), float(p[2])
        y, dy = float(p[3]), float(p[4])
        if y <= 0:
            continue
        rec = {"z": z, "a": a, "cumulative": y, "uncertainty": dy}
        if iso == 0.0:
            ground[f"{z}-{a}"] = rec
        else:
            isomers[f"{z}-{a}m"] = dict(rec, isomer=True)
    out = dict(ground)
    out.update(isomers)
    return out


def fetch(nuclide: str) -> dict:
    tag, energy = ENERGY.get(nuclide, (THERMAL, 0.0253))
    base = BASE.format(nuclide=nuclide)
    mass = _mass_yields(_get(base + f"n-{nuclide}-MT459-YA-{tag}.endfb8.0.txt"))
    independent = _nuclide_yields(_get(base + f"n-{nuclide}-MT454-{tag}.endfb8.0.txt"))
    cumulative = _nuclide_yields(_get(base + f"n-{nuclide}-MT459-{tag}.endfb8.0.txt"))

    total = sum(mass.values())
    if not (1.99 < total < 2.01):
        raise SystemExit(
            f"{nuclide}: chain yields sum to {total:.4f}%, not ~200%. "
            f"The file is incomplete or the columns were read in the wrong order.")

    # Cumulative yield must be >= independent yield for every nuclide: the
    # cumulative figure is the independent one plus everything that feeds it.
    # This catches a mis-ordered column or a wrong file being read.
    for key, rec in independent.items():
        cum = cumulative.get(key)
        if cum is not None and rec["cumulative"] > cum["cumulative"] * 1.001 + 1e-9:
            raise SystemExit(
                f"{nuclide} {key}: independent yield {rec['cumulative']} exceeds "
                f"cumulative {cum['cumulative']}; the files are mismatched.")

    out = {
        "massYields": {str(a): y for a, y in sorted(mass.items())},
        "independent": independent,
        "cumulative": cumulative,
        "massYieldSum": round(total, 6),
        "incidentEnergyMeV": energy,
    }
    if energy != 0.0253:
        out["energyNote"] = (
            f"{nuclide} fission has a threshold; ENDF/B-VIII.0 tabulates its yields "
            f"only at 0.5 and 14 MeV. These are the 0.5 MeV yields.")
    return out


def main(argv: list[str]) -> int:
    nuclides = argv[1:] or DEFAULT
    data = {
        "_layer": "REKORD layer 2: fission-product inventory (yields)",
        "_source": ("ENDF/B-VIII.0 fission-yield sublibraries (MF=8), "
                    "tabulated by S. Okumura, https://github.com/shinokumura/tabfylibs"),
        "_reaction": "neutron-induced fission at 0.0253 eV (thermal)",
        "_note": ("Two yield sets. 'independent' is the direct production rate: atoms "
                  "made at that mass number by fission itself. 'cumulative' adds "
                  "everything that feeds it by decay and is only valid once the chain "
                  "has reached equilibrium -- using it as a production rate overstates "
                  "any nuclide with a long-lived parent (Y-90 is fed by 28.8-y Sr-90, "
                  "so its cumulative yield is ~4000x its independent yield). The "
                  "inventory uses 'independent' plus an explicit chain solve."),
        "_columns": ("The per-nuclide file's header says 'A Z'; the data is Z then A. "
                     "This file is keyed by 'Z-A' after reading in that order. Isomer "
                     "rows are keyed 'Z-Am' and are never merged into the ground state."),
        "_notInThisFile": ("No activity in Bq and no release fraction. Activity needs "
                           "the number of fissions (layer 2 calculation); release is "
                           "layer 3 and is contested."),
        "incidentEnergyMeV": 0.0253,
        "yields": {},
    }
    for n in nuclides:
        print(f"fetching {n} ...", flush=True)
        data["yields"][n] = fetch(n)
        y = data["yields"][n]
        ind = y["independent"].get("55-137", {}).get("cumulative")
        cum = y["cumulative"].get("55-137", {}).get("cumulative")
        print(f"  {len(y['independent'])} nuclides, chain sum {y['massYieldSum']:.3f}%, "
              f"Cs-137 ind {ind:.4f}% cum {cum:.4f}%")

    with open(OUT, "w") as fh:
        json.dump(data, fh, indent=1)
    print(f"wrote {OUT}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
