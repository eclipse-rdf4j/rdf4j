/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.inferencer.fc;

import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.SailIsolationLevelTest;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;

/**
 * An extension of {@link SailIsolationLevelTest} for testing the {@link SchemaCachingRDFSInferencer}.
 */
public class SchemaCachingRDFSInferencerNativeIsolationLevelTest extends SailIsolationLevelTest {

	/*-----------*
	 * Variables *
	 *-----------*/

	@Rule
	public TemporaryFolder tempDir = new TemporaryFolder();

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	protected NotifyingSail createSail() throws SailException {
		try {
			return new SchemaCachingRDFSInferencer(new NativeStore(tempDir.newFolder("nativestore"), "spoc,posc"));
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}

	@Override
	public void testLargeTransactionSerializable() throws InterruptedException {
		// ignored since test is slow
	}

	@Override
	public void testSnapshot() throws Exception {
		// see: https://github.com/eclipse/rdf4j/issues/1794
	}
}
