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
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.io.File;

import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.EvaluationStatistics;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * A filter on the inner side of a nested loop exhausts its input once per outer binding; its pass counts are
 * conditioned on the outer stream and must not train the position-blind filter surface (the 2026-08 operator-feedback
 * deferral, ported to the filter-learning path).
 */
class LmdbFilterReinvocableLearningTest {

	private static final String QUERY_TEMPLATE = """
			SELECT * WHERE {
				?person <urn:test:memberOf> ?organization .
				?person <urn:test:score> ?score .
				FILTER(?score = %s)
			}
			""";

	@Test
	void joinRightArgFilterOutcomeIsNotLearned(@TempDir File dataDir) {
		LmdbStore store = initializedStore(dataDir);
		try {
			EvaluationStatistics statistics = store.getBackingStore().getEvaluationStatistics();
			Filter correlated = filter("5");
			// The filter probes as the right arg of a join: each per-outer-binding invocation passes ~100% of the
			// rows the outer side pre-selected.
			asJoinRightArg(correlated);
			statistics.recordFilterOutcome(correlated, 100L, 0L);

			assertNotEquals(EvaluationStatistics.FilterPassEstimate.Source.LEARNED_FILTER,
					statistics.estimateFilterPass(filter("5")).getSource(),
					"A correlated inner-loop pass ratio must not train the standalone use of the same filter");
		} finally {
			store.shutDown();
		}
	}

	@Test
	void unionAndLeftArgFiltersStillLearn(@TempDir File dataDir) {
		LmdbStore store = initializedStore(dataDir);
		try {
			EvaluationStatistics statistics = store.getBackingStore().getEvaluationStatistics();
			Filter leftArg = filter("5");
			new Join(leftArg, otherPattern());
			statistics.recordFilterOutcome(leftArg, 20L, 80L);

			assertEquals(EvaluationStatistics.FilterPassEstimate.Source.LEARNED_FILTER,
					statistics.estimateFilterPass(filter("5")).getSource(),
					"Left-arg filters run once over their own input — their counts stay trustworthy");
		} finally {
			store.shutDown();
		}
	}

	@Test
	void recordPolicyRestoresOldBehavior(@TempDir File dataDir) {
		String previous = System.getProperty(LmdbReinvocablePositions.FILTER_LEARNING_REINVOCABLE_POLICY_PROPERTY);
		System.setProperty(LmdbReinvocablePositions.FILTER_LEARNING_REINVOCABLE_POLICY_PROPERTY, "record");
		try {
			LmdbStore store = initializedStore(dataDir);
			try {
				EvaluationStatistics statistics = store.getBackingStore().getEvaluationStatistics();
				Filter correlated = filter("5");
				asJoinRightArg(correlated);
				statistics.recordFilterOutcome(correlated, 100L, 0L);

				assertEquals(EvaluationStatistics.FilterPassEstimate.Source.LEARNED_FILTER,
						statistics.estimateFilterPass(filter("5")).getSource(),
						"The record policy is the pre-guard behavior");
			} finally {
				store.shutDown();
			}
		} finally {
			if (previous == null) {
				System.clearProperty(LmdbReinvocablePositions.FILTER_LEARNING_REINVOCABLE_POLICY_PROPERTY);
			} else {
				System.setProperty(LmdbReinvocablePositions.FILTER_LEARNING_REINVOCABLE_POLICY_PROPERTY, previous);
			}
		}
	}

	private static void asJoinRightArg(Filter filter) {
		new Join(otherPattern(), filter);
	}

	private static StatementPattern otherPattern() {
		return new StatementPattern(new Var("x"),
				new Var("p", SimpleValueFactory.getInstance().createIRI("urn:test:outer"), true, true),
				new Var("person"));
	}

	private static LmdbStore initializedStore(File dataDir) {
		LmdbStore store = new LmdbStore(dataDir, new LmdbStoreConfig("spoc,posc"));
		store.init();
		return store;
	}

	private static Filter filter(String constant) {
		Filter[] found = new Filter[1];
		QueryParserUtil.parseTupleQuery(QueryLanguage.SPARQL, QUERY_TEMPLATE.formatted(constant), null)
				.getTupleExpr()
				.visit(new AbstractQueryModelVisitor<RuntimeException>() {
					@Override
					public void meet(Filter node) {
						if (found[0] == null) {
							found[0] = node;
						}
						super.meet(node);
					}
				});
		return found[0];
	}
}
