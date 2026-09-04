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

assert_contains() {
	local output="$1"
	local expected="$2"
	local message="$3"
	if [[ "${output}" != *"${expected}"* ]]; then
		fail "${message}"
	fi
}

assert_not_contains() {
	local output="$1"
	local unexpected="$2"
	local message="$3"
	if [[ "${output}" == *"${unexpected}"* ]]; then
		fail "${message}"
	fi
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

FILTER_RESULTS_DIR="$(mktemp -d "${TMPDIR:-/tmp}/theme-query-history-filter.XXXXXX")"

write_filter_result() {
	local file_name="$1"
	local pharma_q0="$2"
	local pharma_q1="$3"
	local pharma_q2="$4"
	cat > "${FILTER_RESULTS_DIR}/${file_name}" <<RESULT
ThemeQueryBenchmark.executeQuery PHARMA 0 avgt ${pharma_q0} ms/op
ThemeQueryBenchmark.executeQuery PHARMA 1 avgt ${pharma_q1} ms/op
ThemeQueryBenchmark.executeQuery PHARMA 2 avgt ${pharma_q2} ms/op
RESULT
}

write_filter_result results-2026-04-01.md 1.000 0.200 8.500
write_filter_result results-2026-04-02.md 10.000 0.600 10.000

FILTER_OUTPUT="$(bash "${ANALYZER}" --results-dir "${FILTER_RESULTS_DIR}" --sort-regressions --min-delta-ms 0.5 --min-slower-pct 20)"
echo "${FILTER_OUTPUT}"

if [[ "${FILTER_OUTPUT}" != *"PHARMA q0"* ]]; then
	fail "filtered overview should include queries passing both slower percent and delta thresholds"
fi

if [[ "${FILTER_OUTPUT}" == *"PHARMA q1"* ]]; then
	fail "filtered overview should exclude queries below the minimum delta"
fi

if [[ "${FILTER_OUTPUT}" == *"PHARMA q2"* ]]; then
	fail "filtered overview should exclude queries below the minimum slower percent"
fi

PLAN_RESULTS_DIR="$(mktemp -d "${TMPDIR:-/tmp}/theme-query-history-plans.XXXXXX")"

cat > "${PLAN_RESULTS_DIR}/results-2026-04-01.md" <<RESULT
ThemeQueryBenchmark.executeQuery PHARMA 10 avgt 1.000 ms/op
RESULT

cat > "${PLAN_RESULTS_DIR}/results-2026-04-02.md" <<RESULT
ThemeQueryBenchmark.executeQuery PHARMA 10 avgt 2.000 ms/op

# Parameters: (themeName = PHARMA, z_queryIndex = 10)

### Optimized Query ###
optimized-plan-run-2
SELECT * WHERE {
  ?s <http://example.com/optimized> ?o .
}

### Telemetry Query ###
telemetry-plan-run-2
SELECT * WHERE {
  ?s <http://example.com/telemetry> ?o .
}
RESULT

PLANS_ONLY_OUTPUT="$(
	bash "${ANALYZER}" --results-dir "${PLAN_RESULTS_DIR}" --theme PHARMA --query-index 10 --plans-only
)"
echo "${PLANS_ONLY_OUTPUT}"

assert_not_contains \
	"${PLANS_ONLY_OUTPUT}" \
	"results-2026-04-01.md" \
	"plans-only query mode should omit result files without query plans"
assert_contains \
	"${PLANS_ONLY_OUTPUT}" \
	"results-2026-04-02.md" \
	"plans-only query mode should include result files with query plans"
assert_contains \
	"${PLANS_ONLY_OUTPUT}" \
	"Optimized query plan:" \
	"plans-only query mode should print optimized plans"
assert_contains \
	"${PLANS_ONLY_OUTPUT}" \
	"Telemetry query plan:" \
	"plans-only query mode should print telemetry plans"

TELEMETRY_OUTPUT="$(
	bash "${ANALYZER}" --results-dir "${PLAN_RESULTS_DIR}" --theme PHARMA --query-index 10 \
		--plans-only --result-file results-2026-04-02.md --plan-kind telemetry
)"
echo "${TELEMETRY_OUTPUT}"

assert_contains \
	"${TELEMETRY_OUTPUT}" \
	"telemetry-plan-run-2" \
	"telemetry plan mode should print telemetry query plans"
assert_not_contains \
	"${TELEMETRY_OUTPUT}" \
	"optimized-plan-run-2" \
	"telemetry plan mode should not print optimized query plans"

OPTIMIZED_OUTPUT="$(
	bash "${ANALYZER}" --results-dir "${PLAN_RESULTS_DIR}" --theme PHARMA --query-index 10 \
		--plans-only --result-file results-2026-04-02.md --plan-kind optimized
)"
echo "${OPTIMIZED_OUTPUT}"

assert_contains \
	"${OPTIMIZED_OUTPUT}" \
	"optimized-plan-run-2" \
	"optimized plan mode should print optimized query plans"
assert_not_contains \
	"${OPTIMIZED_OUTPUT}" \
	"telemetry-plan-run-2" \
	"optimized plan mode should not print telemetry query plans"
