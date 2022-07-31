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
package org.eclipse.rdf4j.sail.memory;

import java.io.IOException;

import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.SailException;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

/**
 * An extension of {@link MemoryStoreIsolationLevelTest} for testing the class {@link MemoryStore} using on-disk
 * persistence.
 */
public class PersistentMemoryStoreIsolationLevelTest extends MemoryStoreIsolationLevelTest {

	@Rule
	public TemporaryFolder tempDir = new TemporaryFolder();

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	protected Sail createSail() throws SailException {
		MemoryStore sail;
		try {
			sail = new MemoryStore(tempDir.newFolder("memory-store"));
		} catch (IOException e) {
			throw new AssertionError(e);
		}
		sail.setSyncDelay(100);
		return sail;
	}
}
