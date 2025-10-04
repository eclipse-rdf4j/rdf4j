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

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.base.SailDataset;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Extended test for {@link LmdbStore}.
 */
public class LmdbSailStoreTest {

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

	@BeforeEach
	public void before(@TempDir File dataDir) {
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
	public void testSizeNullContextCountsDefaultGraphOnly() {
		try (RepositoryConnection conn = repo.getConnection()) {
			assertEquals("size(null) must count default graph only", 1, conn.size((Resource) null));
		}
	}

	@Test
	public void testSizeUnknownContextIsZero() {
		try (RepositoryConnection conn = repo.getConnection()) {
			assertEquals("size(unknownCtx) must be zero", 0, conn.size(CTX_INV));
		}
	}

	@Test
	public void testSizeMixedValidAndUnknownSkipsUnknown() {
		try (RepositoryConnection conn = repo.getConnection()) {
			assertEquals("size(valid,unknown) must equal size(valid)", 1, conn.size(CTX_1, CTX_INV));
		}
	}

	@Test
	public void testSizeNullAndValidCountsUnion() {
		try (RepositoryConnection conn = repo.getConnection()) {
			assertEquals("size(null,valid) must count default + valid", 2, conn.size((Resource) null, CTX_1));
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

	@ParameterizedTest
	@EnumSource(IsolationLevels.class)
	public void testSizeIsolationLevels(final IsolationLevels isolationLevel) {
		try (final RepositoryConnection conn1 = repo.getConnection();
				final RepositoryConnection conn2 = repo.getConnection()) {
			final int baseSize = 3; // S0, S1, S2
			Assertions.assertEquals(baseSize, conn1.size(), "Size should be " + baseSize);
			Assertions.assertEquals(baseSize, conn2.size(), "Size should be " + baseSize);
			final int count = 100;
			conn1.begin(isolationLevel);
			conn2.begin(isolationLevel);
			for (int i = 0; i < count; i++) {
				conn1.add(F.createStatement(F.createIRI("http://example.org/" + i), RDFS.LABEL,
						F.createLiteral("label" + i)));
			}
			// conn1 should see its own changes
			Assertions.assertEquals(baseSize + count, conn1.size(), "Size should be " + (3 + count));

			// LMDBStore supports: NONE, READ_COMMITTED, SNAPSHOT_READ, SNAPSHOT, and SERIALIZABLE.
			// If an unsupported level (e.g., READ_UNCOMMITTED) is requested,
			// a stronger supported level (e.g., READ_COMMITTED) is used instead.
			if (isolationLevel.equals(IsolationLevels.NONE)) {
				// conn2 should see the changes of conn1
				Assertions.assertEquals(baseSize + count, conn2.size(), "Size should be " + (3 + count));
			} else if (isolationLevel.equals(IsolationLevels.READ_UNCOMMITTED)) {
				// Use a stronger level (READ_COMMITTED) instead of READ_UNCOMMITTED
				Assertions.assertEquals(baseSize, conn2.size(), "Size should be " + (3 + count));
			} else if (isolationLevel.equals(IsolationLevels.READ_COMMITTED)) {
				// conn2 should not see the changes of conn1
				Assertions.assertEquals(baseSize, conn2.size(), "Size should be " + baseSize);
			} else if (isolationLevel.equals(IsolationLevels.SNAPSHOT_READ)) {
				// conn2 should not see the changes of conn1
				Assertions.assertEquals(baseSize, conn2.size(), "Size should be " + baseSize);
			} else if (isolationLevel.equals(IsolationLevels.SNAPSHOT)) {
				// conn2 should not see the changes of conn1
				Assertions.assertEquals(baseSize, conn2.size(), "Size should be " + baseSize);
			} else if (isolationLevel.equals(IsolationLevels.SERIALIZABLE)) {
				// conn2 should not see the changes of conn1
				Assertions.assertEquals(baseSize, conn2.size(), "Size should be " + baseSize);
			} else {
				Assertions.fail("Unsupported isolation level: " + isolationLevel);
			}
			conn1.commit();
			// conn2 should see the changes of conn1 after commit
			if (isolationLevel.equals(IsolationLevels.READ_COMMITTED)
					|| isolationLevel.equals(IsolationLevels.READ_UNCOMMITTED)
					|| isolationLevel.equals(IsolationLevels.SNAPSHOT_READ)) {
				Assertions.assertEquals(baseSize + count, conn2.size(), "Size should be " + (3 + count));
			}
			conn2.commit();
			Assertions.assertEquals(baseSize + count, conn2.size(), "Size should be " + (3 + count));
		}
	}

	@ParameterizedTest
	@EnumSource(value = IsolationLevels.class, names = "NONE", mode = EnumSource.Mode.EXCLUDE)
	public void testSizeWhenRollbackTxn(final IsolationLevels isolationLevel) {
		try (RepositoryConnection conn1 = repo.getConnection();
				RepositoryConnection conn2 = repo.getConnection()) {

			final int baseSize = 3; // S0, S1, S2
			Assertions.assertEquals(baseSize, conn1.size(), "Initial size in conn1 should be " + baseSize);
			Assertions.assertEquals(baseSize, conn2.size(), "Initial size in conn2 should be " + baseSize);

			final int count = 50;

			conn1.begin(isolationLevel);
			conn2.begin(isolationLevel);

			for (int i = 0; i < count; i++) {
				conn1.add(F.createStatement(F.createIRI("http://example.org/rollback/" + i), RDFS.LABEL,
						F.createLiteral("rollback" + i)));
			}

			// conn1 sees its uncommitted changes
			Assertions.assertEquals(baseSize + count, conn1.size(), "conn1 should see uncommitted additions");

			// conn2 should NOT see uncommitted changes
			Assertions.assertEquals(baseSize, conn2.size(), "conn2 should not see uncommitted changes");

			conn1.rollback();

			// After rollback, both connections should see base size
			Assertions.assertEquals(baseSize, conn1.size(), "conn1 should not see rolled-back additions");
			Assertions.assertEquals(baseSize, conn2.size(), "conn2 should not see rolled-back additions");

			conn2.commit();
		}
	}

	@Test
	public void testInferredSourceHasEmptyIterationWithoutInferredStatements() throws SailException {
		LmdbStore sail = (LmdbStore) ((SailRepository) repo).getSail();
		LmdbSailStore backingStore = sail.getBackingStore();

		try (SailDataset dataset = backingStore.getInferredSailSource().dataset(IsolationLevels.NONE);
				CloseableIteration<? extends Statement> iteration = dataset.getStatements(null, null, null)) {
			assertTrue(iteration instanceof EmptyIteration);
			assertFalse(iteration.hasNext());
		}
	}

	@AfterEach
	public void after() {
		repo.shutDown();
	}
}
