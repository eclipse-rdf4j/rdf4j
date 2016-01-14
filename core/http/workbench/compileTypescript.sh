#!/bin/bash
#*******************************************************************************
# Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Distribution License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/org/documents/edl-v10.php.
#*******************************************************************************
scriptdir=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
destdir="$scriptdir/src/main/webapp/scripts"
srcdir=$destdir/ts
cd $srcdir
tsc --noImplicitAny --sourcemap --sourceRoot "/openrdf-workbench/scripts/ts" --outDir $destdir *.ts
echo 'Replaced repository JavaScript files with compiled TypeScript versions.'
