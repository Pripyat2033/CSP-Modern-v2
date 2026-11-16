# Source trust hierarchy — and the ROSFOND data path

Ben's decision, 2026-11-15: **INSAG-7 is not trusted; ROSFOND and Soviet primary
sources are.** This note records the hierarchy, what the model uses today, the
consequences of the decision, and the exact path to swap the data.

## 1. The trust hierarchy

| Rank | Source class | Examples | Why |
|---|---|---|---|
| 1 | **Soviet / Russian primary** | NIKIET design docs, the 1Э-С-II-1983 operating regulation, SKALA restoration material, ROSFOND | The people who **built and ran** the RBMK. They are the only ones who can be right about it. |
| 2 | **Soviet/Russian evaluated data** | **ROSFOND-2010**, ABBN-RF | The Russian national evaluated nuclear data library (IPPE lineage) — the physics the designers would have used. |
| 3 | Western evaluated data | ENDF/B-VIII.0, JEF, JEFF | Competent, but a **different** evaluation — not the RBMK's own. A stand-in, not the truth. |
| 4 | **Secondary analyses** | **INSAG-7**, NUREG-1250 | Written **about** the accident by outside agencies after the fact. Useful for orientation; **not** authoritative. |

Ben's rule: **matching INSAG-7 proves internal consistency with a secondary
source — it does not prove correctness.** The only real test is agreement with the
Soviet primary record or ROSFOND-derived physics.

## 2. What the model uses today

- **Cross-sections:** ENDF/B-VIII.0 (rank 3), via the openmc-data-storage JSON
  mirror. Recorded in `provenance.json` as `lattice.library` = `ENDF/B-VIII.0`,
  status `to_verify`, with "AGREED TARGET: ROSFOND / ABBN-RF (NIKIET lineage)".
- **Anchors:** several values are **anchored to INSAG-7** (rank 4). Most important:
  - `feedback.void_coeff` = +0.020, "verified" against INSAG-7 Table II-I;
  - `xsec.void_worth_scale` = 0.233 — the **derived** lattice void worth is
    *blended down* by this factor so the whole-core result matches INSAG-7;
  - `delayed.beta_eff` = 0.0065, "RBMK literature", to_verify.
- The shipped `rbmk_lattice.json` is therefore **ENDF-derived and INSAG-anchored**.

## 3. The consequence of Ben's decision — this is the real point

If INSAG-7 is not trusted, then **the anchoring itself is the defect.** The model
currently *forces* the derived physics to agree with INSAG-7 (the `void_worth_scale`
blend). Under Ben's hierarchy that is backwards: the **ROSFOND-derived value should
stand on its own**, and INSAG-7 should become a **cross-check** we may ignore when
it disagrees.

So the swap is **two changes, not one**:

1. **Data swap:** ENDF/B-VIII.0 → ROSFOND-2010.
2. **Method change:** remove (or make optional) the INSAG-7 anchoring, so the
   derived void worth, Doppler and beta are reported **as ROSFOND says**, with
   INSAG-7 printed beside them as a comparison rather than used as a target.

Doing (1) without (2) would produce ROSFOND cross-sections *bent back to INSAG-7* —
the worst of both.

## 4. The data path (already built, already tested)

Every stage exists in the repo and its self-tests pass in this sandbox:

```
ROSFOND zip  ─► import_rosfond.py ─► fetch_rosfond.py ─► emit_constants.py ─► rbmk_lattice.json ─► gradle build
 (your PC)      (unpack, name by        (ENDF-6 → per-        (lattice solve,        (the file the
                ENDF head record)       nuclide JSON)        --source rosfond)       Java core loads)
```

- `tools/lattice/import_rosfond.py` — unpacks a `.zip`/dir of ENDF-6 files and
  names each by its ENDF head record (MF=1, MT=451). Self-test: **8/8 pass.**
- `tools/lattice/fetch_rosfond.py` — converts the reactions the lattice needs.
  `--list` currently reports **present: 0, missing: 28** (waiting for the files).
- `tools/lattice/emit_constants.py --source rosfond` — writes `rbmk_lattice.json`.
- **28 nuclides** are needed: U235/238/234/236, Pu239/240/241, O16, H1, C12/13,
  N14, Zr90/91/92/94/96, Nb93, B10/11, Fe56, Ni58, Cr52, Gd155/157, Er166/167, He4.
- The OpenMC path (`tools/openmc/rosfond_to_openmc.py`, NJOY `from_njoy`) is the
  alternative for a full Monte Carlo sweep. **NJOY 2016.78 and openmc 0.16.0 are
  installed and working in this sandbox.**

## 4a. STATUS — the swap was measured, and it is not the fix (2026-11-15)

The swap was carried out and measured; see **`docs/ROSFOND-SWAP-MEASURED.md`**.
Summary: with the INSAG-7 anchor still in, ENDF and ROSFOND give the *identical*
whole-core void worth (+0.0222) — the anchor absorbs the data change. With the
anchor removed, ROSFOND gives +0.1215 (vs ENDF +0.0656) and the core runs away in
30 s. The cause is the **one-node homogenised cell**, which overstates the void
feedback in *both* libraries. So the real fix is the **cell model**, not the data
swap; the anchor stays (0.233) until then, recorded as compensating for that
defect. Nothing was shipped; the reactor is unchanged.

## 5. The blocker — where the data lives

**This sandbox is a cloud machine, NOT Ben's PC.** Verified: no Windows/WSL
mounts, hostname `runtime-…`, home `/home/openhands`. Ben's ROSFOND zip is on his
Windows machine and is **not reachable from here**.

Also verified: ROSFOND **cannot be downloaded from the sandbox** — the IPPE /
Rosatom / IAEA / OECD-NEA portals return 403 or are unreachable, and no public
mirror carries the evaluated files. So the data must be **brought in**.

## 6. What to do — the decision

Two viable routes:

- **Route A (recommended): bring the ROSFOND zip into this sandbox.** Drop the
  file into the workspace (attach it to the conversation, or copy it to
  `/workspace/`). I then run the whole chain here, with the method change of §3,
  and commit a ROSFOND-sourced `rbmk_lattice.json` plus the provenance update.
  This keeps the work here, tested, with nothing for Ben to run.
- **Route B: run it on Ben's WSL2.** `docs/OPENMC-WINDOWS-WSL.md` and
  `docs/DO-I-NEED-OPENMC-AND-ROSFOND.md` already carry the commands. Ben does the
  work; I cannot see or verify the result directly.

**Open questions for Ben:** (a) which route; (b) is the zip the full ROSFOND-2010
or a subset; (c) is it the per-isotope ENDF-6 form `import_rosfond.py` expects.
