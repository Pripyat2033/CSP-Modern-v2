#!/usr/bin/env python3
"""Generate distinct 16x16 block textures for the RBMK plant equipment.

Pure Python (no PIL): a minimal RGBA PNG writer. Each machine gets its own
silhouette, tint and markings so it is recognisable in the world, per the
Director's request to "make it easier to ID them".
"""
import os
import struct
import zlib

OUT = "src/main/resources/assets/csp-modern/textures/block"
S = 16


def png(path, px):
    """px: list of rows, each a list of (r,g,b,a)."""
    raw = bytearray()
    for row in px:
        raw.append(0)
        for (r, g, b, a) in row:
            raw += bytes((r, g, b, a))
    def chunk(typ, data):
        c = struct.pack(">I", len(data)) + typ + data
        c += struct.pack(">I", zlib.crc32(typ + data) & 0xffffffff)
        return c
    ihdr = struct.pack(">IIBBBBB", S, S, 8, 6, 0, 0, 0)
    data = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", ihdr)
    data += chunk(b"IDAT", zlib.compress(bytes(raw), 9)) + chunk(b"IEND", b"")
    with open(path, "wb") as f:
        f.write(data)


def new(fill):
    return [[fill for _ in range(S)] for _ in range(S)]


def put(px, x, y, c):
    if 0 <= x < S and 0 <= y < S:
        px[y][x] = c


def rect(px, x0, y0, x1, y1, c):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            put(px, x, y, c)


def frame(px, c):
    rect(px, 0, 0, S - 1, 0, c)
    rect(px, 0, S - 1, S - 1, S - 1, c)
    rect(px, 0, 0, 0, S - 1, c)
    rect(px, S - 1, 0, S - 1, S - 1, c)


def shade(c, f):
    return (max(0, min(255, int(c[0] * f))), max(0, min(255, int(c[1] * f))),
            max(0, min(255, int(c[2] * f))), c[3])


STEEL = (150, 155, 162, 255)
DARK = (86, 92, 100, 255)
BLUE = (96, 116, 148, 255)
COPPER = (176, 112, 64, 255)
BRASS = (188, 160, 84, 255)
RED = (168, 52, 44, 255)
WHITE = (222, 226, 230, 255)
BLACK = (30, 32, 36, 255)


def steel_plate(px, base=STEEL):
    """A brushed steel plate with a rivet border - the shared machine look."""
    rect(px, 0, 0, S - 1, S - 1, base)
    for y in range(S):
        for x in range(S):
            if (x + y) % 2 == 0:
                put(px, x, y, shade(base, 0.95))
    frame(px, shade(base, 0.72))
    for (x, y) in [(1, 1), (14, 1), (1, 14), (14, 14), (7, 1), (8, 1), (7, 14), (8, 14)]:
        put(px, x, y, shade(base, 0.55))


def gen_steam_separator():
    """A horizontal drum: bright top highlight, dark belly, riveted end rings."""
    px = new(DARK)
    rect(px, 0, 0, S - 1, S - 1, shade(STEEL, 0.7))
    # cylindrical shading: bright band across the middle-top, dark bottom
    for y in range(S):
        f = 1.15 if 4 <= y <= 7 else (0.95 if 2 <= y <= 9 else 0.62)
        rect(px, 0, y, S - 1, y, shade(STEEL, f))
    # end rings (flanges) with bolts
    for x in (1, 2, 13, 14):
        rect(px, x, 1, x, 14, shade(BLUE, 0.95))
    for y in range(2, 15, 3):
        put(px, 1, y, BLACK)
        put(px, 14, y, BLACK)
    frame(px, shade(DARK, 0.8))
    return px


def gen_turbine():
    """A turbine: machined casing with a big circular flange of bolts."""
    px = new(DARK)
    steel_plate(px, BLUE)
    # central rotor circle
    cx, cy = 7.5, 7.5
    for y in range(S):
        for x in range(S):
            d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            if d < 3.0:
                put(px, x, y, shade(BRASS, 1.0))
            elif d < 3.8:
                put(px, x, y, shade(STEEL, 0.8))
    # bolts around the flange
    import math
    for i in range(8):
        a = i * math.pi / 4
        bx = int(round(cx + 5.6 * math.cos(a)))
        by = int(round(cy + 5.6 * math.sin(a)))
        put(px, bx, by, BLACK)
    return px


def gen_condenser():
    """A condenser: rows of tube ends (circles) in a steel shell."""
    px = new(DARK)
    steel_plate(px, shade(STEEL, 0.85))
    for ry in (3, 6, 9, 12):
        for rx in (3, 6, 9, 12):
            put(px, rx, ry, shade(BLUE, 0.7))
            put(px, rx + 1, ry, shade(BLUE, 1.1))
            put(px, rx, ry + 1, shade(BLUE, 0.6))
    return px


def gen_mcp():
    """A pump: a circular volute casing with a discharge nozzle."""
    px = new(DARK)
    steel_plate(px, shade(STEEL, 0.9))
    cx, cy = 7.5, 8.5
    for y in range(S):
        for x in range(S):
            d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            if d < 4.2:
                put(px, x, y, shade(COPPER, 1.0))
            elif d < 5.0:
                put(px, x, y, shade(STEEL, 0.7))
    # discharge nozzle up
    rect(px, 6, 0, 9, 3, shade(COPPER, 0.85))
    rect(px, 7, 1, 8, 2, BLACK)
    return px


def gen_feedwater():
    """A deaerator tank: domed top, level gauge stripe, ladder rungs."""
    px = new(DARK)
    rect(px, 0, 0, S - 1, S - 1, shade(STEEL, 0.8))
    rect(px, 0, 0, S - 1, 4, shade(BLUE, 1.05))       # domed top
    rect(px, 0, 5, S - 1, S - 1, shade(STEEL, 0.9))
    # water level gauge: a vertical window, half full
    rect(px, 2, 6, 4, 14, shade(BLACK, 1.4))
    rect(px, 2, 10, 4, 14, (70, 150, 210, 255))
    # ladder rungs
    for y in range(6, 15, 2):
        rect(px, 12, y, 14, y, shade(WHITE, 0.8))
    frame(px, shade(DARK, 0.8))
    return px


def gen_eccs():
    """An ECCS accumulator: a pressure vessel with a red/white warning band."""
    px = new(DARK)
    rect(px, 0, 0, S - 1, S - 1, shade(STEEL, 0.85))
    rect(px, 0, 0, S - 1, 3, shade(BLUE, 1.0))          # nitrogen cap
    # red/white hazard band across the middle
    for y in range(6, 10):
        for x in range(2, 14):
            put(px, x, y, RED if (x // 2) % 2 == 0 else WHITE)
    # valve on top
    rect(px, 7, 0, 8, 2, shade(BRASS, 1.0))
    frame(px, shade(DARK, 0.8))
    return px


def gen_pipe():
    """A pipe run: copper with flanges at the edges."""
    px = new(DARK)
    rect(px, 0, 0, S - 1, S - 1, shade(COPPER, 0.9))
    for y in range(S):
        f = 1.1 if 4 <= y <= 7 else (0.9 if 2 <= y <= 10 else 0.65)
        rect(px, 0, y, S - 1, y, shade(COPPER, f))
    for x in (1, 14):
        rect(px, x, 0, x, S - 1, shade(BRASS, 0.9))
    frame(px, shade(BLACK, 1.3))
    return px


def gen_control_rod():
    """A control-rod channel (СУЗ): a graphite column with a bright absorber rod."""
    px = new(DARK)
    rect(px, 0, 0, S - 1, S - 1, shade((120, 122, 126, 255), 0.9))
    # graphite block texture (darker, speckled)
    for y in range(S):
        for x in range(S):
            if (x * 7 + y * 13) % 11 == 0:
                put(px, x, y, shade((120, 122, 126, 255), 0.78))
    # the central absorber rod: bright vertical strip with B4C segment bands
    rect(px, 6, 0, 9, S - 1, shade(BLUE, 1.05))
    rect(px, 7, 0, 8, S - 1, shade(WHITE, 0.95))
    for y in range(2, 15, 4):
        rect(px, 6, y, 9, y, shade(DARK, 0.9))
    frame(px, shade(BLACK, 1.4))
    return px


def gen_ubs():
    """Upper biological shield (Схема Е): serpentinite concrete disc, a plug per
    channel, with the ring of lifting eyes it was craned by."""
    px = new(DARK)
    rect(px, 0, 0, S - 1, S - 1, shade((96, 98, 92, 255), 1.0))   # serpentinite
    for y in range(S):                                            # concrete speckle
        for x in range(S):
            if (x * 5 + y * 11) % 7 == 0:
                put(px, x, y, shade((96, 98, 92, 255), 0.85))
    # the steel-graphite plugs: a regular grid of bright discs
    for gy in (3, 8, 13):
        for gx in (3, 8, 13):
            put(px, gx, gy, shade(STEEL, 1.05))
            put(px, gx + 1, gy, shade(STEEL, 0.8))
            put(px, gx, gy + 1, shade(STEEL, 0.7))
    frame(px, shade(BLACK, 1.3))
    return px


def gen_lbs():
    """Lower biological shield: steel-faced serpentinite slab, heavy and plain."""
    px = new(DARK)
    rect(px, 0, 0, S - 1, S - 1, shade((110, 112, 116, 255), 0.95))
    for y in range(S):
        for x in range(S):
            if (x + 2 * y) % 4 == 0:
                put(px, x, y, shade((110, 112, 116, 255), 0.82))
    # a central support ring
    for y in range(S):
        for x in range(S):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if 4.6 < d < 5.4:
                put(px, x, y, shade(STEEL, 0.9))
    frame(px, shade(BLACK, 1.3))
    return px


def gen_vessel():
    """The reactor vessel (кожух): a vertical steel shell with a weld seam."""
    px = new(DARK)
    rect(px, 0, 0, S - 1, S - 1, shade(STEEL, 0.85))
    for y in range(S):
        f = 1.1 if (y % 8) < 2 else 0.9
        rect(px, 0, y, S - 1, y, shade(STEEL, f))
    rect(px, 7, 0, 8, S - 1, shade(DARK, 1.05))     # vertical weld seam
    for y in (2, 13):                                # horizontal seams
        rect(px, 0, y, S - 1, y, shade(DARK, 1.1))
    frame(px, shade(BLACK, 1.25))
    return px


def gen_water_tank():
    """Side biological-shield water tank: a compartment of water with a steel
    frame and a level mark - the gamma shield around the vessel."""
    px = new(DARK)
    rect(px, 0, 0, S - 1, S - 1, (54, 112, 168, 255))          # water
    for y in range(S):
        for x in range(S):
            if (x * 3 + y * 5) % 6 == 0:
                put(px, x, y, (72, 138, 196, 255))             # ripples
    # steel compartment frame
    rect(px, 0, 0, S - 1, 1, shade(STEEL, 0.9))
    rect(px, 0, S - 2, S - 1, S - 1, shade(STEEL, 0.9))
    rect(px, 0, 0, 1, S - 1, shade(STEEL, 0.8))
    rect(px, S - 2, 0, S - 1, S - 1, shade(STEEL, 0.8))
    rect(px, 7, 0, 8, S - 1, shade(STEEL, 0.75))               # divider
    return px


def gen_turbine_cover():
    """The turbine's cylinder cover: a big white painted circle with a ring of
    bolts — what actually shows in the machine hall (1 HP + 4 LP)."""
    px = new(DARK)
    rect(px, 0, 0, S - 1, S - 1, shade(WHITE, 0.9))
    cx, cy = 7.5, 7.5
    for y in range(S):
        for x in range(S):
            d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            if d < 2.2:
                put(px, x, y, shade(WHITE, 1.05))       # hub
            elif d < 6.2:
                put(px, x, y, shade(WHITE, 0.82))       # dished face
            else:
                put(px, x, y, shade(STEEL, 0.75))       # rim
    import math
    for i in range(8):                                  # bolt ring
        a = i * math.pi / 4
        bx = int(round(cx + 5.0 * math.cos(a)))
        by = int(round(cy + 5.0 * math.sin(a)))
        put(px, bx, by, shade(DARK, 1.1))
    frame(px, shade(DARK, 0.85))
    return px


def gen_generator():
    """The TVV-500 generator body: a long horizontal machine with a terminal box,
    a yellow nameplate band and end bells."""
    px = new(DARK)
    rect(px, 0, 0, S - 1, S - 1, shade(STEEL, 0.85))
    for y in range(S):                                  # cylindrical shading
        f = 1.12 if 4 <= y <= 7 else (0.95 if 2 <= y <= 10 else 0.68)
        rect(px, 0, y, S - 1, y, shade(STEEL, f))
    for x in (1, 2, 13, 14):                            # end bells
        rect(px, x, 1, x, 14, shade(BLUE, 0.95))
    rect(px, 2, 3, 13, 4, (200, 176, 80, 255))          # yellow nameplate band
    rect(px, 5, 5, 10, 9, shade(DARK, 1.15))            # terminal box
    frame(px, shade(BLACK, 1.2))
    return px


def gen_mcp_motor():
    """The ГЦН's 6 kV drive motor: a grey induction motor with cooling fins."""
    px = new(DARK)
    rect(px, 0, 0, S - 1, S - 1, shade((150, 152, 156, 255), 0.95))
    for y in range(2, 14, 2):                           # cooling fins
        rect(px, 1, y, 14, y, shade((150, 152, 156, 255), 0.78))
    rect(px, 0, 0, S - 1, 2, shade(BLUE, 1.0))          # terminal cap
    rect(px, 6, 0, 9, 3, shade(DARK, 1.1))              # terminal box
    frame(px, shade(DARK, 1.2))
    return px


def gen_containment_wall():
    """The wall of a sealed compartment: heavy reinforced concrete, board-marked,
    with the steel reinforcement showing at the seams. This is the containment."""
    base = (128, 126, 120, 255)
    px = new(base)
    for y in range(S):
        for x in range(S):
            if (x * 3 + y * 7) % 5 == 0:
                put(px, x, y, shade(base, 0.9))
    # formwork board marks: horizontal bands
    for y in (3, 4, 9, 10):
        rect(px, 0, y, S - 1, y, shade(base, 0.82))
    # reinforcement / aggregate speckle
    for (x, y) in [(2, 2), (12, 6), (5, 12), (13, 13), (8, 7)]:
        put(px, x, y, shade((90, 88, 84, 255), 1.0))
    frame(px, shade((70, 68, 64, 255), 1.0))
    return px


def gen_containment_relief():
    """A pressure-relief panel set into the floor: a steel plate with a vent grid,
    the relief assembly that dumps steam to the distribution corridor."""
    px = new(shade(STEEL, 0.7))
    rect(px, 1, 1, 14, 14, shade(STEEL, 0.95))
    for y in range(3, 13, 2):                       # vent louvres
        rect(px, 3, y, 12, y, shade(DARK, 1.1))
    for x in (2, 13):                               # bolt columns
        for y in range(2, 14, 3):
            put(px, x, y, shade(STEEL, 1.2))
    frame(px, shade(BLACK, 1.3))
    return px


def gen_containment_door():
    """A blast door in a compartment wall: a heavy steel door with a wheel and a
    hazard stripe, sealing the box for its design-basis accident."""
    px = new(shade(STEEL, 0.8))
    rect(px, 2, 1, 13, 14, shade(STEEL, 1.0))
    for y in range(1, 15):                          # panel seams
        if y % 4 == 0:
            rect(px, 2, y, 13, y, shade(STEEL, 0.7))
    for y in range(1, 15):                          # hazard stripe band
        for x in range(2, 14):
            if (x + y) % 4 < 2:
                put(px, x, y, (196, 172, 60, 255))  # yellow
            else:
                put(px, x, y, (40, 40, 44, 255))    # black
    # the hand wheel
    for (dx, dy) in [(0, 0), (1, 0), (-1, 0), (0, 1), (0, -1)]:
        put(px, 7 + dx, 7 + dy, shade(STEEL, 1.35))
    put(px, 7, 7, shade(COPPER, 1.0))
    frame(px, shade(BLACK, 1.3))
    return px


TEXTURES = {
    "steam_separator": gen_steam_separator,
    "turbine": gen_turbine,
    "turbine_cover": gen_turbine_cover,
    "generator": gen_generator,
    "condenser": gen_condenser,
    "mcp": gen_mcp,
    "mcp_motor": gen_mcp_motor,
    "feedwater": gen_feedwater,
    "eccs_accumulator": gen_eccs,
    "plant_pipe": gen_pipe,
    "control_rod": gen_control_rod,
    "ubs": gen_ubs,
    "lbs": gen_lbs,
    "vessel": gen_vessel,
    "water_tank": gen_water_tank,
    "containment_wall": gen_containment_wall,
    "containment_relief": gen_containment_relief,
    "containment_door": gen_containment_door,
}


def gen_controller(base):
    """A machine controller: a control panel with indicator lamps and vents, so
    it reads as the machine's brain, not just another casing block."""
    px = new(shade(base, 0.45))
    rect(px, 1, 1, 14, 14, shade(base, 0.9))
    rect(px, 2, 2, 13, 6, shade(base, 1.15))               # instrument panel
    put(px, 3, 4, (90, 240, 120, 255))                     # running
    put(px, 5, 4, (250, 210, 90, 255))                     # standby
    put(px, 7, 4, (240, 90, 90, 255))                      # trip
    for x in range(9, 13):                                 # gauge needle strip
        put(px, x, 4, shade(base, 1.35))
    for y in range(9, 14, 2):                              # cooling vents
        rect(px, 3, y, 12, y, shade(base, 0.6))
    rect(px, 2, 8, 13, 8, shade(BLACK, 1.2))               # label stripe
    frame(px, shade(BLACK, 1.3))
    return px


# controller base colours: steel with a machine-specific tint
CONTROLLERS = {
    "steam_separator_controller": (150, 150, 158, 255),
    "turbine_controller": (120, 130, 150, 255),
    "condenser_controller": (110, 140, 160, 255),
    "mcp_controller": (140, 140, 120, 255),
    "deaerator_controller": (130, 150, 170, 255),
    "eccs_controller": (170, 120, 120, 255),
}

if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    for name, fn in TEXTURES.items():
        path = os.path.join(OUT, name + ".png")
        png(path, fn())
        print("wrote", path, os.path.getsize(path), "bytes")
    for name, base in CONTROLLERS.items():
        path = os.path.join(OUT, name + ".png")
        png(path, gen_controller(base))
        print("wrote", path, os.path.getsize(path), "bytes")
