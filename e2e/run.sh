#!/usr/bin/env bash
#
# Copyright (c) 2023 Eclipse RDF4J contributors.
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Distribution License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/org/documents/edl-v10.php.
#
# SPDX-License-Identifier: BSD-3-Clause
#

set -e

cd ..
cd docker
./run.sh
./waitForDocker.sh
cd ..
cd e2e

sleep 10

if [ ! -d  'node_modules' ]; then
  echo "npm ci"
  npm ci
fi

docker ps

npx playwright install --with-deps # install browsers
npx playwright test
exit $?

