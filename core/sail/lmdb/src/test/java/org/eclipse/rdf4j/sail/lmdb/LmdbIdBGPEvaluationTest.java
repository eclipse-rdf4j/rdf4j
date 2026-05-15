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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.SingletonIteration;
import org.eclipse.rdf4j.common.iteration.UnionIteration;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.algebra.Compare;
import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.StatementPattern.Index;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.ValueConstant;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.eclipse.rdf4j.query.parser.ParsedTupleQuery;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.base.SailDataset;
import org.eclipse.rdf4j.sail.base.SailDatasetTripleSource;
import org.eclipse.rdf4j.sail.base.SailSource;
import org.eclipse.rdf4j.sail.lmdb.LmdbEvaluationDataset.KeyRangeBuffers;
import org.eclipse.rdf4j.sail.lmdb.join.LmdbIdBGPQueryEvaluationStep;
import org.eclipse.rdf4j.sail.lmdb.model.LmdbValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for the LMDB ID BGP evaluation step.
 */
public class LmdbIdBGPEvaluationTest {

	private static final String NS = "http://example.com/";

	@Test
	public void bgpPrefersContextOverlayDataset(@TempDir java.nio.file.Path tempDir) throws IOException {
		LmdbStore store = new LmdbStore(tempDir.toFile());
		SailRepository repository = new SailRepository(store);
		repository.init();

		ValueFactory vf = SimpleValueFactory.getInstance();
		IRI alice = vf.createIRI(NS, "alice");
		IRI bob = vf.createIRI(NS, "bob");
		IRI knows = vf.createIRI(NS, "knows");
		IRI likes = vf.createIRI(NS, "likes");
		IRI pizza = vf.createIRI(NS, "pizza");

		// Baseline: only the 'knows' triple exists, not 'likes'.
		try (RepositoryConnection conn = repository.getConnection()) {
			conn.add(alice, knows, bob);
		}

		String query = "SELECT ?person ?item\n" +
				"WHERE {\n" +
				"  ?person <http://example.com/knows> ?other .\n" +
				"  ?person <http://example.com/likes> ?item .\n" +
				"}";

		ParsedTupleQuery parsed = QueryParserUtil.parseTupleQuery(QueryLanguage.SPARQL, query, null);
		TupleExpr tupleExpr = parsed.getTupleExpr();

		// unwrap to the top-level Join expression
		TupleExpr current = tupleExpr;
		if (current instanceof org.eclipse.rdf4j.query.algebra.QueryRoot) {
			current = ((org.eclipse.rdf4j.query.algebra.QueryRoot) current).getArg();
		}
		if (current instanceof org.eclipse.rdf4j.query.algebra.Projection) {
			current = ((org.eclipse.rdf4j.query.algebra.Projection) current).getArg();
		}
		if (!(current instanceof Join)) {
			// conservative guard; if the test query changes shape, fail clearly
			throw new AssertionError("expected Join at root of algebra");
		}
		Join join = (Join) current;

		// Flatten the BGP patterns from the Join
		List<org.eclipse.rdf4j.query.algebra.StatementPattern> patterns = new ArrayList<>();
		boolean flattened = LmdbIdBGPQueryEvaluationStep.flattenBGP(join, patterns);
		assertThat(flattened).isTrue();
		assertThat(patterns).hasSize(2);

		// Prepare a baseline dataset (thread-local) and its triple source
		SailSource branch = store.getBackingStore().getExplicitSailSource();
		SailDataset baselineDataset = branch.dataset(IsolationLevels.SNAPSHOT_READ);

		try {
			SailDatasetTripleSource baseTs = new SailDatasetTripleSource(repository.getValueFactory(), baselineDataset);

			// Overlay triple source that returns the extra 'likes' statement in addition to baseline content
			Statement overlayStmt = vf.createStatement(alice, likes, pizza);
			TripleSource overlayTs = new TripleSource() {
				@Override
				public CloseableIteration<? extends Statement> getStatements(Resource subj, IRI pred, Value obj,
						Resource... contexts) {
					boolean isLikes = pred != null && pred.equals(likes);
					boolean subjOk = subj == null || subj.equals(alice);
					boolean objOk = obj == null || obj.equals(pizza);
					CloseableIteration<? extends Statement> base = baseTs.getStatements(subj, pred, obj, contexts);
					if (isLikes && subjOk && objOk) {
						// combine overlay with baseline
						return new UnionIteration<>(new SingletonIteration<>(overlayStmt), base);
					}
					return base;
				}

				@Override
				public ValueFactory getValueFactory() {
					return baseTs.getValueFactory();
				}

				@Override
				public java.util.Comparator<Value> getComparator() {
					// Narrow the wildcard comparator to the interface's return type
					@SuppressWarnings("unchecked")
					java.util.Comparator<Value> cmp = (java.util.Comparator<Value>) baseTs.getComparator();
					return cmp;
				}
			};

			// Build the overlay dataset and a context carrying it
			LmdbEvaluationDataset threadLocal = (LmdbEvaluationDataset) baselineDataset;
			LmdbEvaluationDataset overlayDataset = new LmdbOverlayEvaluationDataset(overlayTs,
					threadLocal.getValueStore());

			QueryEvaluationContext ctx = new LmdbQueryEvaluationContext(null, overlayTs.getValueFactory(),
					overlayTs.getComparator(), overlayDataset, threadLocal.getValueStore());

			// Simulate a thread-local dataset reference that points to the baseline dataset
			LmdbEvaluationStrategy.setCurrentDataset(threadLocal);
			try {
				LmdbIdBGPQueryEvaluationStep step = new LmdbIdBGPQueryEvaluationStep(join, patterns, ctx, null);
				try (CloseableIteration<BindingSet> iter = step.evaluate(EmptyBindingSet.getInstance())) {
					List<BindingSet> results = org.eclipse.rdf4j.common.iteration.Iterations.asList(iter);
					// We expect 1 result because the overlay supplies the missing 'likes' triple.
					assertThat(results).hasSize(1);
				}
			} finally {
				LmdbEvaluationStrategy.clearCurrentDataset();
			}
		} finally {
			baselineDataset.close();
			branch.close();
			repository.shutDown();
		}
	}

	@Test
	public void bgpUsesIdArrayIterator(@TempDir java.nio.file.Path tempDir) throws IOException {
		LmdbStore store = LmdbTestUtil.newStoreWithLmdbEvaluationStrategy(tempDir.toFile());
		SailRepository repository = new SailRepository(store);
		repository.init();

		ValueFactory vf = SimpleValueFactory.getInstance();
		IRI alice = vf.createIRI(NS, "alice");
		IRI bob = vf.createIRI(NS, "bob");
		IRI knows = vf.createIRI(NS, "knows");
		IRI likes = vf.createIRI(NS, "likes");
		IRI pizza = vf.createIRI(NS, "pizza");

		try (RepositoryConnection conn = repository.getConnection()) {
			conn.add(alice, knows, bob);
			conn.add(alice, likes, pizza);
		}

		String query = "SELECT ?person ?item\n" +
				"WHERE {\n" +
				"  ?person <http://example.com/knows> ?other .\n" +
				"  ?person <http://example.com/likes> ?item .\n" +
				"}";

		ParsedTupleQuery parsed = QueryParserUtil.parseTupleQuery(QueryLanguage.SPARQL, query, null);
		TupleExpr tupleExpr = parsed.getTupleExpr();
		TupleExpr current = tupleExpr;
		if (current instanceof org.eclipse.rdf4j.query.algebra.QueryRoot) {
			current = ((org.eclipse.rdf4j.query.algebra.QueryRoot) current).getArg();
		}
		if (current instanceof org.eclipse.rdf4j.query.algebra.Projection) {
			current = ((org.eclipse.rdf4j.query.algebra.Projection) current).getArg();
		}
		if (!(current instanceof Join)) {
			throw new AssertionError("expected Join at root of algebra");
		}
		Join join = (Join) current;

		List<org.eclipse.rdf4j.query.algebra.StatementPattern> patterns = new ArrayList<>();
		boolean flattened = LmdbIdBGPQueryEvaluationStep.flattenBGP(join, patterns);
		assertThat(flattened).isTrue();
		assertThat(patterns).hasSize(2);

		SailSource branch = store.getBackingStore().getExplicitSailSource();
		SailDataset dataset = branch.dataset(IsolationLevels.SNAPSHOT_READ);

		try {
			LmdbEvaluationDataset lmdbDataset = (LmdbEvaluationDataset) dataset;
			RecordingDataset recordingDataset = new RecordingDataset(lmdbDataset);

			SailDatasetTripleSource tripleSource = new SailDatasetTripleSource(repository.getValueFactory(), dataset);

			QueryEvaluationContext ctx = new LmdbQueryEvaluationContext(null, tripleSource.getValueFactory(),
					tripleSource.getComparator(), recordingDataset, lmdbDataset.getValueStore());

			LmdbIdBGPQueryEvaluationStep step = new LmdbIdBGPQueryEvaluationStep(join, patterns, ctx, null);

			try (CloseableIteration<BindingSet> iter = step.evaluate(EmptyBindingSet.getInstance())) {
				List<BindingSet> results = org.eclipse.rdf4j.common.iteration.Iterations.asList(iter);
				assertThat(results).hasSize(1);
			}

			assertThat(recordingDataset.wasLegacyApiUsed()).isFalse();
			assertThat(recordingDataset.wasArrayApiUsed()).isTrue();
		} finally {
			dataset.close();
			branch.close();
			repository.shutDown();
		}
	}

	@Test
	public void repeatedExistsProbeUsesCachedIdResult(@TempDir java.nio.file.Path tempDir) throws IOException {
		LmdbStore store = new LmdbStore(tempDir.toFile());
		SailRepository repository = new SailRepository(store);
		repository.init();

		ValueFactory vf = SimpleValueFactory.getInstance();
		IRI alice = vf.createIRI(NS, "alice");
		IRI bob = vf.createIRI(NS, "bob");
		IRI knows = vf.createIRI(NS, "knows");
		IRI likes = vf.createIRI(NS, "likes");
		IRI pizza = vf.createIRI(NS, "pizza");

		try (RepositoryConnection conn = repository.getConnection()) {
			conn.add(alice, knows, bob);
			conn.add(alice, likes, pizza);
		}

		String query = "SELECT ?person ?item\n" +
				"WHERE {\n" +
				"  ?person <http://example.com/knows> ?other .\n" +
				"  ?person <http://example.com/likes> ?item .\n" +
				"}";

		ParsedTupleQuery parsed = QueryParserUtil.parseTupleQuery(QueryLanguage.SPARQL, query, null);
		TupleExpr tupleExpr = parsed.getTupleExpr();
		TupleExpr current = tupleExpr;
		if (current instanceof org.eclipse.rdf4j.query.algebra.QueryRoot) {
			current = ((org.eclipse.rdf4j.query.algebra.QueryRoot) current).getArg();
		}
		if (current instanceof org.eclipse.rdf4j.query.algebra.Projection) {
			current = ((org.eclipse.rdf4j.query.algebra.Projection) current).getArg();
		}
		if (!(current instanceof Join)) {
			throw new AssertionError("expected Join at root of algebra");
		}
		Join join = (Join) current;

		List<org.eclipse.rdf4j.query.algebra.StatementPattern> patterns = new ArrayList<>();
		boolean flattened = LmdbIdBGPQueryEvaluationStep.flattenBGP(join, patterns);
		assertThat(flattened).isTrue();
		assertThat(patterns).hasSize(2);

		SailSource branch = store.getBackingStore().getExplicitSailSource();
		SailDataset dataset = branch.dataset(IsolationLevels.READ_COMMITTED);

		try {
			LmdbEvaluationDataset lmdbDataset = (LmdbEvaluationDataset) dataset;
			RecordingDataset recordingDataset = new RecordingDataset(lmdbDataset);

			SailDatasetTripleSource tripleSource = new SailDatasetTripleSource(repository.getValueFactory(), dataset);

			QueryEvaluationContext ctx = new LmdbQueryEvaluationContext(null, tripleSource.getValueFactory(),
					tripleSource.getComparator(), recordingDataset, lmdbDataset.getValueStore());

			LmdbIdBGPQueryEvaluationStep step = new LmdbIdBGPQueryEvaluationStep(join, patterns, ctx, null);

			assertThat(step.exists(EmptyBindingSet.getInstance())).isTrue();
			int callsAfterFirstProbe = recordingDataset.arrayApiCallCount();

			assertThat(step.exists(EmptyBindingSet.getInstance())).isTrue();

			assertThat(recordingDataset.arrayApiCallCount()).isEqualTo(callsAfterFirstProbe);
		} finally {
			dataset.close();
			branch.close();
			repository.shutDown();
		}
	}

	@Test
	public void sharedRightProbeUsesBatchIdJoin(@TempDir java.nio.file.Path tempDir) throws IOException {
		LmdbStore store = LmdbTestUtil.newStoreWithLmdbEvaluationStrategy(tempDir.toFile());
		store.setDefaultIsolationLevel(IsolationLevels.READ_COMMITTED);
		SailRepository repository = new SailRepository(store);
		repository.init();

		ValueFactory vf = SimpleValueFactory.getInstance();
		IRI memberOf = vf.createIRI(NS, "batchMemberOf");
		IRI groupLabel = vf.createIRI(NS, "batchGroupLabel");
		IRI group = vf.createIRI(NS, "batchGroup");
		Value label = vf.createLiteral("shared");

		try (RepositoryConnection conn = repository.getConnection()) {
			conn.add(group, groupLabel, label);
			for (int i = 0; i < 1100; i++) {
				conn.add(vf.createIRI(NS, "batchPerson" + i), memberOf, group);
			}
		}

		try (RepositoryConnection conn = repository.getConnection();
				TupleQueryResult result = conn.prepareTupleQuery("""
						SELECT ?person WHERE {
						  ?person <http://example.com/batchMemberOf> <http://example.com/batchGroup> .
						}
						""").evaluate()) {
			int leftCount = 0;
			Set<Value> leftPeople = new HashSet<>();
			while (result.hasNext()) {
				BindingSet row = result.next();
				leftPeople.add(row.getValue("person"));
				leftCount++;
			}
			assertThat(leftCount).as("left scan rows").isEqualTo(1100);
			assertThat(leftPeople).as("left scan unique people").hasSize(1100);
		}

		String query = """
				SELECT ?person ?label WHERE {
				  ?person <http://example.com/batchMemberOf> ?group .
				  ?group <http://example.com/batchGroupLabel> ?label .
				}
				""";

		ParsedTupleQuery parsed = QueryParserUtil.parseTupleQuery(QueryLanguage.SPARQL, query, null);
		TupleExpr current = parsed.getTupleExpr();
		if (current instanceof org.eclipse.rdf4j.query.algebra.QueryRoot) {
			current = ((org.eclipse.rdf4j.query.algebra.QueryRoot) current).getArg();
		}
		if (current instanceof org.eclipse.rdf4j.query.algebra.Projection) {
			current = ((org.eclipse.rdf4j.query.algebra.Projection) current).getArg();
		}
		if (!(current instanceof Join)) {
			throw new AssertionError("expected Join at root of algebra");
		}
		Join join = (Join) current;

		List<StatementPattern> patterns = new ArrayList<>();
		boolean flattened = LmdbIdBGPQueryEvaluationStep.flattenBGP(join, patterns);
		assertThat(flattened).isTrue();
		assertThat(patterns).hasSize(2);

		SailSource branch = store.getBackingStore().getExplicitSailSource();
		SailDataset dataset = branch.dataset(IsolationLevels.READ_COMMITTED);

		try {
			LmdbEvaluationDataset lmdbDataset = (LmdbEvaluationDataset) dataset;
			RecordingDataset recordingDataset = new RecordingDataset(lmdbDataset);
			SailDatasetTripleSource tripleSource = new SailDatasetTripleSource(repository.getValueFactory(), dataset);
			QueryEvaluationContext ctx = new LmdbQueryEvaluationContext(null, tripleSource.getValueFactory(),
					tripleSource.getComparator(), recordingDataset, lmdbDataset.getValueStore());

			LmdbIdBGPQueryEvaluationStep step = new LmdbIdBGPQueryEvaluationStep(join, patterns, ctx, null);

			try (CloseableIteration<BindingSet> iter = step.evaluate(EmptyBindingSet.getInstance())) {
				int count = 0;
				Set<Value> people = new HashSet<>();
				List<Value> duplicates = new ArrayList<>();
				BindingSet extra = null;
				while (iter.hasNext()) {
					BindingSet row = iter.next();
					assertThat(row.getValue("label")).isEqualTo(label);
					Value person = row.getValue("person");
					if (!people.add(person)) {
						duplicates.add(person);
					}
					count++;
					if (count > 1100) {
						extra = row;
					}
				}
				assertThat(duplicates)
						.as("extra row: %s; unique people: %s; count: %s; calls: %s", extra, people.size(), count,
								recordingDataset.describeArrayCalls())
						.isEmpty();
				assertThat(count).as("extra row: %s; unique people: %s", extra, people.size()).isEqualTo(1100);
				assertThat(iter.hasNext()).isFalse();
			}

			int batchSize = Integer.getInteger("org.eclipse.rdf4j.sail.lmdb.idjoin.batch.size", 1024);
			int expectedMaxArrayCalls = 1 + ((1100 + batchSize - 1) / batchSize);
			assertThat(recordingDataset.arrayApiCallCount())
					.as("batch join should probe the shared right key once per batch, not once per left row")
					.isLessThanOrEqualTo(expectedMaxArrayCalls);
		} finally {
			dataset.close();
			branch.close();
			repository.shutDown();
		}
	}

	@Test
	public void bgpEmbeddedIntegerRangeFilterUsesIdPredicate(@TempDir java.nio.file.Path tempDir) throws Exception {
		LmdbStore store = new LmdbStore(tempDir.toFile());
		store.setDefaultIsolationLevel(IsolationLevels.SNAPSHOT_READ);
		System.setProperty("org.eclipse.rdf4j.sail.lmdb.codegen.explain", "true");
		SailRepository repository = new SailRepository(store);
		repository.init();

		ValueFactory vf = SimpleValueFactory.getInstance();
		IRI encounter = vf.createIRI(NS, "encounter");
		IRI includedObservation = vf.createIRI(NS, "includedObservation");
		IRI filteredObservation = vf.createIRI(NS, "filteredObservation");
		IRI hasObservation = vf.createIRI(NS, "hasObservation");
		IRI value = vf.createIRI(NS, "value");

		try (RepositoryConnection conn = repository.getConnection()) {
			conn.add(encounter, hasObservation, includedObservation);
			conn.add(includedObservation, value, vf.createLiteral(41));
			conn.add(encounter, hasObservation, filteredObservation);
			conn.add(filteredObservation, value, vf.createLiteral(39));
		}

		try {
			String query = """
					SELECT ?obs WHERE {
					  ?enc <http://example.com/hasObservation> ?obs .
					  ?obs <http://example.com/value> ?value .
					  FILTER (?value > 40)
					}
					""";
			try (RepositoryConnection conn = repository.getConnection();
					TupleQueryResult result = conn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate()) {
				List<BindingSet> rows = org.eclipse.rdf4j.common.iteration.Iterations.asList(result);
				assertThat(rows)
						.extracting(row -> row.getValue("obs"))
						.containsExactly(includedObservation);
			}
		} finally {
			System.clearProperty("org.eclipse.rdf4j.sail.lmdb.codegen.explain");
			repository.shutDown();
		}
	}

	@Test
	public void generatedIntegerRangeFilterDefersDecimalRowsToColdEvaluation(@TempDir java.nio.file.Path tempDir)
			throws Exception {
		LmdbStore store = new LmdbStore(tempDir.toFile());
		store.setDefaultIsolationLevel(IsolationLevels.SNAPSHOT_READ);
		SailRepository repository = new SailRepository(store);
		repository.init();

		ValueFactory vf = SimpleValueFactory.getInstance();
		IRI includedObservation = vf.createIRI(NS, "includedDecimalObservation");
		IRI filteredObservation = vf.createIRI(NS, "filteredDecimalObservation");
		IRI value = vf.createIRI(NS, "value");

		try (RepositoryConnection conn = repository.getConnection()) {
			conn.add(includedObservation, value,
					vf.createLiteral("41.5", org.eclipse.rdf4j.model.vocabulary.XSD.DECIMAL));
			conn.add(filteredObservation, value,
					vf.createLiteral("39.5", org.eclipse.rdf4j.model.vocabulary.XSD.DECIMAL));
		}

		try {
			String query = """
					SELECT ?obs WHERE {
					  ?obs <http://example.com/value> ?value .
					  FILTER (?value > 40)
					}
					""";
			try (RepositoryConnection conn = repository.getConnection();
					TupleQueryResult result = conn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate()) {
				List<BindingSet> rows = org.eclipse.rdf4j.common.iteration.Iterations.asList(result);
				assertThat(rows)
						.extracting(row -> row.getValue("obs"))
						.containsExactly(includedObservation);
			}
		} finally {
			repository.shutDown();
		}
	}

	@Test
	public void filterOverJoinedPatternsUsesGeneratedIntegerPredicate(@TempDir java.nio.file.Path tempDir)
			throws Exception {
		LmdbStore store = LmdbTestUtil.newStoreWithLmdbEvaluationStrategy(tempDir.toFile());
		store.setDefaultIsolationLevel(IsolationLevels.READ_COMMITTED);
		System.setProperty("org.eclipse.rdf4j.sail.lmdb.codegen.explain", "true");
		SailRepository repository = new SailRepository(store);
		repository.init();

		ValueFactory vf = SimpleValueFactory.getInstance();
		IRI encounter = vf.createIRI(NS, "filteredJoinEncounter");
		IRI includedObservation = vf.createIRI(NS, "filteredJoinIncludedObservation");
		IRI filteredObservation = vf.createIRI(NS, "filteredJoinFilteredObservation");
		IRI hasObservation = vf.createIRI(NS, "hasObservation");
		IRI value = vf.createIRI(NS, "value");

		try (RepositoryConnection conn = repository.getConnection()) {
			conn.add(encounter, hasObservation, includedObservation);
			conn.add(includedObservation, value, vf.createLiteral(41));
			conn.add(encounter, hasObservation, filteredObservation);
			conn.add(filteredObservation, value, vf.createLiteral(39));
		}

		try {
			String query = """
					SELECT ?obs WHERE {
					  ?enc <http://example.com/hasObservation> ?obs .
					  ?obs <http://example.com/value> ?value .
					  FILTER (?value > 40)
					}
					""";
			try (RepositoryConnection conn = repository.getConnection();
					TupleQueryResult result = conn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate()) {
				List<BindingSet> rows = org.eclipse.rdf4j.common.iteration.Iterations.asList(result);
				assertThat(rows)
						.extracting(row -> row.getValue("obs"))
						.containsExactly(includedObservation);
			}

			String optimizedPlan;
			try (RepositoryConnection conn = repository.getConnection()) {
				optimizedPlan = conn.prepareTupleQuery(QueryLanguage.SPARQL, query)
						.explain(org.eclipse.rdf4j.query.explanation.Explanation.Level.Optimized)
						.toString();
			}
			assertThat(String.valueOf(LmdbGeneratedRecordIteratorFactory.getLastExplain()))
					.as(optimizedPlan)
					.contains("filtered-projection", "embedded-integer");
		} finally {
			System.clearProperty("org.eclipse.rdf4j.sail.lmdb.codegen.explain");
			repository.shutDown();
		}
	}

	@Test
	public void minusBranchJoinedFilterUsesGeneratedIntegerPredicate(@TempDir java.nio.file.Path tempDir)
			throws Exception {
		LmdbStore store = LmdbTestUtil.newStoreWithLmdbEvaluationStrategy(tempDir.toFile());
		store.setDefaultIsolationLevel(IsolationLevels.READ_COMMITTED);
		System.setProperty("org.eclipse.rdf4j.sail.lmdb.codegen.explain", "true");
		SailRepository repository = new SailRepository(store);
		repository.init();

		ValueFactory vf = SimpleValueFactory.getInstance();
		IRI excludedEncounter = vf.createIRI(NS, "minusExcludedEncounter");
		IRI includedEncounter = vf.createIRI(NS, "minusIncludedEncounter");
		IRI excludedObservation = vf.createIRI(NS, "minusExcludedObservation");
		IRI includedObservation = vf.createIRI(NS, "minusIncludedObservation");
		IRI hasObservation = vf.createIRI(NS, "hasObservation");
		IRI value = vf.createIRI(NS, "value");

		try (RepositoryConnection conn = repository.getConnection()) {
			conn.add(excludedEncounter, hasObservation, excludedObservation);
			conn.add(excludedObservation, value, vf.createLiteral(39));
			conn.add(includedEncounter, hasObservation, includedObservation);
			conn.add(includedObservation, value, vf.createLiteral(41));
		}

		try {
			String query = """
					SELECT ?enc WHERE {
					  ?enc <http://example.com/hasObservation> ?obs .
					  MINUS {
					    ?enc <http://example.com/hasObservation> ?excludedObs .
					    ?excludedObs <http://example.com/value> ?value .
					    FILTER (?value < 40)
					  }
					}
					""";
			try (RepositoryConnection conn = repository.getConnection();
					TupleQueryResult result = conn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate()) {
				List<BindingSet> rows = org.eclipse.rdf4j.common.iteration.Iterations.asList(result);
				assertThat(rows)
						.extracting(row -> row.getValue("enc"))
						.containsExactly(includedEncounter);
			}

			String optimizedPlan;
			try (RepositoryConnection conn = repository.getConnection()) {
				optimizedPlan = conn.prepareTupleQuery(QueryLanguage.SPARQL, query)
						.explain(org.eclipse.rdf4j.query.explanation.Explanation.Level.Optimized)
						.toString();
			}
			assertThat(String.valueOf(LmdbGeneratedRecordIteratorFactory.getLastExplain()))
					.as(optimizedPlan)
					.contains("filtered-projection", "embedded-integer");
		} finally {
			System.clearProperty("org.eclipse.rdf4j.sail.lmdb.codegen.explain");
			repository.shutDown();
		}
	}

	@Test
	public void optionalJoinedBranchUsesIdJoin(@TempDir java.nio.file.Path tempDir) throws Exception {
		LmdbStore store = LmdbTestUtil.newStoreWithLmdbEvaluationStrategy(tempDir.toFile());
		store.setDefaultIsolationLevel(IsolationLevels.READ_COMMITTED);
		SailRepository repository = new SailRepository(store);
		repository.init();

		ValueFactory vf = SimpleValueFactory.getInstance();
		IRI alice = vf.createIRI(NS, "optionalAlice");
		IRI bob = vf.createIRI(NS, "optionalBob");
		IRI carol = vf.createIRI(NS, "optionalCarol");
		IRI encounter = vf.createIRI(NS, "optionalEncounter");
		IRI practitioner = vf.createIRI(NS, "optionalPractitioner");
		IRI knows = vf.createIRI(NS, "optionalKnows");
		IRI hasEncounter = vf.createIRI(NS, "optionalHasEncounter");
		IRI handledBy = vf.createIRI(NS, "optionalHandledBy");

		try (RepositoryConnection conn = repository.getConnection()) {
			conn.add(alice, knows, bob);
			conn.add(carol, knows, bob);
			conn.add(alice, hasEncounter, encounter);
			conn.add(encounter, handledBy, practitioner);
		}

		try {
			String query = """
					SELECT ?person ?enc ?optPractitioner WHERE {
					  ?person <http://example.com/optionalKnows> ?friend .
					  OPTIONAL {
					    ?person <http://example.com/optionalHasEncounter> ?enc .
					    ?enc <http://example.com/optionalHandledBy> ?practitioner .
					    BIND(?practitioner AS ?optPractitioner)
					  }
					}
					""";
			try (RepositoryConnection conn = repository.getConnection();
					TupleQueryResult result = conn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate()) {
				List<BindingSet> rows = org.eclipse.rdf4j.common.iteration.Iterations.asList(result);
				assertThat(rows).hasSize(2);
				assertThat(rows.stream().filter(row -> row.hasBinding("optPractitioner")).count()).isEqualTo(1);
			}

			String executedPlan;
			try (RepositoryConnection conn = repository.getConnection()) {
				executedPlan = conn.prepareTupleQuery(QueryLanguage.SPARQL, query)
						.explain(org.eclipse.rdf4j.query.explanation.Explanation.Level.Executed)
						.toString();
			}
			assertThat(executedPlan).contains("LeftJoin (LmdbIdLeftJoinIterator)");
		} finally {
			repository.shutDown();
		}
	}

	@Test
	public void recordsChosenIndexesForPatterns(@TempDir java.nio.file.Path tempDir) throws IOException {
		LmdbStore store = new LmdbStore(tempDir.toFile());
		SailRepository repository = new SailRepository(store);
		repository.init();

		ValueFactory vf = SimpleValueFactory.getInstance();
		IRI alice = vf.createIRI(NS, "alice");
		IRI bob = vf.createIRI(NS, "bob");
		IRI knows = vf.createIRI(NS, "knows");
		IRI likes = vf.createIRI(NS, "likes");
		IRI pizza = vf.createIRI(NS, "pizza");

		try (RepositoryConnection conn = repository.getConnection()) {
			conn.add(alice, knows, bob);
			conn.add(alice, likes, pizza);
		}

		String query = "SELECT ?person ?item\n" +
				"WHERE {\n" +
				"  ?person <http://example.com/knows> <http://example.com/bob> .\n" +
				"  ?person <http://example.com/likes> ?item .\n" +
				"}";

		ParsedTupleQuery parsed = QueryParserUtil.parseTupleQuery(QueryLanguage.SPARQL, query, null);
		TupleExpr tupleExpr = parsed.getTupleExpr();
		TupleExpr current = tupleExpr;
		if (current instanceof org.eclipse.rdf4j.query.algebra.QueryRoot) {
			current = ((org.eclipse.rdf4j.query.algebra.QueryRoot) current).getArg();
		}
		if (current instanceof org.eclipse.rdf4j.query.algebra.Projection) {
			current = ((org.eclipse.rdf4j.query.algebra.Projection) current).getArg();
		}
		if (!(current instanceof Join)) {
			throw new AssertionError("expected Join at root of algebra");
		}
		Join join = (Join) current;

		List<org.eclipse.rdf4j.query.algebra.StatementPattern> patterns = new ArrayList<>();
		boolean flattened = LmdbIdBGPQueryEvaluationStep.flattenBGP(join, patterns);
		assertThat(flattened).isTrue();
		assertThat(patterns).hasSize(2);

		SailSource branch = store.getBackingStore().getExplicitSailSource();
		SailDataset dataset = branch.dataset(IsolationLevels.SNAPSHOT_READ);

		try {
			LmdbEvaluationDataset lmdbDataset = (LmdbEvaluationDataset) dataset;
			SailDatasetTripleSource tripleSource = new SailDatasetTripleSource(repository.getValueFactory(), dataset);

			QueryEvaluationContext ctx = new LmdbQueryEvaluationContext(null, tripleSource.getValueFactory(),
					tripleSource.getComparator(), lmdbDataset, lmdbDataset.getValueStore());

			LmdbIdBGPQueryEvaluationStep step = new LmdbIdBGPQueryEvaluationStep(join, patterns, ctx, null);

			try (CloseableIteration<BindingSet> iter = step.evaluate(EmptyBindingSet.getInstance())) {
				while (iter.hasNext()) {
					iter.next();
				}
			}

			assertIndexMatchesDataset(lmdbDataset, patterns.get(0));
			assertIndexMatchesDataset(lmdbDataset, patterns.get(1));
		} finally {
			dataset.close();
			branch.close();
			repository.shutDown();
		}
	}

	private void assertIndexMatchesDataset(LmdbEvaluationDataset dataset,
			org.eclipse.rdf4j.query.algebra.StatementPattern pattern)
			throws IOException {
		long subjId = resolveId(dataset, pattern.getSubjectVar());
		long predId = resolveId(dataset, pattern.getPredicateVar());
		long objId = resolveId(dataset, pattern.getObjectVar());
		long ctxId = resolveId(dataset, pattern.getContextVar());

		String fieldSeq = dataset.selectBestIndex(subjId, predId, objId, ctxId);
		if (fieldSeq == null) {
			assertThat(pattern.getIndex()).isNull();
			return;
		}
		String enumKey = fieldSeq.toUpperCase(Locale.ROOT);
		assertThat(pattern.getIndex()).isEqualTo(Index.valueOf(enumKey));
	}

	private long resolveId(LmdbEvaluationDataset dataset, Var var) throws IOException {
		if (var == null || !var.hasValue()) {
			return LmdbValue.UNKNOWN_ID;
		}
		return dataset.getValueStore().getId(var.getValue());
	}

	private static final class RecordingDataset implements LmdbEvaluationDataset {
		private final LmdbEvaluationDataset delegate;

		RecordingDataset(LmdbEvaluationDataset delegate) {
			this.delegate = delegate;
		}

		@Override
		public RecordIterator getRecordIterator(org.eclipse.rdf4j.query.algebra.StatementPattern pattern,
				BindingSet bindings) {
			legacyApiUsed = true;
			return delegate.getRecordIterator(pattern, bindings);
		}

		@Override
		public RecordIterator getRecordIterator(long[] binding, int subjIndex, int predIndex, int objIndex,
				int ctxIndex,
				long[] patternIds) throws QueryEvaluationException {
			arrayApiUsed = true;
			arrayApiCallCount++;
			recordArrayCall(subjIndex, predIndex, objIndex, ctxIndex, patternIds, binding);
			return delegate.getRecordIterator(binding, subjIndex, predIndex, objIndex, ctxIndex, patternIds);
		}

		@Override
		public RecordIterator getRecordIterator(long[] binding, int subjIndex, int predIndex, int objIndex,
				int ctxIndex, long[] patternIds, KeyRangeBuffers keyBuffers, long[] bindingReuse, long[] quadReuse,
				RecordIterator iteratorReuse, LmdbIdPredicatePlan predicatePlan) throws QueryEvaluationException {
			arrayApiUsed = true;
			arrayApiCallCount++;
			recordArrayCall(subjIndex, predIndex, objIndex, ctxIndex, patternIds, binding);
			return delegate.getRecordIterator(binding, subjIndex, predIndex, objIndex, ctxIndex, patternIds, keyBuffers,
					bindingReuse, quadReuse, iteratorReuse, predicatePlan);
		}

		boolean wasLegacyApiUsed() {
			return legacyApiUsed;
		}

		boolean wasArrayApiUsed() {
			return arrayApiUsed;
		}

		int arrayApiCallCount() {
			return arrayApiCallCount;
		}

		String describeArrayCalls() {
			return arrayCallSummary.toString();
		}

		private void recordArrayCall(int subjIndex, int predIndex, int objIndex, int ctxIndex, long[] patternIds,
				long[] binding) {
			if (arrayApiCallCount > 8) {
				return;
			}
			arrayCallSummary.append('#')
					.append(arrayApiCallCount)
					.append('[')
					.append(subjIndex)
					.append(',')
					.append(predIndex)
					.append(',')
					.append(objIndex)
					.append(',')
					.append(ctxIndex)
					.append("] p=")
					.append(Arrays.toString(patternIds))
					.append(" b=")
					.append(Arrays.toString(binding))
					.append(' ');
		}

		@Override
		public ValueStore getValueStore() {
			return delegate.getValueStore();
		}

		@Override
		public boolean hasTransactionChanges() {
			return delegate.hasTransactionChanges();
		}

		private boolean legacyApiUsed;
		private boolean arrayApiUsed;
		private int arrayApiCallCount;
		private final StringBuilder arrayCallSummary = new StringBuilder();
	}
}
