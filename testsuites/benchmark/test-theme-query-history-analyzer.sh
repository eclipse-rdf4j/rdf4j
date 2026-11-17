#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd -- "${SCRIPT_DIR}/../.." && pwd)"
RESULTS_DIR="${REPO_ROOT}/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results"
ANALYZER="${RESULTS_DIR}/analyze-theme-query-history.sh"

fail() {
	echo "FAIL: $*" >&2
	exit 1
}

assert_contains() {
	local haystack="$1"
	local needle="$2"
	local message="$3"
	if [[ "${haystack}" != *"${needle}"* ]]; then
		fail "${message}"
	fi
}

assert_not_contains() {
	local haystack="$1"
	local needle="$2"
	local message="$3"
	if [[ "${haystack}" == *"${needle}"* ]]; then
		fail "${message}"
	fi
}

assert_rank_count() {
	local haystack="$1"
	local expected="$2"
	local count
	count="$(printf '%s\n' "${haystack}" | grep -Ec '^[1-3]\. results-' || true)"
	if [[ "${count}" != "${expected}" ]]; then
		fail "Expected ${expected} ranked runs, got ${count}"
	fi
}

assert_before() {
	local haystack="$1"
	local first="$2"
	local second="$3"
	local message="$4"
	local first_line
	local second_line
	first_line="$(printf '%s\n' "${haystack}" | grep -nF "${first}" | head -1 | cut -d: -f1)"
	second_line="$(printf '%s\n' "${haystack}" | grep -nF "${second}" | head -1 | cut -d: -f1)"
	if [[ -z "${first_line}" || -z "${second_line}" || "${first_line}" -ge "${second_line}" ]]; then
		fail "${message}"
	fi
}

DEFAULT_OUTPUT="$(bash "${ANALYZER}" --results-dir "${RESULTS_DIR}")"
echo "${DEFAULT_OUTPUT}"

assert_contains "${DEFAULT_OUTPUT}" "Latest run: results-2026-06-03.md" \
	"default mode should pick the newest dated result file"
assert_contains "${DEFAULT_OUTPUT}" "q10: latest 245.865 ms/op | fastest 106.980 ms/op | 56.5% slower than best" \
	"default summary should show the fastest historical MEDICAL_RECORDS q10 run and the slower delta"
assert_contains "${DEFAULT_OUTPUT}" "MEDICAL_RECORDS q10" \
	"default mode should expand MEDICAL_RECORDS q10 details"
assert_contains "${DEFAULT_OUTPUT}" "results-2026-05-13.md: 143.125 ms/op" \
	"default detail should include a known faster historical MEDICAL_RECORDS q10 run"
assert_contains "${DEFAULT_OUTPUT}" "plan no | query yes" \
	"default detail should distinguish query-only historical runs"
assert_contains "${DEFAULT_OUTPUT}" "ENGINEERING q4" \
	"default mode should expand ENGINEERING q4 details"
assert_contains "${DEFAULT_OUTPUT}" "results-main-branch.md: 45.942 ms/op" \
	"default detail should include summary-only historical runs"
assert_contains "${DEFAULT_OUTPUT}" "plan no | query no" \
	"default detail should mark summary-only runs as missing plan and query"
assert_not_contains "${DEFAULT_OUTPUT}" "q10: latest 129.204 ms/op" \
	"default mode should omit PHARMA q10 because the latest run is faster than history"
assert_not_contains "${DEFAULT_OUTPUT}" $'\nPHARMA q10\n  latest:' \
	"default detail section should skip PHARMA q10 because no history is 20% faster"
assert_not_contains "${DEFAULT_OUTPUT}" "ENGINEERING\n  q6:" \
	"default mode should not print queries that are not 20% slower than history"

ALL_OUTPUT="$(bash "${ANALYZER}" --results-dir "${RESULTS_DIR}" --all)"
echo "${ALL_OUTPUT}"

assert_contains "${ALL_OUTPUT}" "MEDICAL_RECORDS" \
	"--all should print every theme"
assert_contains "${ALL_OUTPUT}" "q7: latest 12.008 ms/op | fastest 12.008 ms/op | 10.4% faster than previous best 13.402 ms/op" \
	"--all should print faster-than-history MEDICAL_RECORDS q7"
assert_contains "${ALL_OUTPUT}" "q3: latest 41.824 ms/op | fastest 40.574 ms/op | 3.0% slower than best" \
	"--all should print sub-threshold LIBRARY q3"
assert_contains "${ALL_OUTPUT}" "q10: latest 245.865 ms/op | fastest 106.980 ms/op | 56.5% slower than best" \
	"--all should still print slower queries with fastest historical time and delta"

SORTED_OUTPUT="$(bash "${ANALYZER}" --results-dir "${RESULTS_DIR}" --sort-regressions)"
echo "${SORTED_OUTPUT}"

assert_contains "${SORTED_OUTPUT}" "ELECTRICAL_GRID q7" \
	"--sort-regressions should include the biggest regression"
assert_contains "${SORTED_OUTPUT}" "TRAIN q5" \
	"--sort-regressions should include the second biggest regression"
assert_contains "${SORTED_OUTPUT}" "SOCIAL_MEDIA q7" \
	"--sort-regressions should include the third biggest regression"
assert_before "${SORTED_OUTPUT}" "ELECTRICAL_GRID q7" "PHARMA q0" \
	"--sort-regressions should move larger regressions ahead of smaller ones"
assert_before "${SORTED_OUTPUT}" "SOCIAL_MEDIA q7" "PHARMA q0" \
	"--sort-regressions should keep the near-100% regressions ahead of lower ones"

TOP_OUTPUT="$(bash "${ANALYZER}" --results-dir "${RESULTS_DIR}" --top 3)"
echo "${TOP_OUTPUT}"

assert_contains "${TOP_OUTPUT}" "ELECTRICAL_GRID q7" \
	"--top should keep the biggest regression"
assert_contains "${TOP_OUTPUT}" "TRAIN q5" \
	"--top should keep the second biggest regression"
assert_contains "${TOP_OUTPUT}" "SOCIAL_MEDIA q7" \
	"--top should keep the third biggest regression"
assert_not_contains "${TOP_OUTPUT}" "PHARMA q0" \
	"--top 3 should drop regressions outside the top three"
assert_before "${TOP_OUTPUT}" "ELECTRICAL_GRID q7" "SOCIAL_MEDIA q7" \
	"--top should preserve descending regression order"

PHARMA_OUTPUT="$(bash "${ANALYZER}" --results-dir "${RESULTS_DIR}" --theme PHARMA --query-index 10)"
echo "${PHARMA_OUTPUT}"

assert_rank_count "${PHARMA_OUTPUT}" 3
assert_contains "${PHARMA_OUTPUT}" "Query: PHARMA q10" \
	"query mode should label the selected query"
assert_contains "${PHARMA_OUTPUT}" "Latest score: 129.204 ms/op" \
	"query mode should report the latest run score"
assert_contains "${PHARMA_OUTPUT}" "1. results-2026-06-03.md" \
	"query mode should include today's plan-run PHARMA q10 run first"
assert_contains "${PHARMA_OUTPUT}" "2. results-2026-04-16.md" \
	"query mode should include the fastest historical ThemeQueryBenchmark PHARMA q10 run"
assert_contains "${PHARMA_OUTPUT}" "3. results-2026-04-15.md" \
	"query mode should include the summary-only PHARMA q10 run"
assert_contains "${PHARMA_OUTPUT}" "4. results-2026-04-09-2-full.md" \
	"query mode should prefer the richer equal-score PHARMA q10 run"
assert_contains "${PHARMA_OUTPUT}" "5. results-2026-04-09-2.md" \
	"query mode should include the matching equal-score PHARMA q10 summary run"
assert_contains "${PHARMA_OUTPUT}" "Optimized query plan:" \
	"query mode should print a query plan section"
assert_contains "${PHARMA_OUTPUT}" "not present in this result file" \
	"query mode should explain when plan/query data is missing"
assert_contains "${PHARMA_OUTPUT}" '```sparql' \
	"query mode should render optimized queries in a SPARQL fence"
assert_contains "${PHARMA_OUTPUT}" "SELECT ?pathway (COUNT(DISTINCT ?drug) AS ?drugCount) WHERE {" \
	"query mode should print optimized query text when present"

ENGINEERING_OUTPUT="$(bash "${ANALYZER}" --results-dir "${RESULTS_DIR}" --theme ENGINEERING --query-index 4)"
echo "${ENGINEERING_OUTPUT}"

assert_rank_count "${ENGINEERING_OUTPUT}" 3
assert_contains "${ENGINEERING_OUTPUT}" "1. results-2026-05-06.md" \
	"query mode should rank the fastest ENGINEERING q4 run first"
assert_contains "${ENGINEERING_OUTPUT}" '```text' \
	"query mode should render physical plans in a text fence when present"
assert_contains "${ENGINEERING_OUTPUT}" "plannerId=lmdb-sketch" \
	"query mode should print physical plan content when present"
assert_contains "${ENGINEERING_OUTPUT}" '```sparql' \
	"query mode should print optimized query text for plan-bearing runs"
assert_contains "${ENGINEERING_OUTPUT}" "SELECT (COUNT(DISTINCT ?component) AS ?count) WHERE {" \
	"query mode should include the optimized ENGINEERING q4 query"
