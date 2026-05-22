#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd -- "${SCRIPT_DIR}/../.." && pwd)"
SCRIPT="${REPO_ROOT}/scripts/run-single-benchmark.sh"

set +e
OUTPUT="$(bash "${SCRIPT}" --dry-run --module testsuites/benchmark --class org.eclipse.rdf4j.benchmark.ReasoningBenchmark --method forwardChainingSchemaCachingRDFSInferencer 2>&1)"
STATUS=$?
set -e

echo "${OUTPUT}"

if [[ ${STATUS} -ne 0 ]]; then
        exit ${STATUS}
fi

if [[ "${OUTPUT}" != *"mvn -T 2C -Dmaven.repo.local=.m2_repo -pl testsuites/benchmark -am -P benchmarks\\,quick package"* ]]; then
        echo "Expected optimized parallel Maven command not found in output" >&2
        exit 1
fi

if [[ "${OUTPUT}" != *"ReasoningBenchmark.forwardChainingSchemaCachingRDFSInferencer"* ]]; then
        echo "Expected benchmark method not found in output" >&2
        exit 1
fi

if [[ "${OUTPUT}" == *" clean package"* ]]; then
        echo "Did not expect a clean rebuild in the default fast path" >&2
        exit 1
fi

if [[ "${OUTPUT}" != *$'# On failure, reruns single-threaded:\nmvn -Dmaven.repo.local=.m2_repo -pl testsuites/benchmark -am -P benchmarks\\,quick package'* ]]; then
        echo "Expected single-threaded fallback command not found in output" >&2
        exit 1
fi

set +e
NO_BUILD_OUTPUT="$(bash "${SCRIPT}" --dry-run --no-build --module testsuites/benchmark --class org.eclipse.rdf4j.benchmark.ReasoningBenchmark --method forwardChainingSchemaCachingRDFSInferencer 2>&1)"
NO_BUILD_STATUS=$?
set -e

echo "${NO_BUILD_OUTPUT}"

if [[ ${NO_BUILD_STATUS} -ne 0 ]]; then
        exit ${NO_BUILD_STATUS}
fi

if [[ "${NO_BUILD_OUTPUT}" == *"mvn "* ]]; then
        echo "Did not expect Maven commands when --no-build is enabled" >&2
        exit 1
fi

if [[ "${NO_BUILD_OUTPUT}" != *"java -jar "* ]]; then
        echo "Expected benchmark command when --no-build is enabled" >&2
        exit 1
fi

set +e
CLEAN_OUTPUT="$(bash "${SCRIPT}" --dry-run --clean --module testsuites/benchmark --class org.eclipse.rdf4j.benchmark.ReasoningBenchmark --method forwardChainingSchemaCachingRDFSInferencer 2>&1)"
CLEAN_STATUS=$?
set -e

echo "${CLEAN_OUTPUT}"

if [[ ${CLEAN_STATUS} -ne 0 ]]; then
        exit ${CLEAN_STATUS}
fi

if [[ "${CLEAN_OUTPUT}" != *"mvn -T 2C -Dmaven.repo.local=.m2_repo -pl testsuites/benchmark -am -P benchmarks\\,quick clean package"* ]]; then
        echo "Expected clean rebuild command not found in output" >&2
        exit 1
fi

set +e
JFR_OUTPUT="$(bash "${SCRIPT}" --dry-run --module testsuites/benchmark --class org.eclipse.rdf4j.benchmark.ReasoningBenchmark --method forwardChainingSchemaCachingRDFSInferencer --enable-jfr 2>&1)"
JFR_STATUS=$?
set -e

echo "${JFR_OUTPUT}"

if [[ ${JFR_STATUS} -ne 0 ]]; then
        exit ${JFR_STATUS}
fi

if [[ "${JFR_OUTPUT}" != *"JFR profiling enabled:"* ]]; then
        echo "Expected JFR guidance banner when profiling is enabled" >&2
        exit 1
fi

EXPECTED_JFR_PATH="testsuites/benchmark/target/ReasoningBenchmark.forwardChainingSchemaCachingRDFSInferencer.jfr"
if [[ "${JFR_OUTPUT}" != *"${EXPECTED_JFR_PATH}"* ]]; then
        echo "Expected JFR banner to include the recording destination" >&2
        exit 1
fi

if [[ "${JFR_OUTPUT}" != *"-wi 0"* ]]; then
        echo "Expected JFR run to disable warmup iterations" >&2
        exit 1
fi

if [[ "${JFR_OUTPUT}" != *"-i 10"* ]]; then
        echo "Expected JFR run to force 10 measurement iterations" >&2
        exit 1
fi

if [[ "${JFR_OUTPUT}" != *"-r 10s"* ]]; then
        echo "Expected JFR run to set measurement time to 10 seconds" >&2
        exit 1
fi

if [[ "${JFR_OUTPUT}" != *"-f 1"* ]]; then
        echo "Expected JFR run to enforce a single fork" >&2
        exit 1
fi

if [[ "${JFR_OUTPUT}" != *"-XX:StartFlightRecording:settings=profile\\,dumponexit=true"* ]]; then
        echo "Expected JFR run to enable JFR profiling" >&2
        exit 1
fi

if [[ "${JFR_OUTPUT}" != *"-Drdf4j.benchmark.profiling=true"* ]]; then
        echo "Expected JFR run to mark benchmark profiling mode" >&2
        exit 1
fi

if [[ "${JFR_OUTPUT}" != *"testsuites/benchmark/target/ReasoningBenchmark.forwardChainingSchemaCachingRDFSInferencer.jfr"* ]]; then
        echo "Expected JFR run to emit recording into the module target directory" >&2
        exit 1
fi

set +e
JFR_CPU_OUTPUT="$(bash "${SCRIPT}" --dry-run --module testsuites/benchmark --class org.eclipse.rdf4j.benchmark.ReasoningBenchmark --method forwardChainingSchemaCachingRDFSInferencer --enable-jfr --enable-jfr-cpu-times 2>&1)"
JFR_CPU_STATUS=$?
set -e

echo "${JFR_CPU_OUTPUT}"

if [[ ${JFR_CPU_STATUS} -ne 0 ]]; then
        exit ${JFR_CPU_STATUS}
fi

if [[ "${JFR_CPU_OUTPUT}" != *"jdk.CPUTimeSample#enabled=true"* ]]; then
        echo "Expected CPU time sampling to be enabled when requested" >&2
        exit 1
fi

if [[ "${JFR_CPU_OUTPUT}" != *"report-on-exit=cpu-time-hot-methods"* ]]; then
        echo "Expected CPU time report to be enabled when requested" >&2
        exit 1
fi

set +e
JMH_JFR_OUTPUT="$(bash "${SCRIPT}" --dry-run --module testsuites/benchmark --class org.eclipse.rdf4j.benchmark.ReasoningBenchmark --method forwardChainingSchemaCachingRDFSInferencer --enable-jmh-jfr 2>&1)"
JMH_JFR_STATUS=$?
set -e

echo "${JMH_JFR_OUTPUT}"

if [[ ${JMH_JFR_STATUS} -ne 0 ]]; then
        exit ${JMH_JFR_STATUS}
fi

if [[ "${JMH_JFR_OUTPUT}" != *"JMH JFR profiling enabled:"* ]]; then
        echo "Expected JMH JFR guidance banner when profiling is enabled" >&2
        exit 1
fi

if [[ "${JMH_JFR_OUTPUT}" != *"-prof jfr:dir="* ]]; then
        echo "Expected JMH JFR run to enable the JMH JFR profiler" >&2
        exit 1
fi

if [[ "${JMH_JFR_OUTPUT}" != *"-Drdf4j.benchmark.profiling=true"* ]]; then
        echo "Expected JMH JFR run to mark benchmark profiling mode" >&2
        exit 1
fi

exit 0
