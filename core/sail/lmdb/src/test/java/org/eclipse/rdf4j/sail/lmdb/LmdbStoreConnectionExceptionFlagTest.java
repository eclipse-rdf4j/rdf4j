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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.function.Supplier;

import org.eclipse.rdf4j.collection.factory.api.CollectionFactory;
import org.eclipse.rdf4j.collection.factory.impl.DefaultCollectionFactory;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolver;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.EvaluationStatistics;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.eclipse.rdf4j.query.parser.ParsedTupleQuery;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.eclipse.rdf4j.sail.SailException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Verifies that {@link LmdbStoreConnection#evaluateInternal(TupleExpr, Dataset, BindingSet, boolean)} clears the
 * connection-change flag when the delegate throws a {@link QueryEvaluationException}.
 */
public class LmdbStoreConnectionExceptionFlagTest {

	private static final String NS = "http://example.com/";

	@Test
	public void popsFlagOnQueryEvaluationException(@TempDir Path tempDir) throws Exception {
		LmdbStore store = new LmdbStore(tempDir.toFile());
		store.setEvaluationStrategyFactory(new AlwaysThrowingEvaluationStrategyFactory());
		store.init();

		try (LmdbStoreConnection connection = (LmdbStoreConnection) store.getConnection()) {
			connection.begin();

			ValueFactory vf = store.getValueFactory();
			connection.addStatement(vf.createIRI(NS, "alice"), vf.createIRI(NS, "knows"), vf.createIRI(NS, "bob"));

			ParsedTupleQuery parsed = QueryParserUtil.parseTupleQuery(QueryLanguage.SPARQL,
					"SELECT ?person WHERE { ?person ?p ?o }", null);
			TupleExpr tupleExpr = parsed.getTupleExpr();

			assertFalse(LmdbEvaluationStrategy.hasActiveConnectionChanges(),
					"precondition: no active thread-local flag");

			SailException thrown = assertThrows(SailException.class,
					() -> connection.evaluateInternal(tupleExpr, null, EmptyBindingSet.getInstance(), false));
			assertInstanceOf(QueryEvaluationException.class, thrown.getCause());

			assertFalse(LmdbEvaluationStrategy.hasActiveConnectionChanges(),
					"thread-local flag must be cleared after exception");

			connection.rollback();
		} finally {
			store.shutDown();
		}
	}

	private static final class AlwaysThrowingEvaluationStrategyFactory extends LmdbEvaluationStrategyFactory {

		private Supplier<CollectionFactory> collectionFactory = DefaultCollectionFactory::new;

		AlwaysThrowingEvaluationStrategyFactory() {
			super(null);
		}

		@Override
		public void setCollectionFactory(Supplier<CollectionFactory> collectionFactory) {
			super.setCollectionFactory(collectionFactory);
			this.collectionFactory = collectionFactory;
		}

		@Override
		public EvaluationStrategy createEvaluationStrategy(Dataset dataset, TripleSource tripleSource,
				EvaluationStatistics evaluationStatistics) {
			ThrowingEvaluationStrategy strategy = new ThrowingEvaluationStrategy(tripleSource, dataset,
					getFederatedServiceResolver(), getQuerySolutionCacheThreshold(), evaluationStatistics,
					isTrackResultSize());
			getOptimizerPipeline().ifPresent(strategy::setOptimizerPipeline);
			strategy.setCollectionFactory(collectionFactory);
			return strategy;
		}
	}

	private static final class ThrowingEvaluationStrategy extends LmdbEvaluationStrategy {

		ThrowingEvaluationStrategy(TripleSource tripleSource, Dataset dataset, FederatedServiceResolver resolver,
				long iterationCacheSyncThreshold, EvaluationStatistics evaluationStatistics, boolean trackResultSize) {
			super(tripleSource, dataset, resolver, iterationCacheSyncThreshold, evaluationStatistics, trackResultSize);
		}

		@Override
		public QueryEvaluationStep precompile(TupleExpr expr) {
			throw new QueryEvaluationException("forced failure");
		}
	}
}
