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

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.Projection;
import org.eclipse.rdf4j.query.algebra.QueryRoot;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategyFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.explanation.Explanation;
import org.eclipse.rdf4j.query.explanation.GenericPlanNode;
import org.eclipse.rdf4j.query.explanation.TelemetryMetricNames;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.eclipse.rdf4j.query.parser.ParsedTupleQuery;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.base.SailDataset;
import org.eclipse.rdf4j.sail.base.SailDatasetTripleSource;
import org.eclipse.rdf4j.sail.base.SailSource;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for nested BGP joins using LMDB ID-only join iterators.
 */
public class LmdbIdBGPJoinTest {

	private static final String NS = "http://example.com/";

	@Test
	public void subjectStarUsesOneNaryIndexScan(@TempDir Path tempDir) throws Exception {
		LmdbStore store = newStore(tempDir, "spoc,posc");
		SailRepository repository = new SailRepository(store);
		repository.init();

		String query = "SELECT ?person ?city WHERE {\n" +
				"  ?person <" + NS + "type> <" + NS + "User> .\n" +
				"  ?person <" + NS + "status> <" + NS + "Active> .\n" +
				"  ?person <" + NS + "city> ?city .\n" +
				"}";

		try {
			try (RepositoryConnection conn = repository.getConnection()) {
				ValueFactory vf = SimpleValueFactory.getInstance();
				IRI alice = vf.createIRI(NS, "alice");
				IRI bob = vf.createIRI(NS, "bob");
				IRI type = vf.createIRI(NS, "type");
				IRI user = vf.createIRI(NS, "User");
				IRI status = vf.createIRI(NS, "status");
				IRI active = vf.createIRI(NS, "Active");
				IRI inactive = vf.createIRI(NS, "Inactive");
				IRI city = vf.createIRI(NS, "city");
				IRI oslo = vf.createIRI(NS, "Oslo");
				IRI bergen = vf.createIRI(NS, "Bergen");
				conn.add(alice, type, user);
				conn.add(alice, status, active);
				conn.add(alice, city, oslo);
				conn.add(bob, type, user);
				conn.add(bob, status, inactive);
				conn.add(bob, city, bergen);

				try (var result = conn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate()) {
					List<BindingSet> list = Iterations.asList(result);
					assertThat(list).hasSize(1);
					assertThat(list.get(0).getValue("city")).isEqualTo(oslo);
				}

				assertThat(optimizedAlgorithms(conn, query)).contains("LmdbIdNaryIndexScanIterator");
			}
		} finally {
			repository.shutDown();
		}
	}

	@Test
	public void objectAnchoredStarUsesOneNaryIndexScanWhenObjectLeadingIndexExists(@TempDir Path tempDir)
			throws Exception {
		LmdbStore store = newStore(tempDir, "opsc,spoc,posc");
		SailRepository repository = new SailRepository(store);
		repository.init();

		String query = "SELECT ?target ?a ?b WHERE {\n" +
				"  ?a <" + NS + "likes> ?target .\n" +
				"  ?b <" + NS + "mentions> ?target .\n" +
				"  <" + NS + "anchor> <" + NS + "pointsTo> ?target .\n" +
				"}";

		try {
			try (RepositoryConnection conn = repository.getConnection()) {
				ValueFactory vf = SimpleValueFactory.getInstance();
				IRI target = vf.createIRI(NS, "target");
				IRI other = vf.createIRI(NS, "other");
				IRI alice = vf.createIRI(NS, "alice");
				IRI post = vf.createIRI(NS, "post");
				IRI anchor = vf.createIRI(NS, "anchor");
				conn.add(alice, vf.createIRI(NS, "likes"), target);
				conn.add(post, vf.createIRI(NS, "mentions"), target);
				conn.add(anchor, vf.createIRI(NS, "pointsTo"), target);
				conn.add(vf.createIRI(NS, "noise"), vf.createIRI(NS, "likes"), other);
				conn.add(vf.createIRI(NS, "noise2"), vf.createIRI(NS, "mentions"), other);

				try (var result = conn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate()) {
					List<BindingSet> list = Iterations.asList(result);
					assertThat(list).hasSize(1);
					assertThat(list.get(0).getValue("target")).isEqualTo(target);
				}

				assertThat(optimizedAlgorithms(conn, query)).contains("LmdbIdNaryIndexScanIterator");
			}
		} finally {
			repository.shutDown();
		}
	}

	@Test
	public void nestedThreePatternBGP_usesIdJoinChain(@TempDir Path tempDir) throws Exception {
		LmdbStore store = LmdbTestUtil.newStoreWithLmdbEvaluationStrategy(tempDir.toFile());
		store.setDefaultIsolationLevel(IsolationLevels.READ_COMMITTED);
		SailRepository repository = new SailRepository(store);
		repository.init();

		try (RepositoryConnection conn = repository.getConnection()) {
			ValueFactory vf = SimpleValueFactory.getInstance();
			IRI alice = vf.createIRI(NS, "alice");
			IRI bob = vf.createIRI(NS, "bob");
			IRI carol = vf.createIRI(NS, "carol");
			IRI knows = vf.createIRI(NS, "knows");
			IRI likes = vf.createIRI(NS, "likes");
			IRI pizza = vf.createIRI(NS, "pizza");
			IRI pasta = vf.createIRI(NS, "pasta");

			// Data to satisfy a 3-pattern BGP: ?p knows ?x . ?p likes ?i . ?x likes ?i
			conn.add(alice, knows, bob);
			conn.add(alice, likes, pizza);
			conn.add(bob, likes, pizza);

			// Some extra data
			conn.add(carol, likes, pasta);
		}

		String query = "SELECT ?p ?i WHERE {\n" +
				"  ?p <" + NS + "knows> ?x .\n" +
				"  ?p <" + NS + "likes> ?i .\n" +
				"  ?x <" + NS + "likes> ?i .\n" +
				"}";

		try (RepositoryConnection conn = repository.getConnection()) {
			// Verify correctness
			try (var result = conn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate()) {
				List<BindingSet> list = Iterations.asList(result);
				assertThat(list).hasSize(1);
				assertThat(list.get(0).hasBinding("p")).isTrue();
				assertThat(list.get(0).hasBinding("i")).isTrue();
			}

			// Verify the top join algorithm is our LMDB ID join once implemented
			ParsedTupleQuery parsed = QueryParserUtil.parseTupleQuery(QueryLanguage.SPARQL, query, null);
			TupleExpr tupleExpr = parsed.getTupleExpr();
			SailRepositoryConnection sailRepoConn = (SailRepositoryConnection) conn;
			SailConnection sailConnection = sailRepoConn.getSailConnection();
			sailConnection.explain(Explanation.Level.Optimized, tupleExpr, null, EmptyBindingSet.getInstance(), true,
					0);

			TupleExpr unwrapped = unwrap(tupleExpr);
			assertThat(unwrapped).isInstanceOf(Join.class);
			Join topJoin = (Join) unwrapped;
			// Expect the top join to be marked with our ID join algorithm once nested support is implemented
			assertThat(topJoin.getAlgorithmName()).isEqualTo("LmdbIdJoinIterator");
		} finally {
			repository.shutDown();
		}
	}

	@Test
	public void twoPatternBGP_usesIdJoinChain(@TempDir Path tempDir) throws Exception {
		LmdbStore store = LmdbTestUtil.newStoreWithLmdbEvaluationStrategy(tempDir.toFile());
		store.setDefaultIsolationLevel(IsolationLevels.READ_COMMITTED);
		SailRepository repository = new SailRepository(store);
		repository.init();

		try (RepositoryConnection conn = repository.getConnection()) {
			ValueFactory vf = SimpleValueFactory.getInstance();
			IRI alice = vf.createIRI(NS, "alice");
			IRI bob = vf.createIRI(NS, "bob");
			IRI knows = vf.createIRI(NS, "knows");
			IRI likes = vf.createIRI(NS, "likes");
			IRI pizza = vf.createIRI(NS, "pizza");

			conn.add(alice, knows, bob);
			conn.add(alice, likes, pizza);
		}

		String query = "SELECT ?p ?i WHERE {\n" +
				"  ?p <" + NS + "knows> ?x .\n" +
				"  ?p <" + NS + "likes> ?i .\n" +
				"}";

		try (RepositoryConnection conn = repository.getConnection()) {
			try (var result = conn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate()) {
				List<BindingSet> list = Iterations.asList(result);
				assertThat(list).hasSize(1);
			}

			ParsedTupleQuery parsed = QueryParserUtil.parseTupleQuery(QueryLanguage.SPARQL, query, null);
			TupleExpr tupleExpr = parsed.getTupleExpr();
			SailRepositoryConnection sailRepoConn = (SailRepositoryConnection) conn;
			SailConnection sailConnection = sailRepoConn.getSailConnection();
			sailConnection.explain(Explanation.Level.Optimized, tupleExpr, null, EmptyBindingSet.getInstance(), true,
					0);

			TupleExpr unwrapped = unwrap(tupleExpr);
			assertThat(unwrapped).isInstanceOf(Join.class);
			Join topJoin = (Join) unwrapped;
			assertThat(topJoin.getAlgorithmName()).isEqualTo("LmdbIdJoinIterator");
		} finally {
			repository.shutDown();
		}
	}

	@Test
	public void queryPlanAnnotatesEveryIdJoin(@TempDir Path tempDir) throws Exception {
		LmdbStore store = LmdbTestUtil.newStoreWithLmdbEvaluationStrategy(tempDir.toFile());
		store.setDefaultIsolationLevel(IsolationLevels.READ_COMMITTED);
		SailRepository repository = new SailRepository(store);
		repository.init();

		try (RepositoryConnection conn = repository.getConnection()) {
			ValueFactory vf = SimpleValueFactory.getInstance();
			IRI alice = vf.createIRI(NS, "alice");
			IRI bob = vf.createIRI(NS, "bob");
			IRI carol = vf.createIRI(NS, "carol");
			IRI knows = vf.createIRI(NS, "knows");
			IRI likes = vf.createIRI(NS, "likes");
			IRI pizza = vf.createIRI(NS, "pizza");
			IRI pasta = vf.createIRI(NS, "pasta");

			conn.add(alice, knows, bob);
			conn.add(alice, likes, pizza);
			conn.add(bob, likes, pizza);
			conn.add(carol, likes, pasta);
		}

		String query = "SELECT ?p ?i WHERE {\n" +
				"  ?p <" + NS + "knows> ?x .\n" +
				"  ?p <" + NS + "likes> ?i .\n" +
				"  ?x <" + NS + "likes> ?i .\n" +
				"}";

		try (RepositoryConnection conn = repository.getConnection()) {
			ParsedTupleQuery parsed = QueryParserUtil.parseTupleQuery(QueryLanguage.SPARQL, query, null);
			TupleExpr tupleExpr = parsed.getTupleExpr();

			SailRepositoryConnection sailRepoConn = (SailRepositoryConnection) conn;
			SailConnection sailConnection = sailRepoConn.getSailConnection();
			Explanation explanation = sailConnection.explain(Explanation.Level.Optimized, tupleExpr, null,
					EmptyBindingSet.getInstance(), true, 0);

			GenericPlanNode plan = explanation.toGenericPlanNode();
			List<GenericPlanNode> joinNodes = collectJoinNodes(plan);

			assertThat(joinNodes)
					.withFailMessage("Expected multi-join BGP to produce at least two join nodes in plan but saw %s",
							joinNodes.size())
					.hasSizeGreaterThanOrEqualTo(2);

			joinNodes.forEach(node -> assertThat(node.getAlgorithm())
					.withFailMessage("Plan node %s should show LMDB ID join usage", node.getType())
					.isEqualTo("LmdbIdJoinIterator"));
		} finally {
			repository.shutDown();
		}
	}

	@Test
	public void executedPlansExposeIdJoinRowsAndScanMetrics(@TempDir Path tempDir) throws Exception {
		LmdbStore store = newStore(tempDir, "spoc,posc,opsc");
		SailRepository repository = new SailRepository(store);
		repository.init();

		try {
			try (RepositoryConnection conn = repository.getConnection()) {
				ValueFactory vf = SimpleValueFactory.getInstance();
				IRI alice = vf.createIRI(NS, "alice");
				IRI bob = vf.createIRI(NS, "bob");
				IRI carol = vf.createIRI(NS, "carol");
				IRI knows = vf.createIRI(NS, "knows");
				IRI likes = vf.createIRI(NS, "likes");
				IRI pizza = vf.createIRI(NS, "pizza");
				IRI salad = vf.createIRI(NS, "salad");
				IRI type = vf.createIRI(NS, "type");
				IRI user = vf.createIRI(NS, "User");
				IRI status = vf.createIRI(NS, "status");
				IRI active = vf.createIRI(NS, "Active");
				IRI city = vf.createIRI(NS, "city");
				IRI oslo = vf.createIRI(NS, "Oslo");
				IRI bergen = vf.createIRI(NS, "Bergen");

				conn.add(alice, knows, bob);
				conn.add(alice, likes, pizza);
				conn.add(alice, likes, salad);
				conn.add(bob, knows, carol);
				conn.add(bob, likes, salad);
				conn.add(carol, likes, pizza);
				conn.add(alice, type, user);
				conn.add(alice, status, active);
				conn.add(alice, city, oslo);
				conn.add(bob, type, user);
				conn.add(bob, status, vf.createIRI(NS, "Inactive"));
				conn.add(bob, city, bergen);
			}

			MetricSnapshot nestedLoop = directMetric(store, repository,
					"SELECT ?person ?item WHERE {\n" +
							"  ?person <" + NS + "knows> ?other .\n" +
							"  ?person <" + NS + "likes> ?item .\n" +
							"}",
					join -> join.setAlgorithm("LmdbIdJoinIterator"), "LmdbIdJoinIterator");

			MetricSnapshot merge = directMetric(store, repository,
					"SELECT ?person ?item WHERE {\n" +
							"  ?person <" + NS + "knows> ?other .\n" +
							"  ?person <" + NS + "likes> ?item .\n" +
							"}",
					join -> {
						StatementPattern left = (StatementPattern) join.getLeftArg();
						join.setOrder(left.getSubjectVar());
						join.setMergeJoin(true);
					}, "LmdbIdMergeJoinIterator");

			MetricSnapshot nary = directMetric(store, repository,
					"SELECT ?person ?city WHERE {\n" +
							"  ?person <" + NS + "type> <" + NS + "User> .\n" +
							"  ?person <" + NS + "status> <" + NS + "Active> .\n" +
							"  ?person <" + NS + "city> ?city .\n" +
							"}",
					join -> join.setAlgorithm("LmdbIdJoinIterator"), "LmdbIdNaryIndexScanIterator");

			assertThat(nestedLoop.rows).isEqualTo(3);
			assertThat(nestedLoop.sourceRowsScanned).isPositive();
			assertThat(nestedLoop.leftRowsProbed).isPositive();
			assertThat(nestedLoop.rightRowsScanned).isPositive();

			assertThat(merge.rows).isEqualTo(3);
			assertThat(merge.sourceRowsScanned).isPositive();
			assertThat(merge.leftRowsProbed).isPositive();
			assertThat(merge.rightRowsScanned).isPositive();

			assertThat(nary.rows).isEqualTo(1);
			assertThat(nary.sourceRowsScanned).isPositive();

			System.out.printf(
					"LMDB ID join metrics: nested rows=%d algorithm=%s scanned=%d leftProbes=%d rightRows=%d; "
							+ "merge rows=%d algorithm=%s scanned=%d leftProbes=%d rightRows=%d; "
							+ "nary rows=%d algorithm=%s scanned=%d%n",
					nestedLoop.rows, nestedLoop.algorithm, nestedLoop.sourceRowsScanned,
					nestedLoop.leftRowsProbed, nestedLoop.rightRowsScanned, merge.rows, merge.algorithm,
					merge.sourceRowsScanned, merge.leftRowsProbed, merge.rightRowsScanned, nary.rows,
					nary.algorithm, nary.sourceRowsScanned);
		} finally {
			repository.shutDown();
		}
	}

	private TupleExpr unwrap(TupleExpr tupleExpr) {
		TupleExpr current = tupleExpr;
		if (current instanceof QueryRoot) {
			current = ((QueryRoot) current).getArg();
		}
		if (current instanceof Projection) {
			current = ((Projection) current).getArg();
		}
		if (current instanceof QueryRoot) {
			current = ((QueryRoot) current).getArg();
		}
		return current;
	}

	private static LmdbStore newStore(Path tempDir, String indexes) {
		LmdbStore store = new LmdbStore(tempDir.toFile(), new LmdbStoreConfig(indexes));
		store.setDefaultIsolationLevel(IsolationLevels.READ_COMMITTED);
		store.setEvaluationStrategyFactory(new LmdbEvaluationStrategyFactory(null));
		return store;
	}

	private static List<String> optimizedAlgorithms(RepositoryConnection conn, String query) throws Exception {
		ParsedTupleQuery parsed = QueryParserUtil.parseTupleQuery(QueryLanguage.SPARQL, query, null);
		TupleExpr tupleExpr = parsed.getTupleExpr();
		SailRepositoryConnection sailRepoConn = (SailRepositoryConnection) conn;
		SailConnection sailConnection = sailRepoConn.getSailConnection();
		Explanation explanation = sailConnection.explain(Explanation.Level.Optimized, tupleExpr, null,
				EmptyBindingSet.getInstance(), true, 0);
		List<String> algorithms = new ArrayList<>();
		collectAlgorithms(explanation.toGenericPlanNode(), algorithms);
		return algorithms;
	}

	private static MetricSnapshot directMetric(LmdbStore store, SailRepository repository, String query,
			Consumer<Join> configureJoin,
			String expectedAlgorithm) throws Exception {
		ParsedTupleQuery parsed = QueryParserUtil.parseTupleQuery(QueryLanguage.SPARQL, query, null);
		TupleExpr joinExpr = unwrapStatic(parsed.getTupleExpr());
		assertThat(joinExpr).isInstanceOf(Join.class);
		Join join = (Join) joinExpr;
		configureJoin.accept(join);
		join.setRuntimeTelemetryEnabled(true);

		SailSource branch = store.getBackingStore().getExplicitSailSource();
		SailDataset dataset = branch.dataset(IsolationLevels.READ_COMMITTED);
		try {
			SailDatasetTripleSource tripleSource = new SailDatasetTripleSource(repository.getValueFactory(), dataset);
			EvaluationStrategyFactory factory = store.getEvaluationStrategyFactory();
			EvaluationStrategy strategy = factory.createEvaluationStrategy(null, tripleSource,
					store.getBackingStore().getEvaluationStatistics());
			LmdbEvaluationDataset lmdbDataset = (LmdbEvaluationDataset) dataset;
			LmdbQueryEvaluationContext context = new LmdbQueryEvaluationContext(null, tripleSource.getValueFactory(),
					tripleSource.getComparator(), lmdbDataset, lmdbDataset.getValueStore());

			int rows;
			QueryEvaluationStep step = strategy.precompile(join, context);
			try (CloseableIteration<BindingSet> iter = step.evaluate(EmptyBindingSet.getInstance())) {
				rows = Iterations.asList(iter).size();
			}

			assertThat(join.getAlgorithmName()).isEqualTo(expectedAlgorithm);
			return new MetricSnapshot(expectedAlgorithm, rows, join.getSourceRowsScannedActual(),
					join.getLongMetricActual(TelemetryMetricNames.LEFT_ROWS_PROBED_ACTUAL),
					join.getLongMetricActual(TelemetryMetricNames.RIGHT_ROWS_SCANNED_ACTUAL));
		} finally {
			dataset.close();
			branch.close();
		}
	}

	private static TupleExpr unwrapStatic(TupleExpr tupleExpr) {
		TupleExpr current = tupleExpr;
		if (current instanceof QueryRoot) {
			current = ((QueryRoot) current).getArg();
		}
		if (current instanceof Projection) {
			current = ((Projection) current).getArg();
		}
		if (current instanceof QueryRoot) {
			current = ((QueryRoot) current).getArg();
		}
		return current;
	}

	private static void collectAlgorithms(GenericPlanNode node, List<String> out) {
		if (node == null) {
			return;
		}
		String algorithm = node.getAlgorithm();
		if (algorithm != null) {
			out.add(algorithm);
		}
		List<GenericPlanNode> children = node.getPlans();
		if (children != null) {
			for (GenericPlanNode child : children) {
				collectAlgorithms(child, out);
			}
		}
	}

	private static List<GenericPlanNode> collectJoinNodes(GenericPlanNode root) {
		List<GenericPlanNode> nodes = new ArrayList<>();
		collectJoinNodes(root, nodes);
		return nodes;
	}

	private static void collectJoinNodes(GenericPlanNode node, List<GenericPlanNode> out) {
		if (node == null) {
			return;
		}
		String type = node.getType();
		if (type != null && type.startsWith("Join")) {
			out.add(node);
		}
		List<GenericPlanNode> children = node.getPlans();
		if (children != null) {
			for (GenericPlanNode child : children) {
				collectJoinNodes(child, out);
			}
		}
	}

	private static final class MetricSnapshot {
		final String algorithm;
		final long rows;
		final long sourceRowsScanned;
		final long leftRowsProbed;
		final long rightRowsScanned;

		private MetricSnapshot(String algorithm, long rows, long sourceRowsScanned, long leftRowsProbed,
				long rightRowsScanned) {
			this.algorithm = algorithm;
			this.rows = rows;
			this.sourceRowsScanned = sourceRowsScanned;
			this.leftRowsProbed = leftRowsProbed;
			this.rightRowsScanned = rightRowsScanned;
		}
	}
}
