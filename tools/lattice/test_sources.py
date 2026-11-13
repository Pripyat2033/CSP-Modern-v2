#!/usr/bin/env python3
"""Self-test for the data-source registry (sources.py). Run: python3 test_sources.py

Locks the contract the --source flag relies on: the two sources are distinct, the
default is ENDF/B, the NIKIET-lineage ROSFOND source exists with its own cache
dir, and an unknown name is refused rather than silently defaulting.
"""
from __future__ import annotations

import os

import sources

PASS, FAIL = "PASS", "FAIL"
results = []


def check(name, cond, detail=""):
    results.append(cond)
    print(f"  [{PASS if cond else FAIL}] {name} {detail}")


def main():
    print("lattice source registry self-test\n")

    check("default is the ENDF source", sources.DEFAULT == "endf")
    check("both sources exist", set(sources.SOURCES) == {"endf", "rosfond"},
          f"({sorted(sources.SOURCES)})")

    endf = sources.get("endf")
    ros = sources.get("rosfond")
    check("ENDF is the remote, unattended source", endf.mode == "remote")
    check("ROSFOND is the local (files-provided) source", ros.mode == "local")
    check("the two sources use different cache dirs", endf.cache_dir != ros.cache_dir,
          f"({os.path.basename(endf.cache_dir)} vs {os.path.basename(ros.cache_dir)})")
    check("ROSFOND is labelled as such", ros.library == "ROSFOND-2010", f"({ros.library})")
    check("ENDF is labelled as such", endf.library == "ENDF/B-VIII.0", f"({endf.library})")

    # the environment can select the source (LATTICE_SOURCE)
    os.environ["LATTICE_SOURCE"] = "rosfond"
    check("LATTICE_SOURCE selects the source", sources.current().name == "rosfond")
    del os.environ["LATTICE_SOURCE"]

    # an unknown source is refused, not silently defaulted
    try:
        sources.get("nonsense")
        check("unknown source is refused", False)
    except SystemExit:
        check("unknown source is refused (not silently defaulted)", True)

    n_pass = sum(1 for ok in results if ok)
    print(f"\n{'-'*52}\n{n_pass}/{len(results)} checks passed")
    return 0 if n_pass == len(results) else 1


if __name__ == "__main__":
    raise SystemExit(main())
