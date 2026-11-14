# The 1 m-cube question — answered with pixels, not argument

**Question (Ben, 2026-11-12).** "I have a gut feeling the 3D model of the reactor
parts will be 1 m cubes yet again. I do not think it is possible for an AI to
model an RBMK-1000 at any scale as its real, historically compliant separate
parts — for example the graphite core is made of separate individual graphite
blocks of various different sizes, not one gigantic lump."

**Answer.** The graphite is *already* modelled as separate individual blocks,
each pierced by its own bore — and the 200–600 mm axial length variation is
expressible and correct in real Minecraft. Both are shown on screen below. The
one honest gap is that the *whole core* is still laid out at 1:4, where a single
Minecraft block equals a whole graphite block, so the variation is not yet built
across the full stack. That is an engine-wiring gap, not an impossibility.

## What was actually run

The real Fabric client was booted headless in this sandbox (Xvfb + Mesa
llvmpipe, OpenGL 4.5) and driven by a scripted autopilot
(`src/client/java/com/ben/csp/client/Autopilot.java`) that loads a world, issues
server commands, moves the camera and captures true framebuffer screenshots.
Nothing below is an artist's impression.

Every block claim was checked in-world with `execute if block`, which reported:

```
VERIFY-graphite-mid-OK            VERIFY-graphite-column-top-OK
VERIFY-fuel-channel-OK            VERIFY-control-rod-OK
V-lattice-OK                      V-axial-OK
```

## Evidence

`docs/evidence/contact-sheet.png` (four panels):

| panel | what it shows | source |
|---|---|---|
| in-game, top-down | 14×14 lattice of separate graphite blocks, seams visible | `ingame-lattice-closeup.png` |
| render | the repo's real `graphite.json` — a hollow square frame with a bore, tiled | `render-graphite-model.png` |
| render | `graphite_axial_demo.json` — the six real axial lengths side by side | `render-axial-demo-model.png` |
| in-game, close | the same axial-length demo block, stepped profile visible | `ingame-axial-closeup.png` |

The two "render" panels are produced by `tools/render_block_models.py`, a small
raycaster that reads the **actual block-model JSON files** the game loads (it
invents no geometry) and draws the box edges.

## The two facts, stated precisely

**1. The graphite is separate blocks, each with its own bore.** The existing
`models/block/graphite.json` is not a cube: it is a frame of four walls around a
4/16-of-a-block open shaft (a genuine hole — the block is registered
`nonOpaque`), with a lip on top where blocks interlock. The core builder lays one
such block per lattice cell. `docs/RBMK-CORE-GEOMETRY.md` §1 already records the
real numbers (250 × 250 mm cross-section, 114 mm bore, 200–600 mm axial). The
in-game top-down shot shows the seams and bores directly.

**2. The 200–600 mm axial variation is expressible at correct proportions.**
`tools/gen_graphite_axial_demo.py` writes one block model holding six graphite
columns — 200, 250, 300, 400, 500, 600 mm — on the bit scale (1 unit = 62.5 mm,
so the 250 mm cross-section is 4 units and the longest block is 9.6 units, all
inside one Minecraft block's 16-unit model space). Placed in-game, it renders as
a staircase of the six real lengths. The generator and the block are committed;
the block is registered as `csp-modern:graphite_axial_demo`.

## The honest gap (marked, not hidden)

The project currently builds at **1:4** (`RbmkGeometry.METRES_PER_BLOCK = 0.25`),
where one Minecraft block *is* one graphite block. Two consequences:

- At 1:4 the 200–600 mm variation **cannot** be shown: 200 mm is 0.8 of a block
  and 600 mm is 2.4 blocks, so whole 1:4 blocks cannot express it. This is
  exactly the Director's objection, and it is correct *for the 1:4 layout*.
- Minecraft renders every block at 1 m on screen, so at 1:4 a 250 mm graphite
  block appears 4× oversized and the avatar appears 4× too small.

The bit scale removes both problems: one Minecraft block = 1 m (matching the
render), and sub-block geometry places the true 250 mm cell (4 bits) and the true
axial lengths. The demonstration block proves the geometry works. What remains is
to wire the **bit engine** into `RbmkWorldBuilder` so the whole stack is built
that way rather than one-1:4-block-per-cell. That is the next real task; it is not
a question of whether Minecraft can hold the geometry — it demonstrably can.

## Reproduce

```
# headless client + scripted screenshots
Xvfb :99 -screen 0 1280x720x24 &
DISPLAY=:99 LIBGL_ALWAYS_SOFTWARE=1 \
  CSP_AUTOPILOT=/path/to/script.txt CSP_AUTOPILOT_WORLD=cspdemo \
  ./gradlew runClient

# faithful renders of the real model JSON
python3 tools/render_block_models.py docs/evidence
```
