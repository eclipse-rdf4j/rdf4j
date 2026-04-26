#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd -- "${SCRIPT_DIR}/../.." && pwd)"
ANALYZER="${REPO_ROOT}/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/analyze-theme-query-history.sh"
RESULTS_DIR="$(mktemp -d "${TMPDIR:-/tmp}/theme-query-history.XXXXXX")"

fail() {
	echo "FAIL: $*" >&2
	exit 1
}

write_result() {
	local file_name="$1"
	local score="$2"
	local plan="$3"
	local query_marker="$4"
	cat > "${RESULTS_DIR}/${file_name}" <<RESULT
ThemeQueryBenchmark.executeQuery PHARMA 10 avgt ${score} ms/op

# Parameters: (themeName = PHARMA, z_queryIndex = 10)

### Optimized Query ###
${plan}
SELECT * WHERE {
  ?s <http://example.com/${query_marker}> ?o .
}
RESULT
}

write_result results-2026-04-01.md 1.000 "plan-run-1" "q1"
write_result results-2026-04-02.md 2.000 "plan-run-2" "q2"
write_result results-2026-04-03.md 3.000 "plan-run-3" "q3"
write_result results-2026-04-04.md 4.000 "plan-run-4" "q4"

OUTPUT="$(bash "${ANALYZER}" --results-dir "${RESULTS_DIR}" --theme PHARMA --query-index 10)"
echo "${OUTPUT}"

rank_count="$(printf '%s\n' "${OUTPUT}" | grep -Ec '^[0-9]+\. results-' || true)"
if [[ "${rank_count}" != "4" ]]; then
	fail "query mode should print every matching run, got ${rank_count}"
fi

if [[ "${OUTPUT}" != *"plan-run-4"* || "${OUTPUT}" != *"http://example.com/q4"* ]]; then
	fail "query mode should include the latest run plan and optimized query"
fi
