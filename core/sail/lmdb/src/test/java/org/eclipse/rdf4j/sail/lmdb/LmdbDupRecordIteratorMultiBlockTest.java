/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

/**
 * Verifies that LmdbDupRecordIterator returns all duplicates across multiple MDB_GET_MULTIPLE blocks by inserting more
 * duplicate values than can fit in a single page-chunk for one (subject,predicate) key.
 */
class LmdbDupRecordIteratorMultiBlockTest {

	@TempDir
	File dataDir;

	private LmdbStore store;
	private NotifyingSailConnection con;
	private final ValueFactory vf = SimpleValueFactory.getInstance();

	@BeforeEach
	void setUp() throws Exception {
		LmdbStoreConfig config = new LmdbStoreConfig("spoc,posc");
		config.setDupsortIndices(true);
		config.setDupsortRead(true);
		store = new LmdbStore(dataDir, config);
		store.init();
		con = store.getConnection();
	}

	@AfterEach
	void tearDown() throws Exception {
		if (con != null) {
			con.close();
			con = null;
		}
		if (store != null) {
			store.shutDown();
			store = null;
		}
	}

	@Test
	@Timeout(20)
	void returnsAllDuplicatesAcrossMultipleBlocks() throws Exception {
		// Choose a single subject/predicate and generate many distinct objects so that
		// duplicates exceed a single MDB_GET_MULTIPLE chunk.
		Resource s = vf.createIRI("urn:dup:subject");
		IRI p = vf.createIRI("urn:dup:predicate");

		int duplicates = 1300; // > 1 page of 16-byte dup values on common 4K/16K pages

		con.begin();
		for (int i = 0; i < duplicates; i++) {
			con.addStatement(s, p, vf.createIRI("urn:dup:obj:" + i));
		}
		con.commit();

		int count = 0;
		try (var iter = con.getStatements(s, p, null, true)) {
			while (iter.hasNext()) {
				iter.next();
				count++;
			}
		}

		assertEquals(duplicates, count, "Iterator must return all duplicates, across multiple LMDB blocks");
	}
}
