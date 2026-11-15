# How to send ROSFOND to the agent — the private bridge

Ben asked: *"How do I send you the zip? There's no website or bridge I can use to
upload to, preferably privately and securely."*

**There is a bridge, and it is already set up: GitHub.** This sandbox holds a
`GITHUB_TOKEN` for **your own account (`Pripyat2033`)**, and it can read your
**private** repositories. So a private repo is a private, authenticated transfer
channel between your machine and this sandbox — no third-party upload site, no
public exposure.

---

## The facts (verified 2026-11-15)

- `GITHUB_TOKEN` here = GitHub account **`Pripyat2033`** (yours).
- It can **read private repos** (`granichny-vri`, `Ant_Warz` both resolve).
- It **cannot create** a new repo (returns 403 "Resource not accessible"), so
  **you** create the staging repo; I read from it.
- **`CSP-Modern-v2` is PUBLIC** — do not put the data there.
- The sandbox **can reach the internet** (GitHub, PyPI reachable), but **cannot
  reach your PC** and **cannot download ROSFOND** (IPPE/Rosatom/IAEA/OECD-NEA all
  403 or unreachable).

---

## Route 1 — private repo, whole folder (simplest)

1. On GitHub, create a **new private repository**, e.g. **`rosfond-2010`**.
   (Do **not** initialise it with a README — keeps it clean.)
2. Open it, click **Add file → Upload files**.
3. Drag in the needed `.zip` files from
   `C:\Users\Ben\Downloads\ROSFOND-2010_n_MAT\` (list below), then **Commit**.
4. Tell me the repo name. I then run:

```bash
gh repo clone Pripyat2033/rosfond-2010 /tmp/rosfond -- --depth 1
python3 tools/lattice/import_rosfond.py --from /tmp/rosfond --out tools/lattice/rosfond_raw
python3 tools/lattice/fetch_rosfond.py          # build the cache
python3 tools/lattice/emit_constants.py --source rosfond
```

If you upload the **entire** `ROSFOND-2010_n_MAT` folder instead, step 4 is
identical — `import_rosfond.py` reads a directory of zips recursively and picks
out the 28 it needs.

## Route 2 — private repo, release asset (best for a large single archive)

If you would rather zip the whole library into one file, create the private repo
as above, then **Releases → Draft a new release → attach the archive → Publish**.
Tell me the repo and tag; I download the asset with `gh release download`.

## Route 3 — you run it on your WSL2

If you prefer not to hand the data over at all, run the same four commands inside
your own WSL2 (commands in `docs/OPENMC-WINDOWS-WSL.md`). The result is a
`rbmk_lattice.json` you can commit yourself. I cannot see or verify it directly.

---

## The 28 files the lattice actually needs

Exact names in your folder (from the listing you pasted):

| nuclide | file |
|---|---|
| H1    | `n_0101_1-H-1.zip` |
| He4   | `n_0204_2-He-4.zip` |
| B10   | `n_0510_5-B-10.zip` |
| B11   | `n_0511_5-B-11.zip` |
| C12   | `n_0600_6-C-0.zip` (natural carbon) |
| C13   | `n_0613_6-C-13.zip` |
| N14   | `n_0714_7-N-14.zip` |
| O16   | `n_0816_8-O-16.zip` |
| Cr52  | `n_2452_24-Cr-52.zip` |
| Fe56  | `n_2656_26-FE-56.zip` |
| Ni58  | `n_2858_28-Ni-58.zip` |
| Zr90  | `n_4090_40-Zr-90.zip` |
| Zr91  | `n_4091_40-Zr-91.zip` |
| Zr92  | `n_4092_40-Zr-92.zip` |
| Zr94  | `n_4094_40-Zr-94.zip` |
| Zr96  | `n_4096_40-Zr-96.zip` |
| Nb93  | `n_4193_41-Nb-93.zip` |
| Gd155 | `n_6455_64-Gd-155.zip` |
| Gd157 | `n_6457_64-Gd-157.zip` |
| Er166 | `n_6866_68-Er-166.zip` |
| Er167 | `n_6867_68-Er-167.zip` |
| U234  | `n_9234_92-U-234.zip` |
| U235  | `n_9235_92-U-235.zip` |
| U236  | `n_9236_92-U-236.zip` |
| U238  | `n_9238_92-U-238.zip` |
| Pu239 | `n_9439_94-Pu-239.zip` |
| Pu240 | `n_9440_94-Pu-240.zip` |
| Pu241 | `n_9441_94-Pu-241.zip` |

Only these 28 are read today. The rest of the library is welcome but unused
unless the model is extended (e.g. fission-product inventory).

---

## After the import — the method change that goes with it

Bringing ROSFOND in is only half the job. Per `docs/ROSFOND-DATA-PATH.md`, the
model currently **anchors to INSAG-7** (`xsec.void_worth_scale` blends the derived
void worth down to match INSAG-7). With ROSFOND data, that anchoring must be
removed so the **ROSFOND-derived values stand on their own** and INSAG-7 becomes a
printed cross-check. Doing the data swap without the method change would bend
ROSFOND back to INSAG-7 — the worst of both.

## Security notes

- The staging repo must be **private**. ROSFOND is public data, but there is no
  reason to mirror it publicly.
- The token in this sandbox is **your own account's**; I use it only to read the
  staging repo you name, and to push the CSP work to its existing branch.
- Delete the staging repo once the data is imported; nothing downstream needs it.
