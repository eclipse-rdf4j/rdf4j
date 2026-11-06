#!/usr/bin/env bash
# Run one LMDB/SPARQL workload command under each learned-optimizer rollout profile.
# Usage:
#   scripts/leo-workload-benchmark.sh -- ./run-workload.sh path/to/queries.rq
set -euo pipefail

if [[ "${1:-}" != "--" ]]; then
  echo "usage: $0 -- <workload-command> [args...]" >&2
  exit 64
fi
shift
if [[ $# -eq 0 ]]; then
  echo "missing workload command" >&2
  exit 64
fi

profiles=(
  off
  observe-only
  shadow-explain
  safe-cardinality-correction
  safe-rule-steering
  safe-plan-reranking
)

mkdir -p leo-benchmark-logs
for profile in "${profiles[@]}"; do
  log="leo-benchmark-logs/${profile}.log"
  echo "== profile=${profile} ==" | tee "$log"
  JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Drdf4j.optimizer.lmdb.leo.rolloutProfile=${profile}" \
    "$@" 2>&1 | tee -a "$log"
  echo | tee -a "$log"
done

echo "logs written to leo-benchmark-logs/"
