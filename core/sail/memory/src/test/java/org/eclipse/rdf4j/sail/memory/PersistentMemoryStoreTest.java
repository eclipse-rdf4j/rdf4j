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

import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.testsuite.sail.RDFNotifyingStoreTest;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

/**
 * An extension of RDFStoreTest for testing the class <var>org.eclipse.rdf4j.sesame.sail.memory.MemoryStore</var>.
 */
public class PersistentMemoryStoreTest extends RDFNotifyingStoreTest {

	@Rule
	public TemporaryFolder tempDir = new TemporaryFolder();

	@Override
	protected NotifyingSail createSail() throws SailException {
		try {
			NotifyingSail sail = new MemoryStore(tempDir.newFolder(PersistentMemoryStoreTest.class.getSimpleName()));
			sail.init();
			return sail;
		} catch (IOException e) {
			throw new SailException(e);
		}
	}

}
