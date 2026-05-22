#!/usr/bin/env bash
#
# Copyright (c) 2025 Eclipse RDF4J contributors.
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Distribution License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/org/documents/edl-v10.php.
#
# SPDX-License-Identifier: BSD-3-Clause
#

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
E2E_DIR="${ROOT_DIR}/e2e"
DOCKER_DIR="${ROOT_DIR}/docker"
SERVER_RUNTIME="${1:-${E2E_SERVER_RUNTIME:-spring-boot}}"
SERVER_PID=""
DOCKER_STARTED="false"
SPRING_BOOT_DATA_DIR=""

stop_spring_boot() {
  if [ -z "${SERVER_PID:-}" ]; then
    return
  fi

  if ! kill -0 "$SERVER_PID" 2>/dev/null; then
    return
  fi

  echo "Sending SIGINT to server-boot module (pid=${SERVER_PID})"
  kill -s INT "$SERVER_PID" 2>/dev/null || true

  for _ in 1 2 3 4 5 6 7 8 9 10; do
    if ! kill -0 "$SERVER_PID" 2>/dev/null; then
      echo "server-boot module stopped gracefully after SIGINT"
      wait "$SERVER_PID" 2>/dev/null || true
      return
    fi
    kill -s INT "$SERVER_PID" 2>/dev/null || true
    sleep 0.5
  done

  echo "Sending SIGTERM to server-boot module (pid=${SERVER_PID})"
  kill "$SERVER_PID" 2>/dev/null || true

  for _ in 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20; do
    if ! kill -0 "$SERVER_PID" 2>/dev/null; then
      echo "server-boot module stopped after SIGTERM"
      wait "$SERVER_PID" 2>/dev/null || true
      return
    fi
    sleep 0.5
  done

  echo "Sending SIGKILL to server-boot module (pid=${SERVER_PID})"
  kill -9 "$SERVER_PID" 2>/dev/null || true
  wait "$SERVER_PID" 2>/dev/null || true
}

stop_docker_tomcat() {
  if [ "${DOCKER_STARTED}" != "true" ]; then
    return
  fi

  echo "Stopping Docker/Tomcat RDF4J stack"
  (cd "$DOCKER_DIR" && APP_SERVER=tomcat docker compose down -v) || true
}

cleanup() {
  local status="${1:-$?}"
  trap - EXIT INT TERM
  stop_spring_boot
  stop_docker_tomcat
  exit "$status"
}

usage() {
  echo "Usage: $0 [spring-boot|docker-tomcat]" >&2
}

install_e2e_dependencies() {
  cd "$E2E_DIR"

  if [ ! -d "node_modules" ]; then
    echo "Installing E2E npm dependencies"
    npm ci
  fi

  if [ "${E2E_SKIP_PLAYWRIGHT_INSTALL:-false}" = "true" ]; then
    echo "Skipping Playwright browser install"
  else
    echo "Installing Playwright browsers"
    npx playwright install --with-deps
  fi
}

wait_for_url() {
  local label="$1"
  local url="$2"

  printf 'Waiting for %s at %s' "$label" "$url"
  for _ in $(seq 1 90); do
    if curl --fail --location --silent --output /dev/null "$url"; then
      echo ""
      echo "${label} is ready"
      return
    fi
    ensure_runtime_running
    printf '.'
    sleep 1
  done

  echo ""
  echo "Timed out waiting for ${label} at ${url}" >&2
  return 1
}

ensure_runtime_running() {
  if [ -n "${SERVER_PID:-}" ] && ! kill -0 "$SERVER_PID" 2>/dev/null; then
    echo ""
    echo "server-boot module exited before RDF4J became ready" >&2
    wait "$SERVER_PID" 2>/dev/null || true
    SERVER_PID=""
    return 1
  fi

  if [ "${DOCKER_STARTED}" = "true" ]; then
    local container_id
    container_id="$(cd "$DOCKER_DIR" && APP_SERVER=tomcat docker compose ps -q rdf4j 2>/dev/null || true)"
    if [ -n "$container_id" ] && [ "$(docker inspect -f '{{.State.Running}}' "$container_id" 2>/dev/null || echo false)" != "true" ]; then
      echo ""
      echo "Docker/Tomcat container exited before RDF4J became ready" >&2
      (cd "$DOCKER_DIR" && APP_SERVER=tomcat docker compose logs --tail=200 rdf4j) || true
      return 1
    fi
  fi
}

wait_for_rdf4j() {
  wait_for_url "RDF4J Server" "http://localhost:8080/rdf4j-server/"
  wait_for_url "RDF4J Workbench" "http://localhost:8080/rdf4j-workbench/"
}

start_spring_boot() {
  echo "Building RDF4J for Spring Boot E2E"
  (cd "$ROOT_DIR" && mvn install -Pquick)

  SPRING_BOOT_DATA_DIR="${E2E_DATA_DIR:-$(mktemp -d "${TMPDIR:-/tmp}/rdf4j-e2e.XXXXXX")}"
  echo "Using RDF4J app data directory ${SPRING_BOOT_DATA_DIR}"

  echo "Starting RDF4J Server and Workbench with Spring Boot"
  (
    cd "$ROOT_DIR"
    mvn -pl tools/server-boot spring-boot:run \
      -Dspring-boot.run.jvmArguments="-Dorg.eclipse.rdf4j.appdata.basedir=${SPRING_BOOT_DATA_DIR}"
  ) &
  SERVER_PID=$!
}

start_docker_tomcat() {
  echo "Building Docker/Tomcat RDF4J image"
  (cd "$DOCKER_DIR" && APP_SERVER=tomcat ./build.sh)

  echo "Starting Docker/Tomcat RDF4J container"
  (cd "$DOCKER_DIR" && APP_SERVER=tomcat docker compose up --force-recreate -d)
  DOCKER_STARTED="true"
}

run_playwright() {
  cd "$E2E_DIR"
  npx playwright test
}

trap 'cleanup $?' EXIT
trap 'cleanup 130' INT
trap 'cleanup 143' TERM

case "$SERVER_RUNTIME" in
  spring-boot)
    install_e2e_dependencies
    start_spring_boot
    ;;
  docker | docker-tomcat | tomcat)
    install_e2e_dependencies
    start_docker_tomcat
    ;;
  *)
    usage
    exit 2
    ;;
esac

wait_for_rdf4j
run_playwright

echo "E2E test OK (${SERVER_RUNTIME})"
