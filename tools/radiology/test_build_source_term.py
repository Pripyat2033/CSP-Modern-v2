#!/usr/bin/env python3
"""Checks on the layer-3 source-term builder. No network needed.

Run: python3 tools/radiology/test_build_source_term.py
"""

from __future__ import annotations

import json
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.dirname(os.path.dirname(HERE))
DATA = os.path.join(REPO, "src", "main", "resources", "data", "csp",
                    "radiology", "source-term.json")
INV = os.path.join(REPO, "src", "main", "resources", "data", "csp",
                   "radiology", "inventory.json")
DECAY = os.path.join(HERE, "decay-cache.json")

checks = 0
failures = 0


def check(what, ok, detail=""):
    global checks, failures
    checks += 1
    if not ok:
        failures += 1
    print(f"{'PASS' if ok else 'FAIL'} {what}" + (f"  [{detail}]" if detail else ""))


def main() -> int:
    d = json.load(open(DATA))
    nucs = d["nuclides"]

    check("the file declares its layer", "layer 3" in d["_layer"], d["_layer"])
    check("25 nuclides from Table 3.1", len(nucs) == 25, str(len(nucs)))
    check("counts block is self-consistent",
          d["_counts"]["tableEntries"] == len(nucs)
          and d["_counts"]["fractionsDerived"] + d["_counts"]["fractionsRefused"] == len(nucs),
          json.dumps(d["_counts"]))

    # The primary source must be cited with a URL and a table, not just named.
    src = d["_primarySource"]
    check("primary source is cited with URL and table",
          "Pub1239" in src["url"] and "3.1" in src["table"], src["table"])
    check("source policy states it is a second-best source",
          "second-best" in d["_sourcePolicy"].lower(), "")

    # No fraction outside [0,1], and no refused entry carrying a number.
    bad = [k for k, v in nucs.items()
           if v["releaseFraction"] is not None
           and not (0.0 <= v["releaseFraction"] <= 1.0)]
    check("no fraction outside [0,1]", not bad, str(bad))

    inconsistent = [k for k, v in nucs.items()
                    if (v["releaseFraction"] is not None)
                    != (v["fractionStatus"] == "derived")]
    check("refused entries carry no number", not inconsistent, str(inconsistent))

    # Every refused entry must say why. A blank status is the failure mode.
    unexplained = [k for k, v in nucs.items()
                   if v["fractionStatus"] != "derived" and not v.get("note")]
    check("every refused entry carries a note", not unexplained, str(unexplained))

    # The three refusals are the ones the cross-check found, by name. If a
    # future edit makes one of them 'derived', that is a layer-2 claim and must
    # be justified, not silently accepted.
    refused = {k for k, v in nucs.items() if v["fractionStatus"] != "derived"}
    expected = {"Te129m", "Cs134", "Cs136", "Np239", "Pu238", "Pu239",
                "Pu240", "Pu241", "Pu242", "Cm242"}
    check("the refused set is exactly the expected ten", refused == expected,
          f"missing={expected - refused} extra={refused - expected}")

    # Volatility ordering: the physical check that does not depend on any
    # single published number.
    def mean_of(cls):
        vals = [v["releaseFraction"] for v in nucs.values()
                if v.get("volatilityClass") == cls and v["releaseFraction"] is not None]
        return sum(vals) / len(vals) if vals else float("nan")

    inert, vol, inter, refr = (mean_of(c) for c in
                               ("inert", "volatile", "intermediate", "refractory"))
    check("inert > volatile", inert > vol, f"{inert:.3f} > {vol:.3f}")
    check("volatile > intermediate", vol > inter, f"{vol:.3f} > {inter:.3f}")
    check("intermediate > refractory", inter > refr, f"{inter:.3f} > {refr:.3f}")

    # The refractory mean should land near the quoted fuel-particle fraction.
    # This is the cross-layer agreement the whole layer rests on, so it is
    # asserted with a wide window rather than a tight one.
    ff = d["_fuelReleaseFraction"]
    check("refractory mean is near the fuel-particle fraction",
          0.4 * ff["central"] < refr < 2.0 * ff["central"],
          f"refractory {refr:.4f} vs fuel {ff['central']}")

    check("fuel fraction bounds bracket the central value",
          ff["low"] < ff["central"] < ff["high"],
          f"{ff['low']} < {ff['central']} < {ff['high']}")
    check("the earlier fuel estimate is retained and is larger",
          ff["earlierEstimate"] > ff["high"],
          f"{ff['earlierEstimate']} > {ff['high']}")

    # The derived fractions must actually be released/core, not something else.
    inv = json.load(open(INV))
    atoms = {v["symbol"]: v["atoms"] for v in inv["nuclides"].values()}
    cache = json.load(open(DECAY))
    hl = {v["nuclide"]: v.get("halfLifeSeconds")
          for v in cache.values() if isinstance(v, dict) and "nuclide" in v}
    wrong = []
    for sym, v in nucs.items():
        if v["fractionStatus"] != "derived":
            continue
        core = atoms[sym] * math.log(2) / hl[sym] / 1e15
        expect = v["releasedPbq"] / core
        if abs(expect - v["releaseFraction"]) > 1e-5 * max(1.0, expect):
            wrong.append((sym, expect, v["releaseFraction"]))
    check("every derived fraction is released/core, recomputed independently",
          not wrong, str(wrong[:3]))

    # No dose, transport or deposition may appear in this layer.
    forbidden = [k for k in ("dose", "transport", "deposition", "doseRate")
                 if any(k in json.dumps(v).lower() for v in nucs.values())]
    check("no dose/transport/deposition inside the nuclide records",
          not forbidden, str(forbidden))

    print(f"\ntest_build_source_term: {checks} checks, {failures} failures")
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
