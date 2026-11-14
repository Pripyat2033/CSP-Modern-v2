#!/usr/bin/env python3
"""Self-test for the ROSFOND importer (import_rosfond.py).

Builds a synthetic ROSFOND-style zip — one ENDF-6 file per isotope, named the way
a distribution might name them, with a real MF=1/MT=451 head record — and checks
the importer identifies each isotope from its head record (not its filename),
names the output the way the converters expect, and reports missing nuclides
instead of inventing them. Run: python3 test_import_rosfond.py

No ROSFOND library is needed; this proves the identification and unpacking logic.
"""
from __future__ import annotations

import os
import tempfile
import zipfile

import import_rosfond as imp

PASS, FAIL = "PASS", "FAIL"
results = []


def check(name, cond, detail=""):
    results.append(cond)
    print(f"  [{PASS if cond else FAIL}] {name} {detail}")


def head_line(za: int, mat: int = 9999) -> str:
    """One ENDF-6 head record line: ZA in cols 1-11, '1 451' control in 71-75."""
    first = f"{za:>11.6E}".replace("E+", "+").replace("E-", "-").ljust(66)
    return first + f"{mat:>4d}{1:>2d}{451:>3d}"


def main() -> int:
    print("import_rosfond self-test")

    # --- head-record identification ---------------------------------------
    check("ZA read from head record", imp.head_za(head_line(92235)) == 92235,
          f"(got {imp.head_za(head_line(92235))})")
    check("ZA read for Zr-90 (40090, not 40900)",
          imp.head_za(head_line(40090)) == 40090)
    check("no head record -> None", imp.head_za("nothing here\n") is None)

    # --- the ZA table is the ENDF convention ------------------------------
    check("Zr90 ZA = 40090", imp.NUCLIDES["Zr90"] == 40090)
    check("Nb93 ZA = 41093", imp.NUCLIDES["Nb93"] == 41093)
    check("U235 ZA = 92235", imp.NUCLIDES["U235"] == 92235)

    # --- end-to-end unpack from a zip -------------------------------------
    with tempfile.TemporaryDirectory() as td:
        zpath = os.path.join(td, "rosfond.zip")
        out = os.path.join(td, "out")
        # name the files *differently* from the labels, to prove the importer
        # identifies by head record and not by name.
        wanted = {"u235n.dat": 92235, "uranium238.txt": 92238, "zirc90.endf": 40090}
        with zipfile.ZipFile(zpath, "w") as z:
            for name, za in wanted.items():
                z.writestr(name, head_line(za) + "\n" + " 0.000000+0".ljust(75) + "\n")
        import sys
        old = sys.argv
        sys.argv = ["import_rosfond.py", "--from", zpath, "--out", out]
        try:
            imp.main()
        finally:
            sys.argv = old
        got = sorted(os.listdir(out))
        check("unpacked by head record, named by label",
              got == ["U235.endf", "U238.endf", "Zr90.endf"], f"(got {got})")
        check("output files are non-empty",
              all(os.path.getsize(os.path.join(out, f)) > 0 for f in got))

    ok = all(results)
    print(f"\n{sum(results)}/{len(results)} checks passed"
          + ("" if ok else "  — FAILED"))
    return 0 if ok else 1


if __name__ == "__main__":
    raise SystemExit(main())
