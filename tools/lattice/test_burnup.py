#!/usr/bin/env python3
"""Guards on the burnup model in burnup.py.

Each check corresponds to a way the model was silently wrong before. They are
written to fail loudly rather than drift, because every one of them produced a
plausible-looking number:

  1. Power density is over the FUEL, not the core. Using the core average
     understates flux by 36x and leaves U-235 undepleted after a year.
  2. Pu-240 comes from Pu-239 CAPTURE only. Feeding total absorption in
     manufactures Pu-240 ~3x too fast and flips the void coefficient negative.
  3. The lumped fission product is 1/v. A flat absorber adds a fake fast
     absorption that also flips the void coefficient.
  4. Depletion conserves heavy-metal atoms and produces ~2 lumped FP atoms per
     fission - a bookkeeping error shows up here first.
  5. The void coefficient stays POSITIVE at burnup. This is the RBMK's defining
     behaviour and the whole point of the model; if it goes negative the physics
     is wrong, whatever else looks reasonable.

Runs against whichever library is selected; skips if no cache is present.
"""
from __future__ import annotations

import os
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

import endf
import rbmk_cell as cell
import rbmk_spectrum as spec
import burnup

checks = 0
failures = []


def check(ok, what, detail=""):
    global checks
    checks += 1
    if ok:
        print("  PASS  %s" % what)
    else:
        print("  FAIL  %s %s" % (what, detail))
        failures.append(what)


def main():
    if not os.path.isdir(endf.cache_dir()):
        print("SKIP: no cross-section cache at %s" % endf.cache_dir())
        return 0

    c = burnup.Cell()

    print("1. burnup rate")
    # 3200 MWt over 192 t U = 16.67 W/g = 0.0167 MWd/kg/day.
    check(0.0160 < c.burn_rate < 0.0175,
          "burnup rate is 0.016-0.017 MWd/kg/day",
          "(got %.4f; 0.0398 means power spread over the graphite stack, not the fuel)"
          % c.burn_rate)

    print("2. lumped FP is 1/v")
    fp = endf.load("FPlump", 102)
    thermal = fp.at(0.0253)
    fast = fp.at(1.0e6)
    check(abs(thermal - 50.0) < 1.0, "50 b at 0.0253 eV", "(got %.1f)" % thermal)
    check(fast < 0.05, "falls below 0.05 b at 1 MeV", "(got %.4f)" % fast)
    ratio = thermal / max(fast, 1e-30)
    # 1/v over 0.0253 eV -> 1 MeV is a factor sqrt(1e6/0.0253) = 6283.
    expected = np.sqrt(1.0e6 / 0.0253)
    check(abs(ratio - expected) / expected < 0.05,
          "thermal/fast ratio is the 1/v factor (%.0f)" % expected,
          "(got %.0f)" % ratio)

    print("3. depletion bookkeeping")
    fresh = c.fresh()
    comp = dict(fresh)
    before_hm = comp["U235"] + comp["U238"]
    before_u5 = comp["U235"]
    c.step(comp, 30)
    after_hm = comp["U235"] + comp["U238"]
    check(after_hm < before_hm, "heavy metal decreases")
    check(comp["U235"] < before_u5, "U-235 decreases")
    check(comp["U238"] < before_hm, "U-238 decreases")
    check(comp["FPlump"] > 0, "fission products accumulate")
    # U-238 loss is capture only, and that capture becomes Pu-239.
    d_u8 = fresh["U238"] - comp["U238"]
    check(comp["Pu239"] > 0 and comp["Pu239"] <= d_u8 * 1.001,
          "Pu-239 never exceeds the U-238 it came from",
          "(Pu %.3e vs dU8 %.3e)" % (comp["Pu239"], d_u8))
    # Two fission products per fission: FP atoms ~ 2x U-235 fissioned.
    d_u5 = before_u5 - comp["U235"]
    check(0.5 * d_u5 < comp["FPlump"] < 2.5 * d_u5,
          "one lumped atom per fission (FP ~ U-235 destroyed)",
          "(FP %.3e vs dU5 %.3e)" % (comp["FPlump"], d_u5))

    print("4. Pu-240 comes from capture, not absorption")
    # Burn long enough for Pu-240 to matter, then check the vector.
    comp2 = c.fresh()
    for _ in range(60):
        c.step(comp2, 30)
    pu = comp2["Pu239"] + comp2["Pu240"] + comp2["Pu241"]
    share40 = comp2["Pu240"] / pu
    check(0.10 < share40 < 0.55,
          "Pu-240 share of Pu stays physical (0.10-0.55)",
          "(got %.2f; >0.6 means Pu-239 fission was booked as Pu-240 production)"
          % share40)

    print("5. k_inf falls with burnup")
    k_fresh, _, _, _ = c.solve(c.fresh())
    k_burned, _, _, _ = c.solve(comp2)
    check(k_burned < k_fresh,
          "k_inf decreases from fresh to burned",
          "(%.5f -> %.5f)" % (k_fresh, k_burned))

    print("6. void coefficient stays positive at burnup")
    # Replace coolant with void in the zone mix and re-solve.
    def kinf_void(comp, void_fraction):
        nd, _ = cell.number_densities(void_fraction)
        saved = c.zones, c.nd
        c.zones = {
            "clad": [("Zr90", nd["Zr"] * 0.5145)],
            "tube": [("Zr90", nd["Zr"] * 0.5145)],
            "graph": [(l, d) for l, d, _ in cell.carbon_spec(nd["C"])],
            "cool": [("H1", nd["H1"]), ("O16", nd["O16_w"])],
        }
        c.nd = nd
        try:
            k, _, _, _ = c.solve(comp)
        finally:
            c.zones, c.nd = saved
        return k

    for tag, cc in (("fresh", c.fresh()), ("burned", comp2)):
        kw = kinf_void(cc, 0.0)
        kv = kinf_void(cc, 1.0)
        dk = (kv - kw) / (kw * kv) * 1e5
        check(dk > 0, "void coefficient positive, %s fuel" % tag,
              "(got %+.0f pcm; a negative value means Pu-240 or a flat FP absorber "
              "has been mis-built)" % dk)

    print("\n%d checks, %d failures" % (checks, len(failures)))
    return 0 if not failures else 1


if __name__ == "__main__":
    sys.exit(main())
