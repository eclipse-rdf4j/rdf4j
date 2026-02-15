#!/usr/bin/env bash
set -euo pipefail

usage() {
	cat <<'USAGE'
Usage: scripts/run-hybrid-join-benchmark-matrix.sh [options]

Runs a fixed benchmark scenario in both legacy and hybrid optimizer modes,
stores raw output, and writes a summary artifact.

Options:
  --module <path>                        Benchmark module (default: core/sail/lmdb)
  --class <fqcn>                         Benchmark class (default: org.eclipse.rdf4j.sail.lmdb.benchmark.ThemeQueryBenchmark)
  --method <name>                        Benchmark method (default: executeQuery)
  --query-index <n>                      Theme query index (default: 10)
  --theme <name>                         Theme name (default: SOCIAL_MEDIA)
  --page-cardinality-estimator <bool>    LMDB page estimator toggle (default: true)
  --warmup-iterations <n>                JMH warmup iterations (default: 0)
  --measurement-iterations <n>           JMH measurement iterations (default: 10)
  --warmup-time <duration>               JMH warmup time, e.g. 1s (default: 1s)
  --measurement-time <duration>          JMH measurement time, e.g. 1s (default: 1s)
  --forks <n>                            JMH forks (default: 1)
  --output-dir <path>                    Output directory (default: artifacts/benchmarks/hybrid-join-optimizer)
  --dry-run                              Print commands without executing
  --help                                 Show this help
USAGE
}

SCRIPT_DIR="$(cd -- "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd -- "${SCRIPT_DIR}/.." && pwd)"

module="core/sail/lmdb"
benchmark_class="org.eclipse.rdf4j.sail.lmdb.benchmark.ThemeQueryBenchmark"
benchmark_method="executeQuery"
query_index="10"
theme_name="SOCIAL_MEDIA"
page_cardinality_estimator="true"
warmup_iterations="0"
measurement_iterations="10"
warmup_time="1s"
measurement_time="1s"
forks="1"
output_dir="artifacts/benchmarks/hybrid-join-optimizer"
dry_run="false"

while [[ $# -gt 0 ]]; do
	case "$1" in
	--module)
		module="$2"
		shift 2
		;;
	--class)
		benchmark_class="$2"
		shift 2
		;;
	--method)
		benchmark_method="$2"
		shift 2
		;;
	--query-index)
		query_index="$2"
		shift 2
		;;
	--theme)
		theme_name="$2"
		shift 2
		;;
	--page-cardinality-estimator)
		page_cardinality_estimator="$2"
		shift 2
		;;
	--warmup-iterations)
		warmup_iterations="$2"
		shift 2
		;;
	--measurement-iterations)
		measurement_iterations="$2"
		shift 2
		;;
	--warmup-time)
		warmup_time="$2"
		shift 2
		;;
	--measurement-time)
		measurement_time="$2"
		shift 2
		;;
	--forks)
		forks="$2"
		shift 2
		;;
	--output-dir)
		output_dir="$2"
		shift 2
		;;
	--dry-run)
		dry_run="true"
		shift
		;;
	--help|-h)
		usage
		exit 0
		;;
	*)
		echo "Unknown option: $1" >&2
		usage >&2
		exit 1
		;;
	esac
done

if [[ ! "${output_dir}" = /* ]]; then
	output_dir="${REPO_ROOT}/${output_dir}"
fi

if [[ "${dry_run}" != "true" ]]; then
	mkdir -p "${output_dir}"
fi

metrics_csv="${output_dir}/metrics.csv"
summary_md="${output_dir}/summary.md"
metadata_txt="${output_dir}/run-metadata.txt"

if [[ "${dry_run}" == "true" ]]; then
	echo "Dry-run mode enabled."
fi

run_mode() {
	local mode="$1"
	local mode_log="${output_dir}/${mode}.log"
	local mode_json="${output_dir}/${mode}.json"
	local benchmark_pattern="${benchmark_class}.${benchmark_method}"

	echo "Running mode=${mode}"
	if [[ "${mode}" == "legacy" ]]; then
		local cmd=(
			"${REPO_ROOT}/scripts/run-single-benchmark.sh"
			--module "${module}"
			--class "${benchmark_class}"
			--method "${benchmark_method}"
			--warmup-iterations "${warmup_iterations}"
			--measurement-iterations "${measurement_iterations}"
			--forks "${forks}"
			--jvm-arg "-Drdf4j.optimizer.mode=${mode}"
			--jmh-arg "-p"
			--jmh-arg "z_queryIndex=${query_index}"
			--jmh-arg "-p"
			--jmh-arg "themeName=${theme_name}"
			--jmh-arg "-p"
			--jmh-arg "pageCardinalityEstimator=${page_cardinality_estimator}"
			--jmh-arg "-w"
			--jmh-arg "${warmup_time}"
			--jmh-arg "-r"
			--jmh-arg "${measurement_time}"
			--jmh-arg "-rf"
			--jmh-arg "json"
			--jmh-arg "-rff"
			--jmh-arg "${mode_json}"
		)
		if [[ "${dry_run}" == "true" ]]; then
			printf '%q ' "${cmd[@]}"
			printf '\n'
			return 0
		fi
		"${cmd[@]}" | tee "${mode_log}"
	else
		local benchmark_jar="${REPO_ROOT}/${module}/target/jmh-benchmarks.jar"
		if [[ "${dry_run}" == "true" ]]; then
			local cmd=(
				java
				-jar "${benchmark_jar}"
				-wi "${warmup_iterations}"
				-i "${measurement_iterations}"
				-f "${forks}"
				-jvmArgsAppend "-Drdf4j.optimizer.mode=${mode}"
				-p "z_queryIndex=${query_index}"
				-p "themeName=${theme_name}"
				-p "pageCardinalityEstimator=${page_cardinality_estimator}"
				-w "${warmup_time}"
				-r "${measurement_time}"
				-rf json
				-rff "${mode_json}"
				"${benchmark_pattern}"
			)
			printf '%q ' "${cmd[@]}"
			printf '\n'
			return 0
		fi
		java \
			-jar "${benchmark_jar}" \
			-wi "${warmup_iterations}" \
			-i "${measurement_iterations}" \
			-f "${forks}" \
			-jvmArgsAppend "-Drdf4j.optimizer.mode=${mode}" \
			-p "z_queryIndex=${query_index}" \
			-p "themeName=${theme_name}" \
			-p "pageCardinalityEstimator=${page_cardinality_estimator}" \
			-w "${warmup_time}" \
			-r "${measurement_time}" \
			-rf json \
			-rff "${mode_json}" \
			"${benchmark_pattern}" | tee "${mode_log}"
	fi

	python3 - "${mode_json}" "${mode}" >> "${metrics_csv}" <<'PY'
import json
import math
import statistics
import sys
from pathlib import Path

json_path = Path(sys.argv[1])
mode = sys.argv[2]
payload = json.loads(json_path.read_text(encoding="utf-8"))
if not payload:
    raise SystemExit(f"No benchmark payload in {json_path}")
primary = payload[0].get("primaryMetric", {})
raw_data = primary.get("rawData") or []
samples = [float(x) for row in raw_data for x in row]
score = float(primary.get("score", 0.0))
unit = primary.get("scoreUnit", "unknown")
if not samples:
    samples = [score]
samples.sort()

def percentile(values, p):
    if len(values) == 1:
        return values[0]
    idx = (len(values) - 1) * p
    low = math.floor(idx)
    high = math.ceil(idx)
    if low == high:
        return values[low]
    frac = idx - low
    return values[low] + (values[high] - values[low]) * frac

median = statistics.median(samples)
p95 = percentile(samples, 0.95)
p99 = percentile(samples, 0.99)
print(f"{mode},{score:.9f},{median:.9f},{p95:.9f},{p99:.9f},{len(samples)},{unit}")
PY
}

if [[ "${dry_run}" != "true" ]]; then
	{
		echo "timestamp=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
		echo "module=${module}"
		echo "class=${benchmark_class}"
		echo "method=${benchmark_method}"
		echo "queryIndex=${query_index}"
		echo "themeName=${theme_name}"
		echo "pageCardinalityEstimator=${page_cardinality_estimator}"
		echo "warmupIterations=${warmup_iterations}"
		echo "measurementIterations=${measurement_iterations}"
		echo "warmupTime=${warmup_time}"
		echo "measurementTime=${measurement_time}"
		echo "forks=${forks}"
	} > "${metadata_txt}"
	printf 'mode,avg,median,p95,p99,samples,unit\n' > "${metrics_csv}"
fi

run_mode legacy
run_mode hybrid

if [[ "${dry_run}" == "true" ]]; then
	exit 0
fi

python3 - "${metrics_csv}" "${summary_md}" <<'PY'
import csv
import sys
from pathlib import Path

csv_path = Path(sys.argv[1])
summary_path = Path(sys.argv[2])
rows = list(csv.DictReader(csv_path.open(encoding="utf-8")))
if len(rows) < 2:
    raise SystemExit(f"Expected at least two rows in {csv_path}")

by_mode = {row["mode"]: row for row in rows}
legacy = by_mode.get("legacy")
hybrid = by_mode.get("hybrid")
if legacy is None or hybrid is None:
    raise SystemExit("Missing legacy or hybrid rows in metrics.csv")

legacy_avg = float(legacy["avg"])
hybrid_avg = float(hybrid["avg"])
delta_pct = ((hybrid_avg - legacy_avg) / legacy_avg) * 100.0 if legacy_avg else 0.0

summary_path.write_text(
    "\n".join(
        [
            "# Hybrid Join Optimizer Benchmark Summary",
            "",
            "## Scenario",
            "",
            "Single benchmark method executed in two optimizer modes with identical JMH settings.",
            "",
            "## Results",
            "",
            "| Mode | Avg | Median | P95 | P99 | Samples | Unit |",
            "|---|---:|---:|---:|---:|---:|---|",
            f"| legacy | {float(legacy['avg']):.6f} | {float(legacy['median']):.6f} | {float(legacy['p95']):.6f} | {float(legacy['p99']):.6f} | {legacy['samples']} | {legacy['unit']} |",
            f"| hybrid | {float(hybrid['avg']):.6f} | {float(hybrid['median']):.6f} | {float(hybrid['p95']):.6f} | {float(hybrid['p99']):.6f} | {hybrid['samples']} | {hybrid['unit']} |",
            "",
            f"Hybrid vs legacy avg delta: {delta_pct:.2f}% (negative is faster).",
            "",
            "Planning-time share: not available in this JMH output. Use explain/profiling artifacts for planner split.",
            "",
            "Raw artifacts: `legacy.log`, `hybrid.log`, `legacy.json`, `hybrid.json`, `metrics.csv`.",
        ]
    )
    + "\n",
    encoding="utf-8",
)
PY

echo "Wrote benchmark artifacts to ${output_dir}"
echo "Summary: ${summary_md}"
