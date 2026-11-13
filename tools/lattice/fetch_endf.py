#!/usr/bin/env python3
"""Fetch real evaluated cross sections (ENDF/B-VIII.0) for the RBMK cell.

Downloads per-nuclide, per-reaction pointwise cross-section data from the
openmc-data-storage ENDF/B-VIII.0 JSON mirror (a faithful conversion of the NNDC
library) into a local cache, so the lattice calculation runs on real nuclear data
rather than hand-set numbers.

Reactions (ENDF MT numbers):
    1   total
    2   elastic scattering
    4   inelastic scattering
    16  (n,2n)
    18  fission
    102 radiative capture (n,gamma)

Usage:  python3 fetch_endf.py [--source endf|rosfond] [--force]
"""
from __future__ import annotations

import argparse
import json
import os
import sys
import urllib.request

import sources

BASE = ("https://raw.githubusercontent.com/openmc-data-storage/"
        "ENDF-B-VIII.0-NNDC-json/main/json_files/")
HERE = os.path.dirname(os.path.abspath(__file__))
CACHE = os.path.join(HERE, "endf_cache")

# (element, A) -> local label. Natural elements use the dominant isotope; the
# cell model weights them as natural where it matters.
NUCLIDES = {
    "U_235": "U235", "U_238": "U238", "U_234": "U234", "U_236": "U236",
    "Pu_239": "Pu239", "Pu_240": "Pu240", "Pu_241": "Pu241",
    "O_16": "O16", "H_1": "H1", "C_12": "C12", "C_13": "C13", "N_14": "N14",
    "Zr_90": "Zr90", "Zr_91": "Zr91", "Zr_92": "Zr92", "Zr_94": "Zr94",
    "Zr_96": "Zr96", "Nb_93": "Nb93",
    "B_10": "B10", "B_11": "B11", "Fe_56": "Fe56", "Ni_58": "Ni58",
    "Cr_52": "Cr52", "Gd_155": "Gd155", "Gd_157": "Gd157", "Er_166": "Er166",
    "Er_167": "Er167", "He_4": "He4",
}
MTS = [1, 2, 4, 16, 18, 102, 452]


def fetch(nuclide: str, mt: int, cache: str, force: bool = False) -> bool:
    out = os.path.join(cache, f"{NUCLIDES[nuclide]}_{mt}.json")
    if os.path.exists(out) and not force:
        return True
    url = f"{BASE}{nuclide}_ENDFB-8.0_n_{mt}_294K.json"
    try:
        req = urllib.request.Request(url, headers={"User-Agent": "csp-lattice/1.0"})
        with urllib.request.urlopen(req, timeout=90) as r:
            data = json.loads(r.read())
    except Exception as e:  # a reaction simply may not exist for a nuclide
        if mt in (4, 16, 18):          # inelastic/2n/fission absent is normal
            return False
        print(f"  !! {nuclide} MT={mt}: {type(e).__name__} {str(e)[:60]}")
        return False
    with open(out, "w") as f:
        json.dump(data, f)
    return True


def main() -> int:
    ap = argparse.ArgumentParser(description="Fetch/cache evaluated nuclear data for the lattice.")
    ap.add_argument("--source", choices=sorted(sources.SOURCES), default=None,
                    help="data library (default: %s; or LATTICE_SOURCE)" % sources.DEFAULT)
    ap.add_argument("--force", action="store_true", help="re-download even if cached")
    args = ap.parse_args()

    src = sources.get(args.source)
    if src.mode == "local":
        print(f"source '{src.name}' ({src.library}) is local: {src.note}")
        print("Provide ENDF-6 files and run  python3 fetch_rosfond.py  to build the cache.")
        return 0

    force = args.force
    os.makedirs(src.cache_dir, exist_ok=True)
    got = 0
    for nuc, label in NUCLIDES.items():
        line = []
        for mt in MTS:
            if fetch(nuc, mt, src.cache_dir, force):
                line.append(str(mt))
                got += 1
        print(f"  {label:6s}: MT {', '.join(line) if line else '(none)'}")
    print(f"\n{got} files cached in {src.cache_dir}  [{src.library}]")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
