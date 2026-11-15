# Control room — the options space

Ben's framing: two options — (1) bring in the enterprises to build the room to
scale and specification, or (2) forego the control room and wire the test RBMK
straight to SKALA. He asks whether there are others. This note sets out the whole
space honestly, then recommends a *sequence* rather than a choice.

## The real constraints (accepted)

- **The agent cannot see photographs.** Unit identity and board placement from a
  photo can only be done by a human (Ben).
- **Distant labels are too small to OCR.** A board shot from across the room
  cannot be machine-read; only close-ups can. So OCR is not a general answer.
- **Operators do not read the text anyway.** They read *patterns* — colour-coded,
  grouped, flashing. Ben's own observation, and it is the key one.
- **Ben cannot build the room by hand without errors.**
- **No authoritative placement source is currently known** for any RBMK-1000
  board.

## Options

### A. Enterprises build the room *(Ben's option 1)*
In-world NPC enterprises construct to scale and spec.
- **Solves:** manual labour, Ben's error risk.
- **Does NOT solve:** the spec must still exist. An enterprise builds *to a
  drawing*. This moves *who* builds, not *whether we know where things go*.
- **Available now?** No — the enterprise system is not built yet.

### B. Wire RBMK → SKALA direct, run SKALA *(Ben's option 2)*
- **Solves:** gets the experiment running now.
- **Key point:** SKALA **is** the plant's real operator interface. Its outputs are
  the МТО/МТК displays and the teletypes. Running SKALA *is* running the control
  room's brain — just without the physical board.
- **Verdict:** the correct **immediate** move. It defers the skin, it does not
  abandon it.

### C. Annunciator grammar first *(Ben's own insight — the strongest one)*
Operators read **patterns**, not text: which lamp, what **colour**, what
**grouping**, and whether it **flashes** (unacknowledged) or is steady
(acknowledged).
- The **regulation defines this grammar**: аварийные сигналы, предупредительные
  сигналы, АЗ-1…АЗ-5, the "Кз≤1" signals, **МТО = мнемотабло отклонений**,
  **МТК = мнемотабло контроля**.
- This grammar is **derivable today** and is what makes a control room *work* and
  *teach*. It does not need placement.
- **Verdict:** build this — it is the functional room.

### D. Generate the room's content from the plant's signals
`Instrumentation` currently exposes **7,013 signals** — but that is the
**reactor-side analogue** set only, and it is a modelling count, not a plant
figure. It omits all **discrete** signals (valves, switches, protection contacts,
the alarm lamps themselves) and the **whole non-reactor block** (turbine hall,
feedwater, generator, electrical). So the real signal population is **much
larger**, and the room's content is *not* yet fully defined by the model — it must
be extended. See `docs/INSTRUMENTATION-SCOPE.md`.
- **Verdict:** partially available. The reactor analogue core is there; the
  discrete and balance-of-plant signals that fill most of a real board are not.

### E. Unit 3 as the accessible proxy
Unit 3's control room is documented in the Chornobyl Family material and in
photographs, and Unit 3 is the **same construction stage** as Unit 4.
- **Verdict:** a far richer source than scattered photos. Still needs Ben's eyes,
  but the eyes are available.

### F. Convention-derived placement, flagged and verified
Soviet panel practice is highly conventional: mimic diagrams mirror plant
topology, alarms are grouped by system, АЗ buttons sit at hand level under guard.
- **Verdict:** this is **inference from documented convention**, not guessing — but
  it is only legitimate where convention is actually documented, and every such
  placement must be **flagged as inferred** and verified per board as photos
  arrive.

### G. Human-in-the-loop placement tool
Ben places the boards; a tool keeps him honest — a checklist per board, recorded
provenance, and a verification step.
- **Verdict:** directly addresses "I'll make a mistake."

### H. Virtual board (render, not blocks) — **REJECTED**
Stand at a console and read a **rendered** annunciator panel in the UI.
- **Rejected by Ben, correctly.** In 1986 the room is **entirely physical** — every
  lamp is a real lamp cover with a real cable threaded to it. Rendered video walls
  are a **late-1990s / 2000s** development, when the physical МТК makets were torn
  out and replaced by video display walls. That is (a) anachronistic for 1986 and
  (b) a downgrade — the lamp-cover maket is tactile and beautiful; the video wall
  is not.
- **Verdict: not an option for this reconstruction.**

### I. Split the goals
- **Goal 1 — prove/disprove the rundown test.** Needs RBMK + SKALA + procedure.
  **No control room required.**
- **Goal 2 — teach and experience.** Needs the faithful room. **No deadline.**
- **Verdict:** goal 1 is unblocked today; the room never blocked it.

## Recommendation: a sequence, not a choice

1. **Now:** B — run the experiment on RBMK + SKALA; record that the room is
   deferred, not abandoned.
2. **Next:** C + D — build the annunciator grammar and generate the room's content
   from the plant's signals.
3. **The spec itself:** J — document the boards with the people who worked them.
   This is the only route to a placement spec, because none exists in any archive
   (see below).
4. **Physical room, over time:** E + F + G — Unit 3 as the proxy source, placement
   by convention where documented and flagged, Ben placing with a tool that
   catches errors. Then A — the enterprises take over construction once the spec
   exists.

### J. Document the boards with the people who worked them

The spec does not exist, but the reason is not classification — the plant is
remarkably open, and the panels are publicly famous. **The reason is that nobody
ever sat down and documented them.** A spec that was never written cannot be
*found*; it has to be *made*.

The untapped source is **people** — the operators and engineers who stood at the
boards, including people Ben knows who worked at the plant in 1986. The layout
lives in their hands and memory even though it was never recorded. Turning that
memory into a spec — which lamp, which column, how high — is a real method, and it
is the only one that produces a *sourced* placement spec rather than an inferred
one.

Options A and B are not alternatives. B is what unblocks the science today; A is
what builds the room later. C is the piece that makes the room a room. J is the
piece that makes it *correct*.
