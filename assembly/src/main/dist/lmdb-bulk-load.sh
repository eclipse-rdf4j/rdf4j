#!/bin/sh
#*******************************************************************************
# Copyright (c) 2026 Eclipse RDF4J contributors.
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Distribution License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/org/documents/edl-v10.php.
#
# SPDX-License-Identifier: BSD-3-Clause
#*******************************************************************************

set -eu

lib_directory=$(CDPATH= cd -- "$(dirname -- "$0")/../lib" && pwd)
if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
	java_command="$JAVA_HOME/bin/java"
else
	java_command=java
fi

if [ "$#" -eq 0 ]; then
	set -- --interactive
fi

exec "$java_command" --enable-native-access=ALL-UNNAMED -cp "$lib_directory/*" \
	org.eclipse.rdf4j.tools.lmdb.bulk.LmdbBulkLoad "$@"
