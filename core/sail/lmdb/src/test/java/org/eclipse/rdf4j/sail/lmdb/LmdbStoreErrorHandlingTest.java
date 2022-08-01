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

import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Tests for the correct handling of internal LMDB store errors.
 */
public class LmdbStoreErrorHandlingTest {

	@Rule
	public TemporaryFolder tempDir = new TemporaryFolder();

	@Test
	public void testMapFullError() throws Exception {
		File dataDir = tempDir.newFolder();

		LmdbStoreConfig config = new LmdbStoreConfig("spoc,psoc");
		// set small db size
		config.setValueDBSize(50000);
		config.setTripleDBSize(50000);
		config.setAutoGrow(false);
		Repository repo = new SailRepository(new LmdbStore(dataDir, config));

		RepositoryException expected = null;
		try (RepositoryConnection conn = repo.getConnection()) {
			try {
				conn.begin();
				// add enough triples to force MDB_MAP_FULL error
				for (int i = 0; i < 100000; i++) {
					conn.add(RDFS.RESOURCE, RDFS.LABEL, conn.getValueFactory().createLiteral(i++));
				}
				conn.commit();
			} catch (RepositoryException re) {
				// this is expected and should happen
				conn.rollback();
				expected = re;
			}
		} finally {
			repo.shutDown();
		}
		assertNotNull(expected);
	}
}
