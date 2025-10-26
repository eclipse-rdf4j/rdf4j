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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
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
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;
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
import org.eclipse.rdf4j.sail.lmdb.join.LmdbIdBGPQueryEvaluationStep;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for the LMDB ID BGP evaluation step.
 */
public class LmdbIdBGPEvaluationTest {

	private static final String NS = "http://example.com/";

	@Test
	public void bgpPrefersContextOverlayDataset(@TempDir java.nio.file.Path tempDir) throws Exception {
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
				LmdbIdBGPQueryEvaluationStep step = new LmdbIdBGPQueryEvaluationStep(join, patterns, ctx);
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
}
