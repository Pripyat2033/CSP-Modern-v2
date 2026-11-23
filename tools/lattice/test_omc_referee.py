#!/usr/bin/env python3
"""Checks for the OpenMC referee (docs/K-INF-REFEREE.md).

These do NOT re-run OpenMC - a transport run is minutes, not seconds. They lock
down the parts that are cheap and that would silently rot:

  * the referee module imports and exposes its geometry constants;
  * the in-house solver still returns the number the doc quotes, so a solver
    change shows up here instead of only in a stale markdown table;
  * the rod-worth and pcm arithmetic the doc's tables are built from.

Run:  python3 tools/lattice/test_omc_referee.py
"""
import os
import sys
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

import omc_referee as ref  # noqa: E402


class GeometryConstants(unittest.TestCase):
    """The referee's dimensions must match docs/RBMK-CORE-GEOMETRY.md."""

    def test_pitch_and_bore(self):
        self.assertAlmostEqual(ref.PITCH_CM, 25.0)
        self.assertAlmostEqual(ref.BORE_R, 5.7)
        self.assertAlmostEqual(ref.TUBE_IR, 4.4)
        self.assertAlmostEqual(ref.TUBE_OR, 4.8)

    def test_fuel_rod_dimensions(self):
        self.assertAlmostEqual(ref.ROD_R, 0.68)
        self.assertAlmostEqual(ref.PELLET_R, 0.575)
        self.assertAlmostEqual(ref.CARRIER_R, 0.65)

    def test_uniform_spacing_ratio(self):
        """6 inner + 12 outer rods with equal nearest-neighbour spacing.

        inner-inner chord = r1 (6 rods), outer-outer = 2 r2 sin15, so equal
        spacing forces r2 = 2 r1 cos15. This is geometry, not a source value.
        """
        import math
        self.assertAlmostEqual(ref.UNIFORM_RATIO, 2 * math.cos(math.radians(15)),
                               places=9)
        self.assertAlmostEqual(ref.UNIFORM_RATIO, 1.93185, places=5)

    def test_derived_ring_family_is_a_narrow_band(self):
        """The bore bounds the uniform-spacing family to a finite interval.

        If this ever widens dramatically the doc's +/-3000 pcm claim is wrong.
        """
        lo, hi = ref.ring_family()
        self.assertGreater(lo, 1.0)
        self.assertLess(hi, 2.5)
        self.assertLess(hi - lo, 1.0)

    def test_default_radii_sit_inside_the_family_but_are_not_uniform(self):
        """1.80/3.20 is a legal layout, just not the uniform one.

        Pinned so nobody silently "fixes" the default to look consistent.
        """
        import math
        lo, hi = ref.ring_family()
        self.assertTrue(lo <= ref.RING1_DEF <= hi)
        self.assertAlmostEqual(ref.RING2_DEF, 3.20)
        self.assertNotAlmostEqual(ref.RING2_DEF / ref.RING1_DEF,
                                  ref.UNIFORM_RATIO, places=2)
        # a uniform partner for r1=1.80 would be r2=3.477, still inside the bore
        self.assertLess(1.80 * ref.UNIFORM_RATIO + ref.ROD_R, ref.TUBE_IR)

    def test_rods_fit_inside_the_bore(self):
        """18 rods on the two rings plus the carrier, no overlap.

        Ring radii are an assumption, so this is exactly the invariant that
        keeps the assumption legal; if someone widens them, this fails.
        """
        import math
        for ring, count, offset in ((ref.RING1_DEF, 6, 0.0),
                                    (ref.RING2_DEF, 12, math.pi / 12)):
            for i in range(count):
                a = 2 * math.pi * i / count + offset
                x, y = ring * math.cos(a), ring * math.sin(a)
                r = math.hypot(x, y)
                self.assertLess(r + ref.ROD_R, ref.TUBE_IR,
                                "rod at %.3f cm pokes through the tube" % r)
            # neighbouring rods on the same ring must not touch
            if count > 1:
                sep = 2 * ring * math.sin(math.pi / count)
                self.assertGreater(sep, 2 * ref.ROD_R,
                                   "rods on the %.2f cm ring overlap" % ring)
        # the inner ring must clear the carrier
        sep = ref.RING1_DEF - ref.CARRIER_R
        self.assertGreater(sep, ref.ROD_R)


class BoreReconciliation(unittest.TestCase):
    """The three published bore numbers must reconcile to one tube.

    Repo: 88 mm called INNER.  INSAG-7: "tubes 80 mm in diameter".
    EN wiki: "8.4 cm inner, 4 mm walls".  Russian tube notation is
    outer x wall, so "88 x 4" gives 88 OD / 80 ID -- which is INSAG-7's 80.
    """

    def test_eighty_eight_by_four_gives_an_eighty_millimetre_bore(self):
        od_mm, wall_mm = 88.0, 4.0
        self.assertAlmostEqual(od_mm - 2 * wall_mm, 80.0)

    def test_repo_bore_is_the_outer_diameter_mislabelled(self):
        """TUBE_IR*20 is the repo's bore; it equals the tube OD, not the ID."""
        self.assertAlmostEqual(ref.TUBE_IR * 20.0, 88.0)
        # the true bore is the OD minus two walls
        self.assertAlmostEqual(ref.TUBE_IR * 20.0 - 2 * 4.0, 80.0)

    def test_bore_change_is_a_small_perturbation(self):
        """Measured: 88 -> 80 mm bore moves k_inf by ~148 pcm, not ~1500.

        Pinned so the doc's "148 pcm, 2.3% of the ring band" claim cannot drift
        away from the numbers it was computed from.
        """
        k_88, k_80 = 1.19207, 1.19417
        pcm = lambda k: (k - 1.0) / k * 1e5
        delta = abs(pcm(k_80) - pcm(k_88))
        self.assertAlmostEqual(delta, 148.0, delta=5.0)
        # and it must stay a small fraction of the ring-radii band
        self.assertLess(delta, 0.05 * 6400)


class InHouseSolver(unittest.TestCase):
    """The in-house numbers the doc quotes must still be reproducible."""

    @classmethod
    def setUpClass(cls):
        import endf
        import rbmk_spectrum as spec
        cls.spec = spec
        cls.hom = spec.build_fine(endf.weight_combined, 0.0)
        cls.k, cls.phi = spec.solve_k(cls.hom)

    def test_kinf_is_the_documented_value(self):
        """ENDF/B-VIII.0 fresh cell. If this moves, update the doc's table."""
        self.assertAlmostEqual(self.k, 1.34276, places=4)

    def test_kinf_is_above_the_shipped_table(self):
        """The shipped lattice table still carries the stale 1.2893."""
        self.assertGreater(self.k, 1.28932)

    def test_self_shielding_dominates(self):
        """The finding behind the whole doc: k_inf is hostage to sigma0.

        Varying the Dancoff factor must move k_inf by thousands of pcm, i.e. far
        more than the referee's statistical uncertainty. If this ever stops
        being true, the doc's argument has to be rewritten.
        """
        import rbmk_cell as cell
        orig = cell.dancoff
        try:
            cell.dancoff = lambda: 0.0
            self.spec.cell = cell
            k_dense, _ = self.spec.solve_k(
                self.spec.build_fine(__import__("endf").weight_combined, 0.0))
        finally:
            cell.dancoff = orig
            self.spec.cell = cell
        swing = abs(self.k - k_dense) * 1e5
        self.assertGreater(swing, 3000.0,
                           "self-shielding no longer dominates (%.0f pcm)" % swing)


class ReactivityArithmetic(unittest.TestCase):
    """The conversions used in the doc's implication table."""

    def test_rod_worth_matches_burnup(self):
        """The doc must use the same rod worth the burnup module uses."""
        import burnup
        self.assertAlmostEqual(burnup.ROD_WORTH, 2.0 * 0.0065 / 24.0)
        self.assertAlmostEqual(burnup.ROD_WORTH * 1e5, 54.17, delta=0.01)

    def test_pcm_and_rods(self):
        rod = 2.0 * 0.0065 / 24.0
        k = 1.19207
        p = (k - 1.0) / k * 1e5
        self.assertAlmostEqual(p, 16112, delta=2)
        self.assertAlmostEqual(p / (rod * 1e5), 297, delta=1)

    def test_referee_bracket_is_below_the_in_house_number(self):
        """The conclusion of the doc, stated as an assertion.

        Even the LOOSEST ring layout the bore allows comes out below the
        in-house solver's answer.
        """
        loose, in_house = 1.22487, 1.34276
        self.assertLess(loose, in_house)
        self.assertGreater((in_house - loose) / (in_house * loose) * 1e5, 5000)


if __name__ == "__main__":
    unittest.main(verbosity=2)
