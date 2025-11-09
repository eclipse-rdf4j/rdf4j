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

if [[ "${OUTPUT}" != *"mvn -pl testsuites/benchmark -am -P benchmarks -DskipTests package"* ]]; then
        echo "Expected Maven command not found in output" >&2
        exit 1
fi

if [[ "${OUTPUT}" != *"ReasoningBenchmark.forwardChainingSchemaCachingRDFSInferencer"* ]]; then
        echo "Expected benchmark method not found in output" >&2
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

if [[ "${JFR_OUTPUT}" != *"-XX:StartFlightRecording=settings=profile\\,dumponexit=true"* ]]; then
        echo "Expected JFR run to enable JFR profiling" >&2
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

if [[ "${JFR_CPU_OUTPUT}" != *"-XX:FlightRecorderOptions=enableThreadCpuTime=true\\,enableProcessCpuTime=true"* ]]; then
        echo "Expected CPU time options to be appended when requested" >&2
        exit 1
fi

exit 0
