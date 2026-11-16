#!/usr/bin/env python3
"""Self-test for the ROSFOND resonance-representation handling.

Proves the two facts the "ROSFOND won't work" investigation established:

  1. The MAT-number reader skips the ``$Rev:: ... $Date::`` comment line ROSFOND
     files open with. A wrong MAT makes NJOY wait forever, so this matters.
  2. The resonance-trap guard (fetch_rosfond.resonance_trap) flags a fission or
     capture section that is ZERO at thermal energy — the signature of a file in
     the MF=2 resonance representation — and lets a real (nonzero) section pass.

No ROSFOND library and no NJOY are needed: the sections are synthesised. Run:
    python3 test_reconstruct_rosfond.py
"""
from __future__ import annotations

import fetch_rosfond as f
import reconstruct_rosfond as r

PASS, FAIL = "PASS", "FAIL"
results = []


def check(name, cond, detail=""):
    results.append(cond)
    print(f"  [{PASS if cond else FAIL}] {name} {detail}")


def endf_line(fields: str, mat: int, mf: int, mt: int) -> str:
    """One ENDF-6 record: data in cols 1-66, then MAT/MF/MT in 67-75.

    MAT is 4 wide (66-70), MF 2 wide (70-72), MT 3 wide (72-75).
    """
    return fields.ljust(66)[:66] + f"{mat:>4d}{mf:>2d}{mt:>3d}"


def main() -> int:
    print("reconstruct_rosfond self-test")

    # --- MAT reader skips the $Rev header ---------------------------------
    rev = "$Rev:: 1451     $  $Date:: 2018-01-17#$".ljust(80)
    real = endf_line("  9.424100+4 2.389780+2", 9443, 1, 451)
    text = rev + "\n" + real + "\n"
    check("MAT read from the real record, not the $Rev header",
          r._first_mat(text) == 9443, f"(got {r._first_mat(text)})")

    # a file with only the header has no MAT
    check("header-only file yields no MAT", r._first_mat(rev + "\n") is None)

    # --- the resonance trap ------------------------------------------------
    # An un-reconstructed file: section spans thermal, but is zero there.
    zero_e = [1e-5, 1.0, 1e3, 1e4]
    zero_xs = [0.0, 0.0, 0.0, 0.0]
    check("zero-at-thermal fission is flagged as the resonance trap",
          f.resonance_trap(zero_e, zero_xs) is True)

    # A reconstructed file: real thermal values.
    real_xs = [585.0, 500.0, 20.0, 5.0]
    check("nonzero-at-thermal fission passes the guard",
          f.resonance_trap(zero_e, real_xs) is False)

    # A section that does not reach thermal at all is not a trap (nothing to be
    # zero *at*): e.g. an inelastic threshold reaction starting at 1 keV.
    fast_e = [1e3, 1e4, 1e5]
    check("a section that starts above thermal is not flagged",
          f.resonance_trap(fast_e, [0.0, 1.0, 2.0]) is False)

    # --- end-to-end through convert(): the guard must actually skip ---------
    import os
    import tempfile
    with tempfile.TemporaryDirectory() as raw, tempfile.TemporaryDirectory() as out:
        # Build a minimal ROSFOND-style file: MT=1 (total, nonzero), MT=18
        # (fission, zero at thermal -> must be skipped), MT=102 (capture, zero
        # at thermal -> must be skipped).
        def tab1(mt, values):
            def f11(v):
                # 11-char ENDF float field, implicit exponent (E+ -> +).
                return f"{v:>11.6E}".replace("E+", "+").replace("E-", "-")
            def rec(fields):
                return endf_line("".join(f11(v) for v in fields), 9443, 3, mt)
            lines = [
                rec([94241.0, 238.978, 0, 0, 0, 0]),          # HEAD
                rec([0, 0, 0, 0, 1, len(values)]),            # QM,QI,0,LR,NR,NP
                rec([len(values), 2]),                        # NBT, INT
            ]
            pts = []
            for e, x in values:
                pts += [e, x]
            for i in range(0, len(pts), 6):
                lines.append(rec(pts[i:i + 6]))
            return "\n".join(lines)

        content = "\n".join([
            tab1(1, [(1e-5, 10.0), (1.0, 10.0), (1e4, 10.0)]),   # total: nonzero
            tab1(18, [(1e-5, 0.0), (1.0, 0.0), (1e4, 0.0)]),     # fission: ZERO
            tab1(102, [(1e-5, 0.0), (1.0, 0.0), (1e4, 0.0)]),    # capture: ZERO
        ]) + "\n"
        with open(os.path.join(raw, "Pu241.endf"), "w") as fh:
            fh.write(content)
        written = f.convert("Pu241", 94241, out, raw_dir=raw)
        check("convert() writes total but skips zero fission/capture",
              written == [1], f"(wrote {written})")
        check("no fission file was written for the trapped section",
              not os.path.exists(os.path.join(out, "Pu241_18.json")))

    print("-" * 52)
    n = len(results)
    ok = sum(results)
    print(f"{ok}/{n} checks passed")
    return 0 if ok == n else 1


if __name__ == "__main__":
    raise SystemExit(main())
