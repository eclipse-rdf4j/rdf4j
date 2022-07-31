/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import java.io.IOException;

import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.eclipse.rdf4j.testsuite.sail.SailConcurrencyTest;
import org.eclipse.rdf4j.testsuite.sail.SailInterruptTest;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

/**
 * An extension of {@link SailConcurrencyTest} for testing the class {@link LmdbStore}.
 */
public class LmdbStoreInterruptTest extends SailInterruptTest {

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
			return new LmdbStore(tempDir.newFolder(), new LmdbStoreConfig("spoc,posc"));
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}

}
