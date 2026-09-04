#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd -- "${SCRIPT_DIR}/../.." && pwd)"
RESULTS_SCRIPT="${REPO_ROOT}/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/theme-query-benchmark-results/theme-query-benchmark-results.sh"

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

INPUT="$(mktemp "${TMPDIR:-/tmp}/theme-query-clean-input.XXXXXX")"
FIRST_OUTPUT="$(mktemp "${TMPDIR:-/tmp}/theme-query-clean-first.XXXXXX")"
SECOND_OUTPUT="$(mktemp "${TMPDIR:-/tmp}/theme-query-clean-second.XXXXXX")"
trap 'rm -f "${INPUT}" "${FIRST_OUTPUT}" "${SECOND_OUTPUT}"' EXIT

cat >"${INPUT}" <<'FIXTURE'
```
Benchmark                              (themeName)  (z_queryIndex)  Mode  Cnt      Score      Error  Units
ThemeQueryBenchmark.executeQuery   MEDICAL_RECORDS               0  avgt    3     62.319 +/-  9.381  ms/op

```

```
/Users/havardottestad/.sdkman/candidates/java/25-zulu/zulu-25.jdk/Contents/Home/bin/java -javaagent:/Applications/IntelliJ IDEA.app/Contents/lib/idea_rt.jar=50378 -classpath /tmp/classes org.openjdk.jmh.Main org.eclipse.rdf4j.sail.lmdb.benchmark.ThemeQueryBenchmark.executeQuery
# Warmup Iteration   1: /Users/havardottestad/Documents/Programming/rdf4j-stf/core/sail/lmdb/target/lmdb-theme-query-benchmark/complete
### Original Query ###
SELECT (COUNT(DISTINCT ?patient) AS ?count) WHERE {
?patient a <http://example.com/theme/medical/Patient> .
}

62.319 ms/op

Result "org.eclipse.rdf4j.sail.lmdb.benchmark.ThemeQueryBenchmark.executeQuery":
62.319 +/- 9.381 ms/op [Average]

REMEMBER: The numbers below are just data. To gain reusable insights, you need to follow up on
why the numbers are the way they are. Use profilers (see -prof, -lprof), design factorial
experiments, perform baseline and negative tests that provide experimental control, make sure
the benchmarking environment is safe on JVM/OS/HW level, ask for reviews from the domain experts.
Do not assume the numbers tell you what you want them to tell.

NOTE: Current JVM experimentally supports Compiler Blackholes, and they are in use. Please exercise
extra caution when trusting the results, look into the generated code to check the benchmark still
works, and factor in a small probability of new VM bugs. Additionally, while comparisons between
different JVMs are already problematic, the performance difference caused by different Blackhole
modes can be very significant. Please make sure you use the consistent Blackhole mode for comparisons.

Process finished with exit code 0

```
FIXTURE

bash "${RESULTS_SCRIPT}" clean "${INPUT}" >"${FIRST_OUTPUT}"
bash "${RESULTS_SCRIPT}" clean "${FIRST_OUTPUT}" >"${SECOND_OUTPUT}"

CLEANED="$(cat "${FIRST_OUTPUT}")"

assert_not_contains "${CLEANED}" "org.openjdk.jmh.Main" \
	"clean should remove IntelliJ/java invocation lines"
assert_not_contains "${CLEANED}" "/target/lmdb-theme-query-benchmark/complete" \
	"clean should remove benchmark completion paths"
assert_contains "${CLEANED}" "# Warmup Iteration   1:" \
	"clean should preserve the warmup iteration marker"
assert_not_contains "${CLEANED}" "REMEMBER: The numbers below are just data" \
	"clean should remove JMH reminder block"
assert_not_contains "${CLEANED}" "NOTE: Current JVM experimentally supports Compiler Blackholes" \
	"clean should remove JMH compiler blackhole note"
assert_not_contains "${CLEANED}" "Process finished with exit code 0" \
	"clean should remove IntelliJ process trailer"

assert_contains "${CLEANED}" "ThemeQueryBenchmark.executeQuery   MEDICAL_RECORDS" \
	"clean should preserve the benchmark summary table"
assert_contains "${CLEANED}" "SELECT (COUNT(DISTINCT ?patient) AS ?count) WHERE {" \
	"clean should preserve benchmark query text"
assert_contains "${CLEANED}" "62.319 +/- 9.381 ms/op [Average]" \
	"clean should preserve benchmark result text"

if ! cmp -s "${FIRST_OUTPUT}" "${SECOND_OUTPUT}"; then
	fail "clean should be idempotent"
fi
