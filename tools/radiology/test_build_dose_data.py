#!/usr/bin/env python3
"""Checks on the layer-4 dose data. No network needed.

The important checks here are the ones that could catch the extraction being
wrong, because a misread dose coefficient is a plausible-looking number that
would poison every dose the mod ever reports. Two independent lines of defence:

  * the ingested coefficients are compared against ICRP adult ingestion values
    from the literature, which the TECDOC reproduces exactly -- so a misread
    digit shows up immediately;
  * the physical shape of every table is asserted (monotonic in the right
    direction, conservative values at least as large as best estimates).

Run: python3 tools/radiology/test_build_dose_data.py
"""

from __future__ import annotations

import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.dirname(os.path.dirname(HERE))
DATA = os.path.join(REPO, "src", "main", "resources", "data", "csp",
                    "radiology", "dose-data.json")
TABLES = os.path.join(HERE, "tecdoc1162-tables.json")

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
    nuc = d["nuclides"]

    check("the file declares layer 4", "layer 4" in d["_layer"], d["_layer"])
    check("all 25 layer-3 nuclides carry dose data", len(nuc) == 25, str(len(nuc)))
    check("the source is cited with a URL",
          "TECDOC-1162" in d["_source"]["citation"] and d["_source"]["url"], "")

    # --- The extraction check that matters: ingestion against ICRP adult. ---
    # Units: CF5 is mSv/kBq, so mSv/kBq x 1e-3 = Sv/Bq ... i.e. divide by 1e3
    # after the 1e-3 for mSv->Sv:  CF5 [mSv/kBq] = 1e-6 * [Sv/Bq]^-1.
    icrp_ing = {
        "I131": 2.2e-8, "Cs137": 1.3e-8, "Cs134": 1.9e-8, "Sr90": 2.8e-8,
        "Sr89": 2.6e-9, "Pu239": 2.5e-7, "Ru106": 7.0e-9,
        "Ce144": 5.2e-9, "Ba140": 2.6e-9, "Zr95": 9.5e-10, "I133": 4.3e-9,
    }
    bad = []
    for sym, sv_per_bq in icrp_ing.items():
        e = nuc.get(sym)
        if not e or "perIntake" not in e["ingestion"]:
            bad.append((sym, "absent"))
            continue
        got_sv = e["ingestion"]["perIntake"] * 1e-6
        if abs(got_sv - sv_per_bq) / sv_per_bq > 0.02:
            bad.append((sym, got_sv, sv_per_bq))
    check("ingestion coefficients match ICRP adult values", not bad, str(bad))

    # The same comparison on nuclides OUTSIDE the release, which is a purer test
    # of the extractor: these numbers appear in no other part of this project,
    # so agreement can only come from reading the table correctly.
    tables = json.load(open(TABLES))
    outside = {"Co-60": 3.4e-9, "Ra-226": 2.8e-7, "Th-232": 2.3e-7}
    bad = []
    for key, sv_per_bq in outside.items():
        row = tables["E13"].get(key)
        if not row:
            bad.append((key, "absent"))
            continue
        got = row[0] * 1e-6
        if abs(got - sv_per_bq) / sv_per_bq > 0.02:
            bad.append((key, got, sv_per_bq))
    check("the extractor reproduces ICRP values for nuclides outside the release",
          not bad, str(bad))

    # --- Inhalation must be conservative: never below the ICRP adult value. ---
    # A conservative value is allowed to be larger; being smaller would mean the
    # extraction lost a digit or picked the wrong column.
    icrp_inh = {"I131": 7.4e-9, "Cs137": 4.8e-9, "Cs134": 6.6e-9,
                "Sr90": 4.1e-8, "Pu239": 1.2e-4}
    bad = []
    for sym, sv_per_bq in icrp_inh.items():
        e = nuc[sym]
        cf2 = e["inhalation"]["perHourPerAirConcentration"]
        # CF2 [(mSv/h)/(kBq/m3)] with 1.5 m3/h -> Sv/Bq = CF2*1e-3/1500
        got = cf2 * 1e-3 / 1500.0
        if got < sv_per_bq * 0.99:
            bad.append((sym, got, sv_per_bq))
    check("inhalation factors are conservative (>= ICRP adult)", not bad, str(bad))

    # --- Iodine thyroid factors must dwarf the effective-dose factors. ---
    # This is the whole reason thyroid dose is a separate quantity at Chernobyl:
    # iodine concentrates in the gland, so the thyroid dose is tens of times the
    # whole-body committed effective dose for the same intake.
    i131 = nuc["I131"]
    thy = i131["thyroid"]["adultPerHourPerAirConcentration"]
    eff = i131["inhalation"]["perHourPerAirConcentration"]
    check("I-131 thyroid factor exceeds its effective-dose factor",
          thy > 10.0 * eff, f"thyroid {thy} vs effective {eff}")
    check("child thyroid factor exceeds adult",
          i131["thyroid"]["child10yPerHourPerAirConcentration"] > thy,
          f"child {i131['thyroid']['child10yPerHourPerAirConcentration']} vs adult {thy}")

    # --- Only iodine and tellurium may carry a thyroid factor. ---
    with_thy = {s for s, e in nuc.items()
                if "adultPerHourPerAirConcentration" in e["thyroid"]}
    check("only iodine and tellurium carry thyroid factors",
          all(s.startswith(("I", "Te")) for s in with_thy), str(sorted(with_thy)))

    # --- Noble gases: no inhalation credit, and zero ingestion. ---
    for gas in ("Kr85", "Xe133"):
        e = nuc[gas]
        check(f"{gas} has no inhalation credit",
              e["inhalation"]["status"] == "not-credited", str(e["inhalation"]))
        check(f"{gas} ingestion is zero",
              e["ingestion"]["perIntake"] == 0.0, str(e["ingestion"]))

    # --- Ground dose: the 50-year integral must exceed the first-month value. ---
    bad = []
    for sym, e in nuc.items():
        g = e["ground"]
        d1 = g.get("firstMonthPerDeposition")
        d50 = g.get("fiftyYearPerDeposition")
        if isinstance(d1, float) and isinstance(d50, float) and d50 < d1:
            bad.append((sym, d1, d50))
    check("50-year ground dose exceeds the first month", not bad, str(bad))

    # --- Dispersion: monotonic decrease with distance, for every class. ---
    disp = d["_dispersion"]
    bad = []
    for cls, vals in disp["dilutionFactorByStability"].items():
        if len(vals) != len(disp["distancesKm"]):
            bad.append((cls, "length"))
            continue
        for a, b in zip(vals, vals[1:]):
            if b > a:
                bad.append((cls, "not monotonic"))
                break
    check("dilution factors decrease with distance in every class", not bad, str(bad))

    # --- Dispersion: at each distance, F >= E >= D >= C >= B >= A. ---
    # Stable air keeps the plume together, so its centreline concentration is
    # highest. If this ordering is inverted the classes are labelled backwards.
    bad = []
    order = ["A", "B", "C", "D", "E", "F"]
    for j in range(len(disp["distancesKm"])):
        col = [disp["dilutionFactorByStability"][c][j] for c in order]
        if col != sorted(col):
            bad.append((disp["distancesKm"][j], col))
    check("more stable class gives higher centreline concentration", not bad,
          str(bad[:2]))

    # --- A value must never be silently zero where the source gave one. ---
    zero_but_tabulated = []
    for sym, e in nuc.items():
        cf5 = e["ingestion"].get("perIntake")
        if cf5 == 0.0 and sym not in ("Kr85", "Xe133"):
            # A zero for a non-noble-gas would mean a misread row.
            zero_but_tabulated.append(sym)
    check("no non-noble-gas ingestion factor was read as zero",
          not zero_but_tabulated, str(zero_but_tabulated))

    print(f"\ntest_build_dose_data: {checks} checks, {failures} failures")
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
