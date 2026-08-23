#!/bin/sh
#*******************************************************************************
# Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Distribution License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/org/documents/edl-v10.php.
#*******************************************************************************

JAVA_OPT=-mx512m
JAVA_CMD=${JAVA_CMD:-java}
VECTOR_OPT=

if "$JAVA_CMD" --list-modules 2>/dev/null | grep -q '^jdk\.incubator\.vector@'; then
	VECTOR_OPT=--add-modules=jdk.incubator.vector
fi

lib="$(dirname "${0}")/../lib"
"$JAVA_CMD" $JAVA_OPT $VECTOR_OPT -cp "$lib/$(ls "$lib"|xargs |sed "s; ;:$lib/;g")" org.eclipse.rdf4j.console.Console "$@"
