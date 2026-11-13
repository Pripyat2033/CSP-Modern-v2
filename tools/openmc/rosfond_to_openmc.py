#!/usr/bin/env python3
"""Convert ROSFOND-2010 ENDF-6 files into an OpenMC HDF5 data library.

This is the missing half of "run OpenMC on ROSFOND instead of ENDF/B": OpenMC
does not read ENDF-6 directly, it reads its own HDF5 format. This script takes
ROSFOND ENDF-6 files and produces the HDF5 files + cross_sections.xml that
OpenMC needs, so the same rbmk_cell.py / rbmk_void_coeff.py models run unchanged
on the Soviet/Russian data.

    python3 rosfond_to_openmc.py --from <dir-of-endf-6-files> --out <hdf5-dir>
    python3 rosfond_to_openmc.py --from <dir> --out <hdf5-dir> --list

The nuclides it converts are the ones openmc/rbmk_cell.py actually consumes:
U, O, H, C, Zr, Nb, B, Fe, Ni, Cr, Gd, Er, He. Files are matched by their ENDF
material number (MAT) / ZA, so a ROSFOND file named however IPPE named it will
still be picked up.

Nothing is downloaded here and nothing is faked: a nuclide that is not present
is reported as missing, not invented. Per the mission rule, the source of every
value is recorded (see the emitted MANIFEST).
"""
from __future__ import annotations

import argparse
import glob
import json
import os
import sys

try:
    import openmc.data as data
except ImportError:
    sys.exit("OpenMC python API not found. Run inside the OpenMC environment "
             "(conda env 'openmc-env', or the WSL install from docs/OPENMC-WINDOWS-WSL.md).")

# element symbol -> (Z, [stable A]) for the natural-element fallback.
NATURAL = {
    "H": (1, [1, 2]), "B": (5, [10, 11]), "C": (6, [12, 13]), "N": (7, [14, 15]),
    "O": (8, [16, 17, 18]), "Cr": (24, [50, 52, 53, 54]), "Fe": (26, [54, 56, 57, 58]),
    "Ni": (28, [58, 60, 61, 62, 64]), "Zr": (40, [90, 91, 92, 94, 96]),
    "Nb": (41, [93]), "Gd": (64, [155, 157]), "Er": (68, [166, 167]),
    "U": (92, [234, 235, 238]), "Pu": (94, [239, 240, 241]), "He": (2, [4]),
}

# What rbmk_cell.py needs, as element symbols.
NEEDED = ["U", "O", "H", "C", "Zr", "Nb", "B", "Fe", "Ni", "Cr", "Gd", "Er", "He"]


def _endf_float(s: str) -> float:
    """Parse an ENDF-6 float such as ' 2.906300+4' (no 'e')."""
    s = s.strip()
    for i in range(len(s) - 1, 0, -1):
        if s[i] in "+-":
            s = s[:i] + "e" + s[i:]
            break
    return float(s)


def _meta(path: str):
    """Read the ENDF head record (MF=1, MT=451) to get (Z, A, MAT).

    The ZA is the first field of the head record (Z*1000 + A), e.g. 29063 for
    Cu-63. The MAT number (columns 67-70) is NOT the ZA and must not be used.
    """
    with open(path, "r", errors="ignore") as fh:
        for line in fh:
            if len(line) < 75:
                continue
            if line[70:72].strip() == "1" and line[72:75].strip() == "451":
                try:
                    za = int(round(_endf_float(line[:11])))
                except ValueError:
                    continue
                if za <= 0:
                    continue
                return za // 1000, za % 1000, line[66:70].strip()
    return None


def scan(src_dir: str):
    """Return {element: [(za, path), ...]} for every ENDF-6 file in src_dir.

    Files are identified by their ENDF head record, not their filename, so a
    ROSFOND file named however IPPE named it is still picked up.
    """
    found: dict[str, list] = {}
    for path in sorted(glob.glob(os.path.join(src_dir, "**", "*"), recursive=True)):
        if not os.path.isfile(path):
            continue
        meta = _meta(path)
        if meta is None:
            continue
        z, a, _ = meta
        el = _z_to_symbol(z)
        if el is None:
            continue
        found.setdefault(el, []).append((z * 1000 + a, path))
    return found


def _z_to_symbol(z: int) -> str | None:
    return data.ATOMIC_SYMBOL.get(z)


def convert_one(za: int, path: str, out_dir: str) -> str | None:
    """Convert one ENDF-6 file to OpenMC HDF5 by running NJOY.

    OpenMC's Python API refuses to write HDF5 directly from an ENDF-origin
    object (``IncidentNeutron.export_to_hdf5`` raises NotImplementedError for
    ENDF-sourced data). The supported route is NJOY: ``from_njoy`` processes the
    ENDF-6 with NJOY2016 into ACE/PENDF and builds the HDF5 object from that.
    """
    try:
        nuc = data.IncidentNeutron.from_njoy(path)
    except Exception as exc:  # noqa: BLE001 - report and continue
        print(f"    ! {os.path.basename(path)}: {type(exc).__name__}: {exc}")
        return None
    out = os.path.join(out_dir, f"{nuc.name}.h5")
    nuc.export_to_hdf5(out, "w")
    return out


def convert_thermal(src_dir: str, out_dir: str) -> list[str]:
    """Convert any thermal-scattering ENDF files present (c_Graphite, c_H_in_H2O).

    Like the neutron path, this must go through NJOY (from_njoy); OpenMC will not
    write HDF5 from an ENDF-origin object.
    """
    made = []
    for path in sorted(glob.glob(os.path.join(src_dir, "**", "*"), recursive=True)):
        if not os.path.isfile(path):
            continue
        try:
            ts = data.ThermalScattering.from_njoy(path)
        except Exception:
            continue
        out = os.path.join(out_dir, f"{ts.name}.h5")
        ts.export_to_hdf5(out, "w")
        made.append(out)
    return made


def build_library(out_dir: str) -> str:
    """Write cross_sections.xml for everything in out_dir."""
    lib = data.DataLibrary()
    for h5 in sorted(glob.glob(os.path.join(out_dir, "*.h5"))):
        lib.register_file(h5)
    xml = os.path.join(out_dir, "cross_sections.xml")
    lib.export_to_xml(xml)
    return xml


def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--from", dest="src", required=True,
                    help="directory holding ROSFOND ENDF-6 files")
    ap.add_argument("--out", dest="out", required=True,
                    help="directory to write the OpenMC HDF5 library into")
    ap.add_argument("--list", action="store_true",
                    help="show what is present / needed and exit")
    args = ap.parse_args()

    if not os.path.isdir(args.src):
        sys.exit(f"source directory not found: {args.src}")

    found = scan(args.src)
    present = sorted(found)

    if args.list:
        print("needed elements:", ", ".join(NEEDED))
        print("present in source:", ", ".join(present) or "(none recognised)")
        missing = [e for e in NEEDED if e not in found]
        print("missing:", ", ".join(missing) or "(none)")
        for el in present:
            print(f"  {el}: {[za for za, _ in found[el]]}")
        return

    os.makedirs(args.out, exist_ok=True)
    missing = [e for e in NEEDED if e not in found]
    made, failed = [], []
    for el in NEEDED:
        for za, path in found.get(el, []):
            print(f"  converting {el} (ZA {za}) <- {os.path.basename(path)}")
            out = convert_one(za, path, args.out)
            (made if out else failed).append(out or path)
    thermal = convert_thermal(args.src, args.out)

    if not made:
        sys.exit("nothing converted — check the --from directory holds ENDF-6 files")

    xml = build_library(args.out)
    manifest = {
        "source": "ROSFOND-2010",
        "converted": made,
        "thermal": thermal,
        "missing_elements": missing,
        "cross_sections": xml,
    }
    with open(os.path.join(args.out, "MANIFEST.json"), "w") as fh:
        json.dump(manifest, fh, indent=2)

    print(f"\nconverted {len(made)} nuclide file(s), {len(thermal)} thermal file(s)")
    print(f"missing elements (not present in source): {', '.join(missing) or '(none)'}")
    if failed:
        print(f"failed to convert: {len(failed)} file(s)")
    print(f"\nOpenMC library written to: {xml}")
    print("point OpenMC at it with:")
    print(f"  export OPENMC_CROSS_SECTIONS={xml}")


if __name__ == "__main__":
    main()
