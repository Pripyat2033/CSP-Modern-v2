# Project memory (.openhands/memory/MEMORY.md)

Curated index. Full prose is in `ARCHIVE-narrative.md` and the per-day logs
`YYYY-MM-DD.md`. Keep this under ~6000 chars: the TOP is truncated first, so only
durable, load-bearing facts belong here.

## MISSION (Ben, 2026-11-09) — read before every decision
Forensically reconstruct the Chernobyl RBMK-1000 Unit 4 accident in Minecraft
Fabric (MC 1.20.1). **Absolute rule: "nothing is guessed"** — every constant
cited/derived or explicitly marked UNKNOWN/TO VERIFY.
Ben **distrusts INSAG-7/Western sources**; prefers ROSFOND-2010 and Russian
primary sources / designer intent. Goal: run the Unit 4 turbine rundown test and
see it fail correctly. **Accuracy is a floor, not a ceiling.** The ARL standard
(`PROJECT_NOTES.md` line 18) overrides the SGB: full fidelity, no watering down.
Ben is right that April-26-vs-regular-night is a *discrimination check*, not
validation — real validation is humans working the actual procedure.

## REKORD = 4 layers (the honest build order)
1. **Decay data** — ENSDF. Citable, zero guessing. **DONE.**
2. **Inventory at t=0** — from design-time burnup. **DONE.**
3. **Source term / release** — CONTESTED, estimates vary >2x: must be a sourced
   range, never one invented number. NOT STARTED.
4. **Transport + dose + effects** — dispersion, deposition, dose,
   TissueDamageModel. Needs a Linux `librbmk_solver.so` (only .dylib exists).
The 686 files in `/workspace/rosfond_uploads` are NEUTRON XS, NOT decay data.
**REKORD FORENSICS (2026-11-18): the radiation sim was NEVER written.** Max
lines ever, all commits = 469; jars match source; 8 of the initial 28 radiology
files were ZERO-LINE stubs. What survived is the mandate + data model.

## REKORD LAYERS 1 & 2 — DONE (dde2d7f, d3b5a8f)
Accounts: `docs/REKORD-LAYER-{1-NUCLIDE-DATA,2-INVENTORY}.md`. Neither stores
activity/release/dose; layer 2 is atoms only and REJECTS a file with activity.
Each trap below produced a plausible WRONG number; the tests now enforce them,
so do not "simplify" the code back.
**L1 (decay data, 54 nuclides):** isomer = the level whose `decay` column is
`IT`, not energy order; repeated gamma lines are not strength-ordered (keep the
STRONGEST); X-rays sit inside the gamma list (drop one copy); beta `bm` = MEAN.
**L2 (inventory, 1130 nuclides, mass balance 0.000%):** half-lives span
1e-11..1e22 s — `expm` loses 27% of Ru-106, `expm_multiply` HANGS, QSSA
overshoots +0.08% (the 618 d transient is real). **Use
`solve_ivp(method="Radau", rtol=1e-9, jac=M)`.** Delayed neutron is a SUB-branch
(I-137 = 92.86% Xe-137 + 7.14% Xe-136); blank B-N % = "not tabulated"; solve per
CONNECTED COMPONENT not per mass chain; isomer with T1/2 and no branch = infinite
trap → IT to ground; `2B+` is Z-2. Loader keys by SYMBOL (`Ce144`, not `Ce-144`).

## FABRIC/MC VERSIONS — there is NO "Fabric 1.16"
MC **1.20.1**, loader **0.15.11**, yarn 1.20.1+build.10, Fabric API
**0.91.0+1.20.1**, loom `1.6-SNAPSHOT`. Verified 2026-11-18: loader latest
**0.20.1**, API for 1.20.1 up to **0.92.12+1.20.1**, Loom now **1.19.4**.
Loom **1.16.3** exists → "Fabric 1.16" almost certainly means *Loom* 1.16.
Pins are valid, just conservative. Four distinct numbers; never conflate them.
Nothing needs changing.

## TWO ABSOLUTE RULES (cc2b88a) — ONE CORE, NO FLAG
No accident flag may decide the outcome; emergent physics only. Self-tests must
show opposite outcomes from one code path. Keystone source = the 1983 regulation
(60bf8dd): energoblok = sealed compartments; genplan per Soviet norms, no
invented figures.

## BUILD & RUN
`export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64` (gradle fails without it).
`--offline` now WORKS for compileJava/compileTestJava (the old foojay note is
stale). Self-tests are plain `main()` classes:
`java -cp build/classes/java/{main,test}:src/main/resources:<gson> <FQCN>`
gson: `~/.gradle/caches/transforms-4/*/transformed/instrumented/gson-2.10.1.jar`.
**Two pre-existing failures, NOT yours**: `zone.MaterialSupplySelfTest` and
`rbmk.physics.SpatialReactorCoreSelfTest` fail on the clean tree (verified by
`git stash -u`).

## GOTCHAS THAT COST REAL TIME
- `tools/gen_*.py` have **no arg parsing** — running one "to see" writes dozens
  of files into `src/main/resources/assets/`. Undo: `git clean -fdq` that dir.
- Gradle's fabric-loom lock can be held by an unreapable zombie gradle → use a
  fresh `GRADLE_USER_HOME`. The gradle wrapper jar is gitignored; fetch it.
- Tavily is over quota — use `curl` + `pypdf`.

## SOURCES ON HAND
INSAG-7 `https://www-pub.iaea.org/MTCD/Publications/PDF/Pub913e_web.pdf` (Table
II-II = equipment list spine); NUREG-1250 at `/workspace/tools/nureg1250.pdf`;
ROSFOND ENDF at `/workspace/tools/openmc_data/endfb-viii.0-hdf5`; IAEA NDS
`https://nds.iaea.org/relnsd/v1/data?fields=levels|decay_rads&nuclides=X
&rad_types=g|a|x|bm` (one nuclide at a time, needs a User-Agent).

## ALREADY SOLVED — in the daily logs, don't redo or re-research
- **ROSFOND path** (`2026-11-15.md`, daaed39): MF=3 pointwise XS is ZERO below
  the resonance range, so raw you get a moderator-only cell. Fix = NJOY
  RECONR->BROADR via `tools/lattice/reconstruct_rosfond.py` (**OpenMC is NOT on
  PyPI** and never ran; `pip install njoy==2016.78` suffices). Verified U-235
  586.6 b f / 99.4 b c.
- **Salvage** (`2026-11-15.md`): a vanished sandbox is recoverable by replaying
  `/tmp/traj/event_*.json` into a clean worktree (order matters).
- **Test kit** (`2026-11-12.md`): no server in the repo; build one, then
  `tools/make_energoblok_kit.py` serves a zip on :12000. `/rbmk plant build demo`
  (plain `build` refuses by the hard gate).
- **Engine**: the mod already runs a pure-Java engine live with ~10x headroom.
  FFM is out (needs JDK 22; targets 17). See `docs/ENGINE-DECISION.md`.

## EQUIPMENT REGISTER (f4829e9)
`equipment-register.json` (106 items) + `EquipmentRegister` gate + 18-check test
+ `/rbmk register`. 25 modelled, 78 named-only, 3 unsourced (compressed air,
generator H2 cooling, fire suppression — flagged, NOT guessed). Not yet wired to
the SGB "done" rule; not exhaustive (needs the plant техническое описание).

## VERIFIED PHYSICS RESULTS (do not re-derive)
- **Derived void, not fitted** (0d67c7d): channel energy balance gives outlet
  quality ~15%, mean void 0.25-0.45 (vs the old fitted 0.05); the void
  coefficient matches the sourced INSAG-7 value. A reflective axial BC is
  CORRECT (uniform core = FLAT flux = perfect reflector).
- **ORM is per-rod**; rod worth calibrated (0bf2a54).
- **Radial-flux tilt bug** fixed (59bc0e7). Void "5-7x" discrepancy: BOTH tools
  were wrong (456cc18).
- `ExperimentVariantsSelfTest` 7/7: normal AZ-5 = +0.4% peak; 26 April AZ-5 =
  +190.4% peak — same code path. Power is arbitrary units (ratios only) and the
  model is NOT quantitatively validated (9267 pcm gap to OpenMC).

