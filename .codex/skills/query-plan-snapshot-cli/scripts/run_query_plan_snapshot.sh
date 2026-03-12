#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'USAGE'
Usage:
  run_query_plan_snapshot.sh [--log <path>] [--online] [--tmp-run] -- <QueryPlanSnapshotCli args>

Examples:
  run_query_plan_snapshot.sh --log tmp/qps.log -- \
    --store memory --theme MEDICAL_RECORDS --query-index 0 --query-id med-q0
  run_query_plan_snapshot.sh --tmp-run --log tmp/qps-tmp.log -- \
    --store lmdb --theme LIBRARY --query-index 8 --query-timeout-seconds 5
  run_query_plan_snapshot.sh --log tmp/qps-learned-trace.log -- \
    --compare-existing --query-id med-q0 --retrieve-learned-plan-trace --no-interactive

Notes:
  - Always runs root install first: mvn -T 1C [-o] -Dmaven.repo.local=.m2_repo -Pquick clean install
  - --tmp-run appends QueryPlanSnapshotCli --tmp-run (repo-local tmp/ output + temp LMDB data dir)
  - Pass QueryPlanSnapshotCli args after '--'
USAGE
}

log_file=""
offline_flag="-o"
tmp_run="false"

while [[ $# -gt 0 ]]; do
  case "$1" in
  --log)
    [[ $# -ge 2 ]] || {
      echo "Missing value for --log" >&2
      exit 2
    }
    log_file="$2"
    shift 2
    ;;
  --online)
    offline_flag=""
    shift
    ;;
  --tmp-run)
    tmp_run="true"
    shift
    ;;
  --help|-h)
    usage
    exit 0
    ;;
  --)
    shift
    break
    ;;
  *)
    echo "Unknown wrapper option: $1" >&2
    usage
    exit 2
    ;;
  esac
done

if [[ $# -eq 0 ]]; then
  echo "No QueryPlanSnapshotCli args provided. Pass args after '--'." >&2
  usage
  exit 2
fi

raw_cli_args=("$@")
if [[ "$tmp_run" == "true" ]]; then
  raw_cli_args=(--tmp-run "${raw_cli_args[@]}")
fi
printf -v cli_args '%q ' "${raw_cli_args[@]}"
cli_args="${cli_args% }"

install_cmd=(mvn -T 1C)
if [[ -n "$offline_flag" ]]; then
  install_cmd+=("$offline_flag")
fi
install_cmd+=(-Dmaven.repo.local=.m2_repo -Pquick install)

cli_cmd=(mvn)
if [[ -n "$offline_flag" ]]; then
  cli_cmd+=("$offline_flag")
fi
cli_cmd+=(-Dmaven.repo.local=.m2_repo -pl testsuites/benchmark -DskipTests exec:java@query-plan-snapshot)
cli_cmd+=(-Dexec.args="$cli_args")

echo ">>> Refreshing artifacts"
"${install_cmd[@]}" | tail -200

echo ">>> Running QueryPlanSnapshotCli"
echo ">>> args: $cli_args"

if [[ -n "$log_file" ]]; then
  mkdir -p "$(dirname "$log_file")"
  "${cli_cmd[@]}" | tee "$log_file"
  echo ">>> log: $log_file"
else
  "${cli_cmd[@]}"
fi
