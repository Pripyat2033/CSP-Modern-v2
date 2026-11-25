#!/usr/bin/env python3
"""Build the REKORD layer-4 dose data: conversion factors and dispersion factors.

Layer 3 gave what left the building. This layer turns a release into a dose to a
person, and it does so with published conversion factors rather than a physical
model of the body. That is deliberate: the dose coefficients ARE the model, the
product of decades of ICRP dosimetry, and re-deriving them here would be worse
than quoting them.

THE SOURCE. IAEA-TECDOC-1162, 'Generic procedures for assessment and response
during a radiological emergency' (IAEA, Vienna, August 2000). It is an
emergency-response handbook written to be used by an assessor at the time of an
accident, and it tabulates exactly the four quantities this layer needs:

  Table E3  ground contamination: ambient dose rate per unit deposition, and
            effective dose from deposition over the 1st month, 2nd month and
            50 years.
  Table E6  inhalation: committed effective dose per unit intake, adult.
  Table E7  thyroid: committed equivalent dose per unit intake, adult and child.
  Table E13 ingestion: committed effective dose per unit intake, adult.
  Table E8  atmospheric dilution factors by Pasquill stability class and distance.

HOW THE VALUES WERE OBTAINED, AND WHY THAT MATTERS. The tables are read out of
the PDF by `tools/radiology/extract_tecdoc1162.py`, not typed in by hand, so a
value can be checked against the source. The extraction is verified against
independently known ICRP adult dose coefficients in
`tools/radiology/test_build_dose_data.py`.

WHAT IS CONSERVATIVE AND WHAT IS NOT, stated explicitly because it changes how
a result should be read:

  * Table E6 (inhalation) uses, in the TECDOC's own words, 'the highest value
    from IAEA BSS ... (conservative approach)'. For a nuclide whose lung
    clearance class matters this can be several times the adult value. It is
    kept as published and labelled `conservative`, not silently replaced.
  * Table E7 (thyroid) likewise uses the highest particulate value.
  * Table E13 (ingestion) reproduces the adult ICRP values exactly, so it is
    labelled `adult`.
  * Table E3 (ground) is for open, undisturbed ground with no shielding; the
    TECDOC's own Table E4 shielding factors and occupancy fraction are applied
    downstream, not baked in here.

Usage:
    python3 tools/radiology/build_dose_data.py
"""

from __future__ import annotations

import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.dirname(os.path.dirname(HERE))
RES = os.path.join(REPO, "src", "main", "resources")
TABLES = os.path.join(HERE, "tecdoc1162-tables.json")
OUT = os.path.join(RES, "data", "csp", "radiology", "dose-data.json")

SOURCE = {
    "citation": (
        "International Atomic Energy Agency, Generic Procedures for Assessment "
        "and Response during a Radiological Emergency, IAEA-TECDOC-1162, "
        "IAEA, Vienna (August 2000)"
    ),
    "url": "https://www-pub.iaea.org/MTCD/Publications/PDF/te_1162_prn.pdf",
    "tables": {
        "ground": "Table E3, pp. 98-100 (CF3, CF4)",
        "inhalation": "Table E6, pp. 107-108 (CF2)",
        "thyroid": "Table E7, p. 109 (CF1)",
        "ingestion": "Table E13, pp. 116-117 (CF5)",
        "dilution": "Table E8, p. 111 (DFm)",
        "stability": "Table E9, p. 111",
        "windSpeed": "Table E10, p. 111",
    },
}

# Table E8: dilution factors [m^-2] by distance and Pasquill class.
# Transcribed from the extracted text; the shape (monotonic decrease with
# distance, A steepest / F shallowest) is asserted in the test.
DISTANCES_KM = [0.5, 1, 2, 3, 4, 5, 10, 15, 20, 25, 30]
DILUTION = {
    "A": [5.7e-4, 3.3e-5, 9.2e-7, 7.3e-7, 5.6e-7, 4.4e-7, 2.6e-7, 1.8e-7, 1.4e-7, 1.2e-7, 1.0e-7],
    "B": [6.6e-4, 7.9e-5, 4.3e-6, 1.8e-6, 1.0e-6, 6.2e-7, 3.5e-7, 2.4e-7, 1.8e-7, 1.5e-7, 1.2e-7],
    "C": [7.2e-4, 1.3e-4, 1.3e-5, 5.9e-6, 3.4e-6, 2.1e-6, 7.2e-7, 3.5e-7, 2.5e-7, 2.0e-7, 1.8e-7],
    "D": [7.9e-4, 2.4e-4, 4.2e-5, 2.3e-5, 1.6e-5, 1.1e-5, 4.0e-6, 2.3e-6, 1.4e-6, 9.6e-7, 7.7e-7],
    "E": [8.4e-4, 3.4e-4, 8.6e-5, 4.6e-5, 3.2e-5, 2.4e-5, 9.1e-6, 5.6e-6, 3.6e-6, 2.5e-6, 2.1e-6],
    "F": [8.9e-4, 5.0e-4, 2.0e-4, 1.1e-4, 7.8e-5, 5.7e-5, 2.4e-5, 1.4e-5, 9.2e-6, 6.8e-6, 5.5e-6],
}

# The nuclides layer 3 knows about. A dose factor is carried for each so that a
# released activity can be turned into a dose without a lookup failing at
# runtime. Where the source gives no value (a noble gas that gives no dose by
# ingestion, say) that is recorded as a reason, not as zero.
NUCLIDES = [
    "Kr85", "Xe133", "Te129m", "Te132", "I131", "I133", "Cs134", "Cs136",
    "Cs137", "Sr89", "Sr90", "Ru103", "Ru106", "Ba140", "Zr95", "Mo99",
    "Ce141", "Ce144", "Np239", "Pu238", "Pu239", "Pu240", "Pu241", "Pu242",
    "Cm242",
]

# The TECDOC tabulates some parents together with their short-lived daughters,
# because the daughter is in equilibrium and its dose cannot be separated. The
# key used in the tables differs from the bare symbol for these.
TABLE_KEY = {
    "Cs137": "Cs-137+Ba-137m",
    "Ru106": "Ru-106+Rh-106",
    "Mo99": "Mo-99+Tc-99m",
    "Ce144": "Ce-144+Pr-144",
}


def table_symbol(symbol: str) -> str:
    if symbol in TABLE_KEY:
        return TABLE_KEY[symbol]
    # Cs137 -> Cs-137
    i = 0
    while i < len(symbol) and symbol[i].isalpha():
        i += 1
    return f"{symbol[:i]}-{symbol[i:]}"


def get(table: dict, symbol: str, index: int = 0):
    # The ground table (E3) tabulates some parents with their daughters, while
    # the intake tables (E6/E7/E13) list the parent alone. Try the combined key
    # first, then the bare symbol, so a nuclide is not lost to a key mismatch.
    keys = [table_symbol(symbol)]
    i = 0
    while i < len(symbol) and symbol[i].isalpha():
        i += 1
    bare = f"{symbol[:i]}-{symbol[i:]}"
    if bare not in keys:
        keys.append(bare)
    for key in keys:
        row = table.get(key)
        if row is not None and index < len(row):
            return row[index]
    return None


def main() -> int:
    if not os.path.exists(TABLES):
        print(f"missing {TABLES}; run tools/radiology/extract_tecdoc1162.py first",
              file=sys.stderr)
        return 1
    t = json.load(open(TABLES))

    entries = {}
    missing = []
    for sym in NUCLIDES:
        ground_rate = get(t["E3"], sym, 0)
        d1 = get(t["E3"], sym, 1)
        d2 = get(t["E3"], sym, 2)
        d50 = get(t["E3"], sym, 3)
        inh = get(t["E6"], sym, 0)
        thy_adult = get(t["E7"], sym, 0)
        thy_child = get(t["E7"], sym, 1)
        ing = get(t["E13"], sym, 0)

        e = {}

        # Ground contamination. Units are the source's: (mSv/h)/(kBq/m2) for the
        # rate and mSv/(kBq/m2) for the integrals.
        if ground_rate is None and d1 is None:
            e["ground"] = {"status": "not-tabulated"}
            missing.append((sym, "ground"))
        else:
            e["ground"] = {
                "doseRatePerDeposition": ground_rate,
                "doseRateUnit": "(mSv/h)/(kBq/m2)",
                "firstMonthPerDeposition": d1,
                "secondMonthPerDeposition": d2,
                "fiftyYearPerDeposition": d50,
                "doseUnit": "mSv/(kBq/m2)",
                "basis": "open undisturbed ground, no shielding, no occupancy factor",
            }

        if inh is None or inh == "NC":
            e["inhalation"] = {
                "status": "not-credited" if inh == "NC" else "not-tabulated",
                "note": ("a noble gas: the source records NC, i.e. no dose "
                         "credited by this pathway") if inh == "NC" else None,
            }
        else:
            e["inhalation"] = {
                "perHourPerAirConcentration": inh,
                "unit": "(mSv/h)/(kBq/m3)",
                "basis": "conservative",
                "note": ("highest value from the IAEA Basic Safety Standards, "
                         "per the TECDOC's own note; breathing rate 1.5 m3/h"),
            }

        if thy_adult is None:
            e["thyroid"] = {"status": "not-tabulated"}
        else:
            e["thyroid"] = {
                "adultPerHourPerAirConcentration": thy_adult,
                "child10yPerHourPerAirConcentration": thy_child,
                "unit": "(mSv/h)/(kBq/m3)",
                "basis": "conservative",
                "note": ("only the iodine and tellurium isotopes carry a "
                         "thyroid factor, which is why the table is short: "
                         "these are the nuclides that concentrate in the gland"),
            }

        if ing is None:
            e["ingestion"] = {"status": "not-tabulated"}
        elif ing == "NC":
            e["ingestion"] = {"status": "not-credited"}
        else:
            e["ingestion"] = {
                "perIntake": ing,
                "unit": "mSv/kBq",
                "basis": "adult",
                "note": ("reproduces the ICRP adult ingestion coefficient "
                         "exactly, verified in the test"),
            }

        entries[sym] = e

    doc = {
        "_layer": "REKORD layer 4: transport and dose",
        "_note": (
            "Dose conversion factors and atmospheric dilution factors, quoted "
            "from IAEA-TECDOC-1162. This file turns a release into a dose. It "
            "contains no dose itself: a dose needs a release (layer 3), a "
            "distance, a weather class and an exposure time, all of which are "
            "runtime inputs."
        ),
        "_method": (
            "Inhalation and ingestion dose = intake x conversion factor, per "
            "the TECDOC's own equations (Procedure E5 step 2, Procedure E6). "
            "Ground dose = deposition x conversion factor, adjusted by the "
            "TECDOC's shielding factor and occupancy fraction (Procedure E3 "
            "step 3). Air concentration = release rate x dilution factor / wind "
            "speed (Procedure E5a). No equation here is this project's own."
        ),
        "_source": SOURCE,
        "_basisPolicy": (
            "Each factor is labelled with what it represents. 'conservative' "
            "means the source chose the highest value across body types and "
            "chemical forms, so a dose computed from it is an upper bound, not "
            "a best estimate. 'adult' means the adult value. This distinction "
            "is carried into the data because a conservative number presented "
            "as a best estimate is a lie of the same kind as a guessed number."
        ),
        "_dispersion": {
            "distancesKm": DISTANCES_KM,
            "dilutionFactorByStability": DILUTION,
            "dilutionFactorUnit": "m^-2",
            "windSpeedUnit": "m/s",
            "equation": "C_air [kBq/m3] = Q [kBq/s] * DF [1/m2] / u [m/s]",
            "validity": (
                "constant release rate, wind direction and speed; simple "
                "terrain; ground-level release; no rain; single release point. "
                "The TECDOC states these conditions explicitly and this project "
                "does not extend the factors beyond them."
            ),
            "belowHalfKm": (
                "the TECDOC uses Fig. E1 below 0.5 km because building wake "
                "dominates; the table value at 0.5 km is used here and the "
                "limitation is recorded rather than hidden"
            ),
        },
        "_stabilityClasses": {
            "scheme": "Pasquill",
            "classes": ["A", "B", "C", "D", "E", "F"],
            "meaning": ("A most unstable, F most stable; Table E9 of the TECDOC "
                        "maps weather to a class and Table E10 maps observable "
                        "effects to a wind speed"),
        },
        "_missing": missing,
        "_notInThisFile": (
            "No shielding factor or occupancy fraction (TECDOC Table E4: "
            "shielding 0.2-0.5 depending on building, default occupancy 0.6). "
            "No resuspension. No aquatic pathway. No internal biokinetics "
            "beyond the committed-dose factors themselves."
        ),
        "nuclides": entries,
    }

    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w") as f:
        json.dump(doc, f, indent=1)
        f.write("\n")

    print(f"wrote {OUT}")
    print(f"  nuclides        {len(entries)}")
    print(f"  with ground     {sum(1 for e in entries.values() if 'doseRatePerDeposition' in e['ground'])}")
    print(f"  with inhalation {sum(1 for e in entries.values() if 'perHourPerAirConcentration' in e['inhalation'])}")
    print(f"  with thyroid    {sum(1 for e in entries.values() if 'adultPerHourPerAirConcentration' in e['thyroid'])}")
    print(f"  with ingestion  {sum(1 for e in entries.values() if 'perIntake' in e['ingestion'])}")
    if missing:
        print(f"  missing ground  {missing}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
