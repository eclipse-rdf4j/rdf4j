/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
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
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import org.eclipse.rdf4j.testsuite.sail.SailIsolationLevelTest;
import org.junit.jupiter.api.io.TempDir;

/**
 * An extension of {@link SailIsolationLevelTest} for testing the {@link SchemaCachingRDFSInferencer}.
 */
public class SchemaCachingRDFSInferencerNativeIsolationLevelTest extends SailIsolationLevelTest {

	/*-----------*
	 * Variables *
	 *-----------*/

	@TempDir
	public File tempDir;

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	protected NotifyingSail createSail() throws SailException {
		return new SchemaCachingRDFSInferencer(new NativeStore(tempDir, "spoc,posc"));
	}

	@Override
	public void testLargeTransactionSerializable() {
		// ignored since test is slow
	}

	@Override
	public void testSnapshot() {
		// see: https://github.com/eclipse/rdf4j/issues/1794
	}
}
