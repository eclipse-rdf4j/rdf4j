#!/usr/bin/env bash
set -euo pipefail

# Lightweight helper for repeatable LEO shadow-mode optimizer experiments.
# It does not require a special harness class; pass the command that runs your workload after "--".
# Example:
#   scripts/leo-shadow-benchmark.sh -- ./mvnw -pl core/sail/lmdb -Dtest=LmdbMyWorkloadTest test

if [[ "${1:-}" == "--help" || "${1:-}" == "-h" || $# -eq 0 ]]; then
  cat <<'EOF'
Usage: scripts/leo-shadow-benchmark.sh -- <workload command...>

Runs the supplied workload with LEO observation enabled, rule steering shadowed, and plan ranking shadowed.
Set LEO_BENCH_ENABLE_STEERING=1 to allow guarded rule steering, or LEO_BENCH_ENABLE_PLAN_RANKING=1 to allow
plan-ranking hints in addition to shadow telemetry.
EOF
  exit 0
fi

if [[ "${1:-}" == "--" ]]; then
  shift
fi

export MAVEN_OPTS="${MAVEN_OPTS:-} \
  -Drdf4j.optimizer.lmdb.operatorFeedbackTracking=true \
  -Drdf4j.optimizer.lmdb.leoRuleSteering=${LEO_BENCH_ENABLE_STEERING:-false} \
  -Drdf4j.optimizer.lmdb.leoPlanRanking=${LEO_BENCH_ENABLE_PLAN_RANKING:-false} \
  -Drdf4j.optimizer.lmdb.estimateTrace=${LEO_BENCH_TRACE:-false}"

started_at=$(date -u +%Y-%m-%dT%H:%M:%SZ)
echo "leo-shadow-benchmark started_at=${started_at}" >&2
echo "command=$*" >&2
"$@"
finished_at=$(date -u +%Y-%m-%dT%H:%M:%SZ)
echo "leo-shadow-benchmark finished_at=${finished_at}" >&2
