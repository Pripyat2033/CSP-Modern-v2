# Project memory (.openhands/memory/MEMORY.md)

Curated index. Full prose: `ARCHIVE-narrative.md` + the per-day logs
`YYYY-MM-DD.md`. Keep under ~6000 chars: the TOP is truncated first.

## MISSION (Ben, 2026-11-09) — read before every decision
Forensically reconstruct the Chernobyl RBMK-1000 Unit 4 accident in Minecraft
Fabric (MC 1.20.1). **Absolute rule: "nothing is guessed"** — every constant
cited/derived or explicitly marked UNKNOWN/TO VERIFY. Ben **distrusts
INSAG-7/Western sources**; prefers ROSFOND-2010 and Russian primary sources /
designer intent. Goal: run the Unit 4 turbine rundown test and see it fail
correctly. **Accuracy is a floor, not a ceiling**; the ARL standard
(`PROJECT_NOTES.md` line 18) overrides the SGB. April-26-vs-regular-night is a
*discrimination check*; real validation is humans working the procedure.

## REKORD = 4 layers — ALL FOUR DATA LAYERS NOW DONE
1. **Decay data** — ENSDF. **DONE** (dde2d7f).
2. **Inventory at t=0** — **DONE** (d3b5a8f, c3a4ca6).
3. **Source term / release** — **DONE** (ce89754). A sourced RANGE, never one
   invented number.
4. **Transport + dose** — **DONE** (62422a2).
The old note "layer 4 needs a Linux librbmk_solver.so" was a CONFLATION: that
native lib is the 7-point neutronics stencil, nothing to do with radiology.
The 686 files in `/workspace/rosfond_uploads` are NEUTRON XS, NOT decay data.
**FORENSICS (2026-11-18): the radiation sim was NEVER written** — 469 lines
max; 8 of 28 initial radiology files were ZERO-LINE stubs. Layers 3-4 are
therefore a NEW build, not a reconnection.

## REKORD LAYER 4 — transport + dose (62422a2, 2026-11-18)
`docs/REKORD-LAYER-4-TRANSPORT-DOSE.md`. Source = **IAEA-TECDOC-1162** (2000),
the emergency assessor's handbook: fetch with curl from
`https://www-pub.iaea.org/MTCD/Publications/PDF/te_1162_prn.pdf` (reachable;
iaea.org root and EPA FGR are 403, Tavily is over quota). It tabulates exactly
the 4 things needed: **E3** ground (dose rate + 1mo/2mo/50y per kBq/m2), **E6**
inhalation, **E7** thyroid (adult+10y), **E13** ingestion, **E8** Pasquill
dilution factors 0.5-30 km.
**NOTHING IS TYPED BY HAND** — `tools/radiology/extract_tecdoc1162.py` reads the
PDF into `tecdoc1162-tables.json`; `build_dose_data.py` emits
`resources/data/csp/radiology/dose-data.json` (25 nuclides x 4 pathways).
Extraction verified against published ICRP adult ingestion coefficients —
including **Co-60/Ra-226/Th-232, which appear NOWHERE else in the project**, so
agreement can only come from reading the table right.
**CONSERVATIVE IS LABELLED.** E6/E7 are the TECDOC's "highest value from IAEA
BSS" = an UPPER BOUND; E13 reproduces ICRP adult exactly; E3 is unshielded.
The data carries `basis: conservative|adult` because presenting a bound as a
best estimate is the same lie as a guessed number.
**BUG THE TEST CAUGHT:** TECDOC shielding formula is `E*(SF*OF + (1-OF))`, NOT
`E*(SF+OF*(1-SF))` — they agree at OF=0/1 so it survived; "full shielding gives
zero" failed at 0.13 mSv.
**PDF-table trap:** pypdf returns parent+daughter on ONE row ("Cs-137+Ba-137m",
"Ru-106+Rh-106", "Mo-99+Tc-99m", "Ce-144+Pr-144"). Dropping those rows silently
LOSES 4 major nuclides; `get()` tries the chained key then the bare symbol.
**Refuses rather than invents:** dilution outside 0.5-30 km = NaN; noble gas
inhalation = NaN not 0.0 ("not credited" != "zero").
Tests: DoseSelfTest 27/27, test_build_dose_data.py 17/17.

## REKORD LAYERS 1 & 2 — DONE (dde2d7f, d3b5a8f, c3a4ca6)
Accounts: `docs/REKORD-LAYER-{1-NUCLIDE-DATA,2-INVENTORY}.md`. Neither stores
activity/release/dose; layer 2 is atoms only and REJECTS a file with activity.
**Layer 2 had 7 bugs, each producing a plausible WRONG number — do not
"simplify" the code back.** The 3 found 2026-11-18: (a) **a failed fetch read
as "stable"** — an error record has no half-life, so `if not hl: stable` froze
it (0.75% of the inventory, Nb-102m alone 5.1e25); `_decay_state` tests `error`
FIRST and the generator REFUSES to write such a file. (b) **Isomers at E=0**
(Nb-102, Rh-110, Sb-128, Pm-154): never select one by `energy > 0`.
(c) **blank mode column**: take the route from `decay_rads` `bm`, else derive
from yields. 21 nuclides have NO ENSDF evaluation (`unevaluated` state).
**L1 (54 nuclides):** isomer = the level whose `decay` column is `IT`; gamma
lines are not strength-ordered (keep STRONGEST); X-rays sit in the gamma list;
beta `bm` = MEAN.
**L2 (1130 nuclides, mass balance 0.000%):** half-lives span 1e-11..1e22 s, so
use `solve_ivp(method="Radau", rtol=1e-9, jac=M)` — `expm` loses 27% of Ru-106,
`expm_multiply` HANGS. Delayed neutron is a SUB-branch (I-137 = 92.86% Xe-137 +
7.14% Xe-136); blank B-N % = "not tabulated"; solve per CONNECTED COMPONENT, not
per mass chain; `2B+` is Z-2; loader keys by SYMBOL (`Ce144`).

## TWO ABSOLUTE RULES (cc2b88a) — ONE CORE, NO FLAG
No accident flag may decide the outcome; emergent physics only. Self-tests must
show opposite outcomes from one code path. Keystone source = the 1983 regulation
(60bf8dd): energoblok = sealed compartments; genplan per Soviet norms.

## BUILD & RUN
**A fresh sandbox has NO Java and NO numpy/scipy** (2026-11-18): `apt-get` has no
openjdk-21, so download Temurin
(`api.adoptium.net/v3/binary/latest/21/ga/linux/x64/jdk/hotspot/normal/eclipse`),
`tar xzf`; `pip install numpy scipy`. `./gradlew --offline` FAILS (foojay);
plain `./gradlew compileJava` works. Self-tests are plain `main()` classes;
without MC on the classpath compile ONLY InventoryLoader/Isotope/IsotopeLoader/
IsotopeRegistry/RadiationType + tests. **Two pre-existing failures, NOT yours**:
`zone.MaterialSupplySelfTest`, `physics.SpatialReactorCoreSelfTest`.

## GOTCHAS THAT COST REAL TIME
- **Minecraft/Fabric/OpenMC toolchain: SETTLED, do not revisit or report on.**
  Ben considers dependency talk a waste of time; never raise it unprompted.
- `tools/gen_*.py` have **no arg parsing** — running one "to see" writes dozens
  of files into `src/main/resources/assets/`. Undo: `git clean -fdq` that dir.
- The fabric-loom lock can be held by an unreapable zombie gradle → fresh
  `GRADLE_USER_HOME`; kill by PID, never `pkill -f gradle`. The wrapper jar is
  gitignored; fetch it. A first `compileJava` may sit minutes downloading the
  JDK 17 toolchain — not a hang.
- Tavily is over quota — use `curl` + `pypdf`.
- A vanished sandbox is recoverable by replaying `/tmp/traj/event_*.json` into a
  clean worktree — `2026-11-15.md`.

## SOURCES ON HAND
INSAG-7 `www-pub.iaea.org/MTCD/Publications/PDF/Pub913e_web.pdf` (Table II-II =
equipment list spine); NUREG-1250 `/workspace/tools/nureg1250.pdf`; ENDF/B-VIII.0
HDF5 `/workspace/tools/openmc_data/endfb-viii.0-hdf5`; IAEA NDS
`nds.iaea.org/relnsd/v1/data` (one nuclide at a time, needs a User-Agent).

## ALREADY SOLVED — don't redo or re-research
- **ROSFOND path** (`2026-11-15.md`, daaed39): MF=3 pointwise XS is ZERO below
  the resonance range, so raw you get a moderator-only cell. Fix = NJOY
  RECONR->BROADR in `tools/lattice/reconstruct_rosfond.py` (`pip install
  njoy==2016.78`).
- **OpenMC IS INSTALLED AND WORKS** (2026-11-18; corrects the old "can never
  run" note): v0.16.0 at `/workspace/tools/omc/env/bin/openmc`. NOT on PyPI — a
  prebuilt conda-forge binary, so never pip-install it. Run with that bin on
  `PATH` and `OPENMC_CROSS_SECTIONS=.../endfb-viii.0-hdf5/cross_sections.xml`.
  Verified: a bare U-235 sphere gave k-eff 1.2004 +/- 0.063. `import openmc`
  from the repo root hits the EMPTY `./openmc/` dir — use that env's python.
- **Test kit** (`2026-11-12.md`): no server in the repo; build one, then
  `tools/make_energoblok_kit.py` serves a zip on :12000. Use
  `/rbmk plant build demo` (`build` alone refuses by the gate).
- **Engine**: a pure-Java engine runs live with ~10x headroom; FFM is out (needs
  JDK 22). See `docs/ENGINE-DECISION.md`.

## EQUIPMENT REGISTER (f4829e9)
`equipment-register.json` (106 items) + `EquipmentRegister` gate + test +
`/rbmk register`. 25 modelled, 78 named-only, 3 unsourced (compressed air,
generator H2 cooling, fire suppression — flagged, NOT guessed). Not wired to the
SGB "done" rule.

## VERIFIED PHYSICS RESULTS (do not re-derive)
- **Derived void, not fitted** (0d67c7d): channel energy balance gives outlet
  quality ~15%, mean void 0.25-0.45 (vs the old fitted 0.05); the coefficient
  matches INSAG-7. A reflective axial BC is CORRECT (uniform core = FLAT flux).
- **ORM is per-rod**; rod worth calibrated (0bf2a54). **Radial-flux tilt** fixed
  (59bc0e7); the void "5-7x" gap meant BOTH tools were wrong (456cc18).
- `ExperimentVariantsSelfTest` 7/7: normal AZ-5 = +0.4% peak; 26 April AZ-5 =
  +190.4% — same code path. Power is arbitrary units (ratios only); NOT
  quantitatively validated (9267 pcm gap to OpenMC).
