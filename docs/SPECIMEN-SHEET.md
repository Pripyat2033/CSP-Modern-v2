# SPECIMEN SHEET — what "Science Grade or Better" means here

This is a standing policy, agreed with the Director. It exists because a reconstruction
of ChNPP must not contain anything a real *энергетик* would fail to recognise, and must
not be missing anything they would look for.

## The principle

**Every element is one a real operator would recognise. Nothing is invented.**

A person trained on the real plant should be able to walk our site and know what every
control is, what it does, and where it is — because it is where it was and does what it
did. This is the test for every wall, panel, switch and label.

## The rules

1. **No invented controls, rooms, machines, or documents.** If it is not in the
   reference, it does not go in.
2. **No procedural generation of the plant.** Layouts are placed, not grown. (The
   subsurface geology may be *derived* by process, because the real substrate is itself
   a product of process; the *built plant* may not.)
3. **Every element traces to a reference** — a drawing, a photograph, a manual, an
   official account. The reference is recorded with the element.
4. **Where references conflict or are missing, the element is marked `[UNSOURCED]` and
   flagged to the Director.** We never fill a gap by guessing.
5. **Systemic fidelity is mandatory; visual fidelity is as high as the platform allows.**
   The *logic, position, label and state* of a control must be right (Science Grade).
   The *paint finish* need not be photoreal.

## The density method (how a control room is reproduced)

A real control room has hundreds of controls, each far smaller than a block. We do not
model each as a 3D object. Instead, the technique already proven twice in this project:

- a **panel is a high-resolution picture**, generated from the real layout and painted
  onto the block faces (as the генплан already does), and
- **each control is a named rectangle on that picture**, with a state and a clickable
  hit-box read from the picture coordinates (as the rotary dial already does).

This gives hundreds of controls per room — every lamp, switch and tell-tale in its
correct place with its correct label — at the cost of one texture and a small table,
not hundreds of meshes.

## Acceptance test

> Would a real operator find anything here that was not there, or miss anything that was?
> **Invented → fail. Missing → fail. Otherwise → pass.**

## The MAKET is not shippable (Director's ruling, OPB-82)

The MAKET is a demonstration of the *method* only. It must **never** be shipped or used as
a control panel, because it does not comply with the mandated Soviet regulations — in
particular **OPB-82** (the General Regulations for Nuclear Power Plant Safety) and the
Technical and General Regulations they belong to. A control panel that ignores the
mandated regulations would be a serious fault, and the Director is right to refuse it.

The method stands; the MAKET does not. Any real panel must be built to OPB-82 and the
plant's own design documentation, from references, and checked against them.

## The MAKET

`docs/maket-panel.png` (also at the download page) is a MAKET — a demonstration of the
method: a dense panel of real control types (toggle switches, push-buttons, indicator
lamps, an annunciator tile, an analogue meter) with Cyrillic labels. It shows that the
density is achievable. **It is not a claim about any particular real panel's layout.**
The true layouts must be populated from reference material — which is the point.
