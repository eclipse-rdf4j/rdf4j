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

import java.io.File;

import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.SailException;
import org.junit.jupiter.api.io.TempDir;

/**
 * An extension of {@link MemoryStoreIsolationLevelTest} for testing the class {@link MemoryStore} using on-disk
 * persistence.
 */
public class PersistentMemoryStoreIsolationLevelTest extends MemoryStoreIsolationLevelTest {

	@TempDir
	public File tempDir;

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	protected Sail createSail() throws SailException {
		File dataDir = new File(tempDir, "memory-store");
		dataDir.mkdir();
		MemoryStore sail = new MemoryStore(dataDir);
		sail.setSyncDelay(100);
		return sail;
	}
}
