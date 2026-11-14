#!/usr/bin/env python3
"""Render the mod's ACTUAL block models with a small software raycaster.

Not an artist's impression: it parses the real Minecraft block-model JSON in
src/main/resources/assets/csp-modern/models/block/ (the same files the game
loads), turns each element into a box, and ray-traces the scene. It draws the
box edges so the individual blocks and their seams are unmistakable. Used to
show, on screen, that the RBMK graphite is built from separate individual
blocks - each pierced by its own bore - at several real axial lengths.
"""
import json
import os
import sys

import numpy as np
from PIL import Image

ROOT = "src/main/resources/assets/csp-modern"


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
    for off in offsets:
        off = np.asarray(off, float)
        for (f, t, ref) in els:
            f = f.copy(); t = t.copy()
            f[1] *= yscale; t[1] *= yscale
            lo = np.minimum(f, t) + off
            hi = np.maximum(f, t) + off
            boxes.append((lo, hi, texs[ref]))
    return boxes


def render(boxes, eye, target, size, fov=45.0, up=(0, 1, 0), edge=True):
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
    for bi, (lo, hi, tex) in enumerate(boxes):
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

    L = np.array([-0.4, 0.85, 0.45]); L /= np.linalg.norm(L)
    amb = 0.62
    for k in range(len(idx)):
        lo, hi, tex = boxes[best_box[idx[k]]]
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
        base = tex[ti, tj]
        lam = amb + (1 - amb) * max(0.0, float(np.dot(n, L)))
        img[idx[k]] = np.clip(base * lam, 0, 1)

    if edge:
        thr = 0.045
        e = (u < thr) | (u > 1 - thr) | (v < thr) | (v > 1 - thr)
        img[idx[e]] *= 0.35

    img[~hitmask] = np.array([0.53, 0.71, 0.95])
    # display gamma: graphite is genuinely near-black; lift it so geometry reads
    sky = ~hitmask
    img = np.clip(img, 0, 1) ** 0.45
    img[sky] = np.array([0.53, 0.71, 0.95])
    return (img.reshape(H, W, 3) * 255).astype(np.uint8)


def save(img, name):
    Image.fromarray(img).save(name)
    print("wrote", name, img.shape)


def scene_lattice():
    offs = [(x, 0, z) for x in range(8) for z in range(8)]
    boxes = instances("graphite", offs)
    return render(boxes, eye=(3.5, 9.0, 6.5), target=(3.5, 0.0, 3.5),
                  size=(1100, 720), fov=48)


def scene_heights():
    lens = [200, 250, 300, 400, 500, 600]
    boxes = []
    for i, mm in enumerate(lens):
        h = mm / 250.0
        boxes += instances("graphite", [(i * 1.5, 0, 0)], yscale=h)
    return render(boxes, eye=(3.75, 3.4, 8.0), target=(3.75, 0.9, 0.0),
                  size=(1200, 640), fov=42)


def scene_parts():
    boxes = instances("graphite", [(0, 0, 0)])
    boxes += instances("fuel_channel", [(1.5, 0, 0)])
    return render(boxes, eye=(0.75, 1.35, 3.2), target=(0.75, 0.5, 0.0),
                  size=(1000, 720), fov=40)


if __name__ == "__main__":
    out = sys.argv[1] if len(sys.argv) > 1 else "/tmp"
    save(scene_lattice(), os.path.join(out, "render-lattice.png"))
    save(scene_heights(), os.path.join(out, "render-heights.png"))
    save(scene_parts(), os.path.join(out, "render-parts.png"))
