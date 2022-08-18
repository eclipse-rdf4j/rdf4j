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
	public void testGetCardinality_ParentReferences() throws Exception {
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
		StatementPattern tupleExpr = new StatementPattern(new Var("a"), new Var("b"), new Var("c"));
		Assertions.assertFalse(tupleExpr.isCardinalitySet());

		double cardinality = new EvaluationStatistics().getCardinality(tupleExpr);
		Assertions.assertTrue(tupleExpr.isCardinalitySet());
		Assertions.assertEquals(cardinality, tupleExpr.getCardinality());
	}

	@Test
	public void testCacheCardinalityTripleRef() {
		TripleRef tupleExpr = new TripleRef(new Var("a"), new Var("b"), new Var("c"), new Var("expr"));
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
