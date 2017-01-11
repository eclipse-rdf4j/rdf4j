/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf;

import java.io.IOException;

import org.eclipse.rdf4j.sail.InferencingTest;
import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.inferencer.fc.ForwardChainingSchemaCachingRDFSInferencer;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

public class NativeStoreInferencingTestForwardChainingSchemaCachingRDFSInferencer extends InferencingTest {

	@Rule
	public TemporaryFolder tempDir = new TemporaryFolder();

	@Override
	protected Sail createSail() {
		try {
			NotifyingSail sailStack = new NativeStore(tempDir.newFolder("nativestore"), "spoc,posc");
			sailStack = new ForwardChainingSchemaCachingRDFSInferencer(sailStack, true);
			return sailStack;
		}
		catch (IOException e) {
			throw new AssertionError(e);
		}
	}
}
