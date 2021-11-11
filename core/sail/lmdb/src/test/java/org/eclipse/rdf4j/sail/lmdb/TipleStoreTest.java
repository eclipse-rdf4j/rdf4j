/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Low-level tests for {@link TripleStore}.
 */
public class TipleStoreTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	protected TripleStore tripleStore;

	@Before
	public void before() throws Exception {
		File dataDir = tempFolder.newFolder("triplestore");
		tripleStore = new TripleStore(dataDir, "spoc,posc");
	}

	int count(RecordIterator it) throws IOException {
		int count = 0;
		while (it.next() != null) {
			count++;
		}
		return count;
	}

	@Test
	public void testInferredStmts() throws Exception {
		tripleStore.startTransaction();
		tripleStore.storeTriple(1, 2, 3, 1, false);
		tripleStore.commit();

		assertEquals("Store should have 1 inferred statement", 1,
				count(tripleStore.getTriples(1, 2, 3, 1, false, false)));

		assertEquals("Store should have 0 explicit statements", 0,
				count(tripleStore.getTriples(1, 2, 3, 1, true, true)));

		tripleStore.startTransaction();
		tripleStore.storeTriple(1, 2, 3, 1, true);
		tripleStore.commit();

		assertEquals("Store should have 0 inferred statements", 0,
				count(tripleStore.getTriples(1, 2, 3, 1, false, false)));

		assertEquals("Store should have 1 explicit statements", 1,
				count(tripleStore.getTriples(1, 2, 3, 1, true, true)));
	}

	@After
	public void after() throws Exception {
		tripleStore.close();
	}
}
