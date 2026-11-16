# The ROSFOND swap, measured — and what it actually shows

Measurement note, 2026-11-15. Written because the Director asked for the ROSFOND
swap to be done, and **doing it properly first required measuring it** — and the
measurement says the swap is *not* a file change. No physics was shipped: this
note is the result of the experiment, not a change to the reactor.

## 1. What was asked, and what was done

The standing task (branch `recover/rosfond-data-path`, `docs/ROSFOND-DATA-PATH.md`)
was the swap in **two** parts:

1. **Data:** `rbmk_lattice.json` from ENDF/B-VIII.0 → ROSFOND-2010.
2. **Method:** remove the INSAG-7 anchoring, so the ROSFOND-derived void worth
   stands on its own and INSAG-7 is printed beside it.

Both parts were carried out and **measured**, then reverted, so the shipped physics
is unchanged. The data needed was already present: `tools/lattice/rosfond_raw/`
holds 27 genuine ROSFOND-2010 ENDF-6 files (`Retrieved by E4-util: 2010/02/01`,
`present: 27, missing: 0`), and a completed emission existed at
`/workspace/rosfond_lattice_emitted.json` (`library: ROSFOND-2010`).

## 2. The measurement — four configurations

Whole-core void worth `dk/dvoid` at the working point, and the 30 s power hold at
nominal, from `DerivedVoidSelfTest`:

| Lattice data | `VOID_WORTH_SCALE` | dk/dvoid | 30 s hold | verdict |
|---|---|---|---|---|
| ENDF/B-VIII.0 | 0.233 (current) | **+0.0222** | 0.3% off | stable (the shipped state) |
| ROSFOND-2010 | 0.233 | **+0.0222** | 0.3% off | stable |
| ENDF/B-VIII.0 | 1.0 (anchor off) | +0.0656 | 5.1% off | drifts |
| ROSFOND-2010 | 1.0 (anchor off) | **+0.1215** | **29.7% off** | **runs away** |

Cell-level void worth (computed from the two lattice files):

| Lattice | cell `dkInf/dvoid` |
|---|---|
| ENDF/B-VIII.0 | 0.0817 |
| ROSFOND-2010 | 0.1301 (**1.59× higher**) |

## 3. What this shows — three findings

**Finding 1 — the anchor, not the data, sets the void worth.**
With `VOID_WORTH_SCALE = 0.233` still in, ENDF and ROSFOND give the **identical**
+0.0222. Swapping the data alone therefore changes *nothing* observable: the anchor
absorbs it. This is exactly the "ROSFOND bent back to INSAG-7" trap the design note
warned about — and it is confirmed by measurement, not argument.

**Finding 2 — ROSFOND's own void worth is HIGHER, not lower.**
With the anchor removed, ENDF gives +0.0656 and **ROSFOND gives +0.1215** — roughly
**double** ENDF, and ~6× INSAG-7's +0.020. So the ROSFOND data does not bring the
derived value *toward* the source value; it moves it **further away**. Removing the
anchor on ROSFOND data destabilises the core (30% power drift in 30 s).

**Finding 3 — the anchor is compensating for a real defect in the cell model.**
`RbmkXsec` says so itself: *"the homogenised cell's raw feedbacks are far stronger
than a real RBMK's, because a one-node cell concentrates the whole effect."* The
one-node homogenised cell **overstates** the void feedback — ENDF by ~4×, ROSFOND by
~6×. The anchor is a fudge, but it is a fudge **on top of a genuine modelling
defect**, not on top of correct physics.

## 4. The consequence — the honest position

The swap as specified is **not the fix**. Done fully (data + anchor removed) it
gives a core that runs away, and that runaway is **not the reactor's behaviour** —
it is the one-node cell's artifact showing through. So:

- **Swapping the data and keeping the anchor** = ROSFOND bent back to INSAG-7 = the
  worst of both. Rejected.
- **Swapping the data and removing the anchor** = a runaway core = a cell-model
  defect exposed, not a reactor. Rejected as a *shipped* state.
- **The real work** is the **lattice cell model** — the homogenisation and the
  spectrum calculation that turn a 1-D cell into whole-core feedback. That is where
  the void worth is wrong, and it is wrong in *both* libraries. Until the cell is
  right, no data swap can be honest.

## 5. Open items (flagged, not guessed)

1. **The ROSFOND emission needs verification.** Its cell `kInf(void=0) = 1.289`,
   against ENDF's 1.133. A real RBMK cell is not that reactive; the reconstruction
   (NJOY RECONR→BROADR) may be introducing an error, or the cell geometry may need
   re-checking against the ROSFOND data. **`to_verify` before the data is trusted.**
2. **`njoy` is not installed in this sandbox**, so the reconstruction could not be
   re-run here end-to-end. The emission was accepted as found; it must be
   re-generated and checked on a machine with NJOY.
3. **The spatial core does not shut down** (`fission 0.2741, k_eff 1.0361` after a
   scram) — a known, tracked, pre-existing failure, independent of this swap, still
   open.
4. **The anchor stays** (0.233) until the cell model is fixed — but it is now
   recorded as *compensating for a known cell-model defect*, not as a neutral
   calibration.

## 6. The lesson, for the record

A sourced number beats a computed one — **but only when the computation is sound.**
Here the computation (one-node cell) is unsound, so the sourced anchor was doing
real work: hiding a defect. The fix is not to pick a side between ROSFOND and
INSAG-7; it is to **fix the cell model** so the derived value is trustworthy on its
own. Swapping data first would have moved the error, not removed it.
