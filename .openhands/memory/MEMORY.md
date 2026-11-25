# Project memory (.openhands/memory/MEMORY.md)

Curated index. Full prose is in `ARCHIVE-narrative.md` and the per-day logs
`YYYY-MM-DD.md`. Keep under ~6000 chars: the TOP is truncated first.

## MISSION (Ben, 2026-11-09) — read before every decision
Forensically reconstruct the Chernobyl RBMK-1000 Unit 4 accident in Minecraft
Fabric (MC 1.20.1). **Absolute rule: "nothing is guessed"** — every constant
cited/derived or explicitly marked UNKNOWN/TO VERIFY. Ben **distrusts
INSAG-7/Western sources**; prefers ROSFOND-2010 and Russian primary sources /
designer intent. Goal: run the Unit 4 turbine rundown test and see it fail
correctly. **Accuracy is a floor, not a ceiling**; the ARL standard
(`PROJECT_NOTES.md` line 18) overrides the SGB. April-26-vs-regular-night is a
*discrimination check*, not validation.

## REKORD = 4 layers (the honest build order)
1. **Decay data** — ENSDF. **DONE.**  2. **Inventory at t=0** — **DONE.**
3. **Source term / release** — CONTESTED, varies >2x: a sourced range, never an
   invented number. NOT STARTED.
4. **Transport + dose** — needs a Linux `librbmk_solver.so` (.dylib only).
The 686 files in `/workspace/rosfond_uploads` are NEUTRON XS, NOT decay data.
**FORENSICS (2026-11-18): the radiation sim was NEVER written** — 469 lines
max; 8 of 28 initial radiology files were ZERO-LINE stubs.

## REKORD LAYERS 1 & 2 — DONE (dde2d7f, d3b5a8f, c3a4ca6)
Accounts: `docs/REKORD-LAYER-{1-NUCLIDE-DATA,2-INVENTORY}.md`. Neither stores
activity/release/dose; layer 2 is atoms only and REJECTS a file with activity.
**Layer 2 had 7 bugs, each producing a plausible WRONG number — do not
"simplify" the code back.** Full account: layer-2 doc §"seven bugs". The 3 found
2026-11-18: (a) **a failed fetch read as "stable"** — an error record has no
half-life, so `if not hl: stable` froze it: 0.75% of the inventory, Nb-102m
alone 5.1e25. `_decay_state` tests `error` FIRST; the generator REFUSES to write
a file containing one. (b) **Isomers at E=0** (Nb-102, Rh-110, Sb-128, Pm-154):
never select an isomer by `energy > 0`. (c) **Half-life, blank mode column**:
take the route from `decay_rads` `bm`, else derive from yields
(`yield-chain-derived`). 21 nuclides have NO ENSDF evaluation: distinct
`unevaluated` state; build aborts above 0.01%.
**L1 (54 nuclides):** isomer = the level whose `decay` column is `IT`; gamma
lines are not strength-ordered (keep STRONGEST); X-rays sit in the gamma list
(drop one copy); beta `bm` = MEAN.
**L2 (1130 nuclides, mass balance 0.000%):** half-lives span 1e-11..1e22 s, so
use `solve_ivp(method="Radau", rtol=1e-9, jac=M)` — `expm` loses 27% of Ru-106,
`expm_multiply` HANGS. Delayed neutron is a SUB-branch (I-137 = 92.86% Xe-137 +
7.14% Xe-136); blank B-N % = "not tabulated"; solve per CONNECTED COMPONENT, not
per mass chain; `2B+` is Z-2; loader keys by SYMBOL.

## FABRIC/MC VERSIONS — there is NO "Fabric 1.16"
MC **1.20.1**, loader **0.15.11**, yarn 1.20.1+build.10, Fabric API
**0.91.0+1.20.1**, loom `1.6-SNAPSHOT`. "Fabric 1.16" almost certainly means
*Loom* 1.16 (which exists). Pins are valid, just conservative; four distinct
numbers, never conflate them.

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
`zone.MaterialSupplySelfTest`, `rbmk.physics.SpatialReactorCoreSelfTest`.

## GOTCHAS THAT COST REAL TIME
- `tools/gen_*.py` have **no arg parsing** — running one "to see" writes dozens
  of files into `src/main/resources/assets/`. Undo: `git clean -fdq` that dir.
- The fabric-loom lock can be held by an unreapable zombie gradle → fresh
  `GRADLE_USER_HOME`. The wrapper jar is gitignored; fetch it.
- Tavily is over quota — use `curl` + `pypdf`.
- A vanished sandbox is recoverable by replaying `/tmp/traj/event_*.json` into a
  clean worktree — `2026-11-15.md`.

## SOURCES ON HAND
INSAG-7 `www-pub.iaea.org/MTCD/Publications/PDF/Pub913e_web.pdf` (Table II-II =
equipment list spine); NUREG-1250 at `/workspace/tools/nureg1250.pdf`; ROSFOND
ENDF at `/workspace/tools/openmc_data/endfb-viii.0-hdf5`; IAEA NDS
`nds.iaea.org/relnsd/v1/data` (one nuclide at a time, needs a User-Agent).

## ALREADY SOLVED — in the daily logs, don't redo or re-research
- **ROSFOND path** (`2026-11-15.md`, daaed39): MF=3 pointwise XS is ZERO below
  the resonance range, so raw you get a moderator-only cell. Fix = NJOY
  RECONR->BROADR in `tools/lattice/reconstruct_rosfond.py` (**OpenMC is NOT on
  PyPI**; `pip install njoy==2016.78`).
- **Test kit** (`2026-11-12.md`): no server in the repo; build one, then
  `tools/make_energoblok_kit.py` serves a zip on :12000. Use
  `/rbmk plant build demo` (plain `build` refuses by the gate).
- **Engine**: a pure-Java engine runs live with ~10x headroom; FFM is out (needs
  JDK 22). See `docs/ENGINE-DECISION.md`.

## EQUIPMENT REGISTER (f4829e9)
`equipment-register.json` (106 items) + `EquipmentRegister` gate + test +
`/rbmk register`. 25 modelled, 78 named-only, 3 unsourced (compressed air,
generator H2 cooling, fire suppression — flagged, NOT guessed). Not wired to the
SGB "done" rule; needs the техописание to finish.

## VERIFIED PHYSICS RESULTS (do not re-derive)
- **Derived void, not fitted** (0d67c7d): channel energy balance gives outlet
  quality ~15%, mean void 0.25-0.45 (vs the old fitted 0.05); the coefficient
  matches INSAG-7. A reflective axial BC is CORRECT (uniform core = FLAT flux).
- **ORM is per-rod**; rod worth calibrated (0bf2a54). **Radial-flux tilt** fixed
  (59bc0e7); the void "5-7x" gap meant BOTH tools were wrong (456cc18).
- `ExperimentVariantsSelfTest` 7/7: normal AZ-5 = +0.4% peak; 26 April AZ-5 =
  +190.4% — same code path. Power is arbitrary units (ratios only); NOT
  quantitatively validated (9267 pcm gap to OpenMC).
