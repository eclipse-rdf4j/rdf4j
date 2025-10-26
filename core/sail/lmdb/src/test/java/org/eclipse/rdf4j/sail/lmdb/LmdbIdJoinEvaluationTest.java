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
import java.nio.file.Path;
import java.util.List;

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
import org.eclipse.rdf4j.query.algebra.SingletonSet;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategyFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;
import org.eclipse.rdf4j.query.explanation.Explanation;
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
	public void nonStatementPatternJoinRejected() {
		Join join = new Join(new SingletonSet(),
				new StatementPattern(new Var("s"), new Var("p"), new Var("o")));

		EvaluationStrategy strategy = mock(EvaluationStrategy.class);
		QueryEvaluationContext context = new QueryEvaluationContext.Minimal(null);

		assertThatThrownBy(() -> new LmdbIdJoinQueryEvaluationStep(strategy, join, context))
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

			LmdbIdJoinQueryEvaluationStep step = new LmdbIdJoinQueryEvaluationStep(strategy, join, context);

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
}
