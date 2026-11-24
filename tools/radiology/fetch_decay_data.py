#!/usr/bin/env python3
"""Fetch isotope decay data for REKORD from the IAEA Nuclear Data Services API.

REKORD layer 1 is decay data: for each nuclide, its half-life, its decay modes
with branching ratios, and its emissions (alpha / beta mean energy / gamma / X-ray)
with energies and intensities. Nothing here is typed by hand. Every value is
pulled from IAEA-NDS, which serves the evaluated ENSDF library, and every record
carries the ENSDF publication date and author string the API returns so the
provenance is checkable against the evaluated file itself.

Source: https://nds.iaea.org/relnsd/v1/data  (Nuclear Data Services, IAEA)
        -> decay_rads / levels endpoints, ENSDF-derived.

Why this source: REKORD's mandate (PROJECT_NOTES.md, the ARL standard) forbids
watering down radiological modelling. Half-lives and decay emissions are the one
part of the radiological problem where there is no legitimate disagreement --
they are measured quantities with evaluated uncertainties. So this layer can be
built to full fidelity today with zero guessing, which is exactly what the
project rule ("nothing is guessed") demands.

The isotope SET is not invented here either. Default set = the nuclides that
dominate an RBMK source term, grouped the way release physics groups them
(noble gases, volatiles, semi-volatiles, fuel fragments, actinides, activation
products, and the RBMK-specific iodine/xenon pair). Callers may pass their own
list; the fetcher will fetch any nuclide ENSDF knows.

Usage:
    python3 tools/radiology/fetch_decay_data.py             # default set
    python3 tools/radiology/fetch_decay_data.py Cs137 I131  # explicit
"""

from __future__ import annotations

import json
import sys
import time
import urllib.parse
import urllib.request
from pathlib import Path

API = "https://nds.iaea.org/relnsd/v1/data"

OUT = Path(__file__).resolve().parents[2] / "src/main/resources/data/csp/radiology"

# The RBMK source-term isotope set. Grouped by the release behaviour that
# decides where each one ends up, not by atomic number -- that is the
# distinction a contamination model actually needs.
DEFAULT_SET = {
    # Noble gases: not retained by the plant, released as gas.
    "noble_gas": ["Kr85", "Kr87", "Kr88", "Xe131m", "Xe133", "Xe135"],
    # Volatiles: the iodine/caesium pair that drove the off-site dose.
    "volatile": ["I129", "I131", "I132", "I133", "Cs134", "Cs136", "Cs137"],
    # Semi-volatiles: released as vapour/aerosol in a fuel-fire.
    "semi_volatile": ["Ru103", "Ru106", "Te132", "Sb125", "Ag110m"],
    # Fuel fragments: released only with fuel dispersion (hot particles).
    "particulate": ["Sr89", "Sr90", "Y90", "Y91", "Zr95", "Zr97", "Mo99",
                    "Tc99m", "Tc99", "Ba140", "La140", "Ce141", "Ce144",
                    "Pr143", "Pr144", "Nd147", "Np239", "Pu238", "Pu239",
                    "Pu240", "Pu241", "Am241", "Cm242", "Cm244", "U235",
                    "U238"],
    # Activation / structural: neutron-induced, relevant to the core inventory.
    "activation": ["H3", "C14", "Co60", "Fe55", "Ni63", "Ni59", "Zn65"],
    # RBMK process nuclides: the ones the reactor itself depends on.
    "process": ["Xe135", "Sm149", "Pm149", "I135"],
}

FIELDS_RADS = {
    "gamma": "g",
    "alpha": "a",
    "xray": "x",
    "beta": "bm",
}


def _get(params: dict[str, str]) -> str:
    url = API + "?" + urllib.parse.urlencode(params)
    req = urllib.request.Request(url, headers={"User-Agent": "csp-modern/REKORD"})
    for attempt in range(4):
        try:
            with urllib.request.urlopen(req, timeout=45) as r:
                return r.read().decode("utf-8", "replace")
        except Exception:
            if attempt == 3:
                raise
            time.sleep(2 * (attempt + 1))
    raise RuntimeError("unreachable")


def _rows(text: str) -> tuple[list[str], list[dict[str, str]]]:
    lines = [l for l in text.splitlines() if l.strip()]
    if not lines:
        return [], []
    head = lines[0].split(",")
    rows = []
    for line in lines[1:]:
        vals = line.split(",")
        if len(vals) != len(head):
            continue
        rows.append(dict(zip(head, vals)))
    return head, rows


def fetch_levels(nuclide: str) -> dict:
    """Half-life, spin-parity and decay modes for `nuclide`.

    For a ground state this is the level with no excitation energy. For a
    metastable ('m' suffix) it is the EXCITED level carrying that isomer's own
    half-life -- Tc-99m is the 142.7 keV level of Tc-99, not its ground state.
    Picking the ground state for an 'm' nuclide would quietly substitute the
    wrong half-life (Tc-99m 6.0 h vs Tc-99 211,000 y), which is exactly the kind
    of error this project cannot carry.
    """
    metastable = nuclide.endswith("m") or nuclide.endswith("M")
    # The API only accepts the GROUND nuclide (Tc99m is rejected, Tc99 works), so
    # for an isomer we query the ground nuclide and pick the excited level.
    query = nuclide[:-1] if metastable else nuclide
    _h, rows = _rows(_get({"fields": "levels", "nuclides": query}))
    if metastable:
        candidates = []
        for r in rows:
            if not (r.get("half_life_sec", "") or "").strip():
                continue
            try:
                energy = float(r.get("energy", "0") or 0)
            except ValueError:
                continue
            if energy <= 0:
                continue
            modes = {(r.get(f"decay_{i}", "") or "").strip() for i in (1, 2, 3, 4)}
            candidates.append((energy, "IT" in modes, float(r["half_life_sec"]), r))
        if not candidates:
            raise LookupError(f"{nuclide}: no excited level with a half-life")
        # The isomer is the long-lived excited state that de-excites by internal
        # transition. Picking the lowest excited level is wrong: Tc-99 has a
        # 140.5 keV level living 190 ps, while the isomer is the 142.7 keV level
        # living 6.006 h. Where no level carries IT, fall back to the longest
        # lived excited state and say so.
        it_levels = [c for c in candidates if c[1]]
        pool = it_levels or candidates
        _, has_it, _hl, g = max(pool, key=lambda c: c[2])
        isomer_mode = "IT" if has_it else "longest-lived-excited-level"
    else:
        isomer_mode = None
        ground = [r for r in rows if r.get("energy", "").strip() in ("", "0")]
        if not ground:
            raise LookupError(f"{nuclide}: no ground state in levels")
        g = ground[0]
    modes = []
    for i in (1, 2, 3):
        mode = g.get(f"decay_{i}", "").strip()
        if not mode:
            continue
        branching = g.get(f"decay_{i}_%", "").strip()
        modes.append({"mode": mode, "percent": float(branching) if branching else None})
    hl_sec = g.get("half_life_sec", "").strip()
    return {
        "nuclide": nuclide,
        "z": int(g["z"]),
        "n": int(g["n"]),
        "symbol": g["symbol"],
        "spinParity": g.get("jp", "").strip(),
        "isomer": bool(metastable),
        "isomerSelection": isomer_mode if metastable else None,
        "excitationEnergyKeV": _maybe_float(g.get("energy", "")),
        "halfLife": _parse_quantity(g.get("half_life", ""), g.get("operator_hl", ""),
                                    g.get("unit_hl", "")),
        "halfLifeSeconds": float(hl_sec) if hl_sec else None,
        "halfLifeUncertaintySeconds": _maybe_float(g.get("unc_hl", "")),
        "decayModes": modes,
        "ensdf": {
            "publicationCutoff": g.get("ENSDF_publication_cut-off", "").strip(),
            "authors": g.get("ENSDF_authors", "").strip(),
            "extractionDate": g.get("Extraction_date", "").strip(),
        },
    }


def _maybe_float(s: str):
    s = (s or "").strip()
    if not s:
        return None
    try:
        return float(s)
    except ValueError:
        return None


def _parse_quantity(value: str, operator: str, unit: str):
    """ENSDF half-life as printed: 'number operator unit' is really '30.08' 'Y'
    in the display columns but the API splits the uncertainty into operator/unit."""
    value = (value or "").strip()
    return {"value": value, "unit": (unit or "").strip()} if value else None


def fetch_emissions(nuclide: str, rad_type: str, want_isomer: bool = False,
                    daughters: dict | None = None) -> list[dict]:
    query = nuclide[:-1] if (want_isomer and nuclide[-1:] in "mM") else nuclide
    text = _get({"fields": "decay_rads", "nuclides": query, "rad_types": rad_type})
    lines = [l for l in text.splitlines() if l.strip()]
    # Errors come back as a bare status digit, not a CSV header.
    if not lines or "," not in lines[0]:
        return []
    head, rows = _rows(text)
    if want_isomer:
        # The endpoint serves the ground nuclide's WHOLE decay scheme, ground
        # state and isomer together. The isomer's own emissions are the rows
        # whose decay mode is IT (internal transition); the rest belong to the
        # ground state and would be a different nuclide's spectrum.
        rows = [r for r in rows if r.get("decay", "").strip().upper() == "IT"]
    if daughters is not None:
        # Every row carries the daughter it feeds (d_z/d_n/d_symbol) and the
        # decay mode that produced it. Recording them here means the decay
        # chain topology comes from the evaluated file rather than from
        # chemistry I would otherwise be remembering.
        for r in rows:
            mode = (r.get("decay", "") or "").strip().upper()
            dz = (r.get("d_z", "") or "").strip()
            dn = (r.get("d_n", "") or "").strip()
            ds = (r.get("d_symbol", "") or "").strip()
            if not (mode and dz and dn and ds):
                continue
            z, n = int(dz), int(dn)
            daughters.setdefault((z, n), {"symbol": ds, "modes": set()})["modes"].add(mode)

    out = []
    for r in rows:
        if rad_type == "g":
            energy = _maybe_float(r.get("energy", ""))
            intensity = _maybe_float(r.get("intensity", ""))
            if energy is None:
                continue
            out.append({
                "type": "GAMMA",
                "energyMeV": round(energy / 1000.0, 9),
                "energyKeV": energy,
                "intensity": (intensity or 0.0) / 100.0,
                "intensityPercent": intensity,
            })
        elif rad_type == "a":
            energy = _maybe_float(r.get("energy", ""))
            intensity = _maybe_float(r.get("intensity", ""))
            if energy is None:
                continue
            out.append({
                "type": "ALPHA",
                "energyMeV": round(energy / 1000.0, 9),
                "energyKeV": energy,
                "intensity": (intensity or 0.0) / 100.0,
                "intensityPercent": intensity,
            })
        elif rad_type == "x":
            energy = _maybe_float(r.get("energy", ""))
            intensity = _maybe_float(r.get("intensity", ""))
            if energy is None:
                continue
            out.append({
                "type": "XRAY",
                "energyMeV": round(energy / 1000.0, 9),
                "energyKeV": energy,
                "intensity": (intensity or 0.0) / 100.0,
                "intensityPercent": intensity,
                "shell": r.get("shell", "").strip(),
            })
        elif rad_type == "bm":
            mean = _maybe_float(r.get("mean_energy", ""))
            imax = _maybe_float(r.get("max_energy", ""))
            intensity = _maybe_float(r.get("intensity_beta", ""))
            if mean is None:
                continue
            out.append({
                "type": "BETA",
                "energyMeV": round(mean / 1000.0, 9),
                "energyKeV": mean,
                "maxEnergyKeV": imax,
                "intensity": (intensity or 0.0) / 100.0,
                "intensityPercent": intensity,
            })
    return out


def _merge_duplicate_emissions(emissions: list[dict]) -> list[dict]:
    """Collapse rows sharing the same type and energy.

    ENSDF lists some transitions more than once: the same gamma can appear as the
    evaluated main line and again as a weak branch or an unassigned duplicate.
    Summing them is only safe for independent channels; for the same transition
    listed twice it double-counts dose. The API gives no reliable flag to tell
    the cases apart, so the honest handling is to keep the largest single
    intensity and record how many rows were folded and what was discarded --
    never silently add. Where the discarded sum is material the isotope is
    reported so a human can check it against the evaluated file.
    """
    merged: dict[tuple, dict] = {}
    for e in emissions:
        key = (e["type"], round(e["energyKeV"], 3))
        if key not in merged:
            merged[key] = dict(e, duplicateRows=1, discardedIntensityPercent=0.0)
            continue
        kept = merged[key]
        kept["duplicateRows"] += 1
        # Keep the STRONGEST listing, not the first one seen. ENSDF can repeat a
        # transition, and the repeats are not ordered by strength: I-132's
        # 667.7 keV line appears once at 13.9% and once at 98.7%, which is the
        # real intensity. Keeping the first row would understate the dose from
        # that isotope by a factor of seven.
        kept["discardedIntensityPercent"] = round(
            kept["discardedIntensityPercent"] + (kept["intensityPercent"] or 0.0), 6)
        if (e["intensityPercent"] or 0.0) > (kept["intensityPercent"] or 0.0):
            kept.update({k: e[k] for k in e if k != "intensityPercent"})
            kept["intensityPercent"] = e["intensityPercent"]
            kept["intensity"] = e["intensity"]
    return list(merged.values())


def _dedupe_xray_against_gamma(emissions: list[dict]) -> list[dict]:
    """Drop GAMMA rows that are really X-rays already listed as XRAY.

    The gamma endpoint of IAEA-NDS returns atomic X-rays alongside nuclear
    gammas: Co-60's 7.677 keV row appears in BOTH the gamma list and the X-ray
    list with an identical 3.815% intensity. Physically that is one photon.
    A dose model that summed both lists would count it twice, so the duplicate
    is removed here and the XRAY row is kept, because it carries the shell
    ('KA1', 'KB', ...) that identifies it as atomic.
    """
    xray_energies = {round(e["energyKeV"], 3) for e in emissions if e["type"] == "XRAY"}
    out = []
    dropped = 0
    for e in emissions:
        if e["type"] == "GAMMA" and round(e["energyKeV"], 3) in xray_energies:
            dropped += 1
            continue
        out.append(e)
    if dropped and out:
        out[0]["gammaRowsAlsoListedAsXray"] = dropped
    return out


def build(nuclides: list[str]) -> dict:
    groups = {}
    for group, members in DEFAULT_SET.items():
        for m in members:
            groups.setdefault(m, group)

    isotopes = []
    failures = []
    for n in nuclides:
        try:
            base = fetch_levels(n)
            emissions = []
            is_isomer = n[-1:] in "mM"
            raw_daughters: dict = {}
            for rt in FIELDS_RADS.values():
                emissions.extend(fetch_emissions(n, rt, want_isomer=is_isomer,
                                                 daughters=raw_daughters))
            emissions = _merge_duplicate_emissions(emissions)
            emissions = _dedupe_xray_against_gamma(emissions)
            emissions.sort(key=lambda e: (e["type"], -e["energyKeV"]))
            base["group"] = groups.get(n, "unspecified")
            self_z = base["z"]
            self_n = base["n"]
            base["decayProducts"] = sorted(
                ({"z": z, "n": nn, "symbol": d["symbol"],
                  "nuclide": f"{d['symbol']}{z + nn}",
                  "modes": sorted(d["modes"])}
                 for (z, nn), d in raw_daughters.items()
                 if (z, nn) != (self_z, self_n)),
                key=lambda d: d["nuclide"])
            base["emissions"] = emissions
            base["stable"] = base.get("halfLifeSeconds") is None
            material = [e for e in emissions
                        if e.get("discardedIntensityPercent", 0.0) >= 0.5]
            base["discardedMaterial"] = [
                {"type": e["type"], "energyKeV": e["energyKeV"],
                 "keptPercent": e["intensityPercent"],
                 "discardedPercent": e["discardedIntensityPercent"],
                 "rows": e["duplicateRows"]}
                for e in material
            ]
            base["source"] = (
                "IAEA Nuclear Data Services, ENSDF-derived: "
                f"https://nds.iaea.org/relnsd/v1/data?fields=levels&nuclides={n}"
            )
            isotopes.append(base)
            state = "STABLE" if base["stable"] else f"hl={base['halfLifeSeconds']:.6g}s"
            print(f"  ok  {n:8s} {state:22s} "
                  f"modes={[m['mode'] for m in base['decayModes']]} "
                  f"emissions={len(emissions)}")
        except Exception as e:
            failures.append({"nuclide": n, "error": str(e)})
            print(f"  FAIL {n}: {e}", file=sys.stderr)
        time.sleep(0.3)

    return {
        "_standard": "ARL (Above Real Life) — PROJECT_NOTES.md",
        "_layer": "REKORD layer 1: nuclide decay data",
        "_source": "IAEA Nuclear Data Services (ENSDF), https://nds.iaea.org/relnsd/v1/data",
        "_note": (
            "Every half-life, decay mode, branching ratio and emission energy here "
            "is an evaluated measurement fetched from ENSDF via IAEA-NDS. Nothing "
            "in this file is estimated or reconstructed. The ENSDF publication "
            "date and author string are carried per isotope so any value can be "
            "checked against the evaluated file."
        ),
        "_notInThisFile": (
            "Activity (Bq) is NOT here: it depends on how many atoms are present, "
            "which is a reactor-physics question, not a nuclear-data question. "
            "That is REKORD layer 2 (inventory). The release fraction that turns "
            "an inventory into a source term is layer 3 and is genuinely "
            "contested; it must be sourced as a range."
        ),
        "isotopeCount": len(isotopes),
        "isotopes": isotopes,
        "failures": failures,
    }


def main(argv: list[str]) -> int:
    explicit = [a for a in argv[1:] if not a.startswith("-")]
    if explicit:
        nuclides = explicit
    else:
        nuclides = sorted({n for members in DEFAULT_SET.values() for n in members},
                          key=lambda s: (len(s), s))
    print(f"fetching {len(nuclides)} nuclides from IAEA-NDS/ENSDF")
    doc = build(nuclides)
    OUT.mkdir(parents=True, exist_ok=True)
    target = OUT / "isotopes.json"
    target.write_text(json.dumps(doc, indent=2) + "\n", encoding="utf-8")
    print(f"\nwrote {target} ({doc['isotopeCount']} isotopes, "
          f"{len(doc['failures'])} failures)")
    return 1 if doc["failures"] else 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
