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
package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.algebra.BindingSetAssignment;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TripleRef;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;
import org.eclipse.rdf4j.query.parser.ParsedTupleQuery;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class EvaluationStatisticsTest {

	@Test
	public void testGetCardinality_ParentReferences() {
		ParsedTupleQuery query = QueryParserUtil.parseTupleQuery(QueryLanguage.SPARQL,
				"select * where {?a ?b ?c. ?c <http://a>* ?d}", null);

		TupleExpr expr = query.getTupleExpr();

		TupleExpr clone = expr.clone();

		new EvaluationStatistics().getCardinality(clone);

		ParentCheckingVisitor checker = new ParentCheckingVisitor();
		clone.visit(checker);
		assertThat(checker.getInconsistentNodes()).isEmpty();

		checker.reset();
		expr.visit(checker);
		assertThat(checker.getInconsistentNodes()).isEmpty();
	}

	@Test
	public void testCacheCardinalityStatementPattern() {
		StatementPattern tupleExpr = new StatementPattern(Var.of("a"), Var.of("b"), Var.of("c"));
		Assertions.assertFalse(tupleExpr.isCardinalitySet());

		double cardinality = new EvaluationStatistics().getCardinality(tupleExpr);
		Assertions.assertTrue(tupleExpr.isCardinalitySet());
		Assertions.assertEquals(cardinality, tupleExpr.getCardinality());
	}

	@Test
	public void testCacheCardinalityTripleRef() {
		TripleRef tupleExpr = new TripleRef(Var.of("a"), Var.of("b"), Var.of("c"), Var.of("expr"));
		Assertions.assertFalse(tupleExpr.isCardinalitySet());

		double cardinality = new EvaluationStatistics().getCardinality(tupleExpr);
		Assertions.assertTrue(tupleExpr.isCardinalitySet());
		Assertions.assertEquals(cardinality, tupleExpr.getCardinality());
	}

	@Test
	public void testCacheCardinalityBindingSetAssignment() {
		BindingSetAssignment tupleExpr = new BindingSetAssignment();
		Assertions.assertFalse(tupleExpr.isCardinalitySet());

		double cardinality = new EvaluationStatistics().getCardinality(tupleExpr);
		Assertions.assertTrue(tupleExpr.isCardinalitySet());
		Assertions.assertEquals(cardinality, tupleExpr.getCardinality());
	}

	@Test
	public void costFeedbackReportsOnlyAfterCompletionAndThresholdMiss() {
		EvaluationStatistics statistics = new EvaluationStatistics();
		StatementPattern node = new StatementPattern(Var.of("s"), Var.of("p"), Var.of("o"));
		node.setCostFeedbackTrackingEnabled(true);
		node.setCostFeedbackExpectedRows(100);
		node.setCostFeedbackExpectedWorkRows(100);
		node.setCostFeedbackReportQErrorThreshold(4);
		node.setCostFeedbackActualRows(401);
		node.setCostFeedbackActualWorkRows(100);

		Assertions.assertFalse(statistics.shouldReportCostFeedback(node),
				"An unfinished node must not report learned-cost feedback");

		node.setCostFeedbackCompletedActual(true);
		Assertions.assertTrue(statistics.shouldReportCostFeedback(node),
				"A completed node should report when row q-error crosses the threshold");

		node.setCostFeedbackActualRows(399);
		Assertions.assertFalse(statistics.shouldReportCostFeedback(node),
				"Small estimate misses should stay quiet on the lightweight path");

		node.setCostFeedbackActualRows(100);
		node.setCostFeedbackActualWorkRows(401);
		Assertions.assertTrue(statistics.shouldReportCostFeedback(node),
				"A completed node should report when work q-error crosses the threshold");
	}

	@Test
	public void costFeedbackActualWorkRowsUsesCheapCounters() {
		EvaluationStatistics statistics = new EvaluationStatistics();
		Join join = new Join(new StatementPattern(Var.of("s"), Var.of("p1"), Var.of("x")),
				new StatementPattern(Var.of("x"), Var.of("p2"), Var.of("o")));
		join.setCostFeedbackTrackingEnabled(true);
		join.setCostFeedbackActualRows(10);
		join.setJoinLeftBindingsConsumedActual(120);
		join.setJoinRightBindingsConsumedActual(30);

		Assertions.assertEquals(150.0d, statistics.costFeedbackActualWorkRows(join), 0.0d,
				"Join work should be the cheap consumed-side row count, not just output rows");

		join.setCostFeedbackActualWorkRows(777);
		Assertions.assertEquals(777.0d, statistics.costFeedbackActualWorkRows(join), 0.0d,
				"A store-specific actual work value should override the generic proxy");
	}

	private class ParentCheckingVisitor extends AbstractQueryModelVisitor<RuntimeException> {

		private final ArrayDeque<QueryModelNode> ancestors = new ArrayDeque<>();

		private final List<QueryModelNode> inconsistentNodes = new ArrayList<>();

		public void reset() {
			inconsistentNodes.clear();
			ancestors.clear();
		}

		@Override
		protected void meetNode(QueryModelNode node) throws RuntimeException {
			QueryModelNode expectedParent = ancestors.peekLast();
			if (node.getParentNode() != expectedParent) {
				inconsistentNodes.add(node);
			}

			ancestors.addLast(node);
			super.meetNode(node);
			ancestors.pollLast();
		}

		public List<QueryModelNode> getInconsistentNodes() {
			return inconsistentNodes;
		}
	}
}
