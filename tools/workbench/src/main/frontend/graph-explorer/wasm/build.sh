#!/usr/bin/env bash
# Copyright (c) 2026 Eclipse RDF4J contributors.
# SPDX-License-Identifier: BSD-3-Clause
set -euo pipefail
cd -- "$(dirname -- "$0")"

# Install/unpack Zig 0.15.1 separately, or set GRAPH_LAYOUT_ZIG to its absolute executable path.
# No compiler is downloaded during the application build or at runtime.
compiler="${GRAPH_LAYOUT_ZIG:-zig}"
if [[ "$("$compiler" version)" != "0.15.1" ]]; then
  echo 'Rebuilding stress-pairs.wasm requires the pinned Zig 0.15.1 compiler.' >&2
  exit 1
fi
"$compiler" cc -target wasm32-freestanding -O3 -g0 \
  -ffp-contract=off -fno-fast-math -fno-math-errno -fno-vectorize -fno-slp-vectorize \
  -nostdlib -Wl,--no-entry -Wl,--export-memory -Wl,--export=__heap_base \
  -Wl,-z,stack-size=65536 -Wl,--initial-memory=131072 -Wl,--max-memory=67108864 \
  stress-pairs.c -o stress-pairs.wasm
