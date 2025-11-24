#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

MVN_BIN=${MVN_BIN:-mvn}
MVN_BATCH_OPTS=(-B)

log() {
  printf '\n[run.sh] %s\n' "$1"
}

if [[ "${SKIP_REACTOR_INSTALL:-0}" != "1" ]]; then
  log "Installing the full reactor with -Pquick -DskipTests so server-boot dependencies are available"
  "$MVN_BIN" "${MVN_BATCH_OPTS[@]}" -Pquick -DskipTests install
else
  log "Skipping reactor install because SKIP_REACTOR_INSTALL=1"
fi

log "Running tools/server-boot verification"
"$MVN_BIN" "${MVN_BATCH_OPTS[@]}" -pl tools/server-boot verify
