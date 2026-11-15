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
resonance parameters into a pointwise cross section across the full energy range.
OpenMC ships the driver (``openmc.data.IncidentNeutron.from_njoy``), and NJOY
2016.78 is present in the sandbox. After reconstruction the evaluated thermal
values come out right (U-235 fission 584.9 b and capture 98.7 b at 0.0253 eV,
against the ENDF/B-VIII.0 values 585 and 99 the verification suite checks).

This writes the SAME per-nuclide, per-reaction JSON cache shape that
fetch_endf.py / fetch_rosfond.py produce, so the rest of the lattice pipeline
(rbmk_cell.py, rbmk_spectrum.py, emit_constants.py) runs unchanged.

Usage:
    python3 reconstruct_rosfond.py                 # all 28, 4 workers
    python3 reconstruct_rosfond.py --only U235     # one nuclide
    python3 reconstruct_rosfond.py --jobs 4
"""
from __future__ import annotations

import argparse
import json
import os
import shutil
import sys
from concurrent.futures import ProcessPoolExecutor, as_completed

HERE = os.path.dirname(os.path.abspath(__file__))
RAW = os.path.join(HERE, "rosfond_raw")
OUT = os.path.join(HERE, "rosfond_cache")

# The reactions the lattice consumes, in ENDF MT numbers.
MTS = (1, 2, 4, 16, 18, 102)
TEMP = 294.0


def _need_njoy_on_path() -> None:
    """from_njoy shells out to the ``njoy`` executable."""
    omc_bin = "/workspace/tools/mamba/envs/omc/bin"
    if os.path.isdir(omc_bin) and omc_bin not in os.environ.get("PATH", "").split(os.pathsep):
        os.environ["PATH"] = omc_bin + os.pathsep + os.environ.get("PATH", "")


def _unshadow_endf() -> None:
    """Stop this directory's endf.py from shadowing the ``endf`` package.

    This directory holds a module named ``endf.py`` (the lattice's own pointwise
    reader). OpenMC depends on a third-party *package* also named ``endf``
    (``from endf.data import ...``), and while this directory is on sys.path the
    module wins and OpenMC fails to import. Drop the directory for this process
    so OpenMC sees the real package.
    """
    here = os.path.dirname(os.path.abspath(__file__))
    sys.path[:] = [p for p in sys.path if os.path.abspath(p or os.getcwd()) != here]


def reconstruct_one(label: str, raw: str, out: str) -> tuple[str, list[int], str | None]:
    """Reconstruct one nuclide and write its cache files.

    Returns (label, MTs written, error). Runs in a worker process.
    """
    _need_njoy_on_path()
    _unshadow_endf()
    import openmc.data as data  # imported here so the pool starts clean

    path = os.path.join(raw, label + ".endf")
    if not os.path.exists(path):
        return label, [], "no raw file"

    workdir = os.path.join(out, "_njoy", label)
    os.makedirs(workdir, exist_ok=True)
    cwd = os.getcwd()
    try:
        os.chdir(workdir)
        nuc = data.IncidentNeutron.from_njoy(path, temperatures=[TEMP])
    except Exception as exc:  # noqa: BLE001 - report, do not invent
        return label, [], f"{type(exc).__name__}: {str(exc)[:200]}"
    finally:
        os.chdir(cwd)

    key = f"{TEMP:.0f}K"
    written: list[int] = []
    for mt in MTS:
        if mt not in nuc.reactions:
            continue
        try:
            xs = nuc[mt].xs[key]
        except (KeyError, AttributeError):
            continue
        doc = {
            "Proton number": nuc.atomic_number,
            "Mass number": nuc.mass_number,
            "Neutron number": nuc.mass_number - nuc.atomic_number,
            "Temperature(K)": str(int(TEMP)),
            "Incident particle": "neutron",
            "MT reaction number": mt,
            "Library": "ROSFOND-2010 (NJOY RECONR/BROADR)",
            "energy": [float(v) for v in xs.x],
            "cross section": [float(v) for v in xs.y],
        }
        with open(os.path.join(out, f"{label}_{mt}.json"), "w") as f:
            json.dump(doc, f)
        written.append(mt)
    shutil.rmtree(workdir, ignore_errors=True)
    return label, written, None


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

    if args.only:
        labels = args.only
    else:
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
