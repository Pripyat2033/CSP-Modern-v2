# Installing OpenMC on Windows 11 with WSL2

Written for Ben, 2026-11-13. Every command is the one that actually works, not a
guess. Where a step exists only to work around a platform limit, it says so.

---

## 0. The one fact that decides everything

**There is no native Windows build of OpenMC.** Checked against conda-forge
directly (2026-11-13): the `openmc` package ships for `linux-64`,
`linux-aarch64`, `osx-64`, `osx-arm64` — and **no `win-64`**. So you cannot
`conda install openmc` in a Windows prompt. There are only two routes:

| Route | Verdict |
|---|---|
| **WSL2 (Ubuntu)** | **Recommended.** A real Linux inside Windows; the standard `conda install openmc` works; native speed; GPU compute passes through. |
| **Docker Desktop** | Works, but a second heavy layer on top of WSL2 anyway, and file access to your Windows drives is slower. |

Since you already have WSL2 installed, we use it. The rest of this document is
WSL2 only.

---

## 1. Get into WSL2 and update it

Open **PowerShell** (Windows) and check your distro:

```powershell
wsl -l -v
```

If you have an Ubuntu listed (it should show `VERSION 2`), jump in:

```powershell
wsl -d Ubuntu
```

If nothing is listed, install one (this also enables the needed Windows
features; reboot if it asks):

```powershell
wsl --install -d Ubuntu
```

If your Ubuntu shows `VERSION 1`, convert it (WSL2 is required for GPU passthrough
and is much faster):

```powershell
wsl --set-version Ubuntu 2
```

Everything from here on is typed **inside the Ubuntu shell**, not PowerShell.

---

## 2. Install OpenMC inside WSL2

We use **micromamba** — it is a single static binary, faster than conda, and it
does not touch your shell profile unless asked. `openmc` from conda-forge brings
OpenMC **and** the NJOY2016 converter with it, which we need later.

```bash
# 1. install micromamba into your home directory
cd ~
curl -Ls https://micro.mamba.pm/api/micromamba/linux-64/latest | tar -xvj bin/micromamba
mkdir -p ~/.local/bin
mv bin/micromamba ~/.local/bin/
rmdir bin

# 2. make it available in every shell
echo 'export MAMBA_ROOT_PREFIX=$HOME/micromamba' >> ~/.bashrc
echo 'eval "$($HOME/.local/bin/micromamba shell hook -s bash)"' >> ~/.bashrc
source ~/.bashrc
```

Now create the environment. This pulls OpenMC, its Python API, and NJOY:

```bash
micromamba create -y -n openmc-env -c conda-forge openmc
micromamba activate openmc-env
```

Check it worked:

```bash
openmc --version
njoy --version      # press Ctrl-C to leave the interactive njoy banner
python -c "import openmc; print('openmc python', openmc.__version__)"
```

You should see **OpenMC 0.16.0** (or newer) and **NJOY 2016**.

> **Make `openmc-env` automatic:** so a new shell is ready to go, add
> `micromamba activate openmc-env` as the last line of your `~/.bashrc`.

---

## 3. Get the nuclear data — ROSFOND (the Soviet library)

This is the part that matters for your rule: **the numbers that affect the
reactor must be Soviet, verified, cross-referenced.** That is ROSFOND.

One distinction worth being precise about, because it protects the whole project:

- **OpenMC is a tool** — a Monte Carlo transport calculator. It is US-developed
  (MIT / Argonne), but it contains **no nuclear data of its own**. It is like a
  calculator that has no numbers in it.
- **The data library is the physics.** Change the library and the answer changes.
  So the *data* is what must be Soviet. OpenMC + ROSFOND = Soviet numbers,
  computed with a neutral tool. That is exactly the "Algorithmic Heritage" rule:
  the *method* is standard reactor physics, the *data* is NIKIET-lineage.

OpenMC does not read ROSFOND's ENDF-6 files directly — it needs its own HDF5
format. The converter for that lives in this repo and is now tested:

```
tools/openmc/rosfond_to_openmc.py
```

### 3a. Point the converter at your ROSFOND files

Put the ROSFOND ENDF-6 files somewhere in WSL2 (any folder works; sub-folders are
searched). Then:

```bash
cd ~/CSP-Modern-v2            # your clone, inside WSL2
micromamba activate openmc-env

# see what it found and what is still needed
python3 tools/openmc/rosfond_to_openmc.py --from /path/to/rosfond --out ~/rosfond-openmc --list

# convert everything it can (runs NJOY; ~30 s per nuclide)
python3 tools/openmc/rosfond_to_openmc.py --from /path/to/rosfond --out ~/rosfond-openmc
```

It writes `~/rosfond-openmc/*.h5` and a `cross_sections.xml`, and a
`MANIFEST.json` recording exactly what was converted (provenance).

### 3b. What ROSFOND must contain

The RBMK cell needs these elements. The converter reports any that are missing —
it never invents one:

`U, O, H, C, Zr, Nb, B, Fe, Ni, Cr, Gd, Er, He`

plus the **thermal-scattering** files for graphite and water (`c_Graphite`,
`c_H_in_H2O`) — ROSFOND ships these too, and the converter picks them up
automatically.

If your ROSFOND distribution is the per-isotope ZIP form, the repo's importer
unpacks it first:

```bash
python3 tools/lattice/import_rosfond.py --from /path/to/rosfond/zips
```

### 3c. Tell OpenMC where the library is

```bash
export OPENMC_CROSS_SECTIONS=~/rosfond-openmc/cross_sections.xml
```

Add that line to your `~/.bashrc` so it sticks.

> **Quick start while ROSFOND is being prepared.** If you want to see the model
> run today, OpenMC can also use the international ENDF/B-VIII.0 HDF5 library.
> That is *not* the final source — it is a stand-in to prove the pipeline — but it
> lets you watch the RBMK cell run before the ROSFOND conversion is finished.
> Download it from the ENDF/B-VIII.0 row on <https://openmc.org/data/> and point
> `OPENMC_CROSS_SECTIONS` at its `cross_sections.xml`.

---

## 4. Run the RBMK cell

```bash
cd ~/CSP-Modern-v2/openmc
micromamba activate openmc-env

# build the geometry/materials/settings
python3 rbmk_cell.py
openmc                       # runs the Monte Carlo; a few minutes

# the coolant-void sweep (the point of the model)
python3 rbmk_void_coeff.py
```

The void sweep is the test that matters: it must show the lattice k **rising as
the channel voids** — the RBMK's defining positive void coefficient — computed
from the geometry, with no coefficient fed in.

`rbmk_void_coeff.py` writes `openmc_void_coeff.json`; copy it to
`src/main/resources/openmc_void_coeff.json` and `/rbmk openmc` in game prints it.

---

## 5. The GPU option (for the HPS goal in PROJECT_NOTES)

WSL2 passes the GPU through to Linux, so OpenCL compute works inside the distro
*if* you install the Windows GPU driver (which includes the WSL component) and the
Linux OpenCL runtime. For OpenMC itself this is not required — the CPU runs are
already fast enough — but it is the path to the "offload the matrix maths to the
GPU" ambition. Note this when the platform question (CHARTER §4) is settled.

---

## 6. Troubleshooting the things that actually bite

- **`FileNotFoundError: 'openmc'`** — the Python API shells out to the `openmc`
  binary. Make sure the env is activated (`micromamba activate openmc-env`) so
  `openmc` is on PATH.
- **`No cross_sections.xml found`** — `OPENMC_CROSS_SECTIONS` is not exported in
  this shell. Re-run the export (and check `~/.bashrc`).
- **NJOY exits non-zero (e.g. status 77)** — the ENDF file is already processed
  (a PENDF), not a raw evaluation. Feed ROSFOND's raw evaluated files.
- **Converting is slow** — ~30 s per nuclide is normal; a full library takes a
  while. Convert once; the HDF5 is reused.
- **Files on `/mnt/c/...` are slow** — keep the ROSFOND files and the output
  inside the Linux filesystem (`~/...`), not on a Windows drive.

---

## 7. Why this satisfies the "Soviet sources only" rule

- The **data** is ROSFOND-2010 (Russian national evaluated library, IPPE lineage).
- The **method** is standard two-group lattice physics, the same family NIKIET
  used (the "Algorithmic Heritage" clause in `PROJECT_NOTES.md`).
- Every converted nuclide is recorded in `MANIFEST.json`, so each number traces to
  its file — the "nothing is guessed" rule.
- Where ROSFOND is missing an element, the converter **says so** rather than
  falling back silently to a Western library.

That is the difference between "we used a tool made in the USA" (fine — it holds
no data) and "we used American nuclear data" (not fine). This path is the former.
