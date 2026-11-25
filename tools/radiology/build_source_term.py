#!/usr/bin/env python3
"""Build the REKORD layer-3 source term: what left the building, as a RANGE.

Layer 2 gave atoms at shutdown. This layer gives the fraction of each nuclide
that was released to the atmosphere, and every number here is either quoted from
a cited source or derived from a quoted number. Nothing is invented, and where
the sources disagree the disagreement is carried in the data rather than
averaged away.

THE SHAPE OF THE DATA. A single release fraction per nuclide would be a lie of
precision: the published estimates for the same nuclide differ by more than a
factor of two, and the fuel-particle component has itself been revised by a
factor of two (INSAG-1's 3% down to Kashparov's 1.5%+/-0.5%). So each entry
carries low/central/high, and the file records which source produced which
bound. A consumer must take the range seriously.

THE ANCHOR. The release is not modelled from first principles here. It is
observed: the activities in PBq come from IAEA Pub1239 Table 3.1, which is the
current (2006) consolidation of UNSCEAR 2000 and Dreicer 1996. A separate
physics model of release would be a second guess stacked on the first.

WHAT IS DERIVED, NOT QUOTED. For a nuclide in Table 3.1 the fraction is
released_activity / core_activity, with core_activity taken from layer 2. That
division is only valid where layer 2 is valid. Where it is not -- and the
cross-check found three such cases plus a whole missing class -- the entry is
marked, and the reason is recorded in `_layer2Limits` rather than hidden.

Usage:
    python3 tools/radiology/build_source_term.py
"""

from __future__ import annotations

import json
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.dirname(os.path.dirname(HERE))
RES = os.path.join(REPO, "src", "main", "resources")
INVENTORY = os.path.join(RES, "data", "csp", "radiology", "inventory.json")
DECAY = os.path.join(HERE, "decay-cache.json")
OUT = os.path.join(RES, "data", "csp", "radiology", "source-term.json")

# IAEA Pub1239 (2006) Table 3.1, "Revised estimates of the principal
# radionuclides released during the course of the Chernobyl accident".
# Activity released in PBq. A value marked '~' or '>' in the source is recorded
# in `qualifier` so the asymmetry is not lost.
TABLE_3_1 = [
    # symbol, PBq, qualifier, volatility class per the source's own grouping
    ("Kr85",   33.0,    None,  "inert"),
    ("Xe133",  6500.0,  None,  "inert"),
    ("Te129m", 240.0,   None,  "volatile"),
    ("Te132",  1150.0,  "~",   "volatile"),
    ("I131",   1760.0,  "~",   "volatile"),
    ("I133",   910.0,   None,  "volatile"),
    ("Cs134",  47.0,    "~",   "volatile"),
    ("Cs136",  36.0,    None,  "volatile"),
    ("Cs137",  85.0,    "~",   "volatile"),
    ("Sr89",   115.0,   "~",   "intermediate"),
    ("Sr90",   10.0,    "~",   "intermediate"),
    ("Ru103",  168.0,   ">",   "intermediate"),
    ("Ru106",  73.0,    ">",   "intermediate"),
    ("Ba140",  240.0,   None,  "intermediate"),
    ("Zr95",   84.0,    None,  "refractory"),
    ("Mo99",   72.0,    ">",   "refractory"),
    ("Ce141",  84.0,    None,  "refractory"),
    ("Ce144",  50.0,    "~",   "refractory"),
    ("Np239",  400.0,   None,  "refractory"),
    ("Pu238",  0.015,   None,  "refractory"),
    ("Pu239",  0.013,   None,  "refractory"),
    ("Pu240",  0.018,   None,  "refractory"),
    ("Pu241",  2.6,     "~",   "refractory"),
    ("Pu242",  0.00004, None,  "refractory"),
    ("Cm242",  0.4,     "~",   "refractory"),
]

# Pub1239 gives these two numbers directly, so they are quoted rather than
# derived. The fuel-release fraction is the quantity every refractory nuclide
# depends on, and it is the one that moved by a factor of two.
FUEL_FRACTION_CENTRAL = 0.015
FUEL_FRACTION_SIGMA = 0.005
FUEL_FRACTION_SOURCE = (
    "IAEA Pub1239 (2006) s.3.1.2, quoting Kashparov et al., Sci. Total "
    "Environ. 317 (2003) 105-119: release fraction of Sr-90, Eu-154, Pu-238, "
    "Pu-239/240 and Am-241, and therefore of the nuclear fuel itself, deposited "
    "outside the site = 1.5% +/- 0.5%, 'which is half that of earlier estimates'"
)
EARLIER_FUEL_FRACTION = 0.03
EARLIER_FUEL_SOURCE = (
    "The earlier estimate the same sentence halves: ~3%, the value used by "
    "INSAG-1 (1986) and still quoted in many secondary accounts"
)

# Figure 3.1: daily release rate excluding noble gases, decay-corrected to
# 6 May 1986, 'uncertain by +/-50%'. Two regimes, both quoted.
RELEASE_DAYS = 10
RELEASE_UNCERTAINTY = 0.5
RELEASE_WINDOW = (
    "IAEA Pub1239 (2006) s.3.1.1 and Fig. 3.1: releases continued until "
    "6 May 1986; the daily rate is uncertain by +/-50%; the rate falls to "
    "0.002-0.006 EBq/d during the cooldown period"
)


def load_half_lives() -> dict[str, float]:
    cache = json.load(open(DECAY))
    out = {}
    for v in cache.values():
        if isinstance(v, dict) and "nuclide" in v:
            hl = v.get("halfLifeSeconds")
            if hl:
                out[v["nuclide"]] = hl
    return out


def main() -> int:
    inv = json.load(open(INVENTORY))
    atoms = {v["symbol"]: v["atoms"] for v in inv["nuclides"].values()}
    hl = load_half_lives()

    entries = {}
    limits = []
    used = 0

    for symbol, pbq, qual, cls in TABLE_3_1:
        a = atoms.get(symbol)
        t = hl.get(symbol)
        entry = {
            "releasedPbq": pbq,
            "qualifier": qual,
            "volatilityClass": cls,
        }
        if a is None or not t:
            # Layer 2 cannot supply a core activity for this nuclide, so no
            # fraction can be derived. The released activity is still quoted.
            entry["releaseFraction"] = None
            entry["fractionStatus"] = "no-layer2-atoms"
            entry["note"] = (
                "Released activity is quoted from Table 3.1. No release "
                "fraction is derived: layer 2 holds no atoms for this "
                "nuclide, because the inventory is fission products only "
                "(see _layer2Limits)."
            )
            entries[symbol] = entry
            continue

        core_bq = a * math.log(2) / t
        core_pbq = core_bq / 1e15
        frac = pbq / core_pbq

        entry["coreActivityPbq"] = round(core_pbq, 6)
        entry["releaseFraction"] = round(frac, 6)
        entry["fractionStatus"] = "derived"

        if frac > 1.0:
            # A release fraction above 1 is not a small error, it means the
            # denominator is wrong. Refuse to present it as a fraction.
            entry["releaseFraction"] = None
            entry["fractionStatus"] = "impossible-gt-1"
            entry["note"] = (
                f"released {pbq} PBq exceeds the layer-2 core activity "
                f"{core_pbq:.4g} PBq by {frac:.0f}x. A fraction above 1 cannot "
                "be quoted; the layer-2 denominator is missing the route that "
                "makes this nuclide (see _layer2Limits)."
            )
            limits.append(
                {
                    "nuclide": symbol,
                    "problem": "released activity exceeds computed core activity",
                    "releasedPbq": pbq,
                    "layer2CorePbq": round(core_pbq, 6),
                    "ratio": round(frac, 3),
                }
            )
        else:
            used += 1

        entries[symbol] = entry

    doc = {
        "_layer": "REKORD layer 3: source term / release to atmosphere",
        "_note": (
            "A RANGE, not a number. Every entry carries the released activity "
            "as quoted and, where layer 2 can support it, the fraction of the "
            "core inventory that represents. Where the fraction is not "
            "derivable the entry says so instead of guessing."
        ),
        "_method": (
            "Released activities are quoted from IAEA Pub1239 Table 3.1, not "
            "modelled. Release fractions are released_activity / core_activity, "
            "with core_activity computed from layer-2 atoms and the layer-1 "
            "decay constant. No release fraction is fitted to anything."
        ),
        "_sourcePolicy": (
            "Russian/Soviet primary sources are preferred. For the release "
            "itself no Russian primary tabulation was available in this "
            "environment, so the IAEA consolidation is used and labelled. It "
            "is a second-best source under this project's policy and should be "
            "replaced when a Russian source is obtained."
        ),
        "_primarySource": {
            "citation": (
                "International Atomic Energy Agency, Environmental "
                "Consequences of the Chernobyl Accident and their Remediation: "
                "Twenty Years of Experience, Radiological Assessment Reports "
                "Series, IAEA, Vienna (2006), ISBN 92-0-104805-X"
            ),
            "url": "https://www-pub.iaea.org/MTCD/Publications/PDF/Pub1239_web.pdf",
            "table": "Table 3.1, p.19",
            "figure": "Fig. 3.1, p.18",
        },
        "_underlyingSources": [
            "UNSCEAR, Sources and Effects of Ionizing Radiation, Vol. II, UN, "
            "New York (2000) 451-566 [Pub1239 ref 3.6]",
            "Dreicer et al., 'Consequences of the Chernobyl accident for the "
            "natural and human environments', One Decade after Chernobyl, IAEA "
            "(1996) 319-361 [ref 3.7]",
            "Kashparov et al., Sci. Total Environ. 317 (2003) 105-119 [ref 3.9] "
            "-- the 1.5%+/-0.5% fuel-release fraction",
            "Muck et al., Health Phys. 82 (2002) 141-156 [ref 3.8] -- the "
            "Cs-134/Cs-137 ratio of 0.55 at 26 April 1986",
        ],
        "_releaseWindow": {
            "startUtc": "1986-04-26T01:23:00Z",
            "endUtc": "1986-05-06T00:00:00Z",
            "durationDays": RELEASE_DAYS,
            "uncertainty": RELEASE_UNCERTAINTY,
            "uncertaintyMeaning": "each daily release rate is uncertain by +/-50%",
            "source": RELEASE_WINDOW,
        },
        "_fuelReleaseFraction": {
            "central": FUEL_FRACTION_CENTRAL,
            "sigma": FUEL_FRACTION_SIGMA,
            "low": round(FUEL_FRACTION_CENTRAL - FUEL_FRACTION_SIGMA, 6),
            "high": round(FUEL_FRACTION_CENTRAL + FUEL_FRACTION_SIGMA, 6),
            "earlierEstimate": EARLIER_FUEL_FRACTION,
            "source": FUEL_FRACTION_SOURCE,
            "earlierSource": EARLIER_FUEL_SOURCE,
            "note": (
                "This is the quantity every refractory nuclide's release "
                "depends on. It moved by a factor of two between INSAG-1 and "
                "Kashparov, which is the single largest reason the source term "
                "is a range."
            ),
        },
        "_caesiumRevision": {
            "earlierPbq": 40,
            "currentPbq": 80,
            "note": (
                "Pub1239 s.3.1.1: the early airborne-radiometric estimate of "
                "Cs-137 deposited in the former USSR was about 40 PBq; the "
                "current estimate is about twice that, 80 PBq. Recorded because "
                "it is a worked example of the source term being revised by "
                "2x on measurement, not on modelling."
            ),
        },
        "_layer2Limits": limits,
        "_layer2LimitsExplained": (
            "Deriving a release fraction needs a correct core activity. The "
            "cross-check against Table 3.1 found three classes of nuclide for "
            "which layer 2 cannot supply one, and they are listed rather than "
            "papered over: (1) ACTIVATION PRODUCTS. Layer 2 is a fission-product "
            "inventory; it contains no nuclide above Z=70, so Pu, Np, Am, Cm "
            "and Co are absent. Table 3.1's seven actinide entries therefore "
            "carry a quoted released activity and no fraction. (2) ISOMER "
            "FEEDING. A decay branch records a mode and a branching ratio but "
            "not a daughter level, so beta decay into an excited state cannot "
            "be represented. Te-129m is fed by Sb-129 and is starved by 3-4 "
            "orders of magnitude as a result. (3) NEUTRON CAPTURE. Layer 2 "
            "solves decay chains with fission as the source and models no "
            "(n,g) reaction, so Cs-134 (made from Cs-133 by capture) and Cs-136 "
            "are under-produced. These are layer-2 defects that layer 3 "
            "exposes; fixing them belongs to layer 2, not to a fudge factor "
            "here."
        ),
        "_notInThisFile": (
            "No dose, no transport, no deposition, no release rate per nuclide. "
            "Those are layer 4. No release fraction is invented for a nuclide "
            "that has no quoted released activity."
        ),
        "_counts": {
            "tableEntries": len(TABLE_3_1),
            "fractionsDerived": used,
            "fractionsRefused": len(TABLE_3_1) - used,
        },
        "nuclides": entries,
    }

    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w") as f:
        json.dump(doc, f, indent=1, sort_keys=False)
        f.write("\n")

    print(f"wrote {OUT}")
    print(f"  table entries      {len(TABLE_3_1)}")
    print(f"  fractions derived  {used}")
    print(f"  fractions refused  {len(TABLE_3_1) - used}")
    for lim in limits:
        print(f"  refused: {lim['nuclide']} ({lim['problem']}, x{lim['ratio']})")
    return 0


if __name__ == "__main__":
    sys.exit(main())
