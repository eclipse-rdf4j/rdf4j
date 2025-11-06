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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.common.order.StatementOrder;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.Projection;
import org.eclipse.rdf4j.query.algebra.QueryRoot;
import org.eclipse.rdf4j.query.algebra.SingletonSet;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategyFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;
import org.eclipse.rdf4j.query.explanation.Explanation;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.eclipse.rdf4j.query.parser.ParsedTupleQuery;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.base.SailDataset;
import org.eclipse.rdf4j.sail.base.SailDatasetTripleSource;
import org.eclipse.rdf4j.sail.base.SailSource;
import org.eclipse.rdf4j.sail.lmdb.join.LmdbIdJoinQueryEvaluationStep;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class LmdbIdJoinEvaluationTest {

	private static final String NS = "http://example.com/";

	@Test
	public void simpleJoinUsesIdIterator(@TempDir Path tempDir) throws Exception {
		LmdbStore store = new LmdbStore(tempDir.toFile());
		SailRepository repository = new SailRepository(store);
		repository.init();

		assertThat(store.getEvaluationStrategyFactory().getClass().getSimpleName())
				.isEqualTo("LmdbEvaluationStrategyFactory");

		ValueFactory vf = SimpleValueFactory.getInstance();
		IRI alice = vf.createIRI(NS, "alice");
		IRI bob = vf.createIRI(NS, "bob");
		IRI knows = vf.createIRI(NS, "knows");
		IRI likes = vf.createIRI(NS, "likes");
		IRI pizza = vf.createIRI(NS, "pizza");

		try (RepositoryConnection conn = repository.getConnection()) {
			conn.add(alice, knows, bob);
			conn.add(alice, likes, pizza);
			conn.add(bob, likes, pizza);
		}

		String query = "SELECT ?person ?item\n" +
				"WHERE {\n" +
				"  ?person <http://example.com/knows> ?other .\n" +
				"  ?person <http://example.com/likes> ?item .\n" +
				"}";

		try (RepositoryConnection conn = repository.getConnection()) {
			ParsedTupleQuery parsed = QueryParserUtil.parseTupleQuery(QueryLanguage.SPARQL, query, null);
			TupleExpr tupleExpr = parsed.getTupleExpr();

			SailRepositoryConnection sailRepoConn = (SailRepositoryConnection) conn;
			SailConnection sailConnection = sailRepoConn.getSailConnection();
			try (CloseableIteration<? extends BindingSet> iter = sailConnection.evaluate(tupleExpr, null,
					EmptyBindingSet.getInstance(), true)) {
				List<? extends BindingSet> bindings = Iterations.asList(iter);
				assertThat(bindings).hasSize(1);
			}

			sailConnection.explain(Explanation.Level.Optimized, tupleExpr, null, EmptyBindingSet.getInstance(), true,
					0);

			TupleExpr joinExpr = unwrap(tupleExpr);
			assertThat(joinExpr).isInstanceOf(Join.class);
			Join join = (Join) joinExpr;
			assertThat(join.getAlgorithmName())
					.withFailMessage("left=%s right=%s", join.getLeftArg().getClass(), join.getRightArg().getClass())
					.isEqualTo("LmdbIdJoinIterator");
		} finally {
			repository.shutDown();
		}
	}

	@Test
	public void mergeJoinRequestsLmdbMergeIterator(@TempDir Path tempDir) throws Exception {
		LmdbStore store = new LmdbStore(tempDir.toFile());
		SailRepository repository = new SailRepository(store);
		repository.init();

		ValueFactory vf = SimpleValueFactory.getInstance();
		IRI alice = vf.createIRI(NS, "alice");
		IRI bob = vf.createIRI(NS, "bob");
		IRI carol = vf.createIRI(NS, "carol");
		IRI knows = vf.createIRI(NS, "knows");
		IRI likes = vf.createIRI(NS, "likes");
		IRI pizza = vf.createIRI(NS, "pizza");
		IRI salad = vf.createIRI(NS, "salad");

		try (RepositoryConnection conn = repository.getConnection()) {
			conn.add(alice, knows, bob);
			conn.add(alice, likes, pizza);
			conn.add(alice, likes, salad);
			conn.add(bob, knows, carol);
			conn.add(bob, likes, salad);
		}

		String query = "SELECT ?person ?item\n" +
				"WHERE {\n" +
				"  ?person <http://example.com/knows> ?other .\n" +
				"  ?person <http://example.com/likes> ?item .\n" +
				"}";

		ParsedTupleQuery parsed = QueryParserUtil.parseTupleQuery(QueryLanguage.SPARQL, query, null);
		TupleExpr tupleExpr = parsed.getTupleExpr();

		TupleExpr joinExpr = unwrap(tupleExpr);
		assertThat(joinExpr).isInstanceOf(Join.class);
		Join join = (Join) joinExpr;
		StatementPattern left = (StatementPattern) join.getLeftArg();
		join.setOrder(left.getSubjectVar());
		join.setMergeJoin(true);

		SailSource branch = store.getBackingStore().getExplicitSailSource();
		SailDataset dataset = branch.dataset(IsolationLevels.SNAPSHOT_READ);
		try {
			SailDatasetTripleSource tripleSource = new SailDatasetTripleSource(repository.getValueFactory(), dataset);
			EvaluationStrategyFactory factory = store.getEvaluationStrategyFactory();
			EvaluationStrategy strategy = factory.createEvaluationStrategy(null, tripleSource,
					store.getBackingStore().getEvaluationStatistics());
			LmdbEvaluationDataset lmdbDataset = (LmdbEvaluationDataset) dataset;
			QueryEvaluationContext context = new LmdbQueryEvaluationContext(null,
					tripleSource.getValueFactory(), tripleSource.getComparator(), lmdbDataset,
					lmdbDataset.getValueStore());

			QueryEvaluationStep step = strategy.precompile(join, context);
			try (CloseableIteration<BindingSet> iter = step.evaluate(EmptyBindingSet.getInstance())) {
				List<? extends BindingSet> bindings = Iterations.asList(iter);
				assertThat(bindings).hasSize(3);
			}

			assertThat(join.isMergeJoin()).isTrue();
			assertThat(join.getAlgorithmName())
					.withFailMessage("left=%s right=%s", join.getLeftArg().getClass(), join.getRightArg().getClass())
					.isEqualTo("LmdbIdMergeJoinIterator");
		} finally {
			dataset.close();
			branch.close();
			repository.shutDown();
		}
	}

	@Test
	public void mergeJoinUsesArrayDatasetApi(@TempDir Path tempDir) throws Exception {
		LmdbStore store = new LmdbStore(tempDir.toFile());
		SailRepository repository = new SailRepository(store);
		repository.init();

		ValueFactory vf = SimpleValueFactory.getInstance();
		IRI alice = vf.createIRI(NS, "alice");
		IRI bob = vf.createIRI(NS, "bob");
		IRI carol = vf.createIRI(NS, "carol");
		IRI knows = vf.createIRI(NS, "knows");
		IRI likes = vf.createIRI(NS, "likes");
		IRI pizza = vf.createIRI(NS, "pizza");
		IRI salad = vf.createIRI(NS, "salad");

		try (RepositoryConnection conn = repository.getConnection()) {
			conn.add(alice, knows, bob);
			conn.add(alice, likes, pizza);
			conn.add(alice, likes, salad);
			conn.add(bob, knows, carol);
			conn.add(bob, likes, salad);
		}

		String query = "SELECT ?person ?item\n" +
				"WHERE {\n" +
				"  ?person <http://example.com/knows> ?other .\n" +
				"  ?person <http://example.com/likes> ?item .\n" +
				"}";

		ParsedTupleQuery parsed = QueryParserUtil.parseTupleQuery(QueryLanguage.SPARQL, query, null);
		TupleExpr tupleExpr = parsed.getTupleExpr();
		TupleExpr joinExpr = unwrap(tupleExpr);
		assertThat(joinExpr).isInstanceOf(Join.class);
		Join join = (Join) joinExpr;
		StatementPattern left = (StatementPattern) join.getLeftArg();
		join.setOrder(left.getSubjectVar());
		join.setMergeJoin(true);

		SailSource branch = store.getBackingStore().getExplicitSailSource();
		SailDataset dataset = branch.dataset(IsolationLevels.SNAPSHOT_READ);

		try {
			SailDatasetTripleSource tripleSource = new SailDatasetTripleSource(repository.getValueFactory(), dataset);
			EvaluationStrategyFactory factory = store.getEvaluationStrategyFactory();
			EvaluationStrategy strategy = factory.createEvaluationStrategy(null, tripleSource,
					store.getBackingStore().getEvaluationStatistics());
			LmdbEvaluationDataset lmdbDataset = (LmdbEvaluationDataset) dataset;
			RecordingDataset recordingDataset = new RecordingDataset(lmdbDataset);
			QueryEvaluationContext context = new LmdbQueryEvaluationContext(null, tripleSource.getValueFactory(),
					tripleSource.getComparator(), recordingDataset, lmdbDataset.getValueStore());

			QueryEvaluationStep step = strategy.precompile(join, context);
			try (CloseableIteration<BindingSet> iter = step.evaluate(EmptyBindingSet.getInstance())) {
				List<? extends BindingSet> bindings = Iterations.asList(iter);
				assertThat(bindings).hasSize(3);
			}

			assertThat(recordingDataset.wasLegacyOrderedApiUsed()).isFalse();
			assertThat(recordingDataset.wasArrayOrderedApiUsed()).isTrue();
		} finally {
			dataset.close();
			branch.close();
			repository.shutDown();
		}
	}

	@Test
	public void mergeJoinRespectsQueryBindings(@TempDir Path tempDir) throws Exception {
		LmdbStore store = new LmdbStore(tempDir.toFile());
		SailRepository repository = new SailRepository(store);
		repository.init();

		ValueFactory vf = SimpleValueFactory.getInstance();
		IRI painter = vf.createIRI(NS, "Painter");
		IRI painting = vf.createIRI(NS, "Painting");
		IRI paints = vf.createIRI(NS, "paints");
		IRI picasso = vf.createIRI(NS, "picasso");
		IRI guernica = vf.createIRI(NS, "guernica");

		try (RepositoryConnection conn = repository.getConnection()) {
			conn.add(painter, RDF.TYPE, RDFS.CLASS);
			conn.add(painting, RDF.TYPE, RDFS.CLASS);
			conn.add(picasso, RDF.TYPE, painter);
			conn.add(guernica, RDF.TYPE, painting);
			conn.add(picasso, paints, guernica);
		}

		String query = "SELECT ?X WHERE {\n" +
				"  ?X a ?Y .\n" +
				"  ?Y a <http://www.w3.org/2000/01/rdf-schema#Class> .\n" +
				"}";

		ParsedTupleQuery parsed = QueryParserUtil.parseTupleQuery(QueryLanguage.SPARQL, query, null);
		TupleExpr tupleExpr = parsed.getTupleExpr();
		TupleExpr joinExpr = unwrap(tupleExpr);
		assertThat(joinExpr).isInstanceOf(Join.class);
		Join join = (Join) joinExpr;

		StatementPattern left = (StatementPattern) join.getLeftArg();
		StatementPattern right = (StatementPattern) join.getRightArg();
		Var joinVar = left.getObjectVar();
		join.setOrder(joinVar);
		left.setOrder(joinVar);
		right.setOrder(right.getSubjectVar());
		join.setMergeJoin(true);

		SailSource branch = store.getBackingStore().getExplicitSailSource();
		SailDataset dataset = branch.dataset(IsolationLevels.SNAPSHOT_READ);

		try {
			SailDatasetTripleSource tripleSource = new SailDatasetTripleSource(repository.getValueFactory(), dataset);
			EvaluationStrategyFactory factory = store.getEvaluationStrategyFactory();
			EvaluationStrategy strategy = factory.createEvaluationStrategy(null, tripleSource,
					store.getBackingStore().getEvaluationStatistics());
			LmdbEvaluationDataset lmdbDataset = (LmdbEvaluationDataset) dataset;
			RecordingDataset recordingDataset = new RecordingDataset(lmdbDataset);
			QueryEvaluationContext context = new LmdbQueryEvaluationContext(null,
					tripleSource.getValueFactory(), tripleSource.getComparator(), recordingDataset,
					lmdbDataset.getValueStore());

			QueryEvaluationStep step = strategy.precompile(join, context);

			MapBindingSet bindings = new MapBindingSet(2);

			try (CloseableIteration<BindingSet> iter = step.evaluate(bindings)) {
				List<? extends BindingSet> results = Iterations.asList(iter);
				assertThat(results).hasSize(2);
				assertThat(results).extracting(bs -> bs.getValue("X")).containsExactlyInAnyOrder(picasso, guernica);
			}

			bindings.addBinding("Y", painter);
			try (CloseableIteration<BindingSet> iter = step.evaluate(bindings)) {
				List<? extends BindingSet> results = Iterations.asList(iter);
				assertThat(results).hasSize(1);
				assertThat(results.get(0).getValue("X")).isEqualTo(picasso);
			}

			bindings.addBinding("Z", painting);
			try (CloseableIteration<BindingSet> iter = step.evaluate(bindings)) {
				List<? extends BindingSet> results = Iterations.asList(iter);
				assertThat(results).hasSize(1);
				assertThat(results.get(0).getValue("X")).isEqualTo(picasso);
			}

			bindings.removeBinding("Y");
			try (CloseableIteration<BindingSet> iter = step.evaluate(bindings)) {
				List<? extends BindingSet> results = Iterations.asList(iter);
				assertThat(results).hasSize(2);
				assertThat(results).extracting(bs -> bs.getValue("X")).containsExactlyInAnyOrder(picasso, guernica);
			}

			assertThat(recordingDataset.wasArrayOrderedApiUsed()).isTrue();
			assertThat(join.getAlgorithmName()).isEqualTo("LmdbIdMergeJoinIterator");
		} finally {
			dataset.close();
			branch.close();
			repository.shutDown();
		}
	}

	@Test
	public void mergeJoinFallsBackWhenOrderUnsupported(@TempDir Path tempDir) throws Exception {
		LmdbStore store = new LmdbStore(tempDir.toFile());
		SailRepository repository = new SailRepository(store);
		repository.init();

		ValueFactory vf = SimpleValueFactory.getInstance();
		IRI alice = vf.createIRI(NS, "alice");
		IRI bob = vf.createIRI(NS, "bob");
		IRI ben = vf.createIRI(NS, "ben");
		IRI carol = vf.createIRI(NS, "carol");
		IRI dana = vf.createIRI(NS, "dana");
		IRI erin = vf.createIRI(NS, "erin");
		IRI frank = vf.createIRI(NS, "frank");
		IRI george = vf.createIRI(NS, "george");
		IRI hannah = vf.createIRI(NS, "hannah");
		IRI knows = vf.createIRI(NS, "knows");
		IRI mentors = vf.createIRI(NS, "mentors");
		IRI admires = vf.createIRI(NS, "admires");
		IRI trusts = vf.createIRI(NS, "trusts");

		try (RepositoryConnection conn = repository.getConnection()) {
			conn.add(alice, knows, ben);
			conn.add(alice, mentors, ben);
			conn.add(carol, knows, dana);
			conn.add(carol, mentors, dana);
			conn.add(erin, admires, ben);
			conn.add(george, trusts, ben);
			conn.add(frank, admires, dana);
			conn.add(hannah, trusts, dana);
		}

		String query = "SELECT ?friend ?person ?fan\n" +
				"WHERE {\n" +
				"  ?person ?predicate ?friend .\n" +
				"  ?fan ?fanPredicate ?friend .\n" +
				"}";

		ParsedTupleQuery parsed = QueryParserUtil.parseTupleQuery(QueryLanguage.SPARQL, query, null);
		TupleExpr tupleExpr = parsed.getTupleExpr();
		TupleExpr joinExpr = unwrap(tupleExpr);
		assertThat(joinExpr).isInstanceOf(Join.class);
		Join join = (Join) joinExpr;

		StatementPattern left = (StatementPattern) join.getLeftArg();
		StatementPattern right = (StatementPattern) join.getRightArg();
		Var joinVar = left.getObjectVar();
		join.setOrder(joinVar);
		left.setOrder(joinVar);
		right.setOrder(right.getObjectVar());
		join.setMergeJoin(true);
		assertThat(left.getStatementOrder()).isEqualTo(StatementOrder.O);
		assertThat(right.getStatementOrder()).isEqualTo(StatementOrder.O);

		SailSource branch = store.getBackingStore().getExplicitSailSource();
		SailDataset dataset = branch.dataset(IsolationLevels.SNAPSHOT_READ);

		try {
			SailDatasetTripleSource tripleSource = new SailDatasetTripleSource(repository.getValueFactory(), dataset);
			EvaluationStrategyFactory factory = store.getEvaluationStrategyFactory();
			EvaluationStrategy strategy = factory.createEvaluationStrategy(null, tripleSource,
					store.getBackingStore().getEvaluationStatistics());
			LmdbEvaluationDataset lmdbDataset = (LmdbEvaluationDataset) dataset;
			Method chooser = lmdbDataset.getClass()
					.getDeclaredMethod("chooseIndexForOrder", StatementOrder.class,
							long.class, long.class, long.class, long.class);
			chooser.setAccessible(true);
			Object chosen = chooser.invoke(lmdbDataset, StatementOrder.O, -1L, -1L, -1L, -1L);
			assertThat(chosen).isNull();
			QueryEvaluationContext context = new LmdbQueryEvaluationContext(null, tripleSource.getValueFactory(),
					tripleSource.getComparator(), lmdbDataset, lmdbDataset.getValueStore());

			QueryEvaluationStep step = strategy.precompile(join, context);
			try (CloseableIteration<BindingSet> iter = step.evaluate(EmptyBindingSet.getInstance())) {
				List<? extends BindingSet> bindings = Iterations.asList(iter);
				assertThat(bindings).hasSize(20);
			}

			assertThat(join.getAlgorithmName()).isNotEqualTo("LmdbIdMergeJoinIterator");
		} finally {
			dataset.close();
			branch.close();
			repository.shutDown();
		}
	}

	@Test
	public void nonStatementPatternJoinRejected() {
		Join join = new Join(new SingletonSet(),
				new StatementPattern(new Var("s"), new Var("p"), new Var("o")));

		EvaluationStrategy strategy = mock(EvaluationStrategy.class);
		QueryEvaluationContext context = new QueryEvaluationContext.Minimal(null);

		assertThatThrownBy(() -> new LmdbIdJoinQueryEvaluationStep(strategy, join, context, null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("StatementPattern");
	}

	@Test
	public void joinUsesRecordIteratorsForLeftSide(@TempDir Path tempDir) throws Exception {
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
		TupleExpr joinExpr = unwrap(tupleExpr);
		assertThat(joinExpr).isInstanceOf(Join.class);
		Join join = (Join) joinExpr;

		SailSource branch = store.getBackingStore().getExplicitSailSource();
		SailDataset dataset = branch.dataset(IsolationLevels.SNAPSHOT_READ);
		try {
			SailDatasetTripleSource tripleSource = new SailDatasetTripleSource(repository.getValueFactory(), dataset);
			EvaluationStrategyFactory factory = store.getEvaluationStrategyFactory();
			EvaluationStrategy strategy = factory.createEvaluationStrategy(null, tripleSource,
					store.getBackingStore().getEvaluationStatistics());
			LmdbEvaluationDataset lmdbDataset = (LmdbEvaluationDataset) dataset;
			QueryEvaluationContext context = new LmdbQueryEvaluationContext(null,
					tripleSource.getValueFactory(), tripleSource.getComparator(), lmdbDataset,
					lmdbDataset.getValueStore());

			LmdbIdJoinQueryEvaluationStep step = new LmdbIdJoinQueryEvaluationStep(strategy, join, context, null);

			try (CloseableIteration<BindingSet> iteration = step.evaluate(EmptyBindingSet.getInstance())) {
				Class<?> iteratorClass = iteration.getClass();
				assertThat(iteratorClass.getSimpleName()).isEqualTo("LmdbIdJoinIterator");

				Field leftIteratorField = iteratorClass.getDeclaredField("leftIterator");
				leftIteratorField.setAccessible(true);
				Object leftIterValue = leftIteratorField.get(iteration);
				assertThat(leftIterValue).isInstanceOf(RecordIterator.class);
			}
		} finally {
			dataset.close();
			branch.close();
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

	private static final class RecordingDataset implements LmdbEvaluationDataset {
		private final LmdbEvaluationDataset delegate;
		private boolean legacyOrderedApiUsed;
		private boolean arrayOrderedApiUsed;

		private RecordingDataset(LmdbEvaluationDataset delegate) {
			this.delegate = delegate;
		}

		@Override
		public RecordIterator getRecordIterator(StatementPattern pattern, BindingSet bindings)
				throws QueryEvaluationException {
			return delegate.getRecordIterator(pattern, bindings);
		}

		@Override
		public RecordIterator getRecordIterator(StatementPattern pattern, BindingSet bindings,
				KeyRangeBuffers keyBuffers) throws QueryEvaluationException {
			return delegate.getRecordIterator(pattern, bindings, keyBuffers);
		}

		@Override
		public RecordIterator getRecordIterator(long[] binding, int subjIndex, int predIndex, int objIndex,
				int ctxIndex, long[] patternIds) throws QueryEvaluationException {
			return delegate.getRecordIterator(binding, subjIndex, predIndex, objIndex, ctxIndex, patternIds);
		}

		@Override
		public RecordIterator getRecordIterator(long[] binding, int subjIndex, int predIndex, int objIndex,
				int ctxIndex, long[] patternIds, KeyRangeBuffers keyBuffers, long[] reuse, long[] quadReuse)
				throws QueryEvaluationException {
			return delegate.getRecordIterator(binding, subjIndex, predIndex, objIndex, ctxIndex, patternIds,
					keyBuffers, reuse, quadReuse);
		}

		@Override
		public RecordIterator getOrderedRecordIterator(StatementPattern pattern, BindingSet bindings,
				StatementOrder order) throws QueryEvaluationException {
			legacyOrderedApiUsed = true;
			return delegate.getOrderedRecordIterator(pattern, bindings, order);
		}

		@Override
		public RecordIterator getOrderedRecordIterator(StatementPattern pattern, BindingSet bindings,
				StatementOrder order, KeyRangeBuffers keyBuffers) throws QueryEvaluationException {
			legacyOrderedApiUsed = true;
			return delegate.getOrderedRecordIterator(pattern, bindings, order, keyBuffers);
		}

		@Override
		public RecordIterator getOrderedRecordIterator(long[] binding, int subjIndex, int predIndex, int objIndex,
				int ctxIndex, long[] patternIds, StatementOrder order) throws QueryEvaluationException {
			arrayOrderedApiUsed = true;
			return delegate.getOrderedRecordIterator(binding, subjIndex, predIndex, objIndex, ctxIndex, patternIds,
					order);
		}

		@Override
		public RecordIterator getOrderedRecordIterator(long[] binding, int subjIndex, int predIndex, int objIndex,
				int ctxIndex, long[] patternIds, StatementOrder order, KeyRangeBuffers keyBuffers, long[] bindingReuse,
				long[] quadReuse) throws QueryEvaluationException {
			arrayOrderedApiUsed = true;
			return delegate.getOrderedRecordIterator(binding, subjIndex, predIndex, objIndex, ctxIndex, patternIds,
					order, keyBuffers, bindingReuse, quadReuse);
		}

		boolean wasLegacyOrderedApiUsed() {
			return legacyOrderedApiUsed;
		}

		boolean wasArrayOrderedApiUsed() {
			return arrayOrderedApiUsed;
		}

		@Override
		public ValueStore getValueStore() {
			return delegate.getValueStore();
		}

		@Override
		public boolean hasTransactionChanges() {
			return delegate.hasTransactionChanges();
		}
	}
}
