#!/usr/bin/env python3
"""Build the lattice data cache from local ROSFOND (ENDF-6) files.

The IPPE / OECD-NEA / IAEA portals are not reachable from the sandbox, so ROSFOND
cannot be downloaded here. This script is the other half of the --source rosfond
path: it takes ENDF-6 files you place on disk and converts the reactions the
lattice needs into the same per-nuclide, per-reaction JSON cache that
fetch_endf.py produces from the ENDF/B mirror. After this runs, everything
downstream (emit_constants.py, the Java core) is source-agnostic.

Where to put the files: tools/lattice/rosfond_raw/<name>.endf, one file per
nuclide (the ROSFOND distribution ships one file per isotope). Names are matched
by the label table below; a missing file is reported, not faked.

    python3 fetch_rosfond.py                 # convert everything present
    python3 fetch_rosfond.py --list          # show what it wants and what it has

IMPORTANT — reconstruct first. ROSFOND neutron files are in the resonance
representation (MF=2), and their MF=3 pointwise cross section is ZERO below the
resonance range. Converting them straight gives a moderator-only cell
(k_inf ~0.03). For those files run the NJOY reconstruction instead, which writes
the same cache in one step:

    python3 reconstruct_rosfond.py           # MF=2 -> pointwise, needs njoy

Use THIS script only for files that are already pointwise (MF=3). As a guard, it
refuses to write a fission/capture section that is exactly zero at thermal energy
(the tell of an un-reconstructed file) and says so instead of caching a wrong
number.

This is deliberately NOT a downloader: it does not pretend to reach a portal it
cannot reach. It converts, or it tells you what is missing.
"""
from __future__ import annotations

import argparse
import json
import os

import endf6
import sources

HERE = os.path.dirname(os.path.abspath(__file__))
RAW = os.path.join(HERE, "rosfond_raw")

# The same set the ENDF path caches. ROSFOND ships one file per isotope.
# (label, ZA) — ZA = Z*1000 + A, used for the cache metadata.
NUCLIDES = {
    "U235": 92235, "U238": 92238, "U234": 92234, "U236": 92236,
    "Pu239": 94239, "Pu240": 94240, "Pu241": 94241,
    "O16": 8016, "H1": 1001, "Cnat": 6000, "N14": 7014,
    "Zr90": 40090, "Zr91": 40091, "Zr92": 40092, "Zr94": 40094, "Zr96": 40096,
    "Nb93": 41093, "B10": 5010, "B11": 5011, "Fe56": 26056, "Ni58": 28058,
    "Cr52": 24052, "Gd155": 64155, "Gd157": 64157, "Er166": 68166, "Er167": 68167,
    "He4": 2004,
}
MTS = [1, 2, 4, 16, 18, 102, 452]


def raw_path(label: str, raw_dir: str = RAW) -> str | None:
    """Find a raw ENDF-6 file for a label (.endf / .txt / no extension)."""
    for ext in (".endf", ".txt", ""):
        p = os.path.join(raw_dir, label + ext)
        if os.path.exists(p):
            return p
    return None


def _thermal_xs(energy, xs, ev: float = 0.0253) -> float:
    """The cross section nearest a thermal energy, for the resonance-trap guard."""
    if not energy:
        return 0.0
    i = min(range(len(energy)), key=lambda k: abs(energy[k] - ev))
    return xs[i]


def resonance_trap(energy, xs) -> bool:
    """True if this looks like an un-reconstructed resonance-representation file.

    ROSFOND neutron files carry their thermal/epithermal resonances in MF=2, and
    their MF=3 pointwise cross section is ZERO below the resonance range. A file
    read straight in therefore gives a moderator-only cell. The tell is a fission
    (or capture) section whose value at thermal energy is exactly zero while the
    section still spans the thermal range — the signature of unresolved MF=2, not
    of a genuinely tiny cross section. Such a file must go through NJOY first
    (see reconstruct_rosfond.py).
    """
    if not energy or energy[0] > 1.0:
        return False  # does not reach thermal: nothing to be zero *at*
    return _thermal_xs(energy, xs) == 0.0


def convert(label: str, za: int, out_dir: str, raw_dir: str = RAW) -> list[int]:
    """Convert every needed reaction present in a label's ENDF-6 file."""
    path = raw_path(label, raw_dir)
    if path is None:
        return []
    with open(path, errors="replace") as f:
        text = f.read()
    written = []
    for mt in MTS:
        got = endf6.find_mf3(text, mt)
        if got is None:
            continue
        energy, xs = got
        if mt in (18, 102) and resonance_trap(energy, xs):
            print(f"  !! {label} MT={mt}: zero at thermal energy — this file is in "
                  f"the resonance representation (MF=2); reconstruct it with "
                  f"reconstruct_rosfond.py before converting. Skipping, not faking.")
            continue
        doc = endf6.to_cache(energy, xs, library=sources.get("rosfond").library,
                             za=za, mt=mt)
        with open(os.path.join(out_dir, f"{label}_{mt}.json"), "w") as f:
            json.dump(doc, f)
        written.append(mt)
    return written


def main() -> int:
    ap = argparse.ArgumentParser(description="Convert local ROSFOND ENDF-6 files to the cache.")
    ap.add_argument("--list", action="store_true", help="report present/missing, convert nothing")
    args = ap.parse_args()

    src = sources.get("rosfond")
    have = [l for l in NUCLIDES if raw_path(l) is not None]
    missing = [l for l in NUCLIDES if raw_path(l) is None]

    print(f"ROSFOND source: {src.library}")
    print(f"raw dir: {RAW}")
    print(f"present: {len(have)}  missing: {len(missing)}")
    if args.list:
        for l in missing:
            print(f"  missing: {l}")
        return 0

    if not have:
        print("\nNo raw ENDF-6 files found. Place ROSFOND files in rosfond_raw/ "
              "(one per nuclide) and re-run. Nothing was faked.")
        return 1

    os.makedirs(src.cache_dir, exist_ok=True)
    total = 0
    for label in have:
        written = convert(label, NUCLIDES[label], src.cache_dir)
        total += len(written)
        print(f"  {label:6s}: MT {', '.join(map(str, written)) if written else '(none)'}")
    print(f"\n{total} files cached in {src.cache_dir}  [{src.library}]")
    if missing:
        print(f"NOTE: {len(missing)} nuclides not converted (files absent): "
              + ", ".join(missing))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
