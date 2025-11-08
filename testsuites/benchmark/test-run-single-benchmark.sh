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

exit 0
