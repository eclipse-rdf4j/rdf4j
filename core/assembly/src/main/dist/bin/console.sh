#!/bin/sh
#*******************************************************************************
# Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Distribution License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/org/documents/edl-v10.php.
#*******************************************************************************

JAVA_OPT=-mx512m

lib="$(dirname "${0}")/../lib"
java $JAVA_OPT -cp "$lib/$(ls "$lib"|xargs |sed "s; ;:$lib/;g")" org.eclipse.rdf4j.console.Console $*
