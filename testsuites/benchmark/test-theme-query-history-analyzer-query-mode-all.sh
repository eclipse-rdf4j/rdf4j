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

TODAY_RESULTS_DIR="$(mktemp -d "${TMPDIR:-/tmp}/theme-query-history-today.XXXXXX")"

cat > "${TODAY_RESULTS_DIR}/results-2026-05-29.md" <<RESULT
ThemeQueryBenchmark.executeQuery PHARMA 10 avgt 1.000 ms/op
RESULT

cat > "${TODAY_RESULTS_DIR}/results-2026-06-03.md" <<RESULT
ThemeQueryPlanRunBenchmark.runQuery PHARMA 10 avgt 5.000 ms/op
RESULT

TODAY_OUTPUT="$(bash "${ANALYZER}" --results-dir "${TODAY_RESULTS_DIR}")"
echo "${TODAY_OUTPUT}"

assert_contains \
	"${TODAY_OUTPUT}" \
	"Latest run: results-2026-06-03.md" \
	"overview should use today's plan-run result file as latest"
assert_contains \
	"${TODAY_OUTPUT}" \
	"q10: latest 5.000 ms/op | fastest 1.000 ms/op | 80.0% slower than best" \
	"overview should compare today's plan-run score against historical ThemeQueryBenchmark scores"
assert_contains \
	"${TODAY_OUTPUT}" \
	"results-2026-05-29.md: 1.000 ms/op" \
	"overview detail should include the historical ThemeQueryBenchmark comparison run"

LATEST_FILE_RESULTS_DIR="$(mktemp -d "${TMPDIR:-/tmp}/theme-query-history-latest-file.XXXXXX")"

cat > "${LATEST_FILE_RESULTS_DIR}/results-2026-05-29.md" <<RESULT
ThemeQueryBenchmark.executeQuery PHARMA 10 avgt 1.000 ms/op
RESULT

cat > "${LATEST_FILE_RESULTS_DIR}/results-2026-06-04.md" <<RESULT
ThemeQueryPlanRunBenchmark.runQuery PHARMA 10 avgt 5.000 ms/op
RESULT

cat > "${LATEST_FILE_RESULTS_DIR}/results-2026-06-05.md" <<RESULT
ThemeQueryPlanRunBenchmark.runQuery PHARMA 10 avgt 0.500 ms/op
RESULT

LATEST_FILE_OUTPUT="$(
	bash "${ANALYZER}" --results-dir "${LATEST_FILE_RESULTS_DIR}" \
		--latest-file results-2026-06-04.md
)"
echo "${LATEST_FILE_OUTPUT}"

assert_contains \
	"${LATEST_FILE_OUTPUT}" \
	"Latest run: results-2026-06-04.md" \
	"overview should use the explicitly selected latest result file"
assert_contains \
	"${LATEST_FILE_OUTPUT}" \
	"q10: latest 5.000 ms/op | fastest 1.000 ms/op | 80.0% slower than best" \
	"explicit latest file mode should compare plan-run scores against historical ThemeQueryBenchmark scores"

DYNAMIC_TODAY_RESULTS_DIR="$(mktemp -d "${TMPDIR:-/tmp}/theme-query-history-dynamic-today.XXXXXX")"
TODAY_DATE="$(date +%F)"

cat > "${DYNAMIC_TODAY_RESULTS_DIR}/results-1999-01-01.md" <<RESULT
ThemeQueryBenchmark.executeQuery PHARMA 10 avgt 1.000 ms/op
RESULT

cat > "${DYNAMIC_TODAY_RESULTS_DIR}/results-${TODAY_DATE}.md" <<RESULT
ThemeQueryPlanRunBenchmark.runQuery PHARMA 10 avgt 5.000 ms/op
RESULT

cat > "${DYNAMIC_TODAY_RESULTS_DIR}/results-${TODAY_DATE}-2.md" <<RESULT
ThemeQueryPlanRunBenchmark.runQuery PHARMA 10 avgt 6.000 ms/op
RESULT

DYNAMIC_TODAY_OUTPUT="$(
	bash "${ANALYZER}" --results-dir "${DYNAMIC_TODAY_RESULTS_DIR}" --today
)"
echo "${DYNAMIC_TODAY_OUTPUT}"

assert_contains \
	"${DYNAMIC_TODAY_OUTPUT}" \
	"Latest run: results-${TODAY_DATE}-2.md" \
	"--today should use the newest suffixed result file for the current local date"
assert_contains \
	"${DYNAMIC_TODAY_OUTPUT}" \
	"q10: latest 6.000 ms/op | fastest 1.000 ms/op | 83.3% slower than best" \
	"--today should compare today's plan-run score against historical ThemeQueryBenchmark scores"

MAY_AVERAGE_RESULTS_DIR="$(mktemp -d "${TMPDIR:-/tmp}/theme-query-history-may-average.XXXXXX")"

cat > "${MAY_AVERAGE_RESULTS_DIR}/results-2026-05-01.md" <<RESULT
ThemeQueryBenchmark.executeQuery PHARMA 7 avgt 10.000 ms/op
RESULT

cat > "${MAY_AVERAGE_RESULTS_DIR}/results-2026-05-02.md" <<RESULT
ThemeQueryBenchmark.executeQuery PHARMA 7 avgt 20.000 ms/op
RESULT

cat > "${MAY_AVERAGE_RESULTS_DIR}/results-2026-05-22.md" <<RESULT
ThemeQueryBenchmark.executeQuery PHARMA 7 avgt 1.000 ms/op
RESULT

cat > "${MAY_AVERAGE_RESULTS_DIR}/results-2026-06-03.md" <<RESULT
ThemeQueryPlanRunBenchmark.runQuery PHARMA 7 avgt 18.000 ms/op
RESULT

MAY_AVERAGE_OUTPUT="$(
	bash "${ANALYZER}" --results-dir "${MAY_AVERAGE_RESULTS_DIR}" \
		--baseline-start 2026-05-01 --baseline-end 2026-05-21 --baseline-stat average \
		--min-slower-pct 0
)"
echo "${MAY_AVERAGE_OUTPUT}"

assert_contains \
	"${MAY_AVERAGE_OUTPUT}" \
	"Baseline: average ThemeQueryBenchmark.executeQuery from 2026-05-01 to 2026-05-21" \
	"May average mode should describe the explicit benchmark baseline window"
assert_contains \
	"${MAY_AVERAGE_OUTPUT}" \
	"q7: latest 18.000 ms/op | baseline average 15.000 ms/op | 16.7% slower than baseline" \
	"May average mode should compare today's plan-run score against the May ThemeQueryBenchmark average"
assert_not_contains \
	"${MAY_AVERAGE_OUTPUT}" \
	"1.000 ms/op" \
	"May average mode should ignore ThemeQueryBenchmark rows outside the selected baseline window"
