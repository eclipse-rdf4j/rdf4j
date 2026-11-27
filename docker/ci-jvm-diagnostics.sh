#!/usr/bin/env bash
# Copyright (c) 2026 Eclipse RDF4J contributors.
#
# All rights reserved. This program and the accompanying materials are made available under
# the terms of the Eclipse Distribution License v1.0, available at
# http://www.eclipse.org/org/documents/edl-v10.php.
# SPDX-License-Identifier: BSD-3-Clause

# Sourced by test launchers. Ordinary local Docker behavior is unchanged.
rdf4j_ci_docker_compose() {
  if [[ -z "${RDF4J_JVM_DIAGNOSTICS_DIR:-}" ]]; then
    docker compose "$@"
    return
  fi
  local docker_directory
  docker_directory="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
  mkdir -p "$RDF4J_JVM_DIAGNOSTICS_DIR/docker"
  # The image runs Tomcat/Jetty as an unprivileged UID; grant access only to this scratch bind.
  chmod a+rwx "$RDF4J_JVM_DIAGNOSTICS_DIR/docker"
  export RDF4J_DOCKER_JVM_DIAGNOSTIC_OPTIONS="-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpGzipLevel=1 -XX:HeapDumpPath=/rdf4j-jvm-diagnostics/java_pid%p.hprof.gz -XX:ErrorFile=/rdf4j-jvm-diagnostics/hs_err_pid%p.log"
  docker compose -f "$docker_directory/docker-compose.yml" \
    -f "$docker_directory/docker-compose.jvm-diagnostics.yml" "$@"
}
