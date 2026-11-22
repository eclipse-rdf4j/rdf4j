#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'USAGE'
Usage: bash scripts/run-lmdb-graph-restriction-release.sh RESULTS_DIR [OPTIONS]

  --dry-run             Print the benchmark invocations.
  --profile NAME        experimental or standard (default: both).
  --case NAME           no-graph, native-fusion, rank, partial, or selective (default: all).
  --warmup-floor COUNT  Raise the warmup minimum for all selected modes.
  --worker-cpu NUMBER   Pin the query worker to one chosen Linux logical CPU, using jcmd and taskset.

Run the LMDB GRAPH restriction release fixtures in both surrounding optimizer profiles.
Package the benchmark jar first:
  mvn -B -ntp -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -am -Pbenchmarks,quick package
Use a fresh results directory and a quiet Linux Java 25+ host. Each fixture runs three adjacent
OFF/AUTO fork pairs with alternating order and eight measured one-second iterations per fork.
No-GRAPH and native-fusion controls use twenty one-second warmup iterations. The others use five.
Prefer a dedicated logical CPU for the query worker. Pick one in the process's allowed CPU set
and leave other allowed CPUs for the JVM's GC and compiler threads.

Check all eight measurements in every fork and keep the raw JMH output. If a mode's late-half
mean moves beyond 10% in the same direction in a majority of its forks, rerun both modes of
that case with --warmup-floor 40 and keep both cohorts. A pull request smoke checks result bags.
Release comparisons require this warmed protocol and report uncertainty across independent pairs.
Assess the results with python3 scripts/summarize-lmdb-graph-restriction-release.py RESULTS_DIR.
USAGE
}

if [[ $# -eq 1 && ( $1 == -h || $1 == --help ) ]]; then
  usage
  exit 0
fi
if [[ $# -lt 1 ]]; then
  usage >&2
  exit 2
fi

results=$1
shift
dry_run=false
runner_flags=()
selected_profile=
selected_case=
warmup_floor=0
worker_cpu=
while [[ $# -gt 0 ]]; do
  case "$1" in
    --dry-run) dry_run=true; runner_flags+=(--dry-run); shift ;;
    --profile) selected_profile=${2:?Require an optimizer profile}; shift 2 ;;
    --case) selected_case=${2:?Require a fixture name}; shift 2 ;;
    --warmup-floor) warmup_floor=${2:?Require a warmup iteration count}; shift 2 ;;
    --worker-cpu) worker_cpu=${2:?Require a Linux logical CPU}; shift 2 ;;
    *) usage >&2; exit 2 ;;
  esac
done
case "$selected_profile" in ''|experimental|standard) ;; *) usage >&2; exit 2 ;; esac
case "$selected_case" in ''|no-graph|native-fusion|rank|partial|selective) ;; *) usage >&2; exit 2 ;; esac
if [[ ! $warmup_floor =~ ^(0|[1-9][0-9]*)$ || ( -n $worker_cpu && ! $worker_cpu =~ ^(0|[1-9][0-9]*)$ ) ]]; then
  usage >&2
  exit 2
fi

profilers=(-prof gc)
if [[ -n $worker_cpu ]]; then
  profilers+=(-prof "org.eclipse.rdf4j.sail.lmdb.benchmark.GraphRestrictionWorkerAffinityProfiler:$worker_cpu")
fi
if [[ $dry_run == false ]]; then
  mkdir -p -- "$results"
  if [[ -n $(ls -A -- "$results") ]]; then
    printf 'Results directory already contains files. Choose a fresh directory: %s\n' "$results" >&2
    exit 2
  fi
  results=$(cd -- "$results" && pwd)
fi

cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.."

if [[ $dry_run == false ]]; then
  benchmark_jar=core/sail/lmdb/target/jmh-benchmarks.jar
  if [[ ! -r $benchmark_jar ]]; then
    printf 'Package the LMDB benchmark jar before running: %s\n' "$benchmark_jar" >&2
    exit 2
  fi
  sha256sum "$benchmark_jar" > "$results/benchmark-jar.sha256"
  java -version > "$results/java-version.txt" 2>&1
  printf 'profile\tfixture\tpair\tmode\twarmup\tworker_cpu\tfile\n' > "$results/run-order.tsv"
fi

run_fork() {
  local profile=$1 experimental=$2 name=$3 warmup=$4 pair=$5 mode=$6
  shift 6
  local file="$profile-$name-$pair-$mode"
  local prefix="$results/$file"
  local command=(scripts/run-single-benchmark.sh --no-build --module core/sail/lmdb
    --class org.eclipse.rdf4j.sail.lmdb.benchmark.GraphRestrictionJoinBenchmark --method runQuery
    --warmup-iterations "$warmup" --measurement-iterations 8 --forks 1
    --jvm-arg -Xms2G --jvm-arg -Xmx8G --jvm-arg --enable-native-access=ALL-UNNAMED
    --param "experimentalOptimizations=$experimental" --param "strategy=$mode" --param telemetry=false
    --param graphCount=1 --param indexes=spoc+posc+ospc --param selectivity=1 --param missKind=DICTIONARY_ABSENT
    "${runner_flags[@]}" "$@" -- -w 1s -r 1s -t 1 "${profilers[@]}" -foe true -rf json -rff "$prefix.json")
  if [[ $dry_run == true ]]; then
    RDF4J_BENCHMARK_PLAN_GUARD=false "${command[@]}"
  else
    printf '%s\t%s\t%d\t%s\t%d\t%s\t%s.json\n' "$profile" "$name" "$pair" "$mode" "$warmup" "$worker_cpu" "$file" \
      >> "$results/run-order.tsv"
    RDF4J_BENCHMARK_PLAN_GUARD=false "${command[@]}" 2>&1 | tee "$prefix.log"
  fi
}

run_case() {
  local profile=$1 experimental=$2 name=$3 warmup=$4 rotation=$5
  shift 5
  if [[ -n $selected_case && $selected_case != "$name" ]]; then
    return
  fi
  if (( warmup_floor > warmup )); then
    warmup=$warmup_floor
  fi
  local pair mode
  local modes
  for pair in 1 2 3; do
    if (( (pair + rotation) % 2 == 1 )); then
      modes=(OFF AUTO)
    else
      modes=(AUTO OFF)
    fi
    for mode in "${modes[@]}"; do
      run_fork "$profile" "$experimental" "$name" "$warmup" "$pair" "$mode" "$@"
    done
  done
}

run_profile() {
  local profile=$1 experimental=$2 rotation=$3
  if [[ -n $selected_profile && $selected_profile != "$profile" ]]; then
    return
  fi
  local short_fixture=(--param statementCount=10000 --param outerOccurrences=32 --param distinctKeys=32)
  run_case "$profile" "$experimental" native-fusion 20 "$rotation" "${short_fixture[@]}" \
    --param shape=VARIABLE_GRAPH --param keyShape=SUBJECT
  run_case "$profile" "$experimental" no-graph 20 "$((rotation + 1))" "${short_fixture[@]}" \
    --param shape=NO_GRAPH --param keyShape=SUBJECT
  run_case "$profile" "$experimental" rank 5 "$rotation" "${short_fixture[@]}" \
    --param shape=SEQUENCE_BARRIER --param keyShape=SUBJECT
  run_case "$profile" "$experimental" partial 5 "$((rotation + 1))" "${short_fixture[@]}" \
    --param shape=BODY_BINDS_SELECTOR --param keyShape=PARTIAL
  run_case "$profile" "$experimental" selective 5 "$rotation" --param statementCount=1000000 \
    --param outerOccurrences=100 --param distinctKeys=100 --param shape=BODY_BINDS_SELECTOR --param keyShape=SUBJECT
}

run_profile experimental true 0
run_profile standard false 1
