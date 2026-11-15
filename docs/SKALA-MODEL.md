# The SKALA process computer — what we model, and from where

Scope: **pre-1986 SKALA only.** DIIS (and DIIS-2000) was a 1990s upgrade fitted
at Chernobyl after Unit 4 was destroyed, so it is deliberately absent. The
machine below is the one that existed on the night of 26 April 1986.

Primary source for the details in this document: the **Chornobyl Family**
restoration of Unit 3's surviving SKALA machine (documentary + operator-console
and status-display episodes). Transcripts are kept in `docs/skala-sources/`.
Where a detail was not stated in the sources it is marked *unknown* rather than
guessed.

## The machine

- **Two independent VNIIEM-3 machines**, each a **V-3M** processor.
- **20K (octal) 24-bit words** of ferrite-core memory each, **8K shared**
  between them.
- Not binary: **octal**, 24-bit words; programs in machine code.
- No operating system in the usual sense — a **monitor + supervisor program**.
- Reliability: wire-wrapped backplanes (32 contact points per pin),
  double-layer palladium-coated connectors, sophisticated error correction and
  self-tests. If one machine misses the other's acknowledgement signal it
  declares it faulty, reloads its software from tape, re-routes its interface
  channels and takes over its work.
- Hardware allocation variants: **set 2.0** and **set 1.5** (one machine using
  part of the other's resources). Two singles is also possible.

## The eight main programs (status-display, right panel)

| Key | Narration | Job | Period |
|-----|-----------|-----|--------|
| КРВ | Kerva | coolant flow through the active zone (reads the channel rotameters) | 1/min |
| КЦТК | KTsTK / "Katetika" | reactor-channel integrity (leak detectors) | 1/min |
| КИКД | KIKD / "Kvad" | detector function check | 1/min |
| КИРП | KIRP | *unknown* (not described in the sources) | — |
| ПРИЗМА | PRIZMA | reactor power / neutron-physical model; reload and ORM calculations | periodic |
| ТАП | TAP | *unknown* (not described in the sources) | — |
| ДРЕГ | DREG | diagnostic registration of parameters — the "black box" | continuous |
| РАО | RAO / "Ural" | emergency-deviation registration and alarms | continuous |

## Tape pool (six ES-5070 reel drives)

| Drive | Job |
|-------|-----|
| 1 | Library (boot + main programs), read-only while running |
| 2 | PRIZMA program tape |
| 3 | Snapshot / archive restart (old operating parameters), rewritten daily |
| 4 | Program buffer (while a tape is prepared), otherwise hot reserve |
| 5 | DREG tape — the permanent record |
| 6 | Reserve |

Unit 4's **drive 5** tape was recovered after the accident and became a primary
investigation source.

## The request device (how an operator addressed the machine)

A seven-column code entered on a keypad, read back on indicators:

- **col 1** — equipment group (Cyrillic letter). Known: К=channel, А=reactor,
  Н=pumps, Д=deaerator. Listed but unresolved: У, П, Т(probable turbine), М.
- **col 2** — section number of that group.
- **col 3** — parameter type (Latin letter). Known: N=power, H=level,
  P=pressure, G=flow/consumption. Listed but unresolved: X, S, T(probable temp).
- **cols 4-7** — instance: a device number, or for a reactor channel the
  horizontal/vertical coordinates in the octal notation used on the panel.

Examples from the source: `А0Р3214` → reactor pressure = "6 kgf/cm²";
`К0G4136` → coolant flow in channel (41,36).

Subroutines (a second keyboard): one digit, sign (+/-), four digits — e.g.
`M 0+0001` shows all channels with coolant flow too low. Utilities are triggered
by buttons and indicated by lamps; utilities **4 and 8** work with the channel
displays.

## Functional control groups (24)

The processor does not hold a wire to every sensor. It asks the **V31M link**,
which commands one of **24 functional control groups**; each group physically
connects the selected sensor line through a commutator, and the link converts the
analogue value to digital. **One connection per group at a time.** Status per
group: busy / computer A / computer B / restricted. Known: groups **1-6** =
coolant flow, **13** = control-rod signals, **15** = mnemonic displays. The rest
are *unknown*.

## Status display (three turquoise EL panels + alarm)

- **Left**: operation and loss-of-power of the control-room elements.
- **Centre**: processors A and B — ready / hold / malfunction, the frame that
  glows around the working one, and the configuration (independent / 1.5 / 2.0).
- **Right**: "restriction of functions" — the eight programs.
- **Top rows**: the 24 functional groups.
- **Alarm**: on a malfunction the alarm sounds and the indication blinks. The
  operator switches the alarm off, then acknowledges; blinking stops and the
  indication stays lit until the fault clears.
- The sound generator: two oscillators (a faster one for rhythm, a slower one for
  tone), a driver and an amplifier on a **-24 V** supply; two potentiometers for
  tone and volume. It was **the last alarm of Unit 4**, in the computer hall.

## DREG — the recorder, and its one crucial caveat

DREG records three kinds of signal:

- **discrete** — states of regulators, valves, pumps;
- **individual analogue** — a value each time (e.g. a coolant parameter);
- **switchable analogue** — a shared converter multiplexed over many sensors
  (e.g. control-rod positions). Reading these takes time.

**The caveat:** DREG timestamps a value when it is *processed*, not when it was
*measured*. Under normal operation the lag is invisible; during the accident it
means the printout can show a state that no longer existed. The reconstruction
models this lag deliberately, so it inherits the same uncertainty the real
investigators had.

## Outputs (pre-1986 — no monitors)

- **Two RDA-80 teletypes** (up to four per machine): one for set-point
  deviations, one for operator actions, one for malfunctions, one in reserve.
  One teletype sits in the control room for operator requests.
- **Two large drum printers** (ATsPU-128): one for the most important parameters,
  one for fast recording for post-event analysis.
- **MTK mnemonic displays** (1661 cells — one per fuel channel, octal notation),
  the **selsyn rod-position display** (mechanical, loud), and the **MTO**
  deviation displays (units 1-2 only; units 3-4 use a second MTK).

## What we build

- `Instrumentation` — the analogue front end: **7,013 sourced signals**.
- `com.ben.csp.skala.core.*` — the machine: `SkalaComplex`, `DregRecorder`,
  `RequestCode`, `SkalaProgram`, `SkalaTapeDrive`, `FunctionalGroup`,
  `SkalaStatusDisplay`.
- `SkalaTicker` — runs it on the server clock, reading the live plant.
- `/skala` — in-game commands: `start`, `halt`, `status`, `programs`, `drives`,
  `groups`, `tape`, `alarm`, `request <code>`.
