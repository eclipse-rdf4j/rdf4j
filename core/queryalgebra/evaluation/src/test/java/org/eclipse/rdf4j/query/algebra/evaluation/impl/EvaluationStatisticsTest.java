package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;
import org.eclipse.rdf4j.query.parser.ParsedTupleQuery;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
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
