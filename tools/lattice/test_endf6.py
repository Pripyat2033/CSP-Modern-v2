#!/usr/bin/env python3
"""Self-test for the ENDF-6 MF=3 reader (endf6.py).

Builds a synthetic ENDF-6 record in the fixed-width format — including the
implicit-exponent number form and the 80-column control block — and checks the
reader recovers the exact (energy, cross section) grid, selects the right
reaction, and wraps it in the JSON cache shape. Run: python3 test_endf6.py

This is the proof that the ROSFOND (ENDF-6) path parses real-format data, without
needing the library itself to be present.
"""
from __future__ import annotations



import endf6

PASS, FAIL = "PASS", "FAIL"
results = []


def check(name, cond, detail=""):
    results.append(cond)
    print(f"  [{PASS if cond else FAIL}] {name} {detail}")


def endf_num(v: float) -> str:
    """One ENDF-6 field: exactly 11 columns, implicit exponent (1.234567+5)."""
    if v == 0.0:
        return " 0.000000+0"
    mant, exp = f"{v:.6E}".split("E")
    exp = exp[0] + str(int(exp[1:]))   # "+00" -> "+0"
    return (mant + exp).rjust(11)


def endf_line(vals, mat=9228, mf=3, mt=18, line=1):
    body = "".join(endf_num(v) for v in (list(vals) + [0.0] * 6)[:6])
    ctrl = f"{mat:4d}{mf:2d}{mt:3d}{line:5d}"
    return body + ctrl


def build_section(energy, xs, mat=9228, mf=3, mt=18):
    za = 92235.0
    awr = 233.0
    lines = []
    lines.append(endf_line([za, awr, 0, 0, 0, 0], mat, mf, mt, 1))
    # QM, QI, 0, LR, NR, NP   with NR=1, LR=0
    lines.append(endf_line([0, 0, 0, 0, 1, len(energy)], mat, mf, mt, 2))
    # one (NBT, INT) interpolation pair: NBT=len, INT=2 (lin-lin)
    lines.append(endf_line([len(energy), 2], mat, mf, mt, 3))
    flat = []
    for e, x in zip(energy, xs):
        flat += [e, x]
    for i in range(0, len(flat), 6):
        lines.append(endf_line(flat[i:i + 6], mat, mf, mt, 4 + i // 6))
    return "\n".join(lines) + "\n"


def main():
    print("ENDF-6 MF=3 reader self-test\n")

    E = [1.0e-5, 1.0, 1.0e3, 1.0e6, 2.0e7]
    X = [10.0, 20.0, 5.0, 1.0, 0.5]
    text = build_section(E, X, mt=18)
    # a second reaction in the same file, to prove selection
    text += build_section([1.0, 1.0e6], [99.0, 3.0], mt=102)

    print("1. number format")
    check("implicit exponent 1.234567+5 -> 123456.7",
          abs(endf6._num(" 1.234567+5") - 123456.7) < 1e-3)
    check("plain 0.0 -> 0", endf6._num(" 0.000000+0") == 0.0)

    print("\n2. section selection")
    got = endf6.find_mf3(text, 18)
    check("MF=3/MT=18 is found", got is not None)
    e, x = got
    check("energy grid round-trips exactly", list(e) == E, f"({e})")
    check("cross section round-trips exactly", list(x) == X, f"({x})")
    check("a missing reaction returns None", endf6.find_mf3(text, 4) is None)

    got2 = endf6.find_mf3(text, 102)
    check("MF=3/MT=102 is found separately", got2 is not None and list(got2[1]) == [99.0, 3.0])

    print("\n3. cache shape")
    doc = endf6.to_cache(e, x, library="ROSFOND-2010", za=92235, mt=18)
    check("library is stamped", doc["Library"] == "ROSFOND-2010")
    check("Z/A recovered from ZA", doc["Proton number"] == 92 and doc["Mass number"] == 235)
    check("energy + xs present",
          len(doc["energy"]) == 5 and len(doc["cross section"]) == 5)

    print("\n4. out-of-scope inputs are refused, not silently wrong")
    bad = endf_line([92235.0, 233.0, 0, 0, 0, 0]) + "\n" + \
        endf_line([0, 0, 0, 1, 1, 5]) + "\n"   # LR=1 (resonance reconstruction)
    try:
        endf6.parse_tab1(bad.splitlines())
        check("LR!=0 raises", False)
    except ValueError:
        check("LR!=0 raises (resonance reconstruction out of scope)", True)

    n_pass = sum(1 for ok in results if ok)
    print(f"\n{'-'*52}\n{n_pass}/{len(results)} checks passed")
    return 0 if n_pass == len(results) else 1


if __name__ == "__main__":
    raise SystemExit(main())
