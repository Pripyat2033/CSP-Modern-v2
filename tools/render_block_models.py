#!/usr/bin/env python3
"""Render the mod's ACTUAL block models with a small software raycaster.

This is not an artist's impression. It parses the real Minecraft block-model JSON
in src/main/resources/assets/csp-modern/models/block/ - the same files the game
loads - turns each element into a box, and ray-traces the scene. It draws the box
edges and lifts the very dark graphite texture for display, so the individual
blocks, their bores and their seams are unmistakable on screen.

Used to show that the RBMK graphite is built from separate individual blocks
(each pierced by its own bore) at several real axial lengths, rather than one
solid lump.

Usage:  python3 tools/render_block_models.py <output-dir>
"""
import json
import os
import sys

import numpy as np
from PIL import Image

ROOT = "src/main/resources/assets/csp-modern"
PALETTE = [(0.92, 0.36, 0.30), (0.95, 0.66, 0.25), (0.55, 0.80, 0.34),
           (0.30, 0.70, 0.82), (0.62, 0.50, 0.88), (0.90, 0.50, 0.72)]


def load_tex(ref):
    name = ref.split(":", 1)[1]
    path = os.path.join(ROOT, "textures", name + ".png")
    return np.asarray(Image.open(path).convert("RGB")).astype(np.float64) / 255.0


def load_model(name):
    with open(os.path.join(ROOT, "models", "block", name + ".json")) as f:
        m = json.load(f)
    texs = {}
    els = []
    for e in m["elements"]:
        ref = e["faces"].get("up", e["faces"][list(e["faces"])[0]])["texture"].lstrip("#")
        els.append((np.array(e["from"], float) / 16.0,
                    np.array(e["to"], float) / 16.0, ref))
    for k, v in m["textures"].items():
        if isinstance(v, str) and v.startswith("csp-modern:"):
            texs[k.lstrip("#")] = load_tex(v)
    return els, texs


def instances(model, offsets, yscale=1.0):
    els, texs = load_model(model)
    boxes = []
    for ii, off in enumerate(offsets):
        off = np.asarray(off, float)
        for (f, t, ref) in els:
            f = f.copy(); t = t.copy()
            f[1] *= yscale; t[1] *= yscale
            lo = np.minimum(f, t) + off
            hi = np.maximum(f, t) + off
            boxes.append((lo, hi, texs[ref], ii))
    return boxes


def render(boxes, eye, target, size, fov=45.0, up=(0, 1, 0), edge=True,
           bg=(0.30, 0.42, 0.60), gamma=0.5, schematic=False):
    W, H = size
    eye = np.array(eye, float); target = np.array(target, float)
    fwd = target - eye; fwd /= np.linalg.norm(fwd)
    right = np.cross(fwd, np.array(up, float)); right /= np.linalg.norm(right)
    upv = np.cross(right, fwd)

    ys, xs = np.mgrid[0:H, 0:W]
    px = (xs + 0.5) / W * 2 - 1
    py = 1 - (ys + 0.5) / H * 2
    aspect = W / H
    tanf = np.tan(np.radians(fov) / 2)
    dirs = (fwd[None, None, :]
            + right[None, None, :] * (px * tanf * aspect)[..., None]
            + upv[None, None, :] * (py * tanf)[..., None])
    dirs /= np.linalg.norm(dirs, axis=2, keepdims=True)

    N = W * H
    O = np.broadcast_to(eye, (N, 3)).astype(float).copy()
    D = dirs.reshape(N, 3).copy()

    best_t = np.full(N, np.inf)
    best_box = np.full(N, -1, int)
    best_axis = np.zeros(N, int)
    best_sign = np.zeros(N, int)
    best_u = np.zeros(N)
    best_v = np.zeros(N)

    inv = 1.0 / np.where(np.abs(D) < 1e-12, 1e-12, D)
    for bi, (lo, hi, tex, inst) in enumerate(boxes):
        t1 = (lo[None, :] - O) * inv
        t2 = (hi[None, :] - O) * inv
        tmin = np.minimum(t1, t2).max(axis=1)
        tmax = np.maximum(t1, t2).min(axis=1)
        hit = (tmax >= np.maximum(tmin, 0)) & (tmin > 1e-6) & (tmin < best_t)
        if not hit.any():
            continue
        idx = np.nonzero(hit)[0]
        axis = np.argmax(np.minimum(t1, t2)[idx], axis=1)
        sign = np.sign(D[idx, axis])
        P = O[idx] + D[idx] * tmin[idx][:, None]
        u = np.zeros(len(idx)); v = np.zeros(len(idx))
        for k in range(len(idx)):
            ax = axis[k]
            if ax == 1:
                u[k] = (P[k, 0] - lo[0]) / (hi[0] - lo[0])
                v[k] = (P[k, 2] - lo[2]) / (hi[2] - lo[2])
            elif ax == 2:
                u[k] = (P[k, 0] - lo[0]) / (hi[0] - lo[0])
                v[k] = 1 - (P[k, 1] - lo[1]) / (hi[1] - lo[1])
            else:
                u[k] = (P[k, 2] - lo[2]) / (hi[2] - lo[2])
                v[k] = 1 - (P[k, 1] - lo[1]) / (hi[1] - lo[1])
        best_t[idx] = tmin[idx]
        best_box[idx] = bi
        best_axis[idx] = axis
        best_sign[idx] = sign
        best_u[idx] = u
        best_v[idx] = v

    img = np.zeros((N, 3))
    hitmask = best_box >= 0
    idx = np.nonzero(hitmask)[0]
    axis = best_axis[idx]; sign = best_sign[idx]
    u = best_u[idx]; v = best_v[idx]

    L = np.array([-0.45, 0.85, 0.45]); L /= np.linalg.norm(L)
    L2 = np.array([0.6, 0.3, -0.5]); L2 /= np.linalg.norm(L2)
    amb = 0.55
    for k in range(len(idx)):
        lo, hi, tex, inst = boxes[best_box[idx[k]]]
        ax = axis[k]
        if ax == 1:
            n = np.array([0.0, sign[k], 0.0])
        elif ax == 2:
            n = np.array([0.0, 0.0, sign[k]])
        else:
            n = np.array([sign[k], 0.0, 0.0])
        th, tw, _ = tex.shape
        ti = min(th - 1, max(0, int((1 - v[k]) * th)))
        tj = min(tw - 1, max(0, int(u[k] * tw)))
        if schematic:
            base = np.array(PALETTE[inst % len(PALETTE)], float)
        else:
            base = tex[ti, tj]
        lam = amb + 0.6 * max(0.0, float(np.dot(n, L))) + 0.25 * max(0.0, float(np.dot(n, L2)))
        img[idx[k]] = np.clip(base * lam, 0, 1)

    if edge:
        thr = 0.06
        e = (u < thr) | (u > 1 - thr) | (v < thr) | (v > 1 - thr)
        img[idx[e]] *= (0.06 if schematic else 0.25)

    # display gamma so the near-black graphite reads; sky stays flat
    img[~hitmask] = np.array(bg)
    img = np.clip(img, 0, 1) ** gamma
    img[~hitmask] = np.array(bg)
    return (img.reshape(H, W, 3) * 255).astype(np.uint8)


def save(img, name):
    Image.fromarray(img).save(name)
    print("wrote", name, img.shape)


def autoframe(boxes, size, fov, dirv, target_frac=0.72, gamma=0.5, bg=(0.30, 0.42, 0.60), schematic=False):
    lo = np.min([b[0] for b in boxes], 0); hi = np.max([b[1] for b in boxes], 0)
    ctr = (lo + hi) / 2
    rad = np.linalg.norm(hi - lo) / 2
    dirv = np.array(dirv, float); dirv /= np.linalg.norm(dirv)
    dist = rad / np.tan(np.radians(fov) / 2) / target_frac
    eye = ctr + dirv * dist
    return render(boxes, eye=eye, target=ctr, size=size, fov=fov, edge=True,
                  gamma=gamma, bg=bg, schematic=schematic)


def scene_lattice():
    """8x8 lattice of the real graphite block, viewed down at an angle."""
    b = instances("graphite", [(x, 0, z) for x in range(8) for z in range(8)])
    return autoframe(b, (1400, 900), 45, (0.18, 0.95, 0.80), 0.80)


def scene_lattice_side():
    """The same lattice from the side: individual blocks, seams, the interlock lip."""
    b = instances("graphite", [(x, 0, z) for x in range(8) for z in range(2)])
    return autoframe(b, (1400, 700), 40, (0.0, 0.35, 1.0), 0.82)


def scene_heights():
    """One block at each real axial length (200..600 mm), bit scale."""
    lens = [200, 250, 300, 400, 500, 600]
    b = []
    for i, mm in enumerate(lens):
        b += instances("graphite", [(i * 1.6, 0, 0)], yscale=mm / 250.0)
    return autoframe(b, (1500, 760), 34, (0.20, 0.45, 1.0), 0.72)


def scene_parts():
    """The bore in detail: the graphite frame, then the channel that fills it."""
    b = instances("graphite", [(0, 0, 0)])
    b += instances("fuel_channel", [(1.6, 0, 0)])
    b += instances("control_rod", [(3.2, 0, 0)])
    return autoframe(b, (1500, 820), 34, (0.15, 0.55, 1.0), 0.72)


def scene_heights_schematic():
    lens = [200, 250, 300, 400, 500, 600]
    b = []
    for i, mm in enumerate(lens):
        b += instances("graphite", [(i * 1.6, 0, 0)], yscale=mm / 250.0)
    return autoframe(b, (1500, 760), 34, (0.20, 0.45, 1.0), 0.72,
                     bg=(0.97, 0.97, 0.98), gamma=1.0, schematic=True)


def scene_lattice_schematic():
    b = instances("graphite", [(x, 0, z) for x in range(8) for z in range(8)])
    return autoframe(b, (1400, 900), 45, (0.18, 0.95, 0.80), 0.80,
                     bg=(0.97, 0.97, 0.98), gamma=1.0, schematic=True)


if __name__ == "__main__":
    out = sys.argv[1] if len(sys.argv) > 1 else "/tmp"
    save(scene_lattice(), os.path.join(out, "render-lattice.png"))
    save(scene_lattice_side(), os.path.join(out, "render-lattice-side.png"))
    save(scene_heights(), os.path.join(out, "render-heights.png"))
    save(scene_parts(), os.path.join(out, "render-parts.png"))
    save(scene_heights_schematic(), os.path.join(out, "schematic-heights.png"))
    save(scene_lattice_schematic(), os.path.join(out, "schematic-lattice.png"))
