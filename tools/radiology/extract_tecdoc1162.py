#!/usr/bin/env python3
"""Extract the dose tables from IAEA-TECDOC-1162 into a checkable JSON file.

The point of this script is that no dose coefficient in the layer-4 data file is
typed by hand. Each one is read out of the source PDF, so a reader can take the
PDF, this script and the output and satisfy themselves the numbers are the
source's numbers.

The PDF is not committed (it is a 2.4 MB IAEA publication). Fetch it first:

    curl -sSLo /tmp/te1162.pdf \
      https://www-pub.iaea.org/MTCD/Publications/PDF/te_1162_prn.pdf
    python3 tools/radiology/extract_tecdoc1162.py /tmp/te1162.pdf

Output: tools/radiology/tecdoc1162-tables.json

WHY THE PARSER LOOKS LIKE THIS. The PDF's tables are read by pypdf as one text
line per row, but several rows carry a parent and its short-lived daughter on one
line ("Cs-137+Ba-137m", "Ru-106+Rh-106"), because the daughter is in equilibrium
and its dose cannot be separated. Dropping those rows silently loses Cs-137,
Ru-106, Mo-99 and Ce-144 -- four of the most important nuclides in the release.
So the row pattern accepts chained names and the key is kept exactly as printed.
Downstream code tries the chained key first and the bare symbol second.

A few cells read "NC" (not credited): the source's way of saying the pathway
gives no dose for that nuclide, e.g. a noble gas by ingestion. That is preserved
as the string "NC" rather than turned into 0.0, because "no dose credited" and
"zero dose" are different claims.
"""

from __future__ import annotations

import json
import os
import re
import sys

ROW = re.compile(
    r"^((?:[A-Z][a-z]?-\d{1,3}[a-z]?)(?:\+[A-Z][a-z]?-\d{1,3}[a-z]?)*)\s+"
    r"((?:[A-Z]{1,3}\b\s*)?)"                      # optional 'org.' / 'inorg.'
    r"([A-Z0-9][A-Z0-9.+-]*(?:\s+[A-Z0-9][A-Z0-9.+-]*)*)$"
)

# Page indices (0-based) in the TECDOC PDF. Printed page numbers in parentheses.
PAGES = {
    "E3": (range(105, 108), 4),    # Table E3, printed pp. 98-100
    "E6": (range(114, 116), 1),    # Table E6, printed pp. 107-108
    "E7": (range(116, 117), 2),    # Table E7, printed p. 109
    "E13": (range(123, 125), 1),   # Table E13, printed pp. 116-117
}


def values(text: str) -> list:
    out = []
    for token in text.split():
        if token.upper() == "NC":
            out.append("NC")
            continue
        try:
            out.append(float(token))
        except ValueError:
            pass
    return out


def parse(reader, pages, ncols: int) -> dict:
    out = {}
    for i in pages:
        for line in (reader.pages[i].extract_text() or "").split("\n"):
            s = line.strip()
            m = ROW.match(s)
            if not m:
                continue
            name, qual, rest = m.group(1), m.group(2).strip(), m.group(3)
            v = values(rest)
            if len(v) < ncols:
                continue
            key = name + ((" " + qual) if qual else "")
            out[key] = v[:ncols]
    return out


def main() -> int:
    if len(sys.argv) < 2:
        print(__doc__)
        return 2
    pdf = sys.argv[1]
    try:
        from pypdf import PdfReader
    except ImportError:
        print("pypdf is required: pip install pypdf", file=sys.stderr)
        return 1

    reader = PdfReader(pdf)
    data = {name: parse(reader, pages, ncols) for name, (pages, ncols) in PAGES.items()}

    # A table that came back nearly empty means the page numbers or the layout
    # changed. Fail loudly rather than emit a thin file.
    for name, rows in data.items():
        if len(rows) < 8:
            print(f"table {name} parsed only {len(rows)} rows; refusing to write",
                  file=sys.stderr)
            return 1

    out = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                       "tecdoc1162-tables.json")
    with open(out, "w") as f:
        json.dump(data, f, indent=1, sort_keys=True)
        f.write("\n")

    print(f"wrote {out}")
    for name, rows in data.items():
        print(f"  {name:4} {len(rows)} rows")
    return 0


if __name__ == "__main__":
    sys.exit(main())
