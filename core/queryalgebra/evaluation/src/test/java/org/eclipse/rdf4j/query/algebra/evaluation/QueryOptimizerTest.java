/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.eclipse.rdf4j.query.parser.ParsedTupleQuery;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.junit.jupiter.api.Test;

public abstract class QueryOptimizerTest {

	@Test
	public void testParentReferencesConsistent_pathExpressions() {
		ParsedTupleQuery query = QueryParserUtil.parseTupleQuery(QueryLanguage.SPARQL,
				"select * where {?a ?b ?c. ?c <http://a>* ?d}", null);

		TupleExpr expr = query.getTupleExpr();

		getOptimizer().optimize(expr, null, EmptyBindingSet.getInstance());

		ParentCheckingVisitor checker = new ParentCheckingVisitor();
		expr.visit(checker);

		assertThat(checker.getInconsistentNodes()).isEmpty();
	}

	@Test
	public void testParentReferencesConsistent_filter() {
		ParsedTupleQuery query = QueryParserUtil.parseTupleQuery(QueryLanguage.SPARQL,
				"select * where {?a ?b ?c. FILTER(?c = <urn:foo>) }", null);

		TupleExpr expr = query.getTupleExpr();

		getOptimizer().optimize(expr, null, EmptyBindingSet.getInstance());

		ParentCheckingVisitor checker = new ParentCheckingVisitor();
		expr.visit(checker);

		assertThat(checker.getInconsistentNodes()).isEmpty();
	}

	@Test
	public void testParentReferencesConsistent_subselect() {
		ParsedTupleQuery query = QueryParserUtil.parseTupleQuery(QueryLanguage.SPARQL,
				"select * where {?a ?b ?c. { select ?a ?z where { ?a a ?z } }}", null);

		TupleExpr expr = query.getTupleExpr();

		getOptimizer().optimize(expr, null, EmptyBindingSet.getInstance());

		ParentCheckingVisitor checker = new ParentCheckingVisitor();
		expr.visit(checker);

		assertThat(checker.getInconsistentNodes()).isEmpty();
	}

	public abstract QueryOptimizer getOptimizer();

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
