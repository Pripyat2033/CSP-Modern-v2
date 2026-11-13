#!/usr/bin/env python3
"""Self-test for the ROSFOND -> OpenMC converter (the ENDF head parsing).

The expensive bug this guards against: the ENDF head record's ZA lives in the
FIRST field (Z*1000 + A), while columns 67-70 hold the MAT number. Reading the
MAT as the ZA silently mislabels every nuclide (Cu-63 -> "Cu-25"). These checks
pin the parse without needing OpenMC or NJOY installed, so they run anywhere.

    python3 test_rosfond_to_openmc.py
"""
from __future__ import annotations

import os
import sys
import tempfile

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import rosfond_to_openmc as R  # noqa: E402

PASS = 0
FAIL = 0


def check(name: str, cond: bool) -> None:
    global PASS, FAIL
    if cond:
        PASS += 1
        print(f"  [PASS] {name}")
    else:
        FAIL += 1
        print(f"  [FAIL] {name}")


def _endf_field(x: float) -> str:
    """Format a number as an 11-character ENDF float, e.g. ' 2.906300+4'."""
    mant, exp = f"{x:11.6E}".split("E")
    return f"{mant}{int(exp):+d}".rjust(11)


def _head(za: int, mat: int = 2925) -> str:
    """Build a minimal ENDF MF=1 MT=451 head line (exactly 80 columns).

    Columns: ZA(1-11) AWR(12-22) three blank fields(23-55) ND(56-66)
             MAT(67-70) MF(71-72) MT(73-75) line(76-80).
    """
    line = (
        _endf_field(za) + _endf_field(6.2389)
        + " " * 44            # cols 23-66 except ND; ND left blank is valid
        + f"{mat:>4}"         # MAT, cols 67-70
        + " 1"                # MF, cols 71-72
        + "451"               # MT, cols 73-75
        + "    1"             # line number, cols 76-80
    )
    assert len(line) == 80, f"head line is {len(line)} cols, not 80"
    return line + "\n"


def test_float():
    print("1. ENDF float parsing (no exponent 'e')")
    check("2.906300+4 -> 29063", R._endf_float(" 2.906300+4") == 29063.0)
    check("1.000000-5 -> 1e-5", abs(R._endf_float("1.000000-5") - 1e-5) < 1e-12)
    check("-3.0+0 -> -3", R._endf_float("-3.0+0") == -3.0)


def test_meta_za():
    print("2. the head record yields the ZA, not the MAT")
    with tempfile.TemporaryDirectory() as d:
        p = os.path.join(d, "Cu63.endf")
        with open(p, "w") as fh:
            fh.write(_head(29063, mat=2925))
        meta = R._meta(p)
        check("meta parsed", meta is not None)
        z, a, mat = meta
        check("Z = 29 (not 25 from the MAT)", z == 29)
        check("A = 63", a == 63)
        check("MAT read separately = 2925", mat == "2925")


def test_scan():
    print("3. scan() maps files to elements by content, not filename")
    with tempfile.TemporaryDirectory() as d:
        # deliberately misnamed file: content says Fe-58
        with open(os.path.join(d, "mystery_file.dat"), "w") as fh:
            fh.write(_head(26058, mat=2637))
        found = R.scan(d)
        check("Fe found from content despite filename", "Fe" in found)
        check("Fe ZA is 26058", found.get("Fe", [(None, None)])[0][0] == 26058)


def test_needed_covers_cell():
    print("4. the needed set covers what rbmk_cell.py consumes")
    for el in ("U", "O", "H", "C", "Zr", "Nb"):
        check(f"{el} is in NEEDED", el in R.NEEDED)


def main():
    test_float()
    test_meta_za()
    test_scan()
    test_needed_covers_cell()
    print()
    print("-" * 52)
    print(f"{PASS}/{PASS + FAIL} checks passed")
    sys.exit(0 if FAIL == 0 else 1)


if __name__ == "__main__":
    main()
