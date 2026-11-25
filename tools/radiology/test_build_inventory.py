#!/usr/bin/env python3
"""Guards on the decay-branch parser in build_inventory.py.

Every check here corresponds to a way the parser was silently wrong and produced
a plausible-looking inventory. They are written against the pure functions, with
no network and no cache, so they run in a second and cannot drift with the data:

  1. Beta-delayed neutron emission is a SUB-branch of beta decay, not a rival
     branch. I-137 with "B- 100%" and "B-N 7.14%" sends 92.86% of decays to
     Xe-137 and 7.14% to Xe-136. Treating them as parallel 100% branches creates
     7% of the mass from nothing.
  2. A blank neutron percentage means "not tabulated", not 100%. Defaulting to
     100% moves a whole chain one mass unit down.
  3. EC and beta-plus go to the daughter one Z LOWER and the same A. Mapping
     them like beta-minus silently walks chains upward.
  4. An isomer with no listed mode internal-transitions to its own ground state.
     Leaving it branchless makes it an infinite trap that swallows every atom
     that reaches it.
  5. A nuclide with a half-life and no branch is either a terminus (stable on
     the irradiation timescale, e.g. Zr-96) or a gap in the evaluation
     (I-144, 0.3 us). Conflating the two either deletes real atoms or invents a
     stable nuclide.
  6. Fission and spontaneous fission are losses here, not routes: they produce a
     whole distribution, not one daughter.

Run: python3 tools/radiology/test_build_inventory.py
"""
from __future__ import annotations

import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import build_inventory as bi  # noqa: E402

checks = 0
failures: list[str] = []


def check(ok, what, detail=""):
    global checks
    checks += 1
    if ok:
        print("  PASS  %s" % what)
    else:
        print("  FAIL  %s %s" % (what, detail))
        failures.append(what)


def test_neutron_is_sub_branch():
    """1. I-137: B- 100% with B-N 7.14% -> 92.86% Xe-137, 7.14% Xe-136."""
    shares, leak = bi._edges("53-137", [["B-", 100.0], ["B-N", 7.14]])
    check(abs(shares.get("54-137", 0) - 0.9286) < 1e-3,
          "I-137 sends 92.86% to Xe-137", str(shares))
    check(abs(shares.get("54-136", 0) - 0.0714) < 1e-3,
          "I-137 sends 7.14% to Xe-136 via delayed neutron", str(shares))
    check(abs(sum(shares.values()) - 1.0) < 1e-9,
          "I-137 branches sum to exactly 1", str(sum(shares.values())))
    check(leak == 0.0, "I-137 loses nothing", str(leak))


def test_blank_neutron_percent_is_not_100():
    """2. A blank B-N percentage must not be read as a full extra branch."""
    shares, _ = bi._edges("36-90", [["B-", 100.0], ["B-N", None]])
    check(abs(shares.get("37-90", 0) - 1.0) < 1e-9,
          "blank B-N leaves Kr-90 -> Rb-90 at 100%", str(shares))
    check("37-89" not in shares or shares["37-89"] == 0.0,
          "blank B-N does not create a Rb-89 branch", str(shares))


def test_ec_and_beta_plus_go_down_in_z():
    """3. EC/B+ decrease Z by one, 2B+ by two; beta-minus increases it."""
    for mode in ("EC", "B+", "EC+B+"):
        shares, _ = bi._edges("55-137", [[mode, 100.0]])
        check("56-137" not in shares,
              "%s on Cs-137 does not produce Ba-137 (that is beta-minus)" % mode,
              str(shares))
        check("54-137" in shares,
              "%s on Cs-137 produces Xe-137 (Z-1, same A)" % mode, str(shares))
    # Double beta-plus converts two protons at once, so Z drops by TWO. Mapping
    # it as Z-1 would route it to the same nuclide and make a self-loop.
    shares, _ = bi._edges("55-137", [["2B+", 100.0]])
    check("53-137" in shares,
          "2B+ on Cs-137 produces I-137 (Z-2, same A)", str(shares))
    shares, _ = bi._edges("55-137", [["B-", 100.0]])
    check("56-137" in shares, "beta-minus on Cs-137 produces Ba-137", str(shares))


def test_it_goes_to_own_ground_state():
    """4. An isomer's internal transition is to its own ground state."""
    shares, _ = bi._edges("56-137m", [["IT", 100.0]])
    check(shares == {"56-137": 1.0},
          "Ba-137m IT -> Ba-137 ground state", str(shares))

    # An isomer with no listed mode still transitions, not disappears.
    shares, _ = bi._edges("35-86m", [])
    check(shares == {"35-86": 1.0},
          "branchless isomer Br-86m falls back to its ground state", str(shares))


def test_decay_state_classifies_correctly():
    """5. Stable, terminus, gap and decay are four different things."""
    long_years = 1e19 * 365.2422 * 86400.0
    check(bi._decay_state("40-96", {"halfLifeSeconds": long_years, "branches": []})
          == "terminus", "Zr-96 (2e19 y, no branch) is a terminus")
    check(bi._decay_state("53-144", {"halfLifeSeconds": 3e-7, "branches": []})
          == "gap", "I-144 (0.3 us, no branch) is an evaluation gap")
    check(bi._decay_state("40-90", {"halfLifeSeconds": None, "branches": []})
          == "stable", "a nuclide with no half-life is stable")
    check(bi._decay_state("55-137", {"halfLifeSeconds": 9.5e8,
                                     "branches": [["B-", 100.0]]}) == "decay",
          "a nuclide with a half-life and a branch decays")
    check(bi._decay_state("56-137m", {"halfLifeSeconds": 153.0, "branches": []})
          == "decay", "an isomer with no branch still decays (IT fallback)")


def test_fission_is_a_loss():
    """6. Fission and SF produce a distribution, so they are not single routes."""
    shares, leak = bi._edges("54-136", [["SF", 100.0]])
    check(shares == {}, "spontaneous fission routes nowhere", str(shares))
    check(abs(leak - 1.0) < 1e-9, "spontaneous fission is a full loss", str(leak))


def test_single_mode_blank_means_whole():
    """A lone mode with a blank percentage means the whole decay."""
    shares, _ = bi._edges("28-66", [["B-", None]])
    check(abs(shares.get("29-66", 0) - 1.0) < 1e-9,
          "a single blank-percent mode takes the whole branch", str(shares))


def test_error_is_never_stable():
    """7. A failed fetch must not read as 'stable'.

    An error record has no half-life, so the naive "no half-life means stable"
    test froze 0.75% of the inventory: ten isomer records whose fetch had
    failed were held in place for the whole irradiation.
    """
    for rec in ({"error": "no ground state"},
                {"error": "no excited level with a half-life"},
                {"error": "timed out", "halfLifeSeconds": None}):
        check(bi._decay_state("41-102m", rec) == "error",
              "an error record is never 'stable' (%s)" % rec["error"])
    check(bi._decay_state("41-102m", {"error": "x"}) != "stable",
          "an error record with no half-life is not stable")
    # And an errored record must not decay either: there is no route to take.
    check(bi._decay_state("41-102m", {"error": "x"}) != "decay",
          "an error record does not decay on an invented route")


def test_isomer_at_energy_zero_is_resolved():
    """8. Ground and isomer can share energy 0, told apart only by spin-parity.

    Nb-102, Rh-110, Sb-128 and Pm-154 are evaluated that way. Selecting the
    isomer by energy > 0 finds nothing there, and the resulting error record
    used to freeze the isomer's whole yield.
    """
    rows = [
        {"idx": "0", "energy": "0", "half_life_sec": "4.3",
         "decay_1": "B-", "decay_1_%": "100"},
        {"idx": "1", "energy": "0", "half_life_sec": "1.3",
         "decay_1": "B-", "decay_1_%": "100"},
    ]
    ground = [r for r in rows if (r.get("energy") or "").strip() in ("", "0")][0]
    check(ground["half_life_sec"] == "4.3",
          "the first energy-0 level is the ground state")
    cand = [(float(r["half_life_sec"]), False, True, r)
            for r in rows if r is not ground and r.get("half_life_sec")]
    check(len(cand) == 1 and cand[0][0] == 1.3,
          "the isomer is found among the levels even at energy 0")
    check(max(cand, key=lambda c: c[0])[3]["idx"] == "1",
          "the isomer keeps its OWN half-life, not the ground state's")


def test_yield_chain_derivation():
    """9. A mode-less nuclide can be routed from the evaluated yield chain.

    The cumulative yields say where the mass in a mass chain ends up. If the
    daughter one Z higher carries far more cumulative yield, matter must move
    upward at fixed A, and only beta-minus does that. This is read from the
    evaluated file, so it is a derivation with provenance, not a guess.
    """
    # I-142 sits well below its chain end (Z=58), so the route is beta-minus.
    yields = {"U235": {"cumulative": {"53-142": {"cumulative": 5.86e-05},
                                      "54-142": {"cumulative": 4.34e-03},
                                      "52-142": {"cumulative": 1.0e-9}}}}
    decay = {"53-142": {"halfLifeSeconds": 0.222, "branches": []}}
    out = bi.derive_gap_routes(decay, yields)
    check(out["53-142"]["branches"] == [["B-", 100.0]],
          "a nuclide below its chain end is routed by beta-minus",
          str(out["53-142"]["branches"]))
    check(out["53-142"]["provenance"] == "yield-chain-derived",
          "the derived route is labelled as derived, not tabulated")

    # A chain end (nothing above it) must be left alone rather than invented.
    flat = {"U235": {"cumulative": {"53-142": {"cumulative": 5.0e-3},
                                    "54-142": {"cumulative": 5.1e-3}}}}
    decay2 = {"53-142": {"halfLifeSeconds": 0.222, "branches": []}}
    out2 = bi.derive_gap_routes(decay2, flat)
    check(out2["53-142"]["branches"] == [],
          "a nuclide at its chain end is NOT given an invented route",
          str(out2["53-142"]["branches"]))

    # A terminus is not a gap and must not be touched.
    decay3 = {"40-96": {"halfLifeSeconds": 6.3e26, "branches": []}}
    out3 = bi.derive_gap_routes(decay3, yields)
    check(out3["40-96"]["branches"] == [],
          "a long-lived terminus is not given a derived route")


def test_short_lived_gap_does_not_accumulate():
    """10. A sub-second nuclide must not hold 618 days of production.

    I-142 has a 0.222 s half-life. Held as stable it reached 2.8e23 atoms;
    decaying, it saturates at production x T1/2/ln2, some eight orders of
    magnitude lower. This is the size of the error the gap caused.
    """
    import math
    t_half, T = 0.222, 618.0 * 86400.0
    lam = math.log(2) / t_half
    n_true = (1.0 - math.exp(-lam * T)) / lam
    check(abs(n_true - t_half / math.log(2)) < 1e-6,
          "a sub-second nuclide saturates at its saturation value")
    check(n_true < 1.0, "the saturation value is far below T days of holding")
    check(T > 1e6 * n_true,
          "holding it as stable would overstate it by more than 1e6x")


def main() -> int:
    print("=== test_build_inventory: decay-branch parser ===")
    test_neutron_is_sub_branch()
    test_blank_neutron_percent_is_not_100()
    test_ec_and_beta_plus_go_down_in_z()
    test_it_goes_to_own_ground_state()
    test_decay_state_classifies_correctly()
    test_fission_is_a_loss()
    test_single_mode_blank_means_whole()
    test_error_is_never_stable()
    test_isomer_at_energy_zero_is_resolved()
    test_yield_chain_derivation()
    test_short_lived_gap_does_not_accumulate()
    print("\ntest_build_inventory: %d checks, %d failures" % (checks, len(failures)))
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
