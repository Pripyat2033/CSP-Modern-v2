# OpenMC — the mechanics engine (Minecraft is visualisation only)

The neutron physics is computed here, by [OpenMC](https://docs.openmc.org) — a real
Monte Carlo transport code — against **ENDF/B-VIII.0** cross-section data. Minecraft
only *displays* the result. This is the split the Director set.

## What it computes

`rbmk_cell.py` builds one RBMK fuel channel on its square graphite lattice
(250 mm pitch, 18-rod assembly in an 80 mm pressure tube) as an infinite
reflective cell, and `rbmk_void_coeff.py` sweeps the coolant density from full
water to void to measure the **coolant-void reactivity coefficient**.

Result (independent — no tabulated coefficient is fed in):

| void fraction | k-inf |
|---|---|
| 0.000 (full water) | 1.31074 |
| 0.923 | 1.33236 |
| 1.000 (full void)  | 1.29171 |

The lattice k **rises as the channel voids** and turns over only at full void
(the water is also a moderator). Operating-point coefficient ≈ **+0.015 dk/k per
unit void**, against the documented INSAG-7 / NUREG-1250 value of **+0.02** —
independent corroboration of the RBMK's defining positive void coefficient.

## Running it

Needs the OpenMC env and the data library (installed under `/workspace/tools`):

```sh
export OPENMC_CROSS_SECTIONS=/workspace/tools/openmc_data/endfb-viii.0-hdf5/cross_sections.xml
OMC=/workspace/tools/mamba/envs/omc/bin
cd openmc

# Build the cell and print k-inf:
$OMC/python rbmk_cell.py
$OMC/openmc

# Measure the void coefficient (Monte Carlo sweep; ~5 min):
$OMC/python rbmk_void_coeff.py
```

The sweep is cached in `openmc_void_sweep.json`, so re-analysis is instant;
`--force` re-runs the Monte Carlo.

## Wiring into the mod

`rbmk_void_coeff.py` writes `openmc_void_coeff.json`. That file is copied to
`src/main/resources/openmc_void_coeff.json` and read at runtime by
`OpenMcResult.java`; `/rbmk openmc` in game prints it.
`OpenMcBridgeSelfTest` checks the bridge.
