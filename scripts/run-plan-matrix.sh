#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
WARMUP=2
MEASUREMENTS=5
SEEDS="7,11,19"
THEME=""
INDEXES=""
OUTPUT_DIR=""
ANALYZE=1

usage() {
	cat <<'USAGE'
Usage: scripts/run-plan-matrix.sh [options]

Options:
  --warmup N            Warmup iterations per query (default: 2)
  --measurements N      Measured iterations per query (default: 5)
  --seeds CSV           Random seeds for warm mode (default: 7,11,19)
  --theme NAME          Single theme (e.g. ENGINEERING)
  --indexes CSV         Query indexes (e.g. 0,1,2)
  --output-dir PATH     Artifact output directory
  --no-analyze          Skip post-run analysis
  -h, --help            Show this help
USAGE
}

while [[ $# -gt 0 ]]; do
	case "$1" in
	--warmup)
		WARMUP="$2"
		shift 2
		;;
	--measurements)
		MEASUREMENTS="$2"
		shift 2
		;;
	--seeds)
		SEEDS="$2"
		shift 2
		;;
	--theme)
		THEME="$2"
		shift 2
		;;
	--indexes)
		INDEXES="$2"
		shift 2
		;;
	--output-dir)
		OUTPUT_DIR="$2"
		shift 2
		;;
	--no-analyze)
		ANALYZE=0
		shift
		;;
	-h | --help)
		usage
		exit 0
		;;
	*)
		echo "Unknown option: $1" >&2
		usage
		exit 1
		;;
	esac
done

if [[ -z "${OUTPUT_DIR}" ]]; then
	timestamp="$(date +"%Y%m%dT%H%M%S")"
	OUTPUT_DIR="${ROOT_DIR}/artifacts/plan-matrix/${timestamp}"
fi

mkdir -p "${OUTPUT_DIR}"

MAVEN_ARGS=(
	-o
	"-Dmaven.repo.local=${ROOT_DIR}/.m2_repo"
	-pl
	core/sail/lmdb
	"-Dtest=DpJoinOrderTimingHarnessTest#exportPlanMatrixRecords"
	-Drdf4j.planMatrix.run=true
	"-Drdf4j.planMatrix.warmup=${WARMUP}"
	"-Drdf4j.planMatrix.measurements=${MEASUREMENTS}"
	"-Drdf4j.planMatrix.seeds=${SEEDS}"
	"-Drdf4j.planMatrix.outputDir=${OUTPUT_DIR}"
	verify
)

if [[ -n "${THEME}" ]]; then
	MAVEN_ARGS+=("-Drdf4j.expectedPlans.theme=${THEME}")
fi
if [[ -n "${INDEXES}" ]]; then
	MAVEN_ARGS+=("-Drdf4j.expectedPlans.indexes=${INDEXES}")
fi

(
	cd "${ROOT_DIR}"
	echo "Running plan matrix to ${OUTPUT_DIR}"
	mvn "${MAVEN_ARGS[@]}"
)

if [[ "${ANALYZE}" -eq 1 ]]; then
	python3 "${ROOT_DIR}/scripts/analyze-plan-matrix.py" \
		--records "${OUTPUT_DIR}/records.jsonl" \
		--out-dir "${OUTPUT_DIR}"
fi

echo "Done. Artifacts: ${OUTPUT_DIR}"
