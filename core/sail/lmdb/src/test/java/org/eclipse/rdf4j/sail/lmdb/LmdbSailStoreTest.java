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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.explanation.Explanation;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.base.SailDataset;
import org.eclipse.rdf4j.sail.base.SailSink;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Extended test for {@link LmdbStore}.
 */
public class LmdbSailStoreTest {

	protected Repository repo;
	private File dataDir;

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
		this.dataDir = dataDir;
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

	@Test
	public void testExplainExecutedShowsIndexName() {
		try (RepositoryConnection conn = repo.getConnection()) {
			String actual = conn.prepareTupleQuery("select * { ?s <" + RDFS.LABEL + "> ?o }")
					.explain(Explanation.Level.Executed)
					.toString();

			assertTrue(actual, actual.contains("indexName="));
		}
	}

	@Test
	public void testExplainExecutedHidesEstimateStabilityStats() {
		IRI a1 = F.createIRI("urn:a1");
		IRI a2 = F.createIRI("urn:a2");
		IRI c1 = F.createIRI("urn:c1");
		IRI c2 = F.createIRI("urn:c2");
		IRI b = F.createIRI("urn:b");
		IRI d = F.createIRI("urn:d");
		IRI f1 = F.createIRI("urn:f1");
		IRI f2 = F.createIRI("urn:f2");

		try (RepositoryConnection conn = repo.getConnection()) {
			for (int i = 0; i < 10000; i++) {
				BNode c = F.createBNode();
				BNode f = F.createBNode();
				conn.add(a1, b, c);
				conn.add(c, b, f);
				conn.add(f, d, f1);
				conn.add(F.createBNode(), b, F.createBNode());
			}

			String actual = conn.prepareTupleQuery(
					"select ?b where { ?a ?b ?c. ?c ?d ?f. }")
					.explain(Explanation.Level.Executed)
					.toString();

			assertFalse(actual, actual.contains("sampleCountActual="));
			assertFalse(actual, actual.contains("varianceActual="));
			assertFalse(actual, actual.contains("stddevActual="));
			assertFalse(actual, actual.contains("confidenceScoreActual="));
		}
	}

	@Test
	void approveAllPropagatesPredicateStoreFailureAsSailException() throws Exception {
		LmdbStore sail = (LmdbStore) ((SailRepository) repo).getSail();
		LmdbSailStore backingStore = sail.getBackingStore();
		Field valueStoreField = LmdbSailStore.class.getDeclaredField("valueStore");
		valueStoreField.setAccessible(true);
		ValueStore originalValueStore = (ValueStore) valueStoreField.get(backingStore);
		ValueStore valueStoreSpy = spy(originalValueStore);
		IRI failingPredicate = F.createIRI("urn:failing-predicate");
		SailSink sink = backingStore.getExplicitSailSource().sink(IsolationLevels.NONE);

		doAnswer(invocation -> {
			Value value = invocation.getArgument(0);
			if (failingPredicate.equals(value)) {
				throw new IOException("expected predicate failure");
			}
			return invocation.callRealMethod();
		}).when(valueStoreSpy).storeValue(any(Value.class));

		valueStoreField.set(backingStore, valueStoreSpy);
		try {
			SailException exception = assertThrows(SailException.class,
					() -> sink.approveAll(Set.of(F.createStatement(F.createIRI("urn:subject"), failingPredicate,
							F.createLiteral("object"))), Set.of()));

			assertTrue(exception.getCause() instanceof IOException);
		} finally {
			valueStoreField.set(backingStore, originalValueStore);
			sink.close();
		}
	}

	@Test
	void approveAllBatchesTripleStoreWritesIntoBulkCalls() throws Exception {
		LmdbStore sail = (LmdbStore) ((SailRepository) repo).getSail();
		LmdbSailStore backingStore = sail.getBackingStore();
		backingStore.enableMultiThreading = false;

		Field tripleStoreField = LmdbSailStore.class.getDeclaredField("tripleStore");
		tripleStoreField.setAccessible(true);
		TripleStore originalTripleStore = (TripleStore) tripleStoreField.get(backingStore);
		TripleStore tripleStoreSpy = spy(originalTripleStore);

		Set<Statement> statements = new LinkedHashSet<>();
		for (int i = 0; i < 1025; i++) {
			statements.add(F.createStatement(
					F.createIRI("urn:subject:" + i),
					F.createIRI("urn:predicate:" + (i % 17)),
					F.createIRI("urn:object:" + (i % 29)),
					F.createIRI("urn:context:" + (i % 7))));
		}

		tripleStoreField.set(backingStore, tripleStoreSpy);
		try (SailSink sink = backingStore.getExplicitSailSource().sink(IsolationLevels.NONE)) {
			clearInvocations(tripleStoreSpy);
			sink.approveAll(statements, Set.of());
			sink.flush();

			verify(tripleStoreSpy, atMost(2)).storeTriple(anyLong(), anyLong(), anyLong(), anyLong(), anyBoolean());
		} finally {
			tripleStoreField.set(backingStore, originalTripleStore);
		}
	}

	@AfterEach
	public void after() {
		try {
			repo.shutDown();
		} finally {
			LmdbTestUtil.deleteDir(dataDir);
		}
	}
}
