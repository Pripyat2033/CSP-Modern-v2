#!/usr/bin/env python3
"""Which evaluated nuclear data library the lattice calculation runs on.

The mission's target is a NIKIET-lineage library (ROSFOND / ABBN-RF). The
pipeline is written so the SOURCE is a choice, not a hard-coded fact:

    --source endf      ENDF/B-VIII.0, downloaded from the openmc-data-storage
                       JSON mirror (works unattended, no portal access needed).
    --source rosfond   ROSFOND (Russian national evaluated library, ENDF-6),
                       converted from local ENDF-6 files. The IPPE/NEA portals
                       are not reachable from the sandbox, so this source needs
                       the files provided locally (see fetch_rosfond.py).

Each source has its own cache directory, so both can coexist and a run records
which one it used.
"""
from __future__ import annotations

import os

HERE = os.path.dirname(os.path.abspath(__file__))


class Source:
    def __init__(self, name, library, cache_dir, mode, note):
        self.name = name
        self.library = library          # the label written into the data
        self.cache_dir = os.path.join(HERE, cache_dir)
        self.mode = mode                # "remote" or "local"
        self.note = note


SOURCES = {
    "endf": Source(
        "endf", "ENDF/B-VIII.0", "endf_cache", "remote",
        "openmc-data-storage ENDF/B-VIII.0 JSON mirror"),
    "rosfond": Source(
        "rosfond", "ROSFOND-2010", "rosfond_cache", "local",
        "ROSFOND ENDF-6 files, converted locally (portals unreachable in-sandbox)"),
}

DEFAULT = "endf"


def get(name: str | None) -> Source:
    """Resolve a source by name, or from LATTICE_SOURCE, or the default."""
    key = (name or os.environ.get("LATTICE_SOURCE") or DEFAULT).strip().lower()
    if key not in SOURCES:
        raise SystemExit(f"unknown source '{key}' (have: {', '.join(SOURCES)})")
    return SOURCES[key]


def current() -> Source:
    return get(None)
