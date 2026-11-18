#!/usr/bin/env bash
# Rebuild the OpenMC toolchain for the RBMK void-coefficient study.
#
# WHY THIS EXISTS: the first install lived in ~ (ephemeral). A sandbox reset
# wiped it and the work stalled. Everything here installs under /workspace,
# which persists, and this script rebuilds it in one command if it is ever lost.
#
# Usage:  bash tools/openmc/setup_openmc.sh
# Then:   source /workspace/tools/omc/env.sh
#
# Sizes: OpenMC env ~2 GB; full ENDF/B-VIII.0 HDF5 library ~13 GB extracted
# (690 files, INCLUDING the thermal scattering files c_Graphite and c_H_in_H2O
# that the GitHub mirror omits). Needs ~20 GB free under /workspace.
set -euo pipefail

ROOT=/workspace/tools/omc
ENV=$ROOT/env
DATA=$ROOT/data
LIB=$DATA/endfb-viii.0-hdf5
ARCHIVE_URL="https://anl.box.com/shared/static/uhbxlrx7hvxqw27psymfbhi7bx7s6u6a.xz"

mkdir -p "$ROOT/bin" "$DATA"

# 1. micromamba -------------------------------------------------------------
if [ ! -x "$ROOT/bin/micromamba" ]; then
  echo "== installing micromamba =="
  curl -Ls https://micro.mamba.pm/api/micromamba/linux-64/latest \
    | tar -xj -C "$ROOT/bin" --strip-components=1 bin/micromamba
fi

# 2. OpenMC env (conda-forge) ----------------------------------------------
if [ ! -x "$ENV/bin/openmc" ]; then
  echo "== creating OpenMC env (conda-forge, ~2 GB) =="
  "$ROOT/bin/micromamba" create -y -p "$ENV" -c conda-forge python=3.12 openmc
fi
"$ENV/bin/openmc" --version

# 3. Nuclear data (full ENDF/B-VIII.0 HDF5, with thermal scattering) -------
if [ ! -f "$LIB/cross_sections.xml" ]; then
  echo "== downloading ENDF/B-VIII.0 HDF5 (~3.4 GB compressed) =="
  curl -Ls -o "$ROOT/endfb80.xz" "$ARCHIVE_URL"
  echo "== extracting full library (~13 GB; the model uses natural elements,"
  echo "   so EVERY isotope is needed, not just the dominant ones) =="
  xz -dc "$ROOT/endfb80.xz" | tar -x -C "$DATA"
  rm -f "$ROOT/endfb80.xz"
fi
test -f "$LIB/thermal/c_H_in_H2O.h5" || { echo "MISSING thermal data"; exit 1; }

# 4. env.sh so the model can find it ---------------------------------------
cat > "$ROOT/env.sh" <<EOF
export OPENMC_CROSS_SECTIONS=$LIB/cross_sections.xml
export PATH=$ENV/bin:\$PATH
EOF

echo
echo "OK. OpenMC ready."
echo "  source $ROOT/env.sh"
echo "  cd openmc && python rbmk_cell.py && openmc -n 800 -s 4"
