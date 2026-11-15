# Control-room ceiling comparison (units 1 / 3 / 4)

Photos of three ChNPP main control rooms, collected to examine Ben's observation
that **the ceiling in unit 4 differs from the other units**.

Ben's description:
- Units **1, 2 and 3**: a **suspended (false) ceiling** of white **perforated
  acoustic tiles** — the "little dots" grid.
- Unit **4**: a ceiling of **large square panels (~1 m) with recessed luminaires**
  built into them.

Ben's reason: unit 4 was the **latest generation at the time it was built**, the
model for later RBMK-1000 units.

## Files

| File | Room | Source | Licence |
|------|------|--------|---------|
| `unit1.jpg` | Unit 1 main control room | Wikimedia Commons, *ChNPP Unit1control.JPG* (Carl A. Willis) | CC BY-SA 3.0 |
| `unit3.jpg` | Unit 3 control room | Wikimedia Commons, *Control Room of Chernobyl Nuclear Power Plant Unit 3.jpg* (IAEA Imagebank) | CC BY-SA 2.0 |
| `unit4.jpg` | Unit 4 control room (БЩУ-4) | Wikimedia Commons, *Physics student inside the Control Room of Unit 4 at Chernobyl NPP.jpg* | CC0 |

## Status of the claim: OPEN — not yet verified

**These photos have not been used to confirm the ceiling difference.** They are
stored so the comparison can be made by eye, panel-side.

Two honest caveats:

1. **Automated pixel analysis of these photos was inconclusive** and should not be
   trusted. The three photos have different camera angles, framing and resolution,
   so a texture/grid metric measured on them is not a fair comparison. An earlier
   pass produced numbers that merely reflected the window size, not ceiling tiles;
   those numbers are discarded.
2. No authoritative written source for the unit-3 vs unit-4 ceiling difference was
   found (web search engines blocked automated access in this pass).

## Why this matters for the reconstruction

If the ceiling difference is real, it is a **building / as-built** difference
between two units of the *same* reactor generation (Stage II / Generation 2 —
see `docs/CHNPP-UNIT-GENERATIONS-VERIFICATION.md`). It is therefore a per-unit
layer, not a generation layer, and should be modelled only once confirmed from the
photos (or another source).

## To confirm

Compare the **ceiling band** of `unit4.jpg` against `unit3.jpg` and `unit1.jpg`:
look for (a) a fine perforated-tile grid vs (b) large square panels carrying
recessed lights. If confirmed, record the tile module size and the luminaire
layout for the unit-4 control-room build.
