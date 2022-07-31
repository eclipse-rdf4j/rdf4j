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
import static org.mockito.Mockito.mock;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizer;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.optimizer.StandardQueryOptimizerPipeline;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.eclipse.rdf4j.query.parser.ParsedTupleQuery;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.junit.jupiter.api.Test;

public class ParentReferenceCleanerTest {

	@Test
	public void testStandardOptimizerPipeline() {
		ParsedTupleQuery query = QueryParserUtil.parseTupleQuery(QueryLanguage.SPARQL,
				"select * where {?a ?b ?c. ?c <http://a>* ?d}", null);

		TupleExpr expr = query.getTupleExpr();

		// We are indirectly relying on the ParentReferenceCleaner being part of the standard pipeline. Not the cleanest
		// way to test, but it's a quick way to make sure that our standard pipeline at least results in a consistent
		// tree.
		StandardQueryOptimizerPipeline pipeline = new StandardQueryOptimizerPipeline(mock(EvaluationStrategy.class),
				mock(TripleSource.class), new EvaluationStatistics());

		for (QueryOptimizer optimizer : pipeline.getOptimizers()) {
			optimizer.optimize(expr, null, EmptyBindingSet.getInstance());
		}

		ParentCheckingVisitor checker = new ParentCheckingVisitor();
		expr.visit(checker);

		assertThat(checker.getInconsistentNodes()).isEmpty();
	}

	private class ParentCheckingVisitor extends AbstractQueryModelVisitor<RuntimeException> {

		private final ArrayDeque<QueryModelNode> ancestors = new ArrayDeque<>();

		private final List<QueryModelNode> inconsistentNodes = new ArrayList<>();

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
