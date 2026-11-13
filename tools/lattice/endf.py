#!/usr/bin/env python3
"""Pointwise cross-section access on top of the cached ENDF/B-VIII.0 JSON.

Energies are in eV, cross sections in barns (1 b = 1e-24 cm^2). Interpolation is
log-log between points (the ENDF standard for the resonance and fast ranges).

Provides group averaging against a weight function and the Bondarenko (narrow
resonance) effective cross section used for resonance self-shielding.
"""
from __future__ import annotations

import json
import os

import numpy as np

import sources

HERE = os.path.dirname(os.path.abspath(__file__))

# material labels produced by fetch_endf.py
MT_TOTAL, MT_ELASTIC, MT_INELASTIC, MT_N2N, MT_FISSION, MT_CAPTURE = 1, 2, 4, 16, 18, 102


def cache_dir() -> str:
    """The active source's cache directory (LATTICE_SOURCE / --source selects it)."""
    return sources.current().cache_dir


class Xs:
    """A pointwise cross section, log-log interpolated."""

    def __init__(self, e: np.ndarray, x: np.ndarray):
        # drop duplicate/non-increasing energies, keep strictly increasing
        keep = np.concatenate(([True], np.diff(e) > 0))
        self.e = e[keep]
        self.x = x[keep]
        self.loge = np.log(self.e)

    def at(self, energy):
        energy = np.asarray(energy, dtype=float)
        le = np.log(np.clip(energy, self.e[0], self.e[-1]))
        lx = np.interp(le, self.loge, np.log(np.maximum(self.x, 1e-30)))
        return np.exp(lx)


def load(label: str, mt: int):
    """Load a cached reaction, or None if it was not available."""
    path = os.path.join(cache_dir(), f"{label}_{mt}.json")
    if not os.path.exists(path):
        return None
    with open(path) as f:
        d = json.load(f)
    return Xs(np.asarray(d["energy"], dtype=float), np.asarray(d["cross section"], dtype=float))


def group_average(xs, bounds, weight):
    """Flux-weighted average of xs over each energy group.

    bounds: increasing group boundaries (eV), length G+1 (low -> high).
    weight: callable w(E) -> the assumed weighting spectrum.
    Returns array of length G (group 1 = highest energy, as in the reactor code).
    """
    G = len(bounds) - 1
    out = np.zeros(G)
    for g in range(G):
        lo, hi = bounds[g], bounds[g + 1]
        # sample log-uniformly inside the group for the integral
        E = np.exp(np.linspace(np.log(lo), np.log(hi), 24))
        w = weight(E)
        out[G - 1 - g] = np.trapezoid(xs.at(E) * w, np.log(E)) / np.trapezoid(w, np.log(E))
    return out


def effective_capture(xs_cap, bounds, weight, sigma0, temp_k=294.0):
    """Bondarenko narrow-resonance effective capture cross section per group.

    sigma0 is the background (dilution) cross section per absorber atom in barns;
    the NR flux is phi(E) ~ w(E)/(sigma_cap(E) + sigma0).
    """
    G = len(bounds) - 1
    out = np.zeros(G)
    for g in range(G):
        lo, hi = bounds[g], bounds[g + 1]
        E = np.exp(np.linspace(np.log(lo), np.log(hi), 64))
        w = weight(E)
        s = xs_cap.at(E)
        phi = w / (s + sigma0)
        out[G - 1 - g] = np.trapezoid(s * phi, np.log(E)) / np.trapezoid(phi, np.log(E))
    return out


def weight_1_over_e(E):
    """A 1/E slowing-down weight (adequate above the thermal cut)."""
    return 1.0 / E


def weight_combined(E):
    """Maxwellian below ~0.625 eV, 1/E above — the standard lattice weight."""
    E = np.asarray(E, dtype=float)
    kt = 0.0253
    w = np.where(E < 0.625,
                 (E / kt ** 2) * np.exp(-E / kt),   # ~ Maxwellian (E*exp(-E/kt))
                 1.0 / E)
    return w
