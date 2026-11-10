#!/usr/bin/env python3
"""Builds a MAKET: a high-resolution image of one section of an RBMK-style control
panel, to show the technique for reproducing control rooms.

This is a DEMONSTRATOR of the method (a dense panel of real control types — toggle
switches, push-buttons, indicator lamps, an annunciator tile, an analogue meter — with
Cyrillic labels, at realistic proportions and colour conventions). It is NOT a claim
about the exact layout of any particular real panel: the true layouts must be populated
from reference material, which is the whole point of the technique.

The image is written by hand (no image library is available). It is the same picture a
block-entity renderer would paint on a panel face at runtime, and each control has a
rectangle, so a click can be mapped to the control under it.
"""
import zlib, struct, os

W = H = 256
OUT = os.path.join(os.path.dirname(__file__), "..", "..", "serve_csp", "maket-panel.png")
OUT2 = os.path.join(os.path.dirname(__file__), "..", "docs", "maket-panel.png")


def write_png(path, px):
    raw = bytearray()
    for y in range(H):
        raw.append(0)
        for x in range(W):
            r, g, b = px[y][x]
            raw += bytes((r, g, b, 255))

    def chunk(t, d):
        return struct.pack(">I", len(d)) + t + d + struct.pack(">I", zlib.crc32(t + d) & 0xffffffff)

    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", W, H, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(bytes(raw), 9))
    png += chunk(b"IEND", b"")
    open(path, "wb").write(png)


def canvas(fill):
    return [[fill for _ in range(W)] for _ in range(H)]


def rect(px, x0, y0, x1, y1, c):
    for y in range(max(0, y0), min(H, y1 + 1)):
        for x in range(max(0, x0), min(W, x1 + 1)):
            px[y][x] = c


def disc(px, cx, cy, r, c):
    for y in range(int(cy - r), int(cy + r) + 1):
        for x in range(int(cx - r), int(cx + r) + 1):
            if (x - cx) ** 2 + (y - cy) ** 2 <= r * r:
                if 0 <= x < W and 0 <= y < H:
                    px[y][x] = c


def ring(px, cx, cy, r, w, c):
    for y in range(int(cy - r - w), int(cy + r + w) + 1):
        for x in range(int(cx - r - w), int(cx + r + w) + 1):
            d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            if r - w <= d <= r:
                if 0 <= x < W and 0 <= y < H:
                    px[y][x] = c


def line(px, x0, y0, x1, y1, c, t=1):
    n = int(max(abs(x1 - x0), abs(y1 - y0))) + 1
    for i in range(n):
        x = int(round(x0 + (x1 - x0) * i / max(1, n - 1)))
        y = int(round(y0 + (y1 - y0) * i / max(1, n - 1)))
        rect(px, x, y, x + t - 1, y + t - 1, c)


# ---------------------------------------------------------------- tiny 5x7 font
GLYPHS = {
    "А": [".###.", "#...#", "#...#", "#####", "#...#", "#...#", "#...#"],
    "Б": ["#####", "#....", "#....", "####.", "#...#", "#...#", "####."],
    "В": ["####.", "#...#", "#...#", "####.", "#...#", "#...#", "####."],
    "Г": ["#####", "#....", "#....", "#....", "#....", "#....", "#...."],
    "Е": ["#####", "#....", "#....", "####.", "#....", "#....", "#####"],
    "З": ["####.", "....#", "....#", ".###.", "....#", "....#", "####."],
    "К": ["#...#", "#..#.", "#.#..", "##...", "#.#..", "#..#.", "#...#"],
    "Л": ["..###", ".#..#", ".#..#", ".#..#", ".#..#", ".#..#", ".#..#"],
    "М": ["#...#", "##.##", "#.#.#", "#...#", "#...#", "#...#", "#...#"],
    "Н": ["#...#", "#...#", "#...#", "#####", "#...#", "#...#", "#...#"],
    "О": [".###.", "#...#", "#...#", "#...#", "#...#", "#...#", ".###."],
    "П": ["#####", "#...#", "#...#", "#...#", "#...#", "#...#", "#...#"],
    "С": [".###.", "#...#", "#....", "#....", "#....", "#...#", ".###."],
    "Т": ["#####", "..#..", "..#..", "..#..", "..#..", "..#..", "..#.."],
    "Ь": ["#....", "#....", "#....", "####.", "#...#", "#...#", "####."],
    "Щ": ["#.#.#", "#.#.#", "#.#.#", "#.#.#", "#.#.#", "#.#.#", "#####"],
    "5": ["#####", "#....", "#....", "####.", "....#", "....#", "####."],
    "-": [".....", ".....", ".....", "#####", ".....", ".....", "....."],
    " ": [".....", ".....", ".....", ".....", ".....", ".....", "....."],
}


def text(px, x, y, s, c, scale=1):
    cx = x
    for ch in s:
        g = GLYPHS.get(ch, GLYPHS[" "])
        for gy in range(7):
            for gx in range(5):
                if g[gy][gx] == "#":
                    rect(px, cx + gx * scale, y + gy * scale,
                         cx + gx * scale + scale - 1, y + gy * scale + scale - 1, c)
        cx += 6 * scale


def label(px, x, y, w, s, txt_c=(200, 200, 190), plate=(28, 30, 28)):
    rect(px, x, y, x + w, y + 9, plate)
    rect(px, x, y, x + w, y, (60, 62, 58))
    tx = x + max(1, (w - len(s) * 6) // 2)
    text(px, tx, y + 1, s, txt_c)


# ---------------------------------------------------------------- the panel
PANEL = (58, 62, 56)          # painted steel, greenish-grey
PANEL_HI = (74, 78, 70)
PANEL_LO = (40, 43, 38)
BEZEL = (30, 32, 30)
CREAM = (208, 202, 180)
RED = (196, 44, 38)
GREEN = (46, 170, 70)
WHITE_L = (226, 224, 208)


def build():
    px = canvas(PANEL)
    # panel plate with a light top and dark bottom edge, and fixing screws
    rect(px, 0, 0, W - 1, 1, PANEL_HI)
    rect(px, 0, H - 2, W - 1, H - 1, PANEL_LO)
    for (sx, sy) in ((6, 6), (W - 8, 6), (6, H - 8), (W - 8, H - 8)):
        disc(px, sx, sy, 2, BEZEL)

    # --- top row: analogue meter (left) and an annunciator tile (right)
    # meter
    disc(px, 60, 62, 34, BEZEL)
    disc(px, 60, 62, 30, CREAM)
    for i in range(11):
        a = -160 + i * 32
        import math
        r0, r1 = 24, 28
        x0 = 60 + r0 * math.cos(math.radians(a))
        y0 = 62 + r0 * math.sin(math.radians(a))
        x1 = 60 + r1 * math.cos(math.radians(a))
        y1 = 62 + r1 * math.sin(math.radians(a))
        line(px, x0, y0, x1, y1, (40, 40, 40), 1)
    line(px, 60, 62, 60 + 22 * 0.3, 62 - 22, (170, 30, 26), 2)   # needle
    disc(px, 60, 62, 3, (40, 40, 40))
    label(px, 36, 100, 48, "МОЩН")

    # annunciator (блинкер) tile
    rect(px, 120, 24, 224, 96, BEZEL)
    rect(px, 124, 28, 220, 92, (36, 38, 36))
    rect(px, 128, 32, 216, 60, RED)          # lit window
    rect(px, 132, 36, 212, 56, (230, 90, 80))
    label(px, 128, 66, 88, "АЗ-5")
    disc(px, 232, 34, 3, (200, 200, 60))      # lamp test

    # --- middle: two rows of toggle switches (АП-style) in a frame
    rect(px, 20, 118, 236, 158, BEZEL)
    for i in range(8):
        x = 30 + i * 26
        rect(px, x, 124, x + 14, 152, (44, 46, 44))
        rect(px, x + 2, 126, x + 12, 138, WHITE_L)      # toggle body up
        rect(px, x + 2, 140, x + 12, 150, (150, 146, 130))
    label(px, 30, 160, 60, "СЕТЬ")

    # --- lower: push-buttons and indicator lamps
    for i in range(6):
        x = 34 + i * 34
        col = RED if i % 3 == 0 else (30, 30, 30)
        ring(px, x, 190, 12, 3, BEZEL)
        disc(px, x, 190, 10, col)
        disc(px, x - 3, 187, 3, (240, 240, 230))
    label(px, 26, 208, 80, "СТОП")

    for i in range(6):
        x = 40 + i * 34
        col = GREEN if i % 2 == 0 else (170, 40, 36)
        ring(px, x, 234, 9, 3, BEZEL)
        disc(px, x, 234, 7, col)
        disc(px, x - 2, 231, 2, (250, 250, 240))

    return px


if __name__ == "__main__":
    img = build()
    for p in (OUT, OUT2):
        d = os.path.dirname(p)
        if d:
            os.makedirs(d, exist_ok=True)
        write_png(p, img)
        print("wrote", os.path.normpath(p))
