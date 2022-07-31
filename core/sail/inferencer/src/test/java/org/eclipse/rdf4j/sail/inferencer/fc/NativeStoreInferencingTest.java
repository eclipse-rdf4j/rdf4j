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

import java.io.IOException;

import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import org.eclipse.rdf4j.testsuite.sail.InferencingTest;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

public class NativeStoreInferencingTest extends InferencingTest {

	@Rule
	public TemporaryFolder tempDir = new TemporaryFolder();

	@Override
	protected Sail createSail() {
		try {
			NotifyingSail sailStack = new NativeStore(tempDir.newFolder(), "spoc,posc");
			sailStack = new SchemaCachingRDFSInferencer(sailStack);
			return sailStack;
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}
}
