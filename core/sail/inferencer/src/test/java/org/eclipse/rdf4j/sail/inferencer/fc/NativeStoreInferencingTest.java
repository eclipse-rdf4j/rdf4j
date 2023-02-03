/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.inferencer.fc;

import java.io.File;

import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import org.eclipse.rdf4j.testsuite.sail.InferencingTest;
import org.junit.jupiter.api.io.TempDir;

public class NativeStoreInferencingTest extends InferencingTest {

	@TempDir
	File dataDir;

	@Override
	protected Sail createSail() {
		NotifyingSail sailStack = new NativeStore(dataDir, "spoc,posc");
		sailStack = new SchemaCachingRDFSInferencer(sailStack);
		return sailStack;
	}
}
