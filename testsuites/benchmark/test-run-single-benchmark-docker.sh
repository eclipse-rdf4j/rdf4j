#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd -- "${SCRIPT_DIR}/../.." && pwd)"
SCRIPT="${REPO_ROOT}/scripts/run-single-benchmark-docker.sh"

set +e
OUTPUT="$(bash "${SCRIPT}" org.eclipse.rdf4j.benchmark.ReasoningBenchmark.forwardChainingSchemaCachingRDFSInferencer --dry-run 2>&1)"
STATUS=$?
set -e

echo "${OUTPUT}"

if [[ ${STATUS} -ne 0 ]]; then
        exit ${STATUS}
fi

if [[ "${OUTPUT}" != *"docker create"* ]]; then
        echo "Expected reusable container creation command in dry-run output" >&2
        exit 1
fi

if [[ "${OUTPUT}" != *"sleep infinity"* ]]; then
        echo "Expected reusable benchmark container to stay alive between runs" >&2
        exit 1
fi

if [[ "${OUTPUT}" != *"docker exec"* ]]; then
        echo "Expected benchmark command to run via docker exec" >&2
        exit 1
fi

if [[ "${OUTPUT}" != *"-v ${REPO_ROOT}:/workspace"* ]]; then
        echo "Expected repository bind mount in dry-run output" >&2
        exit 1
fi

if [[ "${OUTPUT}" != *"--enable-jfr"* || "${OUTPUT}" != *"--enable-jfr-cpu-times"* ]]; then
        echo "Expected helper to keep JFR CPU time profiling flags enabled" >&2
        exit 1
fi

exit 0
