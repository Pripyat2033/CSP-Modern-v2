# Do I need OpenMC and ROSFOND to play the mod? — the clear answer

Written for Ben, 2026-11-14. This is the straight answer to your question, with no
maybe. Read §1 and you are done deciding.

---

## 1. The one-line answer

**No. To download the jar, install it and run the reactor in Minecraft, you need
nothing else — no OpenMC, no ROSFOND, no Python, no WSL. Just Minecraft 1.20.1 +
Fabric + Fabric API.**

You need OpenMC and ROSFOND **only** if you want to *recompute* the nuclear
numbers — i.e. re-derive the physics data from scratch. That is a separate,
optional job (the "data pipeline"), and it is the only part that needs Linux/WSL.

---

## 2. Why that is true — how the mod actually works

There are **two halves**, and they are deliberately separated (your own rule:
"OpenMC computes the physics; Minecraft only displays the result").

```
   [ DATA PIPELINE ]                         [ THE MOD ]
   OpenMC / Python (Linux)                   Java (Minecraft)
   runs once, offline            ──────►     runs every time you play
   writes two .json files                    reads those .json files
```

The pipeline's **output** is already baked into the jar you downloaded:

| File inside the jar | What it holds | Who reads it in game |
|---|---|---|
| `openmc_void_coeff.json` | the coolant-void coefficient from an OpenMC run | `OpenMcResult.java` → `/rbmk openmc` |
| `rbmk_lattice.json` | the two-group reactor constants the core runs on | `LatticeTable.java` → the live core |

So when you run `/rbmk openmc`, the mod is **printing a number that was computed
earlier and shipped in the jar**. It does not run OpenMC. It cannot — there is no
OpenMC in the jar, and the mod never calls out to Python. (Verified: the only code
paths are `getResourceAsStream("/openmc_void_coeff.json")` and
`getResourceAsStream("/rbmk_lattice.json")` — plain file reads.)

The live reactor itself runs on the mod's **own built-in solver**
(`RbmkSolver3D` / `ReactorCore`), fed by `rbmk_lattice.json`. No external program is
involved at play time.

---

## 3. So what do I need for each thing I might want to do?

| I want to… | Need OpenMC? | Need ROSFOND? | Need WSL/Linux? |
|---|---|---|---|
| **Play the mod / build the reactor / see it run** | No | No | No |
| Read the shipped physics (`/rbmk openmc`, `/rbmk plant status`) | No | No | No |
| **Recompute** the lattice constants (change geometry, re-derive k) | No* | Yes (for Soviet data) | Yes |
| **Re-run** the full OpenMC Monte Carlo void sweep | Yes | Yes (or ENDF as stand-in) | Yes |
| Produce a Soviet-data result you can cite as ROSFOND-derived | Yes | Yes | Yes |

\* The lattice pipeline (`tools/lattice/`) is **pure Python + numpy** — it reads
ENDF-6 cross-section files and does a 1-D cell solve. It does **not** import OpenMC
at all. So even the constants can be recomputed with just Python, no OpenMC — but
you still need the data files, and to be faithful to your rule those should be
ROSFOND.

**The distinction that matters to you:**
- **OpenMC** = a Monte Carlo *tool*. No nuclear data of its own.
- **ROSFOND** = the Soviet/Russian evaluated *data library* (IPPE lineage). This is
  the physics.

Recomputing with OpenMC but Western ENDF data would be a different answer. The
Soviet-data path is: ROSFOND → convert → run. That is the only reason to install
either one.

---

## 4. If you DO want to recompute — the exact steps

This is the part that needs WSL2. The full, command-by-command guide is already
written and correct: **`docs/OPENMC-WINDOWS-WSL.md`**. Here is the short form, in
order, so the shape is clear:

### 4a. Install OpenMC (only for the OpenMC Monte Carlo path)
1. In **PowerShell**: `wsl -l -v` to confirm you have Ubuntu on WSL **version 2**.
2. `wsl -d Ubuntu` to enter it.
3. Install micromamba and then OpenMC from conda-forge
   (`micromamba create -n openmc-env -c conda-forge openmc`). Full commands in the
   doc, §2.

### 4b. Prepare ROSFOND (this is your zip in Downloads)
Your `ROSFOND-2010` zip is the input. Two possible paths, pick by what you want:

**Path A — the Java core's constants (pure Python, no OpenMC needed):**
```bash
cd ~/CSP-Modern-v2
# 1. unpack your ROSFOND zip into the layout the converters expect
python3 tools/lattice/import_rosfond.py --from /path/to/your/ROSFOND.zip
# 2. convert the ENDF-6 files into the cache
python3 tools/lattice/fetch_rosfond.py
# 3. emit the constants the Java core loads (writes src/main/resources/rbmk_lattice.json)
python3 tools/lattice/emit_constants.py --source rosfond
# 4. rebuild the mod so the new constants are baked in
./gradlew build
```

**Path B — the full OpenMC Monte Carlo sweep:**
```bash
cd ~/CSP-Modern-v2
# 1. convert ROSFOND ENDF-6 -> OpenMC HDF5 (runs NJOY; ~30 s per nuclide)
python3 tools/openmc/rosfond_to_openmc.py --from /path/to/ROSFOND --out ~/rosfond-openmc
# 2. point OpenMC at it
export OPENMC_CROSS_SECTIONS=~/rosfond-openmc/cross_sections.xml
# 3. run the cell and the void sweep
cd openmc && python3 rbmk_cell.py && openmc
python3 rbmk_void_coeff.py
# 4. copy openmc_void_coeff.json into src/main/resources/ and rebuild
```

> **Note on `import_rosfond.py`.** The WSL doc referenced this script but it did
> not exist in the repo — I have now written and tested it (2026-11-14), so the
> command above actually works. It reads each file's ENDF head record to identify
> the isotope (ROSFOND file names vary), writes one file per nuclide, and reports
> anything missing rather than inventing it.

---

## 5. The honest summary

- **Downloading and running the jar: nothing else required.** If you were holding
  off because you thought you needed OpenMC first — you do not. Install Fabric,
  drop in the jar, play.
- **OpenMC** is needed only to *re-run the Monte Carlo void sweep* from scratch.
- **ROSFOND** is needed only if you want the recomputed numbers to be Soviet data
  rather than the ENDF/B-VIII.0 stand-in currently baked in.
- **WSL2** is needed only for those recompute paths. It is already installed, so
  you are ready when you choose to do that.
- Everything in the shipped jar is already computed and consistent; the recompute
  is a fidelity upgrade, not a prerequisite.

**Recommended order:** play the jar first (no setup). Then, when you want the
numbers to be ROSFOND-sourced, do §4b Path A (pure Python, faster, no OpenMC
install), and only install OpenMC if you also want to re-run the full Monte Carlo
sweep yourself.
