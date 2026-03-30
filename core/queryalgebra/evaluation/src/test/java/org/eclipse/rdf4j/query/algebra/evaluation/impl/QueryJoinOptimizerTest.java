/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.UnsupportedQueryLanguageException;
import org.eclipse.rdf4j.query.algebra.BinaryTupleOperator;
import org.eclipse.rdf4j.query.algebra.Extension;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.QueryRoot;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.UnaryTupleOperator;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizerTest;
import org.eclipse.rdf4j.query.algebra.evaluation.optimizer.QueryJoinOptimizer;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.eclipse.rdf4j.query.parser.sparql.SPARQLParser;
import org.junit.jupiter.api.Test;

/**
 * Tests to monitor QueryJoinOptimizer behaviour.
 *
 * @author Mark
 */
public class QueryJoinOptimizerTest extends QueryOptimizerTest {

	@Test
	public void testBindingSetAssignmentOptimization() throws RDF4JException {
		String query = "prefix ex: <ex:>" + "select ?s ?p ?o ?x where {" + " ex:s1 ex:pred ?v. "
				+ " ex:s2 ex:pred 'bah'. {" + "  ?s ?p ?o. " + "  optional {"
				+ "   values ?x {ex:a ex:b ex:c ex:d ex:e ex:f ex:g}. " + "  }" + " }" + "}";
		// optimal order should be existence check of first statement
		// followed by left join evaluation
		String expectedQuery = "prefix ex: <ex:>" + "select ?s ?p ?o ?x where {" + " ex:s2 ex:pred 'bah'. {"
				+ "  ex:s1 ex:pred ?v. {" + "   ?s ?p ?o. " + "   optional {"
				+ "    values ?x {ex:a ex:b ex:c ex:d ex:e ex:f ex:g}. " + "   }" + "  }" + " }" + "}";

		testOptimizer(expectedQuery, query);
	}

	@Test
	public void testContextOptimization() throws RDF4JException {
		String query = "prefix ex: <ex:>" + "select ?x ?y ?z ?g ?p ?o where {" + " graph ?g {" + "  ex:s ?sp ?so. "
				+ "  ?ps ex:p ?po. " + "  ?os ?op 'ex:o'. " + " }" + " ?x ?y ?z. " + "}";
		// optimal order should be ?g graph first
		// as it is all statements about a subject in all graphs
		// rather than all subjects in the default graph:
		// card(?g) << card(?x)
		// and assuming named graph has same access cost as default graph
		String expectedQuery = "prefix ex: <ex:>" + "select ?x ?y ?z ?g ?p ?o where {" + " graph ?g {"
				+ "  ex:s ?sp ?so. " + "  ?ps ex:p ?po. " + "  ?os ?op 'ex:o'. " + " }" + " ?x ?y ?z. " + "}";

		assertThrows(AssertionError.class, () -> testOptimizer(expectedQuery, query));
	}

	@Test
	public void testSES2306AggregateOrderBy() throws Exception {
		String select = "PREFIX ex: <ex:>\n" + "SELECT ((MIN(?x+1) + MAX(?y-1))/2 AS ?r) {\n"
				+ "	?this ex:name ?n . ?this ex:id ?id . ?this ex:prop1 ?x . ?this ex:prop2 ?y .\n"
				+ "} GROUP BY concat(?n, ?id) HAVING (SUM(?x) + SUM(?y) < 5) ORDER BY (COUNT(?x) + COUNT(?y))";

		SPARQLParser parser = new SPARQLParser();
		ParsedQuery q = parser.parseQuery(select, null);
		q.getTupleExpr().visit(new AbstractQueryModelVisitor<Exception>() {

			@Override
			protected void meetUnaryTupleOperator(UnaryTupleOperator node) throws Exception {
				assertNotEquals(node, node.getArg());
				super.meetUnaryTupleOperator(node);
			}
		});
	}

	@Test
	public void testSES2116JoinBind() {

		StringBuilder qb = new StringBuilder();
		qb.append("SELECT ?subject ?name ?row {\n" + "  ?subject <http://localhost/table_1> ?uri .\n"
				+ "  BIND(STR(?uri) AS ?name)\n"
				+ "  ?table <http://linked.opendata.cz/ontology/odcs/tabular/hasRow> ?row .\n"
				+ "  ?table <http://linked.opendata.cz/ontology/odcs/tabular/symbolicName> ?name .\n" + "}");

		SPARQLParser parser = new SPARQLParser();
		ParsedQuery q = parser.parseQuery(qb.toString(), null);
		QueryJoinOptimizer opt = new QueryJoinOptimizer(new EvaluationStatistics(), new EmptyTripleSource());
		QueryRoot optRoot = new QueryRoot(q.getTupleExpr());
		opt.optimize(optRoot, null, null);
		TupleExpr leaf = findLeaf(optRoot);
		assertTrue(leaf.getParentNode() instanceof Extension, "Extension must be evaluated before StatementPattern");
	}

	@Test
	public void bindSubselectJoinOrder() {
		String query = "SELECT * WHERE {\n" + "    BIND (bnode() as ?ct01) \n" + "    { SELECT ?s WHERE {\n"
				+ "            ?s ?p ?o .\n" + "      }\n" + "      LIMIT 10\n" + "    }\n" + "}";

		SPARQLParser parser = new SPARQLParser();
		ParsedQuery q = parser.parseQuery(query, null);
		QueryJoinOptimizer opt = new QueryJoinOptimizer(new EvaluationStatistics(), new EmptyTripleSource());
		QueryRoot optRoot = new QueryRoot(q.getTupleExpr());
		opt.optimize(optRoot, null, null);

		JoinFinder joinFinder = new JoinFinder();
		optRoot.visit(joinFinder);
		Join join = joinFinder.getJoin();

		assertThat(join.getLeftArg()).as("BIND clause should be left-most argument of join")
				.isInstanceOf(Extension.class);
	}

	@Test
	public void testValues() throws RDF4JException {
		String query = String.join("\n", "",
				"prefix ex: <ex:> ",
				"select * where {",
				"	values ?x {ex:a ex:b ex:c ex:d ex:e ex:f ex:g}",
				"	?b a ?x. ",
				"}"
		);

		String expectedQuery = String.join("\n", "",
				"prefix ex: <ex:> ",
				"select * where {",
				"	values ?x {ex:a ex:b ex:c ex:d ex:e ex:f ex:g}",
				"	{",
				"		?b a ?x. ",
				"	}",
				"}"
		);

		testOptimizer(expectedQuery, query);
	}

	@Test
	public void testOptionalWithSubSelect() throws RDF4JException {
		String query = String.join("\n", "",
				"prefix ex: <ex:> ",
				"select * where {",
				"optional { ?b ex:z ?q . }",
				"{",
				"	select ?b ?a ?x where {",
				"	   ex:b ?a ?x. ",
				"      ex:b ex:a ?x. ",
				"}",
				"}",
				"}"
		);

		// we expect the subselect to be optimized too.
		// ex:b ex:a ?x.
		// ex:b ?a ?x.

		SPARQLParser parser = new SPARQLParser();
		ParsedQuery q = parser.parseQuery(query, null);
		QueryJoinOptimizer opt = new QueryJoinOptimizer(new EvaluationStatistics(), new EmptyTripleSource());
		QueryRoot optRoot = new QueryRoot(q.getTupleExpr());
		opt.optimize(optRoot, null, null);

		StatementFinder stmtFinder = new StatementFinder();
		optRoot.visit(stmtFinder);
		List<StatementPattern> stmts = stmtFinder.getStatements();

		assertEquals(stmts.size(), 3);
		assertEquals(stmts.get(0).getSubjectVar().getValue().stringValue(), "ex:b");
		assertEquals(stmts.get(0).getPredicateVar().getValue().stringValue(), "ex:a");
		assertEquals(stmts.get(0).getObjectVar().getValue(), null);
		assertEquals(stmts.get(1).getSubjectVar().getValue().stringValue(), "ex:b");
		assertEquals(stmts.get(1).getPredicateVar().getValue(), null);
		assertEquals(stmts.get(1).getObjectVar().getValue(), null);

	}

	@Test
	public void reorderJoinArgsUsesEstimatorForFirstPattern() throws Exception {
		ValueFactory vf = SimpleValueFactory.getInstance();

		StatementPattern expensive = new StatementPattern(new Var("s1"),
				new Var("p1", vf.createIRI("ex:pExpensive")), new Var("o1"));
		StatementPattern medium = new StatementPattern(new Var("s2"),
				new Var("p2", vf.createIRI("ex:pMedium")), new Var("o2"));
		StatementPattern cheap = new StatementPattern(new Var("s3"),
				new Var("p3", vf.createIRI("ex:pCheap")), new Var("o3"));

		Deque<TupleExpr> ordered = new ArrayDeque<>();
		ordered.add(expensive);
		ordered.add(medium);
		ordered.add(cheap);

		QueryJoinOptimizer optimizer = new QueryJoinOptimizer(new JoinEstimatingStatistics(), new EmptyTripleSource());
		Object joinVisitor = buildJoinVisitor(optimizer);
		Method reorderJoinArgs = joinVisitor.getClass().getDeclaredMethod("reorderJoinArgs", Deque.class);
		reorderJoinArgs.setAccessible(true);

		@SuppressWarnings("unchecked")
		Deque<TupleExpr> reordered = (Deque<TupleExpr>) reorderJoinArgs.invoke(joinVisitor, ordered);

		List<String> predicateOrder = reordered.stream()
				.map(QueryJoinOptimizerTest::getPredicateValue)
				.collect(Collectors.toList());
		assertThat(predicateOrder).containsExactlyInAnyOrder("ex:pCheap", "ex:pMedium", "ex:pExpensive");
		assertThat(predicateOrder.subList(0, 2)).containsExactlyInAnyOrder("ex:pCheap", "ex:pMedium");
		assertThat(predicateOrder.get(2)).isEqualTo("ex:pExpensive");
	}

	@Test
	public void reorderJoinArgsChoosesCheapestInitialJoinCombination() throws Exception {
		ValueFactory vf = SimpleValueFactory.getInstance();

		StatementPattern a = new StatementPattern(new Var("sa"), new Var("pa", vf.createIRI("ex:pA")),
				new Var("oa"));
		StatementPattern b = new StatementPattern(new Var("sb"), new Var("pb", vf.createIRI("ex:pB")),
				new Var("ob"));
		StatementPattern c = new StatementPattern(new Var("sc"), new Var("pc", vf.createIRI("ex:pC")),
				new Var("oc"));

		Deque<TupleExpr> ordered = new ArrayDeque<>();
		ordered.add(a);
		ordered.add(b);
		ordered.add(c);

		QueryJoinOptimizer optimizer = new QueryJoinOptimizer(new PairwiseJoinStatistics(), new EmptyTripleSource());
		Object joinVisitor = buildJoinVisitor(optimizer);
		Method reorderJoinArgs = joinVisitor.getClass().getDeclaredMethod("reorderJoinArgs", Deque.class);
		reorderJoinArgs.setAccessible(true);

		@SuppressWarnings("unchecked")
		Deque<TupleExpr> reordered = (Deque<TupleExpr>) reorderJoinArgs.invoke(joinVisitor, ordered);

		List<String> predicateOrder = reordered.stream()
				.map(QueryJoinOptimizerTest::getPredicateValue)
				.collect(Collectors.toList());
		assertThat(predicateOrder).containsExactlyInAnyOrder("ex:pA", "ex:pB", "ex:pC");
		assertThat(predicateOrder.subList(0, 2)).containsExactlyInAnyOrder("ex:pB", "ex:pC");
		assertThat(predicateOrder.get(2)).isEqualTo("ex:pA");
	}

	@Override
	public QueryJoinOptimizer getOptimizer() {
		return new QueryJoinOptimizer(new EvaluationStatistics(), new EmptyTripleSource());
	}

	private TupleExpr findLeaf(TupleExpr expr) {
		if (expr instanceof UnaryTupleOperator) {
			return findLeaf(((UnaryTupleOperator) expr).getArg());
		} else if (expr instanceof BinaryTupleOperator) {
			return findLeaf(((BinaryTupleOperator) expr).getLeftArg());
		} else {
			return expr;
		}
	}

	void testOptimizer(String expectedQuery, String actualQuery)
			throws MalformedQueryException, UnsupportedQueryLanguageException {
		ParsedQuery pq = QueryParserUtil.parseQuery(QueryLanguage.SPARQL, actualQuery, null);
		QueryJoinOptimizer opt = getOptimizer();
		QueryRoot optRoot = new QueryRoot(pq.getTupleExpr());
		opt.optimize(optRoot, null, null);

		ParsedQuery expectedParsedQuery = QueryParserUtil.parseQuery(QueryLanguage.SPARQL, expectedQuery, null);
		QueryRoot root = new QueryRoot(expectedParsedQuery.getTupleExpr());
		assertQueryModelTrees(root, optRoot);
	}

	private void assertQueryModelTrees(QueryModelNode expected, QueryModelNode actual) {
		assertEquals(expected, actual);
	}

	class JoinFinder extends AbstractQueryModelVisitor<RuntimeException> {

		private Join join;

		@Override
		public void meet(Join join) {
			this.join = join;
		}

		public Join getJoin() {
			return join;
		}
	}

	class StatementFinder extends AbstractQueryModelVisitor<RuntimeException> {

		private final List<StatementPattern> statements = new ArrayList<>();

		@Override
		public void meet(StatementPattern st) {
			this.statements.add(st);
		}

		public List<StatementPattern> getStatements() {
			return statements;
		}
	}

	private Object buildJoinVisitor(QueryJoinOptimizer optimizer) throws Exception {
		Class<?> joinVisitorClass = Class
				.forName("org.eclipse.rdf4j.query.algebra.evaluation.optimizer.QueryJoinOptimizer$JoinVisitor");
		Constructor<?> constructor = joinVisitorClass.getDeclaredConstructor(QueryJoinOptimizer.class);
		constructor.setAccessible(true);
		return constructor.newInstance(optimizer);
	}

	private static String getPredicateValue(TupleExpr expr) {
		return ((StatementPattern) expr).getPredicateVar().getValue().stringValue();
	}

	private static final class PairwiseJoinStatistics extends EvaluationStatistics {
		@Override
		public boolean supportsJoinEstimation() {
			return true;
		}

		@Override
		public double getCardinality(TupleExpr expr) {
			if (expr instanceof StatementPattern) {
				return getStatementCardinality((StatementPattern) expr);
			}

			if (expr instanceof Join) {
				return getJoinCardinality((Join) expr);
			}

			return super.getCardinality(expr);
		}

		private double getStatementCardinality(StatementPattern pattern) {
			String predicate = predicate(pattern);
			if ("ex:pA".equals(predicate)) {
				return 2;
			}
			if ("ex:pB".equals(predicate)) {
				return 3;
			}
			if ("ex:pC".equals(predicate)) {
				return 4;
			}
			return 10;
		}

		private double getJoinCardinality(Join join) {
			String left = predicate(join.getLeftArg());
			String right = predicate(join.getRightArg());

			if (left == null || right == null) {
				return super.getCardinality(join);
			}

			if ((left.equals("ex:pA") && right.equals("ex:pB")) || (left.equals("ex:pB") && right.equals("ex:pA"))) {
				return 100;
			}
			if ((left.equals("ex:pA") && right.equals("ex:pC")) || (left.equals("ex:pC") && right.equals("ex:pA"))) {
				return 80;
			}
			if ((left.equals("ex:pB") && right.equals("ex:pC")) || (left.equals("ex:pC") && right.equals("ex:pB"))) {
				return 5;
			}

			return super.getCardinality(join);
		}

		private String predicate(TupleExpr expr) {
			if (expr instanceof StatementPattern) {
				Var predicateVar = ((StatementPattern) expr).getPredicateVar();
				if (predicateVar != null && predicateVar.hasValue()) {
					return predicateVar.getValue().stringValue();
				}
			}
			return null;
		}
	}

	private static final class JoinEstimatingStatistics extends EvaluationStatistics {

		@Override
		public boolean supportsJoinEstimation() {
			return true;
		}

		@Override
		public double getCardinality(TupleExpr expr) {
			if (expr instanceof StatementPattern) {
				return getStatementCardinality((StatementPattern) expr);
			}

			if (expr instanceof Join) {
				Join join = (Join) expr;
				return getCardinality(join.getLeftArg()) * getCardinality(join.getRightArg());
			}

			return super.getCardinality(expr);
		}

		private double getStatementCardinality(StatementPattern pattern) {
			if (pattern.getPredicateVar() != null && pattern.getPredicateVar().hasValue()) {
				String predicate = pattern.getPredicateVar().getValue().stringValue();
				if (predicate.equals("ex:pCheap")) {
					return 1;
				}
				if (predicate.equals("ex:pMedium")) {
					return 10;
				}
				if (predicate.equals("ex:pExpensive")) {
					return 1000;
				}
			}

			return 100;
		}
	}

}
