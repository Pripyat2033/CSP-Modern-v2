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


def main() -> int:
    print("=== test_build_inventory: decay-branch parser ===")
    test_neutron_is_sub_branch()
    test_blank_neutron_percent_is_not_100()
    test_ec_and_beta_plus_go_down_in_z()
    test_it_goes_to_own_ground_state()
    test_decay_state_classifies_correctly()
    test_fission_is_a_loss()
    test_single_mode_blank_means_whole()
    print("\ntest_build_inventory: %d checks, %d failures" % (checks, len(failures)))
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
