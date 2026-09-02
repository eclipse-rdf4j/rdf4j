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

date

script_directory=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
repository_root=$(CDPATH= cd -- "$script_directory/.." && pwd)
tool_target="$repository_root/tools/lmdb-bulk-load/target"
skip_build=false

if [ "${1:-}" = "--no-build" ]; then
	skip_build=true
	shift
fi

if [ "$#" -eq 0 ]; then
	set -- --interactive
fi

find_tool_jar() {
	find "$tool_target" -maxdepth 1 -type f -name 'rdf4j-lmdb-bulk-load-*-executable.jar' \
		-print 2>/dev/null | sort | tail -n 1
}

tool_jar=$(find_tool_jar)
needs_build=false
if [ -z "$tool_jar" ] || [ ! -f "$tool_jar" ]; then
	needs_build=true
elif find "$repository_root/core/sail/lmdb/src" "$repository_root/tools/lmdb-bulk-load/src" \
		"$repository_root/core/sail/lmdb/pom.xml" "$repository_root/tools/lmdb-bulk-load/pom.xml" \
		-type f -newer "$tool_jar" -print -quit | grep -q .; then
	needs_build=true
fi

if [ "$needs_build" = true ]; then
	if [ "$skip_build" = true ]; then
		echo "LMDB bulk-load tool is missing or stale; rerun without --no-build" >&2
		exit 2
	fi
	(
		cd "$repository_root"
		exec mvn -B -ntp -o -Dmaven.repo.local=.m2_repo -pl tools/lmdb-bulk-load -am -Pquick package
	)
	tool_jar=$(find_tool_jar)
fi

if [ -z "$tool_jar" ] || [ ! -f "$tool_jar" ]; then
	echo "Could not find the built LMDB bulk-load tool in $tool_target" >&2
	exit 1
fi

if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
	java_command="$JAVA_HOME/bin/java"
else
	java_command=java
fi

exec "$java_command" --enable-native-access=ALL-UNNAMED -jar "$tool_jar" "$@"

date