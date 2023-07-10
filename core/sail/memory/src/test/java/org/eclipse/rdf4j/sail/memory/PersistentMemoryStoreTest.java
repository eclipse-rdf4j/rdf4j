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

import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.testsuite.sail.RDFNotifyingStoreTest;
import org.junit.jupiter.api.io.TempDir;

/**
 * An extension of RDFStoreTest for testing the class <var>org.eclipse.rdf4j.sesame.sail.memory.MemoryStore</var>.
 */
public class PersistentMemoryStoreTest extends RDFNotifyingStoreTest {

	@TempDir
	public File dataDir;

	@Override
	protected NotifyingSail createSail() throws SailException {
		NotifyingSail sail = new MemoryStore(dataDir);
		sail.init();
		return sail;
	}
}
