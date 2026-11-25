# REKORD layer 4 — transport and dose

## What this layer is

The path from a release to a dose to a person: how the plume disperses, what
lands on the ground, what is inhaled, what is ingested, and what that means in
sieverts.

Three pathways, matching the four ways the accident actually delivered dose
(Pub1239 §5.1: cloud shine, inhalation from the cloud and resuspended material,
ground shine, ingestion):

| pathway | equation (TECDOC-1162) | dominant at Chernobyl? |
|---|---|---|
| Ground shine | `E = C_ground × CF4`, then shielding and occupancy | yes, long term |
| Inhalation | `E = C_air × CF2 × hours` | yes, Pripyat before evacuation |
| Ingestion | `E = intake × CF5` | yes, rural areas afterwards |

Air concentration comes from Procedure E5a: `C_air = Q × DF / u`.

## What it is NOT

No shielding model of its own (the TECDOC's Table E4 factors are used, with
their defaults stated). No resuspension. No aquatic pathway. No biokinetic model
beyond the committed-dose coefficients. No decision about what a dose *means* —
that is the medical literature's business.

And no fitted constant anywhere: **every factor is quoted**.

## The source

**IAEA-TECDOC-1162**, *Generic procedures for assessment and response during a
radiological emergency* (IAEA, Vienna, August 2000). It is an emergency
assessor's handbook, written to be used during an accident, and it tabulates
exactly the quantities this layer needs:

| table | quantity | pages |
|---|---|---|
| E3 | ground: dose rate and 1-month / 2-month / 50-year integrals per unit deposition | 98–100 |
| E6 | inhalation: committed effective dose per unit intake, adult | 107–108 |
| E7 | thyroid: committed equivalent dose per unit intake, adult and 10-year-old | 109 |
| E13 | ingestion: committed effective dose per unit intake, adult | 116–117 |
| E8 | atmospheric dilution factors by Pasquill class and distance | 111 |
| E9/E10 | weather → stability class, observable effects → wind speed | 111 |

Fetch with:

```bash
curl -sSLo /tmp/te1162.pdf \
  https://www-pub.iaea.org/MTCD/Publications/PDF/te_1162_prn.pdf
python3 tools/radiology/extract_tecdoc1162.py /tmp/te1162.pdf
python3 tools/radiology/build_dose_data.py
```

## Why the values are extracted, not typed

**No dose coefficient in the data file is hand-entered.** Each is read out of the
PDF by `extract_tecdoc1162.py`, so anyone can take the PDF and the script and
check it. The extraction is then verified against values it was not derived
from: `test_build_dose_data.py` compares the ingested coefficients against
published ICRP adult values, and — the purer test — does the same for **Co-60,
Ra-226 and Th-232, which appear nowhere else in this project**. Agreement there
can only come from reading the table correctly.

## Conservative versus best estimate — carried in the data

This distinction matters more than it looks, so each factor is labelled:

* **Inhalation (E6)** is `conservative`. The TECDOC's own note says it uses *"the
  highest value from IAEA BSS"*. For a nuclide whose lung-clearance class
  matters, that can be several times the adult value. A dose from this pathway
  is an **upper bound**.
* **Thyroid (E7)** is likewise `conservative`.
* **Ingestion (E13)** is `adult` — it reproduces the ICRP adult coefficients
  exactly (verified: I-131 2.2E-8 Sv/Bq, Cs-137 1.3E-8, Sr-90 2.8E-8, Pu-239
  2.5E-7).
* **Ground (E3)** is for open, undisturbed ground, **unshielded**; shielding and
  occupancy are applied downstream, not baked in.

A conservative number presented as a best estimate is a lie of the same kind as a
guessed number. So `DoseCalculator` returns the label with the value.

## The thyroid is its own quantity, and why

`DoseCalculator.thyroidDose` is separate from `inhalationDose` because that
separation **is** the radiological story of Chernobyl. I-131 concentrates in the
thyroid, so the same intake delivers far more dose there than to the body:

```
1 kBq/m3 I-131, 1 hour, adult:
  thyroid    0.230 mSv
  effective  0.011 mSv     (21x lower)
child thyroid 0.410 mSv    (1.8x the adult)
```

That factor is what makes a childhood thyroid cancer epidemic possible from
doses that would otherwise be unremarkable.

## What is deliberately refused

The dilution factors are fitted over **0.5–30 km**. Outside that range
`dilutionFactor` returns `NaN` rather than extrapolating — a number invented past
the edge of a fitted table is exactly the plausible fiction this project bans. The
same applies to an unknown stability class.

A noble gas returns `NaN` for inhalation dose, **not 0.0**. "The source credits no
dose by this pathway" and "this pathway delivers zero dose" are different claims,
and a caller must be able to tell them apart.

## Validation — and the bug it caught

`DoseSelfTest` — 27 checks, 0 failures. Three groups:

**Unit sanity.** Each pathway computed by hand from the published factor:
1 kBq/m² Cs-137 → 0.13 mSv over 50 y; 1 kBq/m³ I-131 for 1 h → 0.011 mSv;
1 kBq I-131 ingested → 0.022 mSv. A factor-of-1000 slip (mSv/Sv, kBq/Bq) is the
most likely way this layer could be wrong, and it produces plausible answers — so
these are pinned to four significant figures.

**Physical ordering.** Stable air (F) gives a higher downwind concentration than
unstable (A); higher wind speed dilutes more; dose falls with distance;
shielding reduces dose; child thyroid dose exceeds adult.

**Published anchors.**

* *Pripyat.* 1 MBq/m² Cs-137 with the TECDOC's default building shielding and
  occupancy gives **75 mSv over 50 years**. Pub1239 puts Pripyat residents in the
  low tens of mSv for the two days before evacuation, with the long-term
  accumulation of the same order — so the pathway lands where measurement says
  it should.
* *The epidemic.* A child breathing 1 kBq/m³ I-131 for 24 h gets **9.8 mSv** to
  the thyroid; a heavily exposed child (100 kBq/m³ for 3 days) gets **2952 mSv**
  — inside the hundreds-to-thousands of mSv range that Pub1239 and UNSCEAR
  attribute the childhood thyroid cancer epidemic to. A model that could not
  reproduce that range could not explain the observed health effect.

**The bug the test caught.** The first version had the TECDOC's shielding formula
inverted — `E×(SF + OF×(1−SF))` instead of `E×(SF×OF + (1−OF))`. The two agree
when `OF` is 0 or 1, so most checks passed and the error survived. The check
"full shielding for all time gives zero dose" failed at 0.13 mSv and exposed it.
The formula is now transcribed from the source text with the source's own
wording in the comment: *"it is assumed that, for the rest of the time, there is
no shielding; default — 0.6"*.

## Known limits

* The dispersion factors assume constant release rate, wind direction and speed,
  simple terrain, ground-level release, no rain, and a single release point. The
  TECDOC states these conditions explicitly; this project does not extend the
  factors beyond them. The Chernobyl release violated several of them (it was
  not constant, and the plume did not stay on one bearing), so a single
  downwind calculation is a *scenario*, not a reconstruction of the actual
  deposition field.
* Below 0.5 km the TECDOC uses Fig. E1 because building wake dominates. The
  0.5 km table value is used and the limitation is recorded in the data file.
* No shielding survey, no occupancy survey, no resuspension, no aquatic pathway.
* Inhalation and thyroid factors are upper bounds, by the source's own choice.
* The whole-accident dose to the population is not computed here and should not
  be: it needs a deposition field, which is a measurement, not a calculation.
