#!/usr/bin/env python3
"""Guard the ROSFOND-2010 numbers the model was measured on.

`rosfond_pinned.json` records what the lattice produced on the ROSFOND-2010
files Ben supplied. This test re-derives those quantities from the live cache
and fails if any of them has moved.

WHY A PIN AND NOT A RANGE CHECK:

`verify.py` already checks the physics against textbook bands (U-235 fission
585 +- 10 b and so on). Those bands are wide by design - they say "this is
plausible nuclear data", not "this is the run we based the model on". A silent
change inside the band would pass verification while quietly moving the reactor
model, which is exactly the failure mode this project exists to prevent.

The pin is exact to the digits stored, because the cache is a fixed file: the
same library read by the same code must give the same number. If it does not,
something changed - the cache was rebuilt from a different evaluation, the
interpolation changed, or the source selector fell back to ENDF - and that must
be a deliberate, recorded decision rather than an accident.

Regenerate ONLY when you intend to change the basis, and say why in the commit:

    python3 test_rosfond_pinned.py --regen

Requires the ROSFOND cache (`import_rosfond.py` + `reconstruct_rosfond.py`).
Without it the test SKIPS rather than fails: a missing local dataset is not a
wrong answer, and the CI runner has no copy of the IPPE distribution.
"""
from __future__ import annotations

import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
PIN = os.path.join(HERE, "rosfond_pinned.json")

checks = 0
failures = []


def check(ok, what, detail=""):
    global checks
    checks += 1
    if not ok:
        failures.append(f"{what} {detail}".strip())
        print(f"  [FAIL] {what} {detail}")
    else:
        print(f"  [PASS] {what} {detail}")


def derive():
    """Recompute the pinned quantities from the live ROSFOND cache."""
    import numpy as np
    import sources
    import endf
    import rbmk_cell1d as c1d

    sources.get("rosfond")
    out = {"nuclides": {}}
    for key in json.load(open(PIN))["nuclides"]:
        if key == "U238_RI":
            continue
        lab, mt = key.rsplit("_", 1)
        xs = endf.load(lab, int(mt))
        out["nuclides"][key] = None if xs is None else round(float(xs.at(0.0253)), 4)

    u = endf.load("U238", 102)
    E = np.exp(np.linspace(np.log(0.5), np.log(1e6), 6000))
    out["nuclides"]["U238_RI"] = round(float(np.trapezoid(u.at(E) / E, E)), 3)

    kw, _, _ = c1d.solve_cell(0.0)
    kv, _, _ = c1d.solve_cell(1.0)
    rho = (kv - kw) / (kw * kv)
    out["cell"] = {"kWater": round(float(kw), 5), "kVoid": round(float(kv), 5),
                   "rho": round(float(rho), 6), "betaEff": 0.0065,
                   "rhoInBeta": round(float(rho / 0.0065), 3)}
    return out


def main(argv):
    import sources
    if not os.path.exists(os.path.join(HERE, "rosfond_cache", "U235_18.json")):
        print("SKIP: no ROSFOND cache present (run import_rosfond + reconstruct_rosfond)")
        return 0

    live = derive()
    if "--regen" in argv:
        pin = json.load(open(PIN))
        pin.update(live)
        json.dump(pin, open(PIN, "w"), indent=2)
        print(f"regenerated {PIN}")
        return 0

    pin = json.load(open(PIN))
    print(f"Pinned ROSFOND-2010 values ({len(pin['nuclides'])} quantities + cell)\n")

    print("1. Thermal cross sections are unchanged")
    for key, want in pin["nuclides"].items():
        got = live["nuclides"][key]
        check(got == want, key, f"got {got}, pinned {want}")

    print("\n2. The cell result the model was measured on is unchanged")
    for key, want in pin["cell"].items():
        got = live["cell"][key]
        check(got == want, key, f"got {got}, pinned {want}")

    print("\n3. The library in use is the pinned one")
    check(sources.current().library == pin["library"], "active library",
          f"got {sources.current().library}, pinned {pin['library']}")

    print("\n4. The void coefficient is positive and material")
    check(live["cell"]["kVoid"] > live["cell"]["kWater"], "water -> steam raises k",
          f"(k_water={live['cell']['kWater']} k_void={live['cell']['kVoid']})")
    check(live["cell"]["rhoInBeta"] > 1.0, "void worth exceeds one beta",
          f"(= {live['cell']['rhoInBeta']} beta)")

    print("\n" + "-" * 52)
    print(f"{checks - len(failures)}/{checks} checks passed")
    if failures:
        print("\nPIN BROKEN - the model's basis has moved:")
        for f in failures:
            print(f"  - {f}")
        print("\nIf this is intended, regenerate with --regen and record why.")
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
