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

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.SingletonIteration;
import org.eclipse.rdf4j.common.iteration.UnionIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.Projection;
import org.eclipse.rdf4j.query.algebra.QueryRoot;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategyFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.parser.ParsedTupleQuery;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.base.SailDataset;
import org.eclipse.rdf4j.sail.base.SailDatasetTripleSource;
import org.eclipse.rdf4j.sail.base.SailSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Verifies that the LMDB ID join optimization is disabled when the active transaction contains changes (represented via
 * an overlay dataset).
 */
public class LmdbIdJoinDisableOnChangesTest {

	private static final String NS = "http://example.com/";

	@Test
	public void idJoinDisabledWhenOverlayPresent(@TempDir java.nio.file.Path tempDir) throws Exception {
		LmdbStore store = new LmdbStore(tempDir.toFile());
		SailRepository repository = new SailRepository(store);
		repository.init();

		ValueFactory vf = SimpleValueFactory.getInstance();
		IRI alice = vf.createIRI(NS, "alice");
		IRI bob = vf.createIRI(NS, "bob");
		IRI knows = vf.createIRI(NS, "knows");
		IRI likes = vf.createIRI(NS, "likes");
		IRI pizza = vf.createIRI(NS, "pizza");

		// Baseline: only the 'knows' triple exists
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
		TupleExpr current = tupleExpr;
		if (current instanceof QueryRoot) {
			current = ((QueryRoot) current).getArg();
		}
		if (current instanceof Projection) {
			current = ((Projection) current).getArg();
		}
		if (!(current instanceof Join)) {
			throw new AssertionError("expected Join at root of algebra");
		}

		// Build an overlay triple source that contributes the missing 'likes' triple
		SailSource branch = store.getBackingStore().getExplicitSailSource();
		SailDataset baselineDataset = branch
				.dataset(org.eclipse.rdf4j.common.transaction.IsolationLevels.SNAPSHOT_READ);
		try {
			SailDatasetTripleSource baseTs = new SailDatasetTripleSource(repository.getValueFactory(), baselineDataset);
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
					@SuppressWarnings("unchecked")
					java.util.Comparator<Value> cmp = (java.util.Comparator<Value>) baseTs.getComparator();
					return cmp;
				}
			};

			// Prepare evaluation strategy over the baseline triple source
			EvaluationStrategyFactory factory = store.getEvaluationStrategyFactory();
			EvaluationStrategy strategy = factory.createEvaluationStrategy(null, baseTs,
					store.getBackingStore().getEvaluationStatistics());

			// Install overlay dataset into the evaluation strategy's thread-local so precompile can see it
			LmdbEvaluationDataset overlayDataset = new LmdbOverlayEvaluationDataset(overlayTs,
					((LmdbEvaluationDataset) baselineDataset).getValueStore());
			LmdbEvaluationStrategy.setCurrentDataset(overlayDataset);
			try {
				// Precompile the Join; since the overlay carries transaction changes the ID join must be disabled
				strategy.precompile(current);

				assertThat(current).isInstanceOf(Join.class);
				Join join = (Join) current;
				assertThat(join.getAlgorithmName()).isNotEqualTo("LmdbIdJoinIterator");
			} finally {
				LmdbEvaluationStrategy.clearCurrentDataset();
			}
		} finally {
			baselineDataset.close();
			branch.close();
			repository.shutDown();
		}
	}
}
