# REKORD layer 1 — nuclide decay data

## What this layer is

The nuclear data every radiological calculation rests on: for each nuclide, its
half-life, its decay modes with branching ratios, its emissions (gamma, alpha,
beta, X-ray) with energies and intensities, and its decay daughters.

## What it is NOT

There is **no activity, no source term, no release fraction and no dose** in this
layer, and none should be added to it. Activity needs an inventory (how many
atoms are present) — that is layer 2. The release fraction that turns an
inventory into a source term is layer 3, and it is genuinely contested.

A green `IsotopeDataSelfTest` means the nuclear data is right. It says nothing
about whether a source term has been modelled. Do not read it as "radiation
works".

## Where the numbers come from

`tools/radiology/fetch_decay_data.py` queries the IAEA Nuclear Data Services API
(`https://nds.iaea.org/relnsd/v1/data`), which serves the evaluated ENSDF
library. **Nothing in the data file is hand-typed.** Each isotope record carries
the ENSDF publication date and author string the API returns, so any value can be
checked against the evaluated file:

```bash
curl -s "https://nds.iaea.org/relnsd/v1/data?fields=levels&nuclides=Cs137"
curl -s "https://nds.iaea.org/relnsd/v1/data?fields=decay_rads&nuclides=Cs137&rad_types=g"
```

Regenerate with:

```bash
python3 tools/radiology/fetch_decay_data.py           # default RBMK set
python3 tools/radiology/fetch_decay_data.py Cs137     # explicit nuclides
```

## Why this layer can be built to full fidelity today

Decay data is **measurement**. There is no legitimate disagreement about the
half-life of Cs-137; it is a quantity with a published uncertainty. So the ARL
standard can be met here completely, with zero guessing. The parts of the
radiological problem that cannot be built this way — inventory, release,
transport, dose — are explicitly later layers and are not faked here.

## Traps found and handled (do not undo these)

These were each a real error caught during construction, not hypotheticals:

1. **Isomers.** The API rejects `Tc99m`; you must query `Tc99` and select the
   excited level that carries IT (internal transition) decay. Selecting the
   lowest excited level gives a 190 ps level instead of the 6.006 h isomer —
   a wrong half-life by nine orders of magnitude.
2. **Repeated transitions.** ENSDF can list the same gamma twice. I-132's
   667.7 keV line appears once at 13.9% and once at 98.7%; keeping the first row
   would understate its dose sevenfold. The merge keeps the **strongest** listing.
3. **X-rays in the gamma list.** The gamma endpoint returns atomic X-rays too.
   Co-60's 7.677 keV row appears in both the gamma and X-ray lists at 3.815%;
   summing both lists double-counts the photon. The GAMMA duplicate is dropped.
4. **Beta mean energy, not endpoint.** `rad_types=b` is not valid; `bm` returns
   the beta *mean* energy, which is what dosimetry needs. Using the endpoint
   would overstate beta dose severalfold.
5. **Stable nuclides.** Sm-149 is stable. It is flagged `stable` with zero decay
   constant, not given a fabricated half-life.

## Verification

`IsotopeDataSelfTest` (38 checks) compares loaded values against published
values written independently into the test, not read from the same file:
half-lives (Cs-137, I-131, Sr-90, Co-60, H-3, Pu-239, Xe-135, Kr-88, Ru-106,
Ce-144), gamma lines and intensities (Cs-137 661.657 keV/85.1%, Co-60
1173.228/1332.492 keV, I-131 364.489 keV, Cs-134 604.721 keV, Xe-135
249.794 keV), decay constants, specific activity, decay chains, stable handling,
isomer handling, release groups, and the no-double-count rule.

Run:

```bash
./gradlew compileJava compileTestJava --offline
java -cp build/classes/java/main:build/classes/java/test:src/main/resources:<gson> \
  com.ben.csp.radiology.IsotopeDataSelfTest
```

## What is deliberately marked unverified

`provenance.json` → `radiology.release_groups` is `to_verify`. The *classes*
(noble gas / volatile / semi-volatile / particulate) are standard release
chemistry, but the assignment of each nuclide to a class is this project's own
and has not been checked against a Soviet primary source. Caesium and iodine
retention are plant-dependent, so this needs a Russian source before it is
trusted.
