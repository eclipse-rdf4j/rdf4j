#!/usr/bin/env bash
#
# Copyright (c) 2026 Eclipse RDF4J contributors.
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Distribution License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/org/documents/edl-v10.php.
#
# SPDX-License-Identifier: BSD-3-Clause
#

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
MVN_BIN="${MVN_BIN:-mvn}"
RUN_REGULAR_DOCKER="${RUN_REGULAR_DOCKER:-1}"
SKIP_REACTOR_INSTALL="${SKIP_REACTOR_INSTALL:-0}"
SPRING_BOOT_APPDATA="$(mktemp -d "${TMPDIR:-/tmp}/rdf4j-e2e-compression.XXXXXX")"
SERVER_PID=""
DOCKER_STARTED=0

cleanup() {
  if [[ -n "${SERVER_PID}" ]] && kill -0 "${SERVER_PID}" 2>/dev/null; then
    kill -s INT "${SERVER_PID}" 2>/dev/null || true
    for _ in 1 2 3 4 5 6 7 8 9 10; do
      if ! kill -0 "${SERVER_PID}" 2>/dev/null; then
        wait "${SERVER_PID}" 2>/dev/null || true
        break
      fi
      sleep 0.5
    done
    if kill -0 "${SERVER_PID}" 2>/dev/null; then
      kill "${SERVER_PID}" 2>/dev/null || true
      wait "${SERVER_PID}" 2>/dev/null || true
    fi
  fi

  if [[ "${DOCKER_STARTED}" == "1" ]]; then
    (cd "${PROJECT_ROOT}/docker" && docker compose down -v)
  fi
}

trap cleanup EXIT

run_compression_spec() {
  local target_name="$1"
  (cd "${SCRIPT_DIR}" && RDF4J_E2E_TARGET_NAME="${target_name}" npm run test:compression)
}

wait_for_server() {
  local url="$1"
  for _ in {1..60}; do
    if curl -fsS "${url}" >/dev/null; then
      return
    fi
    if [[ -n "${SERVER_PID}" ]] && ! kill -0 "${SERVER_PID}" 2>/dev/null; then
      printf 'Server process exited before %s became reachable\n' "${url}" >&2
      return 1
    fi
    sleep 1
  done
  printf 'Timed out waiting for %s\n' "${url}" >&2
  return 1
}

if [[ ! -d "${SCRIPT_DIR}/node_modules" ]]; then
  (cd "${SCRIPT_DIR}" && npm ci)
fi

if [[ "${SKIP_REACTOR_INSTALL}" != "1" ]]; then
  (cd "${PROJECT_ROOT}" && "${MVN_BIN}" -o -Dmaven.repo.local=.m2_repo -Pquick clean install)
fi

(cd "${PROJECT_ROOT}" && "${MVN_BIN}" -o -Dmaven.repo.local=.m2_repo \
  -Dspring-boot.run.jvmArguments="-Dorg.eclipse.rdf4j.appdata.basedir=${SPRING_BOOT_APPDATA} -Dorg.eclipse.rdf4j.rio.jsonld_secure_mode=false" \
  -pl tools/server-boot spring-boot:run) &
SERVER_PID=$!
wait_for_server "http://127.0.0.1:8080/rdf4j-server/"
run_compression_spec "spring-boot"

cleanup
SERVER_PID=""

if [[ "${RUN_REGULAR_DOCKER}" != "1" ]]; then
  exit 0
fi

(cd "${PROJECT_ROOT}/docker" && APP_SERVER="${APP_SERVER:-tomcat}" ./run.sh)
DOCKER_STARTED=1
wait_for_server "http://127.0.0.1:8080/rdf4j-server/"
run_compression_spec "regular-spring-docker"
