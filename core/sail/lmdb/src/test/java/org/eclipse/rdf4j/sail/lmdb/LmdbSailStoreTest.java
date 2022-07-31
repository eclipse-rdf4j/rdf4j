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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Extended test for {@link LmdbStore}.
 */
public class LmdbSailStoreTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	protected Repository repo;

	protected final ValueFactory F = SimpleValueFactory.getInstance();

	protected final IRI CTX_1 = F.createIRI("urn:one");
	protected final IRI CTX_2 = F.createIRI("urn:two");
	protected final IRI CTX_INV = F.createIRI("urn:invalid");

	protected final Statement S0 = F.createStatement(F.createIRI("http://example.org/0"), RDFS.LABEL,
			F.createLiteral("zero"));
	protected final Statement S1 = F.createStatement(F.createIRI("http://example.org/1"), RDFS.LABEL,
			F.createLiteral("one"));
	protected final Statement S2 = F.createStatement(F.createIRI("http://example.org/2"), RDFS.LABEL,
			F.createLiteral("two"));

	@Before
	public void before() throws Exception {
		File dataDir = tempFolder.newFolder("dbmodel");
		repo = new SailRepository(new LmdbStore(dataDir, new LmdbStoreConfig("spoc,posc")));
		repo.init();

		try (RepositoryConnection conn = repo.getConnection()) {
			conn.add(S0);
			conn.add(S1, CTX_1);
			conn.add(S2, CTX_2);
		}
	}

	@Test
	public void testRemoveValidContext() {
		try (RepositoryConnection conn = repo.getConnection()) {
			conn.remove((IRI) null, null, null, CTX_1);
		}
		try (RepositoryConnection conn = repo.getConnection()) {
			assertTrue("Statement 0 incorrectly removed", conn.hasStatement(S0, false));
			assertFalse("Statement 1 still not removed", conn.hasStatement(S1, false, CTX_1));
			assertTrue("Statement 2 incorrectly removed", conn.hasStatement(S2, false, CTX_2));
		}
	}

	@Test
	public void testRemoveEmptyContext() {
		try (RepositoryConnection conn = repo.getConnection()) {
			conn.remove((IRI) null, null, null, (Resource) null);
		}
		try (RepositoryConnection conn = repo.getConnection()) {
			assertFalse("Statement 0 still not removed", conn.hasStatement(S0, false));
			assertTrue("Statement 1 incorrectly removed", conn.hasStatement(S1, false, CTX_1));
			assertTrue("Statement 2 incorrectly removed", conn.hasStatement(S2, false, CTX_2));
		}
	}

	@Test
	public void testRemoveInvalidContext() {
		try (RepositoryConnection conn = repo.getConnection()) {
			conn.remove((IRI) null, null, null, CTX_INV);
		}
		try (RepositoryConnection conn = repo.getConnection()) {
			assertTrue("Statement 0 incorrectly removed", conn.hasStatement(S0, false));
			assertTrue("Statement 1 incorrectly removed", conn.hasStatement(S1, false, CTX_1));
			assertTrue("Statement 2 incorrectly removed", conn.hasStatement(S2, false, CTX_2));
		}
	}

	@Test
	public void testRemoveMultipleValidContext() {
		try (RepositoryConnection conn = repo.getConnection()) {
			conn.remove((IRI) null, null, null, CTX_1, CTX_2);
		}
		try (RepositoryConnection conn = repo.getConnection()) {
			assertTrue("Statement 0 incorrectly removed", conn.hasStatement(S0, false));
			assertFalse("Statement 1 still not removed", conn.hasStatement(S1, false, CTX_1));
			assertFalse("Statement 2 still not removed", conn.hasStatement(S2, false, CTX_2));
		}
	}

	@Test
	public void testClearMultipleValidContext() {
		try (RepositoryConnection conn = repo.getConnection()) {
			conn.clear(CTX_1, CTX_2);
		}
		try (RepositoryConnection conn = repo.getConnection()) {
			assertTrue("Statement 0 incorrectly removed", conn.hasStatement(S0, false));
			assertFalse("Statement 1 still not removed", conn.hasStatement(S1, false, CTX_1));
			assertFalse("Statement 2 still not removed", conn.hasStatement(S2, false, CTX_2));
		}
	}

	@Test
	public void testPassConnectionBetweenThreads() throws InterruptedException {
		RepositoryConnection[] conn = { null };
		TupleQuery[] query = { null };
		TupleQueryResult[] result = { null };

		try {
			Thread t = new Thread(() -> {
				conn[0] = repo.getConnection();
			});
			t.start();
			t.join();

			t = new Thread(() -> {
				query[0] = conn[0].prepareTupleQuery("select * { ?s ?p ?o } ");
			});
			t.start();
			t.join();

			t = new Thread(() -> {
				result[0] = query[0].evaluate();
			});
			t.start();
			t.join();

			assertEquals(3, result[0].stream().count());
		} finally {
			conn[0].close();
		}
	}

	@Test
	public void testPassConnectionBetweenThreadsWithTx() throws InterruptedException {
		RepositoryConnection[] conn = { null };
		TupleQuery[] query = { null };
		TupleQueryResult[] result = { null };

		try {
			Thread t = new Thread(() -> {
				conn[0] = repo.getConnection();
				conn[0].setIsolationLevel(IsolationLevels.READ_UNCOMMITTED);
				conn[0].begin();
			});
			t.start();
			t.join();

			t = new Thread(() -> {
				query[0] = conn[0].prepareTupleQuery("select * { ?s ?p ?o } ");
			});
			t.start();
			t.join();

			t = new Thread(() -> {
				result[0] = query[0].evaluate();
			});
			t.start();
			t.join();

			if (result[0].hasNext()) {
				conn[0].commit();
			}
			assertEquals(3, result[0].stream().count());
		} finally {
			conn[0].close();
		}
	}

	@After
	public void after() throws Exception {
		repo.shutDown();
	}
}
