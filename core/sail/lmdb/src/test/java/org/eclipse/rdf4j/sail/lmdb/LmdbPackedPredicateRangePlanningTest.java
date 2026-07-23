/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.explanation.Explanation;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.eclipse.rdf4j.sail.lmdb.sketch.SketchBasedJoinEstimator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * End-to-end activation tests for packed predicate-object range guarantees: stored ranges must drive packed planning
 * (contradiction, tautology, and anchor alternatives) with observable proof output, never silently.
 */
class LmdbPackedPredicateRangePlanningTest {

	private static final SimpleValueFactory VF = SimpleValueFactory.getInstance();

	private static final IRI IRI_PREDICATE = VF.createIRI("http://example.com/iriValued");
	private static final IRI INT_PREDICATE = VF.createIRI("http://example.com/intValued");
	private static final IRI LEFT_PREDICATE = VF.createIRI("http://example.com/leftInt");
	private static final IRI RIGHT_PREDICATE = VF.createIRI("http://example.com/rightInt");

	@Test
	void iriOnlyRangeWithIsLiteralFilterPlansEmptySet(@TempDir File dataDir) throws Exception {
		String query = """
				SELECT ?s WHERE {
				  ?s <http://example.com/iriValued> ?o .
				  FILTER(isLiteral(?o))
				}
				""";
		SailRepository repository = repository(dataDir);
		try {
			try (RepositoryConnection connection = repository.getConnection()) {
				connection.add(VF.createIRI("http://example.com/s1"), IRI_PREDICATE,
						VF.createIRI("http://example.com/o1"));
			}
			makeLmdbOptimizerReady(repository);

			String plan = explainOptimized(repository, query);
			assertTrue(plan.contains("EmptySet"),
					"An IRI-only stored range contradicts isLiteral; the packed planner must select EmptySet\n"
							+ plan);
			assertEquals(0, countResults(repository, query));
		} finally {
			repository.shutDown();
		}
	}

	@Test
	void iriOnlyRangeWithIsIriFilterKeepsResultsAndProof(@TempDir File dataDir) throws Exception {
		String query = """
				SELECT ?s WHERE {
				  ?s <http://example.com/iriValued> ?o .
				  FILTER(isIRI(?o))
				}
				""";
		SailRepository repository = repository(dataDir);
		try {
			try (RepositoryConnection connection = repository.getConnection()) {
				connection.add(VF.createIRI("http://example.com/s1"), IRI_PREDICATE,
						VF.createIRI("http://example.com/o1"));
			}
			makeLmdbOptimizerReady(repository);

			String plan = explainOptimized(repository, query);
			assertTrue(plan.contains("optimizer.objectGuarantee=RdfTermDomain[IRI]"), plan);
			assertFalse(plan.contains("EmptySet"), plan);
			assertEquals(1, countResults(repository, query));
		} finally {
			repository.shutDown();
		}
	}

	@Test
	void kindMismatchedEqualityPlansEmptySet(@TempDir File dataDir) throws Exception {
		String query = """
				SELECT ?s WHERE {
				  ?s <http://example.com/intValued> ?o .
				  FILTER(?o = <http://example.com/nonValue>)
				}
				""";
		SailRepository repository = repository(dataDir);
		try {
			try (RepositoryConnection connection = repository.getConnection()) {
				connection.add(VF.createIRI("http://example.com/s1"), INT_PREDICATE, VF.createLiteral("7", XSD.INT));
			}
			makeLmdbOptimizerReady(repository);

			String plan = explainOptimized(repository, query);
			assertTrue(plan.contains("EmptySet"),
					"A literal-only stored range can never equal an IRI constant; expected EmptySet\n" + plan);
			assertEquals(0, countResults(repository, query));
		} finally {
			repository.shutDown();
		}
	}

	@Test
	void outOfBoundsIntegerEqualityPlansEmptySet(@TempDir File dataDir) throws Exception {
		String query = """
				SELECT ?s WHERE {
				  ?s <http://example.com/intValued> ?o .
				  FILTER(?o = 9)
				}
				""";
		SailRepository repository = repository(dataDir);
		try {
			try (RepositoryConnection connection = repository.getConnection()) {
				connection.add(VF.createIRI("http://example.com/s1"), INT_PREDICATE, VF.createLiteral("7", XSD.INT));
			}
			makeLmdbOptimizerReady(repository);

			String plan = explainOptimized(repository, query);
			assertTrue(plan.contains("EmptySet"),
					"A canonical-integer range of [7, 7] can never equal 9; expected EmptySet\n" + plan);
			assertEquals(0, countResults(repository, query));
		} finally {
			repository.shutDown();
		}
	}

	@Test
	void unusedKnownRangeReportsStableInapplicabilityReason(@TempDir File dataDir) throws Exception {
		String query = """
				SELECT ?s ?o WHERE {
				  ?s <http://example.com/intValued> ?o .
				}
				""";
		SailRepository repository = repository(dataDir);
		try {
			try (RepositoryConnection connection = repository.getConnection()) {
				connection.add(VF.createIRI("http://example.com/s1"), INT_PREDICATE, VF.createLiteral("7", XSD.INT));
			}
			makeLmdbOptimizerReady(repository);

			String plan = explainOptimized(repository, query);
			assertTrue(plan.contains("optimizer.objectGuarantee="), plan);
			assertTrue(plan.contains("optimizer.guaranteeOptions=generated=0"),
					"A visible unused guarantee must not stay silent\n" + plan);
			assertTrue(plan.contains("optimizer.guaranteeOptionReason="), plan);
			assertEquals(1, countResults(repository, query));
		} finally {
			repository.shutDown();
		}
	}

	@Test
	void sharedVariableJoinIntersectsDomainsToContradiction(@TempDir File dataDir) throws Exception {
		String query = """
				SELECT ?a ?b WHERE {
				  ?a <http://example.com/iriValued> ?o .
				  ?b <http://example.com/intValued> ?o .
				  FILTER(?o = 7)
				}
				""";
		SailRepository repository = repository(dataDir);
		try {
			try (RepositoryConnection connection = repository.getConnection()) {
				connection.add(VF.createIRI("http://example.com/s1"), IRI_PREDICATE,
						VF.createIRI("http://example.com/o1"));
				connection.add(VF.createIRI("http://example.com/s2"), INT_PREDICATE,
						VF.createLiteral("7", XSD.INT));
			}
			makeLmdbOptimizerReady(repository);

			String plan = explainOptimized(repository, query);
			assertTrue(plan.contains("EmptySet"),
					"IRI-only and literal-only ranges for the shared variable intersect to an empty domain\n"
							+ plan);
			assertEquals(0, countResults(repository, query));
		} finally {
			repository.shutDown();
		}
	}

	@Test
	void unionDomainsWidenWithoutOverclaiming(@TempDir File dataDir) throws Exception {
		// The BIND branch carries no range fact for ?o, so the union must drop the left branch's [3, 3] fact
		// rather than let it wrongly contradict the FILTER and erase the BIND row.
		String unprovenBranchQuery = """
				SELECT ?s WHERE {
				  {
				    ?s <http://example.com/leftInt> ?o .
				  } UNION {
				    ?s <http://example.com/iriValued> ?ignore .
				    BIND(9 AS ?o)
				  }
				  FILTER(?o = 9)
				}
				""";
		String bothBranchesQuery = """
				SELECT ?s WHERE {
				  {
				    ?s <http://example.com/leftInt> ?o .
				  } UNION {
				    ?s <http://example.com/rightInt> ?o .
				  }
				  FILTER(?o = 9)
				}
				""";
		SailRepository repository = repository(dataDir);
		try {
			try (RepositoryConnection connection = repository.getConnection()) {
				connection.add(VF.createIRI("http://example.com/s1"), LEFT_PREDICATE, VF.createLiteral("3", XSD.INT));
				connection.add(VF.createIRI("http://example.com/s2"), RIGHT_PREDICATE,
						VF.createLiteral("5", XSD.INT));
				connection.add(VF.createIRI("http://example.com/s3"), IRI_PREDICATE,
						VF.createIRI("http://example.com/o3"));
			}
			makeLmdbOptimizerReady(repository);

			assertEquals(1, countResults(repository, unprovenBranchQuery),
					"The union must not apply the proven left-branch range to the unproven BIND branch");

			String contradictionPlan = explainOptimized(repository, bothBranchesQuery);
			assertTrue(contradictionPlan.contains("EmptySet"),
					"9 lies outside both branch ranges; expected a contradiction plan\n" + contradictionPlan);
			assertEquals(0, countResults(repository, bothBranchesQuery));
		} finally {
			repository.shutDown();
		}
	}

	@Test
	void insertBroadeningTheRangeInvalidatesContradictionPlans(@TempDir File dataDir) throws Exception {
		String query = """
				SELECT ?s WHERE {
				  ?s <http://example.com/intValued> ?o .
				  FILTER(?o = 9)
				}
				""";
		SailRepository repository = repository(dataDir);
		try {
			try (RepositoryConnection connection = repository.getConnection()) {
				connection.add(VF.createIRI("http://example.com/s1"), INT_PREDICATE, VF.createLiteral("7", XSD.INT));
			}
			makeLmdbOptimizerReady(repository);
			assertTrue(explainOptimized(repository, query).contains("EmptySet"));
			assertEquals(0, countResults(repository, query));

			try (RepositoryConnection connection = repository.getConnection()) {
				connection.add(VF.createIRI("http://example.com/s2"), INT_PREDICATE, VF.createLiteral("9", XSD.INT));
			}
			makeLmdbOptimizerReady(repository);

			String broadenedPlan = explainOptimized(repository, query);
			assertFalse(broadenedPlan.contains("EmptySet"),
					"After the insert broadens the range to [7, 9], the cached contradiction plan must not be reused\n"
							+ broadenedPlan);
			assertEquals(1, countResults(repository, query));
		} finally {
			repository.shutDown();
		}
	}

	private static SailRepository repository(File dataDir) {
		LmdbStoreConfig config = new LmdbStoreConfig("spoc,posc")
				.setOptimizerSamplingEnabled(false)
				.setBackgroundRawSamplingMaxMillisPerCycle(0L);
		SailRepository repository = new SailRepository(new LmdbStore(dataDir, config));
		repository.init();
		return repository;
	}

	private static void makeLmdbOptimizerReady(SailRepository repository) throws InterruptedException {
		LmdbStore sail = (LmdbStore) repository.getSail();
		SketchBasedJoinEstimator estimator = sail.getBackingStore().getSketchBasedJoinEstimator();
		estimator.stop();
		estimator.rebuild();
		LmdbPlannerAwait.awaitEstimatorReady(estimator);
		LmdbPlannerAwait.awaitSketchesReady(sail);
	}

	private static String explainOptimized(SailRepository repository, String query) {
		try (RepositoryConnection connection = repository.getConnection()) {
			Explanation explanation = connection.prepareTupleQuery(query).explain(Explanation.Level.Optimized);
			return explanation.toString();
		}
	}

	private static int countResults(SailRepository repository, String query) {
		try (RepositoryConnection connection = repository.getConnection();
				TupleQueryResult result = connection.prepareTupleQuery(query).evaluate()) {
			int count = 0;
			while (result.hasNext()) {
				result.next();
				count++;
			}
			return count;
		}
	}
}
