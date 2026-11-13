#!/usr/bin/env python3
"""Minimal ENDF-6 reader: pointwise (MF=3) cross sections -> the JSON cache shape.

The lattice pipeline already consumes a per-nuclide, per-reaction JSON cache
(energy in eV, cross section in barns) produced by fetch_endf.py from the
openmc-data-storage mirror. ROSFOND is distributed as ENDF-6, so to run the same
pipeline on ROSFOND data we need exactly one extra piece: a reader that turns an
ENDF-6 MF=3 section into that same cache shape. That is this module.

Scope: TAB1 pointwise data (the LR=0, NR>=1 case), which is what MF=3 neutron
cross sections are. It does not attempt to be a general ENDF toolkit — no
resonance reconstruction (MF=2), no covariance (MF=33). Those are the caller's
problem (NJOY/OpenMC), and for group averaging we need the pointwise MF=3 grid.

The ENDF-6 fixed-width format: 80-character lines, six 11-character numeric
fields (cols 1-66) and the MAT/MF/MT/line control block (cols 67-80). Numbers may
carry an exponent with no 'E' (e.g. ``1.234567+5`` means 1.234567e+5).
"""
from __future__ import annotations

# ENDF-6 uses 11-character fields, six per line, then the 14-char control block.
_FIELD = 11
_NFIELDS = 6
_LINE = 80


def _num(field: str) -> float:
    """Parse one ENDF-6 numeric field, including the implicit-exponent form."""
    s = field.strip()
    if not s:
        return 0.0
    # an exponent sign with no 'E': 1.234567+5 -> 1.234567e+5
    for i in range(1, len(s)):
        if s[i] in "+-":
            s = s[:i] + "e" + s[i:]
            break
    try:
        return float(s)
    except ValueError:
        return 0.0


def _fields(line: str) -> list[float]:
    line = line.ljust(_LINE)
    return [_num(line[j * _FIELD:(j + 1) * _FIELD]) for j in range(_NFIELDS)]


def _control(line: str) -> tuple[int, int, int]:
    """MAT, MF, MT from cols 67-80 (0-indexed 66-75)."""
    line = line.ljust(_LINE)
    mat = line[66:70].strip()
    mf = line[70:72].strip()
    mt = line[72:75].strip()
    return (int(mat) if mat else 0, int(mf) if mf else 0, int(mt) if mt else 0)


def sections(text: str):
    """Yield (mat, mf, mt, lines) for every section in an ENDF-6 text blob."""
    raw = text.splitlines()
    cur_key = None
    cur: list[str] = []
    for line in raw:
        if not line.strip():
            continue
        mat, mf, mt = _control(line)
        key = (mat, mf, mt)
        if key != cur_key:
            if cur_key is not None:
                yield (*cur_key, cur)
            cur_key, cur = key, []
        cur.append(line)
    if cur_key is not None:
        yield (*cur_key, cur)


def parse_tab1(lines: list[str]) -> tuple[list[float], list[float]]:
    """Parse a TAB1 record's lines into (energy, xs). Assumes NR>=1, LR=0."""
    # line 0: HEAD (ZA, AWR, ...). line 1: QM, QI, 0, LR, NR, NP.
    head2 = _fields(lines[1])
    lr = int(head2[3])
    nr = int(head2[4])
    np_ = int(head2[5])
    if lr != 0:
        raise ValueError(f"MF=3 section with LR={lr} (resonance reconstruction) is out of scope")
    if nr < 1:
        raise ValueError("MF=3 TAB1 with NR=0 (no interpolation law) is out of scope")
    # NR (NBT, INT) pairs follow, packed 3 pairs per line.
    idx = 2
    pairs = []
    while len(pairs) < nr:
        f = _fields(lines[idx]); idx += 1
        for k in range(0, _NFIELDS, 2):
            if len(pairs) < nr:
                pairs.append((int(f[k]), int(f[k + 1])))
    # NP (E, xs) pairs, three pairs (six values) per line.
    energy: list[float] = []
    xs: list[float] = []
    while len(energy) < np_:
        f = _fields(lines[idx]); idx += 1
        for k in range(0, _NFIELDS, 2):
            if len(energy) < np_:
                energy.append(f[k])
                xs.append(f[k + 1])
    return energy, xs


def find_mf3(text: str, mt: int, mat: int | None = None):
    """Return (energy, xs) for MF=3/MT, or None if the section is absent."""
    for m, mf, t, lines in sections(text):
        if mf == 3 and t == mt and (mat is None or m == mat):
            try:
                return parse_tab1(lines)
            except (ValueError, IndexError):
                return None
    return None


def to_cache(energy, xs, *, library: str, za: int,
             mt: int, temperature: str = "294") -> dict:
    """Wrap parsed data in the JSON cache shape the lattice pipeline reads."""
    z = za // 1000
    a = za % 1000
    return {
        "Proton number": z,
        "Mass number": a,
        "Neutron number": a - z,
        "Temperature(K)": temperature,
        "Incident particle": "neutron",
        "MT reaction number": mt,
        "Library": library,
        "energy": [float(v) for v in energy],
        "cross section": [float(v) for v in xs],
    }
