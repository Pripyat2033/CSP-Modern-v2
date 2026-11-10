#!/usr/bin/env python3
"""Generates the block textures and 3D block models for the site devices
(rotary telephone, genplan board, teleprinter), so they read as machines and not
1 m cubes.

Textures are written as 16x16 RGBA PNGs by hand (no image library is available).
Models are emitted as Minecraft block models with explicit boxes (elements). The
telephone has ten dial states so the rotary dial can be turned in play.
"""
import zlib, struct, os, json, math

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources",
                    "assets", "csp-modern")
TEX = os.path.join(ROOT, "textures", "block")
MODELS = os.path.join(ROOT, "models", "block")
STATES = os.path.join(ROOT, "blockstates")
for d in (TEX, MODELS, STATES):
    os.makedirs(d, exist_ok=True)

W = H = 16


def write_png(path, pixels):
    raw = bytearray()
    for y in range(H):
        raw.append(0)
        for x in range(W):
            r, g, b, a = pixels[y][x]
            raw += bytes((r, g, b, a))

    def chunk(t, d):
        return struct.pack(">I", len(d)) + t + d + struct.pack(">I", zlib.crc32(t + d) & 0xffffffff)

    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", W, H, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(bytes(raw), 9))
    png += chunk(b"IEND", b"")
    open(path, "wb").write(png)


def canvas(fill=(0, 0, 0, 255)):
    return [[fill for _ in range(W)] for _ in range(H)]


def rect(px, x0, y0, x1, y1, c):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            if 0 <= x < W and 0 <= y < H:
                px[y][x] = c


def dot(px, x, y, c):
    if 0 <= x < W and 0 <= y < H:
        px[y][x] = c


def speckle(px, c, step, seed=1):
    n = seed
    for y in range(H):
        for x in range(W):
            n = (n * 1103515245 + 12345) & 0x7fffffff
            if n % step == 0:
                px[y][x] = c


# ---------------------------------------------------------------- textures

def tex_vertushka_body():
    px = canvas((108, 30, 30, 255))
    rect(px, 0, 0, W - 1, 0, (140, 46, 46, 255))
    rect(px, 0, 0, 0, H - 1, (140, 46, 46, 255))
    rect(px, 0, H - 1, W - 1, H - 1, (78, 20, 20, 255))
    rect(px, W - 1, 0, W - 1, H - 1, (78, 20, 20, 255))
    rect(px, 2, 2, W - 3, H - 3, (120, 36, 36, 255))
    for (x, y) in ((3, 3), (12, 3), (3, 12), (12, 12)):
        dot(px, x, y, (60, 14, 14, 255))
    speckle(px, (96, 26, 26, 255), 11, seed=7)
    return px


def tex_vertushka_handset():
    px = canvas((26, 26, 28, 255))
    rect(px, 0, 3, W - 1, 5, (44, 44, 48, 255))
    rect(px, 0, 11, W - 1, 12, (18, 18, 20, 255))
    speckle(px, (36, 36, 40, 255), 9, seed=3)
    return px


def tex_vertushka_dial(turn):
    """A rotary dial that reads as one: a cream disc with ten large dark finger holes,
    rotated by `turn` steps (36 deg each) so the holes move as the dial is used. The
    corners are transparent, so on the block the disc reads as round, not square."""
    px = canvas((0, 0, 0, 0))
    cx = cy = 7.5
    for y in range(H):
        for x in range(W):
            d = math.hypot(x - cx, y - cy)
            if d <= 7.7:
                px[y][x] = (46, 46, 50, 255)          # dark rim
            if d <= 7.0:
                px[y][x] = (204, 198, 182, 255)       # cream dial face
            if d <= 6.5:
                px[y][x] = (224, 218, 202, 255)
    for i in range(10):
        a = math.radians(i * 36 + 18 + turn * 36)
        hx = cx + 5.0 * math.cos(a)
        hy = cy + 5.0 * math.sin(a)
        for y in range(H):
            for x in range(W):
                if math.hypot(x - hx, y - hy) <= 1.15:
                    px[y][x] = (24, 24, 28, 255)      # the finger hole
    for y in range(H):
        for x in range(W):
            if math.hypot(x - cx, y - cy) <= 1.0:
                px[y][x] = (40, 40, 44, 255)          # centre / finger stop
    return px


def tex_teletype_body():
    px = canvas((74, 79, 85, 255))
    rect(px, 0, 0, W - 1, 1, (96, 101, 108, 255))
    rect(px, 0, H - 2, W - 1, H - 1, (54, 58, 63, 255))
    for y in range(4, H - 3, 3):
        rect(px, 2, y, W - 3, y, (60, 64, 69, 255))
    speckle(px, (84, 89, 95, 255), 13, seed=5)
    return px


def tex_teletype_keys():
    px = canvas((34, 37, 41, 255))
    for row in range(4):
        y = 3 + row * 3
        for col in range(6):
            x = 1 + col * 2 + (1 if row % 2 else 0)
            rect(px, x, y, x, y + 1, (196, 190, 174, 255))
            rect(px, x, y, x, y, (226, 222, 210, 255))
    return px


def tex_teletype_paper():
    px = canvas((232, 226, 208, 255))
    for y in range(3, H - 2, 2):
        rect(px, 2, y, W - 4, y, (150, 146, 134, 255))
    rect(px, 0, 0, W - 1, 0, (208, 202, 186, 255))
    return px


def tex_genplan_board():
    px = canvas((58, 46, 34, 255))
    rect(px, 0, 0, W - 1, 1, (82, 66, 48, 255))
    rect(px, 0, 0, 1, H - 1, (82, 66, 48, 255))
    rect(px, 0, H - 2, W - 1, H - 1, (40, 32, 24, 255))
    rect(px, W - 2, 0, W - 1, H - 1, (40, 32, 24, 255))
    speckle(px, (68, 54, 40, 255), 15, seed=9)
    return px


# ---------------------------------------------------------------- models

FACES = ("down", "up", "north", "south", "west", "east")


def box(f, t, tex, uv=None):
    faces = {}
    for face in FACES:
        fdef = {"texture": tex}
        if uv is not None:
            fdef["uv"] = uv
        faces[face] = fdef
    return {"from": f, "to": t, "faces": faces}


def emit_model(name, textures, elements):
    with open(os.path.join(MODELS, name + ".json"), "w") as fh:
        json.dump({"credit": "CSP-Modern", "textures": textures, "elements": elements},
                  fh, indent=2)


def vertushka_elements(dial_tex):
    return [
        box([2, 0, 4], [14, 7, 13], "#body"),             # the bakelite body
        box([4.0, 0.5, 3.4], [12.0, 6.5, 3.9], "#dial", uv=[0, 0, 16, 16]),  # the round dial, whole disc mapped
        box([3, 7, 5], [13, 7.7, 11], "#handset"),        # the handset, resting on top
        box([2.5, 7, 5], [4.5, 8.7, 11], "#handset"),
        box([11.5, 7, 5], [13.5, 8.7, 11], "#handset"),
    ]


def build_models():
    t = "csp-modern:block/"
    for d in range(10):
        tex = {
            "particle": t + "vertushka_body",
            "body": t + "vertushka_body",
            "handset": t + "vertushka_handset",
            "dial": t + "vertushka_dial_" + str(d),
        }
        name = "vertushka" if d == 0 else "vertushka_dial_" + str(d)
        emit_model(name, tex, vertushka_elements(tex))

    emit_model("teletype_printer", {
        "particle": t + "teletype_body",
        "body": t + "teletype_body",
        "keys": t + "teletype_keys",
        "paper": t + "teletype_paper",
    }, [
        box([1, 0, 2], [15, 4, 14], "#body"),
        box([3, 4, 3], [13, 7, 9], "#body"),
        box([2, 4, 9.5], [14, 4.9, 13], "#keys"),
        box([4.5, 7, 4.0], [11.5, 10, 4.3], "#paper"),
    ])

    emit_model("genplan", {
        "particle": t + "genplan_board",
        "board": t + "genplan_board",
    }, [
        box([0, 0, 8], [16, 16, 16], "#board"),
    ])


def build_blockstate():
    variants = {}
    for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for d in range(10):
            model = "csp-modern:block/vertushka" if d == 0 \
                else "csp-modern:block/vertushka_dial_" + str(d)
            variants["dial=" + str(d) + ",facing=" + facing] = {"model": model, "y": y}
    with open(os.path.join(STATES, "vertushka.json"), "w") as fh:
        json.dump({"variants": variants}, fh, indent=2)


if __name__ == "__main__":
    write_png(os.path.join(TEX, "vertushka_body.png"), tex_vertushka_body())
    write_png(os.path.join(TEX, "vertushka_handset.png"), tex_vertushka_handset())
    for d in range(10):
        write_png(os.path.join(TEX, "vertushka_dial_%d.png" % d), tex_vertushka_dial(d))
    write_png(os.path.join(TEX, "teletype_body.png"), tex_teletype_body())
    write_png(os.path.join(TEX, "teletype_keys.png"), tex_teletype_keys())
    write_png(os.path.join(TEX, "teletype_paper.png"), tex_teletype_paper())
    write_png(os.path.join(TEX, "genplan_board.png"), tex_genplan_board())
    build_models()
    build_blockstate()
    print("generated textures, models and the vertushka blockstate")
