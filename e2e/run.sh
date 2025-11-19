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

set -e

SERVER_PID=""

cleanup() {
  if [ -z "${SERVER_PID:-}" ]; then
    return
  fi

  # If the process is already gone, nothing to do
  if ! kill -0 "$SERVER_PID" 2>/dev/null; then
    return
  fi

  echo "Sending SIGINT to server-boot module (pid=$SERVER_PID)"
  kill -s INT "$SERVER_PID" 2>/dev/null || true

  # Wait for graceful shutdown after SIGINT
  for i in 1 2 3 4 5 6 7 8 9 10; do
    if ! kill -0 "$SERVER_PID" 2>/dev/null; then
      echo "server-boot module stopped gracefully after SIGINT"
      wait "$SERVER_PID" 2>/dev/null || true
      return
    fi
    kill -s INT "$SERVER_PID" 2>/dev/null || true
    sleep 0.5
  done

  # Still alive: send a more aggressive TERM
  echo "Sending SIGTERM to server-boot module (pid=$SERVER_PID)"
  kill "$SERVER_PID" 2>/dev/null || true

  # Wait for graceful shutdown after SIGTERM
  for i in 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20; do
    if ! kill -0 "$SERVER_PID" 2>/dev/null; then
      echo "server-boot module stopped after SIGTERM"
      wait "$SERVER_PID" 2>/dev/null || true
      return
    fi
    sleep 0.5
  done

  # Still alive after: kill definitively
  echo "Sending SIGKILL to server-boot module (pid=$SERVER_PID)"
  kill -9 "$SERVER_PID" 2>/dev/null || true
  wait "$SERVER_PID" 2>/dev/null || true
}

trap cleanup EXIT

npm install

cd ..

mvn -q install -Pquick

mvn -pl tools/server-boot spring-boot:run &
SERVER_PID=$!
# server-boot module will be stopped automatically on script exit (see cleanup trap above).

cd e2e

sleep 10

if [ ! -d 'node_modules' ]; then
  echo "npm ci"
  npm ci
fi

npx playwright install --with-deps # install browsers
npx playwright test

status_npx=$?

cd ..

# test for error code
if [ $status_npx -ne 0 ]; then
  echo "Error in E2E test"
  exit $status_npx
fi

echo "E2E test OK"

# don't redo the whole build process just for making another docker image
export SKIP_BUILD="skip"
