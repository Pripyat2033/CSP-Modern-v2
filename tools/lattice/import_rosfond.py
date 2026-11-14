#!/usr/bin/env python3
"""Unpack a ROSFOND-2010 distribution (the per-isotope ZIP form) for the lattice.

ROSFOND-2010 is distributed by IPPE as a set of archives, one per isotope, with
names like ``U235.endf`` / ``92235.endf`` inside. The lattice converters
(``fetch_rosfond.py`` for the pure-Python path, ``../openmc/rosfond_to_openmc.py``
for the OpenMC path) expect one ENDF-6 file per nuclide, named by the element
label the model uses (``U235``, ``Zr90``, ``C12``, ...). This script bridges the
two: it opens whatever the distribution ships, reads each file's ENDF head record
(MF=1, MT=451) to learn its ZA, and writes it out under the label the converters
look for.

Nothing is downloaded and nothing is invented: a file whose ZA is not in the
needed set is reported and skipped, and a needed nuclide with no file present is
reported as missing.

    python3 import_rosfond.py --from ~/Downloads/rosfond.zip --out rosfond_raw
    python3 import_rosfond.py --from ~/Downloads/rosfond --out rosfond_raw --list

``--from`` may be a single .zip, a directory of .zip files, or a directory of
loose ENDF-6 files (it is searched recursively). ``--out`` defaults to
``tools/lattice/rosfond_raw`` (what fetch_rosfond.py reads).
"""
from __future__ import annotations

import argparse
import glob
import io
import os
import shutil
import sys
import tempfile
import zipfile

HERE = os.path.dirname(os.path.abspath(__file__))

# label -> ZA (Z*1000 + A). Must match fetch_rosfond.py / rosfond_to_openmc.py.
# ZA = Z*1000 + A (ENDF convention): Zr90 = 40*1000+90 = 40090, not 40900.
NUCLIDES = {
    "U235": 92235, "U238": 92238, "U234": 92234, "U236": 92236,
    "Pu239": 94239, "Pu240": 94240, "Pu241": 94241,
    "O16": 8016, "H1": 1001, "C12": 6012, "C13": 6013, "N14": 7014,
    "Zr90": 40090, "Zr91": 40091, "Zr92": 40092, "Zr94": 40094, "Zr96": 40096,
    "Nb93": 41093, "B10": 5010, "B11": 5011, "Fe56": 26056, "Ni58": 28058,
    "Cr52": 24052, "Gd155": 64155, "Gd157": 64157, "Er166": 68166, "Er167": 68167,
    "He4": 2004,
}
ZA_TO_LABEL = {za: label for label, za in NUCLIDES.items()}


def _endf_float(field: str) -> float:
    """Parse an ENDF-6 float such as ' 2.906300+4' (implicit exponent, no 'e')."""
    s = field.strip()
    for i in range(len(s) - 1, 0, -1):
        if s[i] in "+-":
            s = s[:i] + "e" + s[i:]
            break
    return float(s)


def head_za(text: str) -> int | None:
    """ZA from the ENDF head record (MF=1, MT=451).

    In ENDF-6 the head record's *first* line carries ZA in columns 1-11 AND the
    '1 451' control in columns 70-75, so both come off the same line. Returns
    None if no head record is found. This is the only reliable identifier: file
    names vary between ROSFOND distributions.
    """
    for line in text.splitlines():
        if len(line) < 75:
            continue
        if line[70:72].strip() == "1" and line[72:75].strip() == "451":
            field = line[:11].strip()
            if not field:
                return None
            try:
                return int(round(_endf_float(field)))
            except ValueError:
                return None
    return None


def _iter_candidates(src: str):
    """Yield (name, text) for every ENDF file found under src (dir, zip or zips)."""
    if os.path.isfile(src) and src.lower().endswith(".zip"):
        with zipfile.ZipFile(src) as z:
            for info in z.infolist():
                if info.is_dir():
                    continue
                try:
                    text = z.read(info).decode("utf-8", errors="replace")
                except Exception:
                    continue
                yield info.filename, text
        return
    if os.path.isfile(src):
        with open(src, errors="replace") as f:
            yield os.path.basename(src), f.read()
        return
    # directory: any .zip inside, plus loose ENDF files
    for zp in sorted(glob.glob(os.path.join(src, "**", "*.zip"), recursive=True)):
        yield from _iter_candidates(zp)
    for path in sorted(glob.glob(os.path.join(src, "**", "*"), recursive=True)):
        if not os.path.isfile(path) or path.lower().endswith(".zip"):
            continue
        try:
            with open(path, errors="replace") as f:
                yield path, f.read()
        except Exception:
            continue


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--from", dest="src", required=True,
                    help="ROSFOND .zip, a directory of .zips, or a directory of ENDF-6 files")
    ap.add_argument("--out", dest="out", default=os.path.join(HERE, "rosfond_raw"),
                    help="output dir (default tools/lattice/rosfond_raw)")
    ap.add_argument("--list", action="store_true",
                    help="report what would be imported; write nothing")
    args = ap.parse_args()

    if not os.path.exists(args.src):
        sys.exit(f"not found: {args.src}")

    found: dict[str, str] = {}          # label -> source name
    for name, text in _iter_candidates(args.src):
        za = head_za(text)
        if za is None:
            continue
        label = ZA_TO_LABEL.get(za)
        if label is None:
            continue
        if label in found:
            continue
        found[label] = name
        if not args.list:
            os.makedirs(args.out, exist_ok=True)
            with open(os.path.join(args.out, label + ".endf"), "w") as f:
                f.write(text)

    missing = sorted(set(NUCLIDES) - set(found))
    print(f"ROSFOND import: source {args.src}")
    print(f"  out: {args.out}" + ("  (--list, nothing written)" if args.list else ""))
    print(f"  matched {len(found)} of {len(NUCLIDES)} needed nuclides")
    for label in sorted(found):
        print(f"    {label:6s} <- {found[label]}")
    if missing:
        print(f"  missing ({len(missing)}): {', '.join(missing)}")
        print("  These were NOT invented. Add their files and re-run if they matter.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
