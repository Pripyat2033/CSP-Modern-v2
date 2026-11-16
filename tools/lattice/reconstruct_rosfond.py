#!/usr/bin/env python3
"""Reconstruct ROSFOND resolved resonances (MF=2) into the pointwise cache.

WHY THIS EXISTS — the root cause of "ROSFOND won't work":

ROSFOND neutron files are evaluated in the *unresolved/resolved-resonance*
representation. Their MF=3 (pointwise) cross sections are **zero below the
resonance range** (for U-235, below ~2.25 keV): every thermal and epithermal
resonance lives in **MF=2** as resonance parameters. Reading MF=3 straight into
the lattice gives a moderator-only cell — k_inf collapses to ~0.03 and the
fission term is ~1e-4. That is not a code bug; it is what the file contains.

The reconstruction is the standard one: run NJOY (RECONR -> BROADR) to turn the
resonance parameters into a pointwise cross section across the full energy range,
then read the resulting PENDF back with the lattice's own ENDF-6 reader
(``endf6.py``). After reconstruction the evaluated thermal values come out right
(U-235 fission ~585 b and capture ~99 b at 0.0253 eV, the values the verification
suite checks).

WHY NJOY DIRECTLY, NOT OPENMC:

The first cut drove ``openmc.data.IncidentNeutron.from_njoy``. That works where
OpenMC is installed, but OpenMC is not on PyPI and cannot be added to the sandbox,
so the reconstruction could not actually be RUN here — only described. NJOY
itself *is* installable (``pip install njoy==2016.78`` ships the real binary), and
the lattice already owns a complete MF=3 reader, so this script drives NJOY with a
hand-written RECONR/BROADR deck and parses the PENDF output with ``endf6.py``. No
OpenMC, no extra data package — one less thing that can fail.

This writes the SAME per-nuclide, per-reaction JSON cache shape that
fetch_endf.py / fetch_rosfond.py produce, so the rest of the lattice pipeline
(rbmk_cell.py, rbmk_spectrum.py, emit_constants.py) runs unchanged.

Usage:
    python3 reconstruct_rosfond.py                 # all raw files, 4 workers
    python3 reconstruct_rosfond.py --only U235     # one nuclide
    python3 reconstruct_rosfond.py --jobs 4

Requires the ``njoy`` executable on PATH (``pip install njoy==2016.78``). The
script says so plainly and stops if it is missing; it never invents data.
"""
from __future__ import annotations

import argparse
import json
import os
import shutil
import subprocess
import sys
import tempfile
from concurrent.futures import ProcessPoolExecutor, as_completed

import endf6
from fetch_rosfond import NUCLIDES as _NUCLIDES

HERE = os.path.dirname(os.path.abspath(__file__))
RAW = os.path.join(HERE, "rosfond_raw")
OUT = os.path.join(HERE, "rosfond_cache")

# The reactions the lattice consumes, in ENDF MT numbers.
MTS = (1, 2, 4, 16, 18, 102)
TEMP = 294.0
ERR = 0.001


def _have_njoy() -> str | None:
    return shutil.which("njoy")


def _first_mat(text: str) -> int | None:
    """The MAT number of the first real ENDF-6 section, or None.

    A real record is 80 columns with MAT in 66-70, MF in 70-72, MT in 72-75.
    ROSFOND files open with a ``$Rev:: ... $Date::`` comment line that is shorter
    and would otherwise parse as a bogus MAT, so require the full control block
    with a sane MF/MT. A wrong MAT makes NJOY wait forever on stdin.
    """
    for line in text.splitlines():
        if len(line) < 75 or not line.strip():
            continue
        mat, mf, mt = line[66:70].strip(), line[70:72].strip(), line[72:75].strip()
        if mat.isdigit() and mf.isdigit() and mt.isdigit():
            if 1 <= int(mf) <= 99 and 1 <= int(mt) <= 999 and int(mat) >= 1:
                return int(mat)
    return None


def _deck(mat: int, temp: float) -> str:
    """The RECONR -> BROADR deck. Cards match what OpenMC's driver writes.

    Card 2 of RECONR is ``mat ncards ngrid``; ncards=2 is the two descriptive
    lines below. BROADR reads the PENDF on tape 21 and writes tape 22.
    """
    return (
        "reconr\n"
        "20 21\n"
        "'ROSFOND PENDF'/\n"
        f"{mat} 2/\n"
        f"{ERR}/ err\n"
        "'ROSFOND'/\n"
        "'Processed by NJOY'/\n"
        "0/\n"
        "broadr\n"
        "20 21 22\n"
        f"{mat} 1 0 0 0. /\n"
        f"{ERR}/ errthn\n"
        f"{temp}\n"
        "0/\n"
        "stop\n"
    )


def reconstruct_one(label: str, raw: str, out: str) -> tuple[str, list[int], str | None]:
    """Reconstruct one nuclide and write its cache files.

    Returns (label, MTs written, error). Runs in a worker process.
    """
    if _have_njoy() is None:
        return label, [], "njoy not on PATH (pip install njoy==2016.78)"

    path = os.path.join(raw, label + ".endf")
    if not os.path.exists(path):
        return label, [], "no raw file"

    with open(path, errors="replace") as f:
        text = f.read()
    mat = _first_mat(text)
    if mat is None:
        return label, [], "not an ENDF-6 file (no MAT field found)"

    workdir = tempfile.mkdtemp(prefix=f"njoy_{label}_", dir=out)
    try:
        # NJOY reads ENDF-6 on tape 20 and writes the broadened PENDF on tape 22.
        shutil.copyfile(path, os.path.join(workdir, "tape20"))
        open(os.path.join(workdir, "tape21"), "w").close()
        open(os.path.join(workdir, "tape22"), "w").close()
        deck = os.path.join(workdir, "input")
        with open(deck, "w") as f:
            f.write(_deck(mat, TEMP))
        proc = subprocess.run(["njoy", "input"], cwd=workdir, capture_output=True,
                              text=True, timeout=900)
        pendf = os.path.join(workdir, "tape22")
        if proc.returncode != 0 or not os.path.exists(pendf) or os.path.getsize(pendf) == 0:
            tail = (proc.stderr or proc.stdout or "").strip().splitlines()[-1:] or [""]
            return label, [], f"njoy failed: {tail[0][:160]}"
        with open(pendf, errors="replace") as f:
            broadened = f.read()
    finally:
        shutil.rmtree(workdir, ignore_errors=True)

    written: list[int] = []
    for mt in MTS:
        got = endf6.find_mf3(broadened, mt, mat=mat)
        if got is None:
            continue
        energy, xs = got
        doc = endf6.to_cache(energy, xs, library="ROSFOND-2010 (NJOY RECONR/BROADR)",
                             za=_za(label), mt=mt, temperature=str(int(TEMP)))
        with open(os.path.join(out, f"{label}_{mt}.json"), "w") as f:
            json.dump(doc, f)
        written.append(mt)
    if not written:
        return label, [], "reconstructed but no lattice reaction (MT 1/2/4/16/18/102) found"
    return label, written, None


# ZA = Z*1000 + A, used only for the cache metadata. The isotope set is the same
# one fetch_rosfond.py converts; C13 is added because ROSFOND ships it as its own
# file even though the lattice does not consume it directly.
_ZA = dict(_NUCLIDES, C13=6013)


def _za(label: str) -> int:
    """ZA for a label; fall back to 0 (unknown) rather than guess a nuclide."""
    return _ZA.get(label, 0)


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--raw", default=RAW)
    ap.add_argument("--out", default=OUT)
    ap.add_argument("--only", nargs="*", help="labels to reconstruct (default: all raw files)")
    ap.add_argument("--jobs", type=int, default=4)
    args = ap.parse_args()
    raw, out = args.raw, args.out
    os.makedirs(out, exist_ok=True)

    if _have_njoy() is None:
        sys.exit("njoy not found on PATH. Install it with:  pip install njoy==2016.78")

    if args.only:
        labels = args.only
    else:
        if not os.path.isdir(raw):
            sys.exit(f"raw dir {raw} does not exist")
        labels = sorted(f[:-5] for f in os.listdir(raw) if f.endswith(".endf"))
    if not labels:
        sys.exit(f"no .endf files in {raw}")

    print(f"reconstructing {len(labels)} nuclide(s) with NJOY, {args.jobs} at a time")
    done = failed = 0
    with ProcessPoolExecutor(max_workers=args.jobs) as pool:
        futs = {pool.submit(reconstruct_one, lab, raw, out): lab for lab in labels}
        for fut in as_completed(futs):
            label, mts, err = fut.result()
            if err:
                failed += 1
                print(f"  FAIL {label:6s} {err}")
            else:
                done += 1
                print(f"  ok   {label:6s} MT {', '.join(map(str, mts))}")
    print(f"\n{done} reconstructed, {failed} failed -> {out}")
    return 1 if failed else 0


if __name__ == "__main__":
    raise SystemExit(main())
