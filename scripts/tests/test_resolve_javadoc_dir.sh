#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LIB_DIR="${SCRIPT_DIR}/../lib"

# shellcheck disable=SC1090
source "${LIB_DIR}/javadoc.sh"

workdir="$(mktemp -d)"
trap 'rm -rf "${workdir}"' EXIT

mkdir -p "${workdir}/target/reports/apidocs"
resolved="$(resolve_javadoc_dir "${workdir}")"
if [[ "${resolved}" != "${workdir}/target/reports/apidocs" ]]; then
  echo "Expected reports directory but got ${resolved}" >&2
  exit 1
fi

rm -rf "${workdir}/target"
mkdir -p "${workdir}/target/site/apidocs"
resolved="$(resolve_javadoc_dir "${workdir}")"
if [[ "${resolved}" != "${workdir}/target/site/apidocs" ]]; then
  echo "Expected site directory but got ${resolved}" >&2
  exit 1
fi

mkdir -p "${workdir}/target/reports/apidocs"
resolved="$(resolve_javadoc_dir "${workdir}")"
if [[ "${resolved}" != "${workdir}/target/reports/apidocs" ]]; then
  echo "Expected reports to take precedence but got ${resolved}" >&2
  exit 1
fi

rm -rf "${workdir}/target"
if resolve_javadoc_dir "${workdir}"; then
  echo "Expected failure when no javadoc directory exists" >&2
  exit 1
else
  echo "resolve_javadoc_dir correctly fails when directories are missing" >&2
fi
