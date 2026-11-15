# Rod signals — the 211 control channels are seven rod types, each with several signals

Ben challenged the model's "211 control channels × 1 signal (position)". He is
right: the 211 is the correct **total**, but it is not **one type** and each rod
does not carry **one** signal. The units 3/4 regulation settles both.

## The 211 rods are seven types

Regulation §2.11.6 (*Наличие штатных и дополнительных поглотителей в активной
зоне*, line 775) lists the inventory:

| Rod type | Count | Meaning |
|----------|-------|---------|
| **ЛАР** | 12 | Local automatic regulators (ЛАР) |
| **АР** | 12 | Automatic regulators (АР) |
| **АЗ** | 24 | Emergency protection rods (АЗ) |
| **ПК-АЗ** | 24 | Overcompensation — emergency protection (ПК-АЗ) |
| **ПК-РР** | 12 | Overcompensation — manual regulator (ПК-РР) |
| **РР** | 103 | Manual regulators (РР) |
| **УСП** | 24 | Shortened absorber rods (УСП) |
| **Total** | **211** | |

`12 + 12 + 24 + 24 + 12 + 103 + 24 = 211` — this matches the second-generation
control-channel count exactly, and it is a **hard confirmation from the plant's own
regulation**.

The model currently treats all 211 as a single undifferentiated rod type with a
single `POS` signal. That is the error: **rod type matters** (an АЗ rod is not an
РР rod, and the УСП rods are shortened and physically different).

## Overcompensation — the ПК signals (Ben's first point)

The abbreviation list (line 215) defines:

> **ПК-АЗ, РР — перекомпенсация — АЗ, РР**

So the **ПК (перекомпенсация / overcompensation)** function exists for both the
**АЗ** rods (ПК-АЗ, 24) and the **РР** rods (ПК-РР, 12). These are **separate
signals** — an overcompensation signal per rod — which the model does not carry.
Ben's recollection of an overcompensation signal (and the АЗ/РР pairing) is
**confirmed by the regulation**.

## Limit switches — the ВК / НК signals (Ben's second point)

The abbreviation list (line 140) defines:

> **ВК — верхний концевик** (upper limit switch)

The text uses **концевики** throughout (§2.11.7-2.11.8, ch. 6.5.5, 6.6.4, ch. 12,
13):

- §2.11.7 — checking each rod "на 1 м **от нижнего концевика**" (from the lower
  limit switch); УСП rods checked "на 1 м **от верхнего концевика**".
- §2.11.6 — "24 стержня АЗ должны находиться **на верхних концевиках**".
- §12 — "стержни ПК-АЗ **на верхние концевики**".

So each rod carries **limit-switch signals** — at minimum a **lower (НК)** and an
**upper (ВК)** limit. The model does not carry these.

## Selsyn dial lamps — the green and orange lights (Ben's second point)

On the **selsyn** rod-position indicators, Ben reports:

- a **green light at 6 o'clock** — rod at **full depth** (fully inserted);
- an **orange light at 12 o'clock** — rod at the **top of its stops** (fully
  withdrawn).

These are two further **discrete lamp signals per rod**, distinct from the
continuous position. They are exactly the kind of "lamp" signal the model omits,
and they matter because operators read lamps, not numbers (Ben's own point).

**Not yet found in the regulation text** — the regulation names the limit switches
but not the lamp colours/positions on the selsyn dial. The green/orange lamps are
recorded here as **Ben's sourced recollection**, to be matched against a
photograph of a selsyn indicator when one is available.

## Consequence: the signal count is understated per rod

The model gives each control channel **1** signal. A real control channel carries
at least:

1. rod **position** (analogue, selsyn) — the model has this;
2. **НК** lower limit switch (discrete) — model lacks;
3. **ВК** upper limit switch (discrete) — model lacks;
4. **ПК** overcompensation (ПК-АЗ / ПК-РР) — model lacks;
5. selsyn **green "full depth"** lamp — model lacks;
6. selsyn **orange "top of stops"** lamp — model lacks.

So the "211 × 1" figure is wrong: the true per-rod signal population is several
times larger, before the discrete signals of the rest of the plant are counted at
all. This is one concrete instance of the general finding in
`docs/INSTRUMENTATION-SCOPE.md`: **the model carries the analogue reactor core and
almost none of the discrete signals SKALA actually monitored.**
