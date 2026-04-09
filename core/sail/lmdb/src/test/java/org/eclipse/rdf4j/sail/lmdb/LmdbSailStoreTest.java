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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
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
import org.eclipse.rdf4j.sail.lmdb.benchmark.FoafCliqueQueryCatalog;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
	public void testExplainOptimizedDoesNotUseLftjWithDefaultIndexes(@TempDir File dataDir) {
		Repository repository = createRepository(dataDir, new LmdbStoreConfig("spoc,posc"), conn -> {
		});

		try (RepositoryConnection conn = repository.getConnection()) {
			String actual = conn.prepareTupleQuery(cyclicQuery())
					.explain(Explanation.Level.Optimized)
					.toString();

			assertFalse(actual, actual.contains("LmdbLftjTupleExpr"));
		} finally {
			repository.shutDown();
		}
	}

	@Test
	public void testExplainOptimizedDoesNotUseLftjForAcyclicQueryWithStrongIndexes(@TempDir File dataDir) {
		LmdbStoreConfig config = new LmdbStoreConfig("spoc,sopc,psoc,posc,ospc,opsc");
		Repository repository = createRepository(dataDir, config, conn -> {
		});

		try (RepositoryConnection conn = repository.getConnection()) {
			String actual = conn.prepareTupleQuery(chainQuery())
					.explain(Explanation.Level.Optimized)
					.toString();

			assertFalse(actual, actual.contains("LmdbLftjTupleExpr"));
		} finally {
			repository.shutDown();
		}
	}

	@Test
	public void testExplainOptimizedDoesNotUseLftjWithoutFullStrongIndexCoverage(@TempDir File dataDir) {
		LmdbStoreConfig config = new LmdbStoreConfig("spoc,sopc,psoc,posc,ospc");
		Repository repository = createRepository(dataDir, config, conn -> {
		});

		try (RepositoryConnection conn = repository.getConnection()) {
			String actual = conn.prepareTupleQuery(cyclicQuery())
					.explain(Explanation.Level.Optimized)
					.toString();

			assertFalse(actual, actual.contains("LmdbLftjTupleExpr"));
		} finally {
			repository.shutDown();
		}
	}

	@Test
	public void testExplainOptimizedUsesLftjForCyclicQueryWithStrongIndexes(@TempDir File dataDir) {
		LmdbStoreConfig config = new LmdbStoreConfig("spoc,sopc,psoc,posc,ospc,opsc");
		Repository repository = createRepository(dataDir, config, conn -> {
		});

		try (RepositoryConnection enabledConnection = repository.getConnection()) {
			String actualPlan = enabledConnection.prepareTupleQuery(cyclicQuery())
					.explain(Explanation.Level.Optimized)
					.toString();
			assertTrue(actualPlan, actualPlan.contains("LmdbLftjTupleExpr"));
		} finally {
			repository.shutDown();
		}
	}

	@Test
	public void testExplainOptimizedUsesStableCycleIndexOrder(@TempDir File dataDir) {
		LmdbStoreConfig config = new LmdbStoreConfig("spoc,sopc,psoc,posc,ospc,opsc");
		Repository repository = createRepository(dataDir, config, conn -> {
		});

		try (RepositoryConnection connection = repository.getConnection()) {
			String actualPlan = connection.prepareTupleQuery(stableCycleQuery())
					.explain(Explanation.Level.Optimized)
					.toString();
			assertTrue(actualPlan, actualPlan.contains("LmdbLftjTupleExpr"));
			assertTrue(actualPlan, actualPlan.contains("varOrder=a,b,c"));
			assertTrue(actualPlan, actualPlan.contains("indexes=psoc,psoc,posc"));
		} finally {
			repository.shutDown();
		}
	}

	@Test
	public void testExplainOptimizedShowsMixedCycleInequalitiesAndValuesAwareOrder(@TempDir File dataDir) {
		LmdbStoreConfig config = new LmdbStoreConfig("spoc,sopc,psoc,posc,ospc,opsc");
		Repository repository = createRepository(dataDir, config, conn -> {
		});

		try (RepositoryConnection connection = repository.getConnection()) {
			String actualPlan = connection.prepareTupleQuery(
					FoafCliqueQueryCatalog.QueryScenario.CYCLE5_VALUES_DISTINCT_MAILBOX_ORDERED.query())
					.explain(Explanation.Level.Optimized)
					.toString();
			assertTrue(actualPlan, actualPlan.contains("LmdbLftjTupleExpr"));
			assertTrue(actualPlan, actualPlan.contains("varOrder=city,a,"));
			assertTrue(actualPlan, actualPlan.contains(
					"inequalities=[a!=b,a!=c,a!=d,a!=e,b!=c,b!=d,b!=e,c!=d,c!=e,d!=e]"));
		} finally {
			repository.shutDown();
		}
	}

	@Test
	public void testCyclicQueryMatchesResultsWhenLftjActivates(@TempDir File disabledDir, @TempDir File enabledDir) {
		LmdbStoreConfig disabled = new LmdbStoreConfig("spoc,posc");
		LmdbStoreConfig enabled = new LmdbStoreConfig("spoc,sopc,psoc,posc,ospc,opsc");

		Repository disabledRepository = createRepository(disabledDir, disabled, this::seedCyclicData);
		Repository enabledRepository = createRepository(enabledDir, enabled, this::seedCyclicData);
		String query = cyclicQuery();

		try (RepositoryConnection enabledConnection = enabledRepository.getConnection()) {
			String actualPlan = enabledConnection.prepareTupleQuery(query)
					.explain(Explanation.Level.Optimized)
					.toString();
			assertTrue(actualPlan, actualPlan.contains("LmdbLftjTupleExpr"));
		}

		try {
			assertEquals(evaluate(disabledRepository, query), evaluate(enabledRepository, query));
		} finally {
			disabledRepository.shutDown();
			enabledRepository.shutDown();
		}
	}

	@Test
	public void testRejectsCustomEvaluationStrategyFactoryWhenLftjEnabled() {
		LmdbStoreConfig config = new LmdbStoreConfig();
		LmdbStore store = new LmdbStore(config);
		store.setEvaluationStrategyFactory(
				new org.eclipse.rdf4j.query.algebra.evaluation.impl.StrictEvaluationStrategyFactory());

		Assertions.assertThrows(IllegalStateException.class, store::getEvaluationStrategyFactory);
	}

	private Repository createRepository(File dataDir, LmdbStoreConfig config, Consumer<RepositoryConnection> seed) {
		Repository repository = new SailRepository(new LmdbStore(dataDir, config));
		repository.init();
		try (RepositoryConnection connection = repository.getConnection()) {
			seed.accept(connection);
		}
		return repository;
	}

	private String chainQuery() {
		return """
				SELECT * WHERE {
				  ?a <urn:p1> ?b .
				  ?b <urn:p2> ?c .
				  ?c <urn:p3> ?d .
				}
				""";
	}

	private String cyclicQuery() {
		return """
				SELECT ?a ?b ?c WHERE {
				  ?a <urn:p1> ?b .
				  ?b <urn:p2> ?c .
				  ?c <urn:p3> ?a .
				}
				""";
	}

	private String stableCycleQuery() {
		return """
				SELECT ?a ?b ?c WHERE {
				  ?a <urn:knows> ?b .
				  ?b <urn:knows> ?c .
				  ?c <urn:knows> ?a .
				}
				""";
	}

	private void seedCyclicData(RepositoryConnection connection) {
		IRI a1 = F.createIRI("urn:a1");
		IRI a2 = F.createIRI("urn:a2");
		IRI b1 = F.createIRI("urn:b1");
		IRI b2 = F.createIRI("urn:b2");
		IRI c1 = F.createIRI("urn:c1");
		IRI c2 = F.createIRI("urn:c2");

		connection.add(a1, F.createIRI("urn:p1"), b1);
		connection.add(a2, F.createIRI("urn:p1"), b2);
		connection.add(a1, F.createIRI("urn:p1"), b2);
		connection.add(b1, F.createIRI("urn:p2"), c1);
		connection.add(b2, F.createIRI("urn:p2"), c2);
		connection.add(c1, F.createIRI("urn:p3"), a1);
		connection.add(c2, F.createIRI("urn:p3"), a2);
	}

	private List<String> evaluate(Repository repository, String query) {
		List<String> rows = new ArrayList<>();
		try (RepositoryConnection connection = repository.getConnection();
				TupleQueryResult result = connection.prepareTupleQuery(query).evaluate()) {
			while (result.hasNext()) {
				var bindingSet = result.next();
				rows.add(bindingSet.getValue("a").stringValue() + "|" + bindingSet.getValue("b").stringValue() + "|"
						+ bindingSet.getValue("c").stringValue());
			}
		}
		rows.sort(String::compareTo);
		return rows;
	}

	@AfterEach
	public void after() {
		repo.shutDown();
	}
}
