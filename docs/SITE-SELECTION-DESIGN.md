# Design: commanding the zone by talking to people, not by typing commands

Problem Ben raised (2026-11-15): standing up a prombaza currently takes a burst of
slash commands (`/prombaza pos1`, `/pos2`, `/directorate designate`, `/zone
spawn`, …). That is both **too many commands for an admin** and **historically
impossible** — nobody in 1970 entered commands into a terminal. The interface
should be **conversation**, and the plant's own history (`docs/HISTORY-SITE-
SELECTION.md`) shows the real sequence was a survey, a decision, and a director
talking to people.

## The machinery already exists — it is just not used for this

- `entity/dialogue/DialogueListener` turns **plain chat** into speech (any message
  not starting with `/`).
- `entity/dialogue/DialogueEngine` is a **SHRDLU** design: a small closed world, the
  answer is derived and it says plainly when it does not understand. It already
  knows who is within earshot and who was last spoken to.
- `entity/dialogue/DialogueIntent` parses intents: GREET, COME, FOLLOW, STAND_DOWN,
  GO_POST, STATUS, WHO, PLACE, HELP.
- `entity/SekretarEntity` — the secretary — exists and is **not spawned**; the
  `agent/Sekretar` class only logs paperwork.
- `planning/Planerka` is a full **планёрка** model (schedule, open, minutes,
  decisions, summons) — and is wired to nothing.
- `telephony/DialogueListener` already routes speech **down a telephone line**.

So the answer to "we should be able to command the zone by talking" is: **extend
`DialogueIntent` and `WorkerSpeech` with zone intents, and put a secretary in the
office to take the Director's instructions.** No new subsystem; wire what is there.

## The scene to build: the Director arrives at the site

This is the historically-true opening, and it is *all dialogue*:

1. **The Minister's summons.** The Director is told the need (the 1966 decree) and
   given **the survey's candidates** — "sixteen points were surveyed; two remain:
   Ladyzhyn and Kopachi."
2. **The choice.** The Director **chooses the site** — a decision, in words. Not
   `/zone spawn`.
3. **The arrival.** The Director stands in the marsh (the snowy-field scene, staged
   as a reconstruction). A **helicopter** brings officials; there is a toast.
4. **The directorate.** The secretary opens the office; the Director talks to her.
5. **The muster.** The Director talks to the workforce, and the enterprises come
   onto the ground — the `/zone spawn` logic, but **triggered by a conversation**,
   not a command.

## Concrete plan (phased, each phase small)

**Phase A — the secretary who carries the zone. [DONE — 2026-11-15]**
Built, and built so the two paths cannot drift:
- `zone/ZoneOperations` — the zone's operations (found the site, the site report,
  muster the workforce, the desk report, work the wagons) in **one place**. Both
  the operator's commands and the spoken word call it. `ZoneCommand` and
  `PrombazaCommand` were refactored to call it, so the command is no longer a
  second implementation.
- `DialogueIntent` gained the zone intents (`FOUND_SITE`, `SITE_REPORT`, `MUSTER`,
  `PROMBAZA_REPORT`, `DELIVER`), checked **before** the personal intents so "how is
  the site" is a site report and not a worker's status.
- `entity/dialogue/SecretarySpeech` — the secretary's side. She answers the zone
  intents by running the operations and reading back the world's own report.
- `WorkerSpeech` routes zone intents to her; **any other worker says "that is for
  the office, comrade — speak to the secretary."**
- `Muster.placeSecretary` puts a secretary at the office when the site is founded,
  so there is always someone to talk to.

So the player founds and runs the zone **by walking up to the secretary and
speaking** — "found the site", "how is the site", "muster the workforce", "the
desk", "work the wagons". The slash commands still exist as an admin back door,
but they are no longer the only way, and they no longer contain their own copy of
the logic.

**Phase B — the site-choice conversation. [TODO]**
- A `SiteCandidates` model: the 16 surveyed points, 2 finalists, each with its
  sourced facts (distance to load, water, soil).
- The Director picks one by speaking; the choice seeds the prombaza's location and
  name. The `ZoneState.spawn` hardcoded `"pb1"` is replaced by the chosen id —
  this also fixes the supply-line mismatch noted in the compliance-desk doc.

**Phase C — the arrival scene. [TODO]**
- A scripted "helicopter and toast" moment, clearly marked a **reconstruction**
  (the source is unverified — see HISTORY-SITE-SELECTION §7).

## Rules

- **Speech is the interface; commands are the admin back door.** A player should be
  able to run the whole founding by talking. Keep `/zone spawn` for scripting and
  recovery, but it should not be the *only* way.
- **Do not invent dialogue as fact.** Where a line is a reconstruction (the toast),
  say so in the text and in the docs.
- **The secretary is the hub.** She takes the Director's words and dispatches them
  — that is the period-correct interface, and it is already half-built.
