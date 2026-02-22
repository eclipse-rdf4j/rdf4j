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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.UnsupportedQueryLanguageException;
import org.eclipse.rdf4j.query.algebra.BinaryTupleOperator;
import org.eclipse.rdf4j.query.algebra.Compare;
import org.eclipse.rdf4j.query.algebra.Extension;
import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.Not;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.QueryRoot;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.UnaryTupleOperator;
import org.eclipse.rdf4j.query.algebra.ValueConstant;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizerTest;
import org.eclipse.rdf4j.query.algebra.evaluation.optimizer.QueryJoinOptimizer;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.eclipse.rdf4j.query.parser.sparql.SPARQLParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Tests to monitor QueryJoinOptimizer behaviour.
 *
 * @author Mark
 */
public class QueryJoinOptimizerTest extends QueryOptimizerTest {

	@AfterEach
	public void clearRuntimeTelemetryRegistry() {
		QueryRuntimeTelemetryRegistry.clear();
	}

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

		assertThat(reordered.removeFirst()).isSameAs(cheap);
		assertThat(reordered.removeFirst()).isSameAs(medium);
		assertThat(reordered.removeFirst()).isSameAs(expensive);
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

		assertThat(reordered.removeFirst()).isSameAs(b);
		assertThat(reordered.removeFirst()).isSameAs(c);
	}

	@Test
	public void reorderJoinArgsPrefersHistoricallySelectivePatternWhenEstimatorIsFlat() throws Exception {
		ValueFactory vf = SimpleValueFactory.getInstance();

		StatementPattern broad = new StatementPattern(new Var("s1"), new Var("p1", vf.createIRI("ex:pBroad")),
				new Var("o1"));
		broad.setSourceRowsScannedActual(1_000);
		broad.setSourceRowsMatchedActual(900);
		broad.setSourceRowsFilteredActual(100);

		StatementPattern medium = new StatementPattern(new Var("s2"), new Var("p2", vf.createIRI("ex:pMedium")),
				new Var("o2"));
		medium.setSourceRowsScannedActual(1_000);
		medium.setSourceRowsMatchedActual(300);
		medium.setSourceRowsFilteredActual(700);

		StatementPattern selective = new StatementPattern(new Var("s3"), new Var("p3", vf.createIRI("ex:pSelective")),
				new Var("o3"));
		selective.setSourceRowsScannedActual(1_000);
		selective.setSourceRowsMatchedActual(5);
		selective.setSourceRowsFilteredActual(995);

		Deque<TupleExpr> ordered = new ArrayDeque<>();
		ordered.add(broad);
		ordered.add(medium);
		ordered.add(selective);

		QueryJoinOptimizer optimizer = new QueryJoinOptimizer(new FlatJoinStatistics(), new EmptyTripleSource());
		Object joinVisitor = buildJoinVisitor(optimizer);
		Method reorderJoinArgs = joinVisitor.getClass().getDeclaredMethod("reorderJoinArgs", Deque.class);
		reorderJoinArgs.setAccessible(true);

		@SuppressWarnings("unchecked")
		Deque<TupleExpr> reordered = (Deque<TupleExpr>) reorderJoinArgs.invoke(joinVisitor, ordered);

		assertThat(reordered.removeFirst()).isSameAs(selective);
	}

	@Test
	public void reorderJoinArgsPrefersHistoricallyRescanHeavyPatternOnLeftWhenEstimatorIsFlat() throws Exception {
		ValueFactory vf = SimpleValueFactory.getInstance();

		StatementPattern stable = new StatementPattern(new Var("s1"), new Var("p1", vf.createIRI("ex:pStable")),
				new Var("o1"));
		stable.setSourceRowsScannedActual(10_000);
		stable.setSourceRowsMatchedActual(10_000);
		stable.setSourceRowsFilteredActual(0);
		stable.setJoinLeftBindingsConsumedActual(1_000);
		stable.setJoinRightBindingsConsumedActual(2_000);
		stable.setJoinRightIteratorsCreatedActual(1_000);

		StatementPattern rescanHeavy = new StatementPattern(new Var("s2"),
				new Var("p2", vf.createIRI("ex:pRescanHeavy")), new Var("o2"));
		rescanHeavy.setSourceRowsScannedActual(10_000);
		rescanHeavy.setSourceRowsMatchedActual(10_000);
		rescanHeavy.setSourceRowsFilteredActual(0);
		rescanHeavy.setJoinLeftBindingsConsumedActual(1_000);
		rescanHeavy.setJoinRightBindingsConsumedActual(1_000_000);
		rescanHeavy.setJoinRightIteratorsCreatedActual(1_000);

		Deque<TupleExpr> ordered = new ArrayDeque<>();
		ordered.add(stable);
		ordered.add(rescanHeavy);

		QueryJoinOptimizer optimizer = new QueryJoinOptimizer(new FlatJoinStatistics(), new EmptyTripleSource());
		Object joinVisitor = buildJoinVisitor(optimizer);
		Method reorderJoinArgs = joinVisitor.getClass().getDeclaredMethod("reorderJoinArgs", Deque.class);
		reorderJoinArgs.setAccessible(true);

		@SuppressWarnings("unchecked")
		Deque<TupleExpr> reordered = (Deque<TupleExpr>) reorderJoinArgs.invoke(joinVisitor, ordered);

		assertThat(reordered.removeFirst()).isSameAs(rescanHeavy);
	}

	@Test
	public void reorderJoinArgsPromotesSparseProbePatternToLeftWhenEstimatorDifferenceIsSmall() throws Exception {
		ValueFactory vf = SimpleValueFactory.getInstance();

		StatementPattern stableA = new StatementPattern(new Var("s1"), new Var("p1", vf.createIRI("ex:pStableA")),
				new Var("o1"));
		stableA.setSourceRowsScannedActual(1_000_000);
		stableA.setSourceRowsMatchedActual(1_000_000);
		stableA.setSourceRowsFilteredActual(0);
		stableA.setJoinLeftBindingsConsumedActual(100_000);
		stableA.setJoinRightBindingsConsumedActual(100_000);
		stableA.setJoinRightIteratorsCreatedActual(100_000);

		StatementPattern stableB = new StatementPattern(new Var("s2"), new Var("p2", vf.createIRI("ex:pStableB")),
				new Var("o2"));
		stableB.setSourceRowsScannedActual(1_000_000);
		stableB.setSourceRowsMatchedActual(1_000_000);
		stableB.setSourceRowsFilteredActual(0);
		stableB.setJoinLeftBindingsConsumedActual(100_000);
		stableB.setJoinRightBindingsConsumedActual(100_000);
		stableB.setJoinRightIteratorsCreatedActual(100_000);

		StatementPattern sparseProbe = new StatementPattern(new Var("s3"),
				new Var("p3", vf.createIRI("ex:pSparseProbe")),
				new Var("o3"));
		sparseProbe.setSourceRowsScannedActual(1_000_000);
		sparseProbe.setSourceRowsMatchedActual(1_000_000);
		sparseProbe.setSourceRowsFilteredActual(0);
		sparseProbe.setJoinLeftBindingsConsumedActual(100_000);
		sparseProbe.setJoinRightBindingsConsumedActual(100);
		sparseProbe.setJoinRightIteratorsCreatedActual(100_000);

		Deque<TupleExpr> ordered = new ArrayDeque<>();
		ordered.add(stableA);
		ordered.add(stableB);
		ordered.add(sparseProbe);

		QueryJoinOptimizer optimizer = new QueryJoinOptimizer(new SlightEstimatorBiasStatistics(),
				new EmptyTripleSource());
		Object joinVisitor = buildJoinVisitor(optimizer);
		Method reorderJoinArgs = joinVisitor.getClass().getDeclaredMethod("reorderJoinArgs", Deque.class);
		reorderJoinArgs.setAccessible(true);

		@SuppressWarnings("unchecked")
		Deque<TupleExpr> reordered = (Deque<TupleExpr>) reorderJoinArgs.invoke(joinVisitor, ordered);

		assertThat(reordered.removeFirst()).isSameAs(sparseProbe);
	}

	@Test
	public void reorderJoinArgsConsidersSparseUrgencyEvenWhenCandidateSetIsCardinalityTrimmed() throws Exception {
		ValueFactory vf = SimpleValueFactory.getInstance();

		StatementPattern low1 = new StatementPattern(new Var("s1"), new Var("p1", vf.createIRI("ex:pLow1")),
				new Var("o1"));
		StatementPattern low2 = new StatementPattern(new Var("s2"), new Var("p2", vf.createIRI("ex:pLow2")),
				new Var("o2"));
		StatementPattern low3 = new StatementPattern(new Var("s3"), new Var("p3", vf.createIRI("ex:pLow3")),
				new Var("o3"));
		StatementPattern mid4 = new StatementPattern(new Var("s4"), new Var("p4", vf.createIRI("ex:pMid4")),
				new Var("o4"));
		StatementPattern mid5 = new StatementPattern(new Var("s5"), new Var("p5", vf.createIRI("ex:pMid5")),
				new Var("o5"));
		StatementPattern mid6 = new StatementPattern(new Var("s6"), new Var("p6", vf.createIRI("ex:pMid6")),
				new Var("o6"));

		StatementPattern sparseOutlier = new StatementPattern(new Var("s7"),
				new Var("p7", vf.createIRI("ex:pSparseOutlier")), new Var("o7"));
		sparseOutlier.setSourceRowsScannedActual(1_000_000);
		sparseOutlier.setSourceRowsMatchedActual(1_000_000);
		sparseOutlier.setSourceRowsFilteredActual(0);
		sparseOutlier.setJoinLeftBindingsConsumedActual(200_000);
		sparseOutlier.setJoinRightBindingsConsumedActual(200);
		sparseOutlier.setJoinRightIteratorsCreatedActual(200_000);

		Deque<TupleExpr> ordered = new ArrayDeque<>();
		ordered.add(low1);
		ordered.add(low2);
		ordered.add(low3);
		ordered.add(mid4);
		ordered.add(mid5);
		ordered.add(mid6);
		ordered.add(sparseOutlier);

		QueryJoinOptimizer optimizer = new QueryJoinOptimizer(new AsymmetricSparseStartPairStatistics(),
				new EmptyTripleSource());
		Object joinVisitor = buildJoinVisitor(optimizer);
		Method reorderJoinArgs = joinVisitor.getClass().getDeclaredMethod("reorderJoinArgs", Deque.class);
		reorderJoinArgs.setAccessible(true);

		@SuppressWarnings("unchecked")
		Deque<TupleExpr> reordered = (Deque<TupleExpr>) reorderJoinArgs.invoke(joinVisitor, ordered);

		assertThat(reordered.removeFirst()).isSameAs(sparseOutlier);
	}

	@Test
	public void reorderJoinArgsDoesNotOverreactToLowSampleDenseFanoutTelemetry() throws Exception {
		ValueFactory vf = SimpleValueFactory.getInstance();

		StatementPattern stableDirectionalWinner = new StatementPattern(new Var("s1"),
				new Var("p1", vf.createIRI("ex:pStableDirectionalWinner")), new Var("o1"));

		StatementPattern lowSampleDenseFanout = new StatementPattern(new Var("s2"),
				new Var("p2", vf.createIRI("ex:pLowSampleDenseFanoutReorder")), new Var("o2"));
		lowSampleDenseFanout.setSourceRowsScannedActual(10_000);
		lowSampleDenseFanout.setSourceRowsMatchedActual(10_000);
		lowSampleDenseFanout.setSourceRowsFilteredActual(0);
		lowSampleDenseFanout.setJoinLeftBindingsConsumedActual(3);
		lowSampleDenseFanout.setJoinRightBindingsConsumedActual(60);
		lowSampleDenseFanout.setJoinRightIteratorsCreatedActual(3);

		Deque<TupleExpr> ordered = new ArrayDeque<>();
		ordered.add(stableDirectionalWinner);
		ordered.add(lowSampleDenseFanout);

		QueryJoinOptimizer optimizer = new QueryJoinOptimizer(new LowSampleAsymmetricJoinStatistics(),
				new EmptyTripleSource());
		Object joinVisitor = buildJoinVisitor(optimizer);
		Method reorderJoinArgs = joinVisitor.getClass().getDeclaredMethod("reorderJoinArgs", Deque.class);
		reorderJoinArgs.setAccessible(true);

		@SuppressWarnings("unchecked")
		Deque<TupleExpr> reordered = (Deque<TupleExpr>) reorderJoinArgs.invoke(joinVisitor, ordered);

		assertThat(reordered.removeFirst()).isSameAs(stableDirectionalWinner);
	}

	@Test
	public void selectNextTupleExprPrefersHistoricallyRescanHeavyPatternWhenSignalsAreOtherwiseEqual()
			throws Exception {
		ValueFactory vf = SimpleValueFactory.getInstance();

		StatementPattern stable = new StatementPattern(new Var("s1"), new Var("p1", vf.createIRI("ex:pStable")),
				new Var("o1"));
		stable.setSourceRowsScannedActual(10_000);
		stable.setSourceRowsMatchedActual(10_000);
		stable.setSourceRowsFilteredActual(0);
		stable.setJoinLeftBindingsConsumedActual(1_000);
		stable.setJoinRightBindingsConsumedActual(2_000);
		stable.setJoinRightIteratorsCreatedActual(1_000);

		StatementPattern rescanHeavy = new StatementPattern(new Var("s2"),
				new Var("p2", vf.createIRI("ex:pRescanHeavy")), new Var("o2"));
		rescanHeavy.setSourceRowsScannedActual(10_000);
		rescanHeavy.setSourceRowsMatchedActual(10_000);
		rescanHeavy.setSourceRowsFilteredActual(0);
		rescanHeavy.setJoinLeftBindingsConsumedActual(1_000);
		rescanHeavy.setJoinRightBindingsConsumedActual(1_000_000);
		rescanHeavy.setJoinRightIteratorsCreatedActual(1_000);

		List<TupleExpr> expressions = new ArrayList<>();
		expressions.add(stable);
		expressions.add(rescanHeavy);

		Map<TupleExpr, Double> cardinalityMap = new java.util.HashMap<>();
		cardinalityMap.put(stable, 100.0);
		cardinalityMap.put(rescanHeavy, 100.0);

		Map<TupleExpr, List<Var>> varsMap = new java.util.HashMap<>();
		varsMap.put(stable, stable.getVarList());
		varsMap.put(rescanHeavy, rescanHeavy.getVarList());

		Map<Var, Integer> varFreqMap = new java.util.HashMap<>();
		for (Var var : stable.getVarList()) {
			varFreqMap.put(var, 1);
		}
		for (Var var : rescanHeavy.getVarList()) {
			varFreqMap.put(var, 1);
		}

		QueryJoinOptimizer optimizer = new QueryJoinOptimizer(new FlatJoinStatistics(), new EmptyTripleSource());
		Object joinVisitor = buildJoinVisitor(optimizer);
		Method selectNextTupleExpr = joinVisitor.getClass()
				.getDeclaredMethod("selectNextTupleExpr", List.class,
						Map.class, Map.class, Map.class);
		selectNextTupleExpr.setAccessible(true);

		TupleExpr selected = (TupleExpr) selectNextTupleExpr
				.invoke(joinVisitor, expressions, cardinalityMap, varsMap, varFreqMap);

		assertThat(selected).isSameAs(rescanHeavy);
	}

	@Test
	public void selectNextTupleExprPrefersIteratorWasteHeavyPatternOverFanoutHeavyPattern() throws Exception {
		ValueFactory vf = SimpleValueFactory.getInstance();

		StatementPattern fanoutHeavy = new StatementPattern(new Var("s1"),
				new Var("p1", vf.createIRI("ex:pFanoutHeavy")), new Var("o1"));
		fanoutHeavy.setSourceRowsScannedActual(10_000);
		fanoutHeavy.setSourceRowsMatchedActual(10_000);
		fanoutHeavy.setSourceRowsFilteredActual(0);
		fanoutHeavy.setJoinLeftBindingsConsumedActual(1_000);
		fanoutHeavy.setJoinRightBindingsConsumedActual(1_000_000);
		fanoutHeavy.setJoinRightIteratorsCreatedActual(1_000);

		StatementPattern iteratorWasteHeavy = new StatementPattern(new Var("s2"),
				new Var("p2", vf.createIRI("ex:pIteratorWasteHeavy")), new Var("o2"));
		iteratorWasteHeavy.setSourceRowsScannedActual(10_000);
		iteratorWasteHeavy.setSourceRowsMatchedActual(10_000);
		iteratorWasteHeavy.setSourceRowsFilteredActual(0);
		iteratorWasteHeavy.setJoinLeftBindingsConsumedActual(1_000);
		iteratorWasteHeavy.setJoinRightBindingsConsumedActual(100);
		iteratorWasteHeavy.setJoinRightIteratorsCreatedActual(100_000);

		List<TupleExpr> expressions = new ArrayList<>();
		expressions.add(fanoutHeavy);
		expressions.add(iteratorWasteHeavy);

		Map<TupleExpr, Double> cardinalityMap = new java.util.HashMap<>();
		cardinalityMap.put(fanoutHeavy, 100.0);
		cardinalityMap.put(iteratorWasteHeavy, 100.0);

		Map<TupleExpr, List<Var>> varsMap = new java.util.HashMap<>();
		varsMap.put(fanoutHeavy, fanoutHeavy.getVarList());
		varsMap.put(iteratorWasteHeavy, iteratorWasteHeavy.getVarList());

		Map<Var, Integer> varFreqMap = new java.util.HashMap<>();
		for (Var var : fanoutHeavy.getVarList()) {
			varFreqMap.put(var, 1);
		}
		for (Var var : iteratorWasteHeavy.getVarList()) {
			varFreqMap.put(var, 1);
		}

		QueryJoinOptimizer optimizer = new QueryJoinOptimizer(new FlatJoinStatistics(), new EmptyTripleSource());
		Object joinVisitor = buildJoinVisitor(optimizer);
		Method selectNextTupleExpr = joinVisitor.getClass()
				.getDeclaredMethod("selectNextTupleExpr", List.class, Map.class, Map.class, Map.class);
		selectNextTupleExpr.setAccessible(true);

		TupleExpr selected = (TupleExpr) selectNextTupleExpr
				.invoke(joinVisitor, expressions, cardinalityMap, varsMap, varFreqMap);

		assertThat(selected).isSameAs(iteratorWasteHeavy);
	}

	@Test
	public void selectNextTupleExprUsesHistoricalRuntimeTelemetryWhenCurrentNodeTelemetryIsMissing() throws Exception {
		ValueFactory vf = SimpleValueFactory.getInstance();

		StatementPattern stable = new StatementPattern(new Var("s1"), new Var("p1", vf.createIRI("ex:pStable")),
				new Var("o1"));
		StatementPattern rescanHeavy = new StatementPattern(new Var("s2"),
				new Var("p2", vf.createIRI("ex:pRescanHeavy")), new Var("o2"));

		StatementPattern stableHistorical = stable.clone();
		stableHistorical.setJoinLeftBindingsConsumedActual(1_000);
		stableHistorical.setJoinRightBindingsConsumedActual(2_000);
		stableHistorical.setJoinRightIteratorsCreatedActual(1_000);
		QueryRuntimeTelemetryRegistry.record(stableHistorical);

		StatementPattern rescanHeavyHistorical = rescanHeavy.clone();
		rescanHeavyHistorical.setJoinLeftBindingsConsumedActual(1_000);
		rescanHeavyHistorical.setJoinRightBindingsConsumedActual(1_000_000);
		rescanHeavyHistorical.setJoinRightIteratorsCreatedActual(1_000);
		QueryRuntimeTelemetryRegistry.record(rescanHeavyHistorical);

		List<TupleExpr> expressions = new ArrayList<>();
		expressions.add(stable);
		expressions.add(rescanHeavy);

		Map<TupleExpr, Double> cardinalityMap = new java.util.HashMap<>();
		cardinalityMap.put(stable, 100.0);
		cardinalityMap.put(rescanHeavy, 100.0);

		Map<TupleExpr, List<Var>> varsMap = new java.util.HashMap<>();
		varsMap.put(stable, stable.getVarList());
		varsMap.put(rescanHeavy, rescanHeavy.getVarList());

		Map<Var, Integer> varFreqMap = new java.util.HashMap<>();
		for (Var var : stable.getVarList()) {
			varFreqMap.put(var, 1);
		}
		for (Var var : rescanHeavy.getVarList()) {
			varFreqMap.put(var, 1);
		}

		QueryJoinOptimizer optimizer = new QueryJoinOptimizer(new FlatJoinStatistics(), new EmptyTripleSource());
		Object joinVisitor = buildJoinVisitor(optimizer);
		Method selectNextTupleExpr = joinVisitor.getClass()
				.getDeclaredMethod("selectNextTupleExpr", List.class, Map.class, Map.class, Map.class);
		selectNextTupleExpr.setAccessible(true);

		TupleExpr selected = (TupleExpr) selectNextTupleExpr
				.invoke(joinVisitor, expressions, cardinalityMap, varsMap, varFreqMap);

		assertThat(selected).isSameAs(rescanHeavy);
	}

	@Test
	public void selectNextTupleExprUsesHistoricalFilterTelemetryWhenCurrentNodeTelemetryIsMissing() throws Exception {
		ValueFactory vf = SimpleValueFactory.getInstance();

		StatementPattern stable = new StatementPattern(new Var("s1"), new Var("p1", vf.createIRI("ex:pStable")),
				new Var("o1"));
		Filter sparseFilter = new Filter(
				new StatementPattern(new Var("s2"), new Var("p2", vf.createIRI("ex:pDate")), new Var("date")),
				new Compare(new Var("date"), new ValueConstant(vf.createLiteral("2024-01-01")),
						Compare.CompareOp.EQ));

		Filter sparseFilterHistorical = sparseFilter.clone();
		sparseFilterHistorical.setJoinLeftBindingsConsumedActual(40_000);
		sparseFilterHistorical.setJoinRightBindingsConsumedActual(200);
		sparseFilterHistorical.setJoinRightIteratorsCreatedActual(40_000);
		QueryRuntimeTelemetryRegistry.record(sparseFilterHistorical);

		List<TupleExpr> expressions = new ArrayList<>();
		expressions.add(stable);
		expressions.add(sparseFilter);

		Map<TupleExpr, Double> cardinalityMap = new java.util.HashMap<>();
		cardinalityMap.put(stable, 100.0);
		cardinalityMap.put(sparseFilter, 100.0);

		Map<TupleExpr, List<Var>> varsMap = new java.util.HashMap<>();
		varsMap.put(stable, stable.getVarList());
		List<Var> sparseFilterVars = List.of(new Var("s2"), new Var("date"));
		varsMap.put(sparseFilter, sparseFilterVars);

		Map<Var, Integer> varFreqMap = new java.util.HashMap<>();
		for (Var var : stable.getVarList()) {
			varFreqMap.put(var, 1);
		}
		for (Var var : sparseFilterVars) {
			varFreqMap.put(var, 1);
		}

		QueryJoinOptimizer optimizer = new QueryJoinOptimizer(new FlatJoinStatistics(), new EmptyTripleSource());
		Object joinVisitor = buildJoinVisitor(optimizer);
		Method selectNextTupleExpr = joinVisitor.getClass()
				.getDeclaredMethod("selectNextTupleExpr", List.class, Map.class, Map.class, Map.class);
		selectNextTupleExpr.setAccessible(true);

		TupleExpr selected = (TupleExpr) selectNextTupleExpr
				.invoke(joinVisitor, expressions, cardinalityMap, varsMap, varFreqMap);

		assertThat(selected).isSameAs(sparseFilter);
	}

	@Test
	public void selectNextTupleExprPrefersHistoricallySelectiveFilterOverSiblingStatementPatterns() throws Exception {
		ValueFactory vf = SimpleValueFactory.getInstance();

		StatementPattern encounterType = new StatementPattern(new Var("enc"),
				new Var("pType", vf.createIRI("ex:pType")),
				new Var("encType"));
		StatementPattern handledBy = new StatementPattern(new Var("enc"),
				new Var("pHandledBy", vf.createIRI("ex:pHandledBy")),
				new Var("practitioner"));

		Filter recordedOnFilter = new Filter(
				new StatementPattern(new Var("enc"), new Var("pRecordedOn", vf.createIRI("ex:pRecordedOn")),
						new Var("date")),
				new Compare(new Var("date"), new ValueConstant(vf.createLiteral("2024-01-01")),
						Compare.CompareOp.EQ));

		Filter historicalRecordedOnFilter = recordedOnFilter.clone();
		historicalRecordedOnFilter.setJoinLeftBindingsConsumedActual(25_000);
		historicalRecordedOnFilter.setJoinRightBindingsConsumedActual(135);
		historicalRecordedOnFilter.setJoinRightIteratorsCreatedActual(25_000);
		QueryRuntimeTelemetryRegistry.record(historicalRecordedOnFilter);

		List<TupleExpr> expressions = new ArrayList<>();
		expressions.add(encounterType);
		expressions.add(handledBy);
		expressions.add(recordedOnFilter);

		Map<TupleExpr, Double> cardinalityMap = new java.util.HashMap<>();
		cardinalityMap.put(encounterType, 25_000.0);
		cardinalityMap.put(handledBy, 25_000.0);
		cardinalityMap.put(recordedOnFilter, 25_000.0);

		Map<TupleExpr, List<Var>> varsMap = new java.util.HashMap<>();
		varsMap.put(encounterType, encounterType.getVarList());
		varsMap.put(handledBy, handledBy.getVarList());
		List<Var> recordedOnFilterVars = List.of(new Var("enc"), new Var("date"));
		varsMap.put(recordedOnFilter, recordedOnFilterVars);

		Map<Var, Integer> varFreqMap = new java.util.HashMap<>();
		for (List<Var> vars : varsMap.values()) {
			for (Var var : vars) {
				varFreqMap.merge(var, 1, Integer::sum);
			}
		}

		QueryJoinOptimizer optimizer = new QueryJoinOptimizer(new FlatJoinStatistics(), new EmptyTripleSource());
		Object joinVisitor = buildJoinVisitor(optimizer);
		Method selectNextTupleExpr = joinVisitor.getClass()
				.getDeclaredMethod("selectNextTupleExpr", List.class, Map.class, Map.class, Map.class);
		selectNextTupleExpr.setAccessible(true);

		TupleExpr selected = (TupleExpr) selectNextTupleExpr
				.invoke(joinVisitor, expressions, cardinalityMap, varsMap, varFreqMap);

		assertThat(selected).isSameAs(recordedOnFilter);
	}

	@Test
	public void selectNextTupleExprUsesFilterHistoryToBiasChildStatementPatternWhenOnlyPatternsRemain()
			throws Exception {
		ValueFactory vf = SimpleValueFactory.getInstance();

		StatementPattern stable = new StatementPattern(new Var("enc"),
				new Var("pStable", vf.createIRI("ex:pStable")),
				new Var("stableValue"));
		StatementPattern recordedOn = new StatementPattern(new Var("enc"),
				new Var("pRecordedOn", vf.createIRI("ex:pRecordedOn")),
				new Var("date"));

		Filter recordedOnFilterHistory = new Filter(recordedOn.clone(),
				new Compare(new Var("date"), new ValueConstant(vf.createLiteral("2024-01-01")),
						Compare.CompareOp.EQ));
		recordedOnFilterHistory.setJoinLeftBindingsConsumedActual(30_000);
		recordedOnFilterHistory.setJoinRightBindingsConsumedActual(120);
		recordedOnFilterHistory.setJoinRightIteratorsCreatedActual(30_000);
		QueryRuntimeTelemetryRegistry.record(recordedOnFilterHistory);

		List<TupleExpr> expressions = new ArrayList<>();
		expressions.add(stable);
		expressions.add(recordedOn);

		Map<TupleExpr, Double> cardinalityMap = new java.util.HashMap<>();
		cardinalityMap.put(stable, 25_000.0);
		cardinalityMap.put(recordedOn, 25_000.0);

		Map<TupleExpr, List<Var>> varsMap = new java.util.HashMap<>();
		varsMap.put(stable, stable.getVarList());
		varsMap.put(recordedOn, recordedOn.getVarList());

		Map<Var, Integer> varFreqMap = new java.util.HashMap<>();
		for (List<Var> vars : varsMap.values()) {
			for (Var var : vars) {
				varFreqMap.merge(var, 1, Integer::sum);
			}
		}

		QueryJoinOptimizer optimizer = new QueryJoinOptimizer(new FlatJoinStatistics(), new EmptyTripleSource());
		Object joinVisitor = buildJoinVisitor(optimizer);
		Method selectNextTupleExpr = joinVisitor.getClass()
				.getDeclaredMethod("selectNextTupleExpr", List.class, Map.class, Map.class, Map.class);
		selectNextTupleExpr.setAccessible(true);

		TupleExpr selected = (TupleExpr) selectNextTupleExpr
				.invoke(joinVisitor, expressions, cardinalityMap, varsMap, varFreqMap);

		assertThat(selected).isSameAs(recordedOn);
	}

	@Test
	public void selectNextTupleExprPrefersHighScanRescanPatternOnLeftWhenJoinChurnMatches() throws Exception {
		ValueFactory vf = SimpleValueFactory.getInstance();

		StatementPattern lowScan = new StatementPattern(new Var("s1"),
				new Var("p1", vf.createIRI("ex:pLowScan")), new Var("o1"));
		lowScan.setSourceRowsScannedActual(20_000);
		lowScan.setSourceRowsMatchedActual(20_000);
		lowScan.setSourceRowsFilteredActual(0);
		lowScan.setJoinLeftBindingsConsumedActual(1_000);
		lowScan.setJoinRightBindingsConsumedActual(1_000);
		lowScan.setJoinRightIteratorsCreatedActual(100_000);

		StatementPattern highScan = new StatementPattern(new Var("s2"),
				new Var("p2", vf.createIRI("ex:pHighScan")), new Var("o2"));
		highScan.setSourceRowsScannedActual(2_000_000);
		highScan.setSourceRowsMatchedActual(2_000_000);
		highScan.setSourceRowsFilteredActual(0);
		highScan.setJoinLeftBindingsConsumedActual(1_000);
		highScan.setJoinRightBindingsConsumedActual(1_000);
		highScan.setJoinRightIteratorsCreatedActual(100_000);

		List<TupleExpr> expressions = new ArrayList<>();
		expressions.add(lowScan);
		expressions.add(highScan);

		Map<TupleExpr, Double> cardinalityMap = new java.util.HashMap<>();
		cardinalityMap.put(lowScan, 100.0);
		cardinalityMap.put(highScan, 100.0);

		Map<TupleExpr, List<Var>> varsMap = new java.util.HashMap<>();
		varsMap.put(lowScan, lowScan.getVarList());
		varsMap.put(highScan, highScan.getVarList());

		Map<Var, Integer> varFreqMap = new java.util.HashMap<>();
		for (Var var : lowScan.getVarList()) {
			varFreqMap.put(var, 1);
		}
		for (Var var : highScan.getVarList()) {
			varFreqMap.put(var, 1);
		}

		QueryJoinOptimizer optimizer = new QueryJoinOptimizer(new FlatJoinStatistics(), new EmptyTripleSource());
		Object joinVisitor = buildJoinVisitor(optimizer);
		Method selectNextTupleExpr = joinVisitor.getClass()
				.getDeclaredMethod("selectNextTupleExpr", List.class, Map.class, Map.class, Map.class);
		selectNextTupleExpr.setAccessible(true);

		TupleExpr selected = (TupleExpr) selectNextTupleExpr
				.invoke(joinVisitor, expressions, cardinalityMap, varsMap, varFreqMap);

		assertThat(selected).isSameAs(highScan);
	}

	@Test
	public void selectNextTupleExprPrefersAbsoluteRightChurnPatternWhenRatiosMatch() throws Exception {
		ValueFactory vf = SimpleValueFactory.getInstance();

		StatementPattern lowerAbsoluteChurn = new StatementPattern(new Var("s1"),
				new Var("p1", vf.createIRI("ex:pLowerAbsoluteChurn")), new Var("o1"));
		lowerAbsoluteChurn.setSourceRowsScannedActual(10_000);
		lowerAbsoluteChurn.setSourceRowsMatchedActual(10_000);
		lowerAbsoluteChurn.setSourceRowsFilteredActual(0);
		lowerAbsoluteChurn.setJoinLeftBindingsConsumedActual(1_000);
		lowerAbsoluteChurn.setJoinRightBindingsConsumedActual(10_000);
		lowerAbsoluteChurn.setJoinRightIteratorsCreatedActual(1_000);

		StatementPattern higherAbsoluteChurn = new StatementPattern(new Var("s2"),
				new Var("p2", vf.createIRI("ex:pHigherAbsoluteChurn")), new Var("o2"));
		higherAbsoluteChurn.setSourceRowsScannedActual(1_000_000);
		higherAbsoluteChurn.setSourceRowsMatchedActual(1_000_000);
		higherAbsoluteChurn.setSourceRowsFilteredActual(0);
		higherAbsoluteChurn.setJoinLeftBindingsConsumedActual(100_000);
		higherAbsoluteChurn.setJoinRightBindingsConsumedActual(1_000_000);
		higherAbsoluteChurn.setJoinRightIteratorsCreatedActual(100_000);

		List<TupleExpr> expressions = new ArrayList<>();
		expressions.add(lowerAbsoluteChurn);
		expressions.add(higherAbsoluteChurn);

		Map<TupleExpr, Double> cardinalityMap = new java.util.HashMap<>();
		cardinalityMap.put(lowerAbsoluteChurn, 100.0);
		cardinalityMap.put(higherAbsoluteChurn, 100.0);

		Map<TupleExpr, List<Var>> varsMap = new java.util.HashMap<>();
		varsMap.put(lowerAbsoluteChurn, lowerAbsoluteChurn.getVarList());
		varsMap.put(higherAbsoluteChurn, higherAbsoluteChurn.getVarList());

		Map<Var, Integer> varFreqMap = new java.util.HashMap<>();
		for (Var var : lowerAbsoluteChurn.getVarList()) {
			varFreqMap.put(var, 1);
		}
		for (Var var : higherAbsoluteChurn.getVarList()) {
			varFreqMap.put(var, 1);
		}

		QueryJoinOptimizer optimizer = new QueryJoinOptimizer(new FlatJoinStatistics(), new EmptyTripleSource());
		Object joinVisitor = buildJoinVisitor(optimizer);
		Method selectNextTupleExpr = joinVisitor.getClass()
				.getDeclaredMethod("selectNextTupleExpr", List.class, Map.class, Map.class, Map.class);
		selectNextTupleExpr.setAccessible(true);

		TupleExpr selected = (TupleExpr) selectNextTupleExpr
				.invoke(joinVisitor, expressions, cardinalityMap, varsMap, varFreqMap);

		assertThat(selected).isSameAs(higherAbsoluteChurn);
	}

	@Test
	public void selectNextTupleExprPrefersSparseRightMatchPatternWhenIteratorMetricsMissing() throws Exception {
		ValueFactory vf = SimpleValueFactory.getInstance();

		StatementPattern sparseRightMatches = new StatementPattern(new Var("s1"),
				new Var("p1", vf.createIRI("ex:pSparseRightMatches")), new Var("o1"));
		sparseRightMatches.setSourceRowsScannedActual(1_000_000);
		sparseRightMatches.setSourceRowsMatchedActual(1_000_000);
		sparseRightMatches.setSourceRowsFilteredActual(0);
		sparseRightMatches.setJoinLeftBindingsConsumedActual(100_000);
		sparseRightMatches.setJoinRightBindingsConsumedActual(1_000);

		StatementPattern denseRightMatches = new StatementPattern(new Var("s2"),
				new Var("p2", vf.createIRI("ex:pDenseRightMatches")), new Var("o2"));
		denseRightMatches.setSourceRowsScannedActual(1_000_000);
		denseRightMatches.setSourceRowsMatchedActual(1_000_000);
		denseRightMatches.setSourceRowsFilteredActual(0);
		denseRightMatches.setJoinLeftBindingsConsumedActual(100_000);
		denseRightMatches.setJoinRightBindingsConsumedActual(100_000);

		List<TupleExpr> expressions = new ArrayList<>();
		expressions.add(sparseRightMatches);
		expressions.add(denseRightMatches);

		Map<TupleExpr, Double> cardinalityMap = new java.util.HashMap<>();
		cardinalityMap.put(sparseRightMatches, 100.0);
		cardinalityMap.put(denseRightMatches, 100.0);

		Map<TupleExpr, List<Var>> varsMap = new java.util.HashMap<>();
		varsMap.put(sparseRightMatches, sparseRightMatches.getVarList());
		varsMap.put(denseRightMatches, denseRightMatches.getVarList());

		Map<Var, Integer> varFreqMap = new java.util.HashMap<>();
		for (Var var : sparseRightMatches.getVarList()) {
			varFreqMap.put(var, 1);
		}
		for (Var var : denseRightMatches.getVarList()) {
			varFreqMap.put(var, 1);
		}

		QueryJoinOptimizer optimizer = new QueryJoinOptimizer(new FlatJoinStatistics(), new EmptyTripleSource());
		Object joinVisitor = buildJoinVisitor(optimizer);
		Method selectNextTupleExpr = joinVisitor.getClass()
				.getDeclaredMethod("selectNextTupleExpr", List.class, Map.class, Map.class, Map.class);
		selectNextTupleExpr.setAccessible(true);

		TupleExpr selected = (TupleExpr) selectNextTupleExpr
				.invoke(joinVisitor, expressions, cardinalityMap, varsMap, varFreqMap);

		assertThat(selected).isSameAs(sparseRightMatches);
	}

	@Test
	public void selectNextTupleExprPrefersSparseRightMatchPatternWhenDenseAlternativeHasHighRightFanout()
			throws Exception {
		ValueFactory vf = SimpleValueFactory.getInstance();

		StatementPattern sparseRightMatches = new StatementPattern(new Var("s1"),
				new Var("p1", vf.createIRI("ex:pSparseRightMatchesWithIterators")), new Var("o1"));
		sparseRightMatches.setSourceRowsScannedActual(1_000_000);
		sparseRightMatches.setSourceRowsMatchedActual(1_000_000);
		sparseRightMatches.setSourceRowsFilteredActual(0);
		sparseRightMatches.setJoinLeftBindingsConsumedActual(100_000);
		sparseRightMatches.setJoinRightBindingsConsumedActual(500);
		sparseRightMatches.setJoinRightIteratorsCreatedActual(100_000);

		StatementPattern denseRightMatches = new StatementPattern(new Var("s2"),
				new Var("p2", vf.createIRI("ex:pDenseRightMatchesWithIterators")), new Var("o2"));
		denseRightMatches.setSourceRowsScannedActual(1_000_000);
		denseRightMatches.setSourceRowsMatchedActual(1_000_000);
		denseRightMatches.setSourceRowsFilteredActual(0);
		denseRightMatches.setJoinLeftBindingsConsumedActual(100_000);
		denseRightMatches.setJoinRightBindingsConsumedActual(5_000_000);
		denseRightMatches.setJoinRightIteratorsCreatedActual(100_000);

		List<TupleExpr> expressions = new ArrayList<>();
		expressions.add(sparseRightMatches);
		expressions.add(denseRightMatches);

		Map<TupleExpr, Double> cardinalityMap = new java.util.HashMap<>();
		cardinalityMap.put(sparseRightMatches, 110.0);
		cardinalityMap.put(denseRightMatches, 100.0);

		Map<TupleExpr, List<Var>> varsMap = new java.util.HashMap<>();
		varsMap.put(sparseRightMatches, sparseRightMatches.getVarList());
		varsMap.put(denseRightMatches, denseRightMatches.getVarList());

		Map<Var, Integer> varFreqMap = new java.util.HashMap<>();
		for (Var var : sparseRightMatches.getVarList()) {
			varFreqMap.put(var, 1);
		}
		for (Var var : denseRightMatches.getVarList()) {
			varFreqMap.put(var, 1);
		}

		QueryJoinOptimizer optimizer = new QueryJoinOptimizer(new FlatJoinStatistics(), new EmptyTripleSource());
		Object joinVisitor = buildJoinVisitor(optimizer);
		Method selectNextTupleExpr = joinVisitor.getClass()
				.getDeclaredMethod("selectNextTupleExpr", List.class, Map.class, Map.class, Map.class);
		selectNextTupleExpr.setAccessible(true);

		TupleExpr selected = (TupleExpr) selectNextTupleExpr
				.invoke(joinVisitor, expressions, cardinalityMap, varsMap, varFreqMap);

		assertThat(selected).isSameAs(sparseRightMatches);
	}

	@Test
	public void selectNextTupleExprDoesNotPreferDenseRightFanoutWhenProbeRescanCostsAreComparable() throws Exception {
		ValueFactory vf = SimpleValueFactory.getInstance();

		StatementPattern moderateFanout = new StatementPattern(new Var("s1"),
				new Var("p1", vf.createIRI("ex:pModerateFanout")), new Var("o1"));
		moderateFanout.setSourceRowsScannedActual(1_000_000);
		moderateFanout.setSourceRowsMatchedActual(1_000_000);
		moderateFanout.setSourceRowsFilteredActual(0);
		moderateFanout.setJoinLeftBindingsConsumedActual(100_000);
		moderateFanout.setJoinRightBindingsConsumedActual(80_000);
		moderateFanout.setJoinRightIteratorsCreatedActual(80_000);

		StatementPattern denseFanout = new StatementPattern(new Var("s2"),
				new Var("p2", vf.createIRI("ex:pDenseFanout")), new Var("o2"));
		denseFanout.setSourceRowsScannedActual(1_000_000);
		denseFanout.setSourceRowsMatchedActual(1_000_000);
		denseFanout.setSourceRowsFilteredActual(0);
		denseFanout.setJoinLeftBindingsConsumedActual(100_000);
		denseFanout.setJoinRightBindingsConsumedActual(500_000);
		denseFanout.setJoinRightIteratorsCreatedActual(80_000);

		List<TupleExpr> expressions = new ArrayList<>();
		expressions.add(moderateFanout);
		expressions.add(denseFanout);

		Map<TupleExpr, Double> cardinalityMap = new java.util.HashMap<>();
		cardinalityMap.put(moderateFanout, 100.0);
		cardinalityMap.put(denseFanout, 90.0);

		Map<TupleExpr, List<Var>> varsMap = new java.util.HashMap<>();
		varsMap.put(moderateFanout, moderateFanout.getVarList());
		varsMap.put(denseFanout, denseFanout.getVarList());

		Map<Var, Integer> varFreqMap = new java.util.HashMap<>();
		for (Var var : moderateFanout.getVarList()) {
			varFreqMap.put(var, 1);
		}
		for (Var var : denseFanout.getVarList()) {
			varFreqMap.put(var, 1);
		}

		QueryJoinOptimizer optimizer = new QueryJoinOptimizer(new FlatJoinStatistics(), new EmptyTripleSource());
		Object joinVisitor = buildJoinVisitor(optimizer);
		Method selectNextTupleExpr = joinVisitor.getClass()
				.getDeclaredMethod("selectNextTupleExpr", List.class, Map.class, Map.class, Map.class);
		selectNextTupleExpr.setAccessible(true);

		TupleExpr selected = (TupleExpr) selectNextTupleExpr
				.invoke(joinVisitor, expressions, cardinalityMap, varsMap, varFreqMap);

		assertThat(selected).isSameAs(moderateFanout);
	}

	@Test
	public void selectNextTupleExprDoesNotOverreactToLowSampleDenseFanoutTelemetry() throws Exception {
		ValueFactory vf = SimpleValueFactory.getInstance();

		StatementPattern stableCardinalityWinner = new StatementPattern(new Var("s1"),
				new Var("p1", vf.createIRI("ex:pStableCardinalityWinner")), new Var("o1"));

		StatementPattern lowSampleDenseFanout = new StatementPattern(new Var("s2"),
				new Var("p2", vf.createIRI("ex:pLowSampleDenseFanout")), new Var("o2"));
		lowSampleDenseFanout.setSourceRowsScannedActual(10_000);
		lowSampleDenseFanout.setSourceRowsMatchedActual(10_000);
		lowSampleDenseFanout.setSourceRowsFilteredActual(0);
		lowSampleDenseFanout.setJoinLeftBindingsConsumedActual(3);
		lowSampleDenseFanout.setJoinRightBindingsConsumedActual(60);
		lowSampleDenseFanout.setJoinRightIteratorsCreatedActual(3);

		List<TupleExpr> expressions = new ArrayList<>();
		expressions.add(stableCardinalityWinner);
		expressions.add(lowSampleDenseFanout);

		Map<TupleExpr, Double> cardinalityMap = new java.util.HashMap<>();
		cardinalityMap.put(stableCardinalityWinner, 100.0);
		cardinalityMap.put(lowSampleDenseFanout, 110.0);

		Map<TupleExpr, List<Var>> varsMap = new java.util.HashMap<>();
		varsMap.put(stableCardinalityWinner, stableCardinalityWinner.getVarList());
		varsMap.put(lowSampleDenseFanout, lowSampleDenseFanout.getVarList());

		Map<Var, Integer> varFreqMap = new java.util.HashMap<>();
		for (Var var : stableCardinalityWinner.getVarList()) {
			varFreqMap.put(var, 1);
		}
		for (Var var : lowSampleDenseFanout.getVarList()) {
			varFreqMap.put(var, 1);
		}

		QueryJoinOptimizer optimizer = new QueryJoinOptimizer(new FlatJoinStatistics(), new EmptyTripleSource());
		Object joinVisitor = buildJoinVisitor(optimizer);
		Method selectNextTupleExpr = joinVisitor.getClass()
				.getDeclaredMethod("selectNextTupleExpr", List.class, Map.class, Map.class, Map.class);
		selectNextTupleExpr.setAccessible(true);

		TupleExpr selected = (TupleExpr) selectNextTupleExpr
				.invoke(joinVisitor, expressions, cardinalityMap, varsMap, varFreqMap);

		assertThat(selected).isSameAs(stableCardinalityWinner);
	}

	@Test
	public void selectNextTupleExprDoesNotOverreactToSparseProbeTelemetryWhenRightIteratorsAreBatched()
			throws Exception {
		ValueFactory vf = SimpleValueFactory.getInstance();

		StatementPattern stableCardinalityWinner = new StatementPattern(new Var("s1"),
				new Var("p1", vf.createIRI("ex:pStableCardinalityWinnerWithIteratorCoverage")), new Var("o1"));
		stableCardinalityWinner.setSourceRowsScannedActual(1_000_000);
		stableCardinalityWinner.setSourceRowsMatchedActual(1_000_000);
		stableCardinalityWinner.setSourceRowsFilteredActual(0);
		stableCardinalityWinner.setJoinLeftBindingsConsumedActual(100_000);
		stableCardinalityWinner.setJoinRightBindingsConsumedActual(20_000);
		stableCardinalityWinner.setJoinRightIteratorsCreatedActual(20_000);

		StatementPattern sparseButBatchedRightProbe = new StatementPattern(new Var("s2"),
				new Var("p2", vf.createIRI("ex:pSparseButBatchedRightProbe")), new Var("o2"));
		sparseButBatchedRightProbe.setSourceRowsScannedActual(1_000_000);
		sparseButBatchedRightProbe.setSourceRowsMatchedActual(1_000_000);
		sparseButBatchedRightProbe.setSourceRowsFilteredActual(0);
		sparseButBatchedRightProbe.setJoinLeftBindingsConsumedActual(100_000);
		sparseButBatchedRightProbe.setJoinRightBindingsConsumedActual(100);
		sparseButBatchedRightProbe.setJoinRightIteratorsCreatedActual(25);

		List<TupleExpr> expressions = new ArrayList<>();
		expressions.add(stableCardinalityWinner);
		expressions.add(sparseButBatchedRightProbe);

		Map<TupleExpr, Double> cardinalityMap = new java.util.HashMap<>();
		cardinalityMap.put(stableCardinalityWinner, 100.0);
		cardinalityMap.put(sparseButBatchedRightProbe, 105.0);

		Map<TupleExpr, List<Var>> varsMap = new java.util.HashMap<>();
		varsMap.put(stableCardinalityWinner, stableCardinalityWinner.getVarList());
		varsMap.put(sparseButBatchedRightProbe, sparseButBatchedRightProbe.getVarList());

		Map<Var, Integer> varFreqMap = new java.util.HashMap<>();
		for (Var var : stableCardinalityWinner.getVarList()) {
			varFreqMap.put(var, 1);
		}
		for (Var var : sparseButBatchedRightProbe.getVarList()) {
			varFreqMap.put(var, 1);
		}

		QueryJoinOptimizer optimizer = new QueryJoinOptimizer(new FlatJoinStatistics(), new EmptyTripleSource());
		Object joinVisitor = buildJoinVisitor(optimizer);
		Method selectNextTupleExpr = joinVisitor.getClass()
				.getDeclaredMethod("selectNextTupleExpr", List.class, Map.class, Map.class, Map.class);
		selectNextTupleExpr.setAccessible(true);

		TupleExpr selected = (TupleExpr) selectNextTupleExpr
				.invoke(joinVisitor, expressions, cardinalityMap, varsMap, varFreqMap);

		assertThat(selected).isSameAs(stableCardinalityWinner);
	}

	@Test
	public void selectNextTupleExprPrefersExtremeIteratorChurnPatternEvenWhenCardinalityIsSlightlyHigher()
			throws Exception {
		ValueFactory vf = SimpleValueFactory.getInstance();

		StatementPattern lowCardinalityStablePattern = new StatementPattern(new Var("s1"),
				new Var("p1", vf.createIRI("ex:pLowCardinalityStablePattern")), new Var("o1"));
		lowCardinalityStablePattern.setSourceRowsScannedActual(2_000);
		lowCardinalityStablePattern.setSourceRowsMatchedActual(2_000);
		lowCardinalityStablePattern.setSourceRowsFilteredActual(0);
		lowCardinalityStablePattern.setJoinLeftBindingsConsumedActual(2_000);
		lowCardinalityStablePattern.setJoinRightBindingsConsumedActual(2_000);
		lowCardinalityStablePattern.setJoinRightIteratorsCreatedActual(2_000);

		StatementPattern extremeIteratorChurnPattern = new StatementPattern(new Var("s2"),
				new Var("p2", vf.createIRI("ex:pExtremeIteratorChurnPattern")), new Var("o2"));
		extremeIteratorChurnPattern.setSourceRowsScannedActual(25_000_000);
		extremeIteratorChurnPattern.setSourceRowsMatchedActual(25_000_000);
		extremeIteratorChurnPattern.setSourceRowsFilteredActual(0);
		extremeIteratorChurnPattern.setJoinLeftBindingsConsumedActual(5_000_000);
		extremeIteratorChurnPattern.setJoinRightBindingsConsumedActual(500);
		extremeIteratorChurnPattern.setJoinRightIteratorsCreatedActual(10_000_000);

		List<TupleExpr> expressions = new ArrayList<>();
		expressions.add(lowCardinalityStablePattern);
		expressions.add(extremeIteratorChurnPattern);

		Map<TupleExpr, Double> cardinalityMap = new java.util.HashMap<>();
		cardinalityMap.put(lowCardinalityStablePattern, 10.0);
		cardinalityMap.put(extremeIteratorChurnPattern, 100.0);

		Map<TupleExpr, List<Var>> varsMap = new java.util.HashMap<>();
		varsMap.put(lowCardinalityStablePattern, lowCardinalityStablePattern.getVarList());
		varsMap.put(extremeIteratorChurnPattern, extremeIteratorChurnPattern.getVarList());

		Map<Var, Integer> varFreqMap = new java.util.HashMap<>();
		for (Var var : lowCardinalityStablePattern.getVarList()) {
			varFreqMap.put(var, 1);
		}
		for (Var var : extremeIteratorChurnPattern.getVarList()) {
			varFreqMap.put(var, 1);
		}

		QueryJoinOptimizer optimizer = new QueryJoinOptimizer(new FlatJoinStatistics(), new EmptyTripleSource());
		Object joinVisitor = buildJoinVisitor(optimizer);
		Method selectNextTupleExpr = joinVisitor.getClass()
				.getDeclaredMethod("selectNextTupleExpr", List.class, Map.class, Map.class, Map.class);
		selectNextTupleExpr.setAccessible(true);

		TupleExpr selected = (TupleExpr) selectNextTupleExpr
				.invoke(joinVisitor, expressions, cardinalityMap, varsMap, varFreqMap);

		assertThat(selected).isSameAs(extremeIteratorChurnPattern);
	}

	@Test
	public void selectNextTupleExprPrefersHighRightRowsPerIteratorPatternWhenLeftFanoutMatches() throws Exception {
		ValueFactory vf = SimpleValueFactory.getInstance();

		StatementPattern stableIteratorYield = new StatementPattern(new Var("s1"),
				new Var("p1", vf.createIRI("ex:pStableIteratorYield")), new Var("o1"));
		stableIteratorYield.setSourceRowsScannedActual(200_000);
		stableIteratorYield.setSourceRowsMatchedActual(200_000);
		stableIteratorYield.setSourceRowsFilteredActual(0);
		stableIteratorYield.setJoinLeftBindingsConsumedActual(1_000);
		stableIteratorYield.setJoinRightBindingsConsumedActual(2_000);
		stableIteratorYield.setJoinRightIteratorsCreatedActual(1_000);

		StatementPattern burstyIteratorYield = new StatementPattern(new Var("s2"),
				new Var("p2", vf.createIRI("ex:pBurstyIteratorYield")), new Var("o2"));
		burstyIteratorYield.setSourceRowsScannedActual(200_000);
		burstyIteratorYield.setSourceRowsMatchedActual(200_000);
		burstyIteratorYield.setSourceRowsFilteredActual(0);
		burstyIteratorYield.setJoinLeftBindingsConsumedActual(1_000);
		burstyIteratorYield.setJoinRightBindingsConsumedActual(2_000);
		burstyIteratorYield.setJoinRightIteratorsCreatedActual(100);

		List<TupleExpr> expressions = new ArrayList<>();
		expressions.add(stableIteratorYield);
		expressions.add(burstyIteratorYield);

		Map<TupleExpr, Double> cardinalityMap = new java.util.HashMap<>();
		cardinalityMap.put(stableIteratorYield, 100.0);
		cardinalityMap.put(burstyIteratorYield, 100.0);

		Map<TupleExpr, List<Var>> varsMap = new java.util.HashMap<>();
		varsMap.put(stableIteratorYield, stableIteratorYield.getVarList());
		varsMap.put(burstyIteratorYield, burstyIteratorYield.getVarList());

		Map<Var, Integer> varFreqMap = new java.util.HashMap<>();
		for (Var var : stableIteratorYield.getVarList()) {
			varFreqMap.put(var, 1);
		}
		for (Var var : burstyIteratorYield.getVarList()) {
			varFreqMap.put(var, 1);
		}

		QueryJoinOptimizer optimizer = new QueryJoinOptimizer(new FlatJoinStatistics(), new EmptyTripleSource());
		Object joinVisitor = buildJoinVisitor(optimizer);
		Method selectNextTupleExpr = joinVisitor.getClass()
				.getDeclaredMethod("selectNextTupleExpr", List.class, Map.class, Map.class, Map.class);
		selectNextTupleExpr.setAccessible(true);

		TupleExpr selected = (TupleExpr) selectNextTupleExpr
				.invoke(joinVisitor, expressions, cardinalityMap, varsMap, varFreqMap);

		assertThat(selected).isSameAs(burstyIteratorYield);
	}

	@Test
	public void selectNextTupleExprDoesNotOverprioritizeBalancedProbeYieldTelemetry() throws Exception {
		ValueFactory vf = SimpleValueFactory.getInstance();

		StatementPattern stableCardinalityWinner = new StatementPattern(new Var("s1"),
				new Var("p1", vf.createIRI("ex:pStableCardinalityWinnerBalancedProbeYield")), new Var("o1"));
		stableCardinalityWinner.setSourceRowsScannedActual(1_000_000);
		stableCardinalityWinner.setSourceRowsMatchedActual(1_000_000);
		stableCardinalityWinner.setSourceRowsFilteredActual(0);
		stableCardinalityWinner.setJoinLeftBindingsConsumedActual(150_000);
		stableCardinalityWinner.setJoinRightBindingsConsumedActual(120_000);
		stableCardinalityWinner.setJoinRightIteratorsCreatedActual(120_000);

		StatementPattern balancedButScanHeavyProbe = new StatementPattern(new Var("s2"),
				new Var("p2", vf.createIRI("ex:pBalancedButScanHeavyProbe")), new Var("o2"));
		balancedButScanHeavyProbe.setSourceRowsScannedActual(30_000_000);
		balancedButScanHeavyProbe.setSourceRowsMatchedActual(30_000_000);
		balancedButScanHeavyProbe.setSourceRowsFilteredActual(0);
		balancedButScanHeavyProbe.setJoinLeftBindingsConsumedActual(150_000);
		balancedButScanHeavyProbe.setJoinRightBindingsConsumedActual(165_000);
		balancedButScanHeavyProbe.setJoinRightIteratorsCreatedActual(150_000);

		List<TupleExpr> expressions = new ArrayList<>();
		expressions.add(stableCardinalityWinner);
		expressions.add(balancedButScanHeavyProbe);

		Map<TupleExpr, Double> cardinalityMap = new java.util.HashMap<>();
		cardinalityMap.put(stableCardinalityWinner, 100.0);
		cardinalityMap.put(balancedButScanHeavyProbe, 105.0);

		Map<TupleExpr, List<Var>> varsMap = new java.util.HashMap<>();
		varsMap.put(stableCardinalityWinner, stableCardinalityWinner.getVarList());
		varsMap.put(balancedButScanHeavyProbe, balancedButScanHeavyProbe.getVarList());

		Map<Var, Integer> varFreqMap = new java.util.HashMap<>();
		for (Var var : stableCardinalityWinner.getVarList()) {
			varFreqMap.put(var, 1);
		}
		for (Var var : balancedButScanHeavyProbe.getVarList()) {
			varFreqMap.put(var, 1);
		}

		QueryJoinOptimizer optimizer = new QueryJoinOptimizer(new FlatJoinStatistics(), new EmptyTripleSource());
		Object joinVisitor = buildJoinVisitor(optimizer);
		Method selectNextTupleExpr = joinVisitor.getClass()
				.getDeclaredMethod("selectNextTupleExpr", List.class, Map.class, Map.class, Map.class);
		selectNextTupleExpr.setAccessible(true);

		TupleExpr selected = (TupleExpr) selectNextTupleExpr
				.invoke(joinVisitor, expressions, cardinalityMap, varsMap, varFreqMap);

		assertThat(selected).isSameAs(stableCardinalityWinner);
	}

	@Test
	public void selectNextTupleExprDoesNotOverprioritizeProductiveProbeYieldTelemetry() throws Exception {
		ValueFactory vf = SimpleValueFactory.getInstance();

		StatementPattern stableCardinalityWinner = new StatementPattern(new Var("s1"),
				new Var("p1", vf.createIRI("ex:pStableCardinalityWinnerProductiveProbeYield")), new Var("o1"));

		StatementPattern productiveProbePattern = new StatementPattern(new Var("s2"),
				new Var("p2", vf.createIRI("ex:pProductiveProbePattern")), new Var("o2"));
		productiveProbePattern.setJoinLeftBindingsConsumedActual(200_000);
		productiveProbePattern.setJoinRightBindingsConsumedActual(260_000);
		productiveProbePattern.setJoinRightIteratorsCreatedActual(160_000);

		List<TupleExpr> expressions = new ArrayList<>();
		expressions.add(stableCardinalityWinner);
		expressions.add(productiveProbePattern);

		Map<TupleExpr, Double> cardinalityMap = new java.util.HashMap<>();
		cardinalityMap.put(stableCardinalityWinner, 55.0);
		cardinalityMap.put(productiveProbePattern, 105.0);

		Map<TupleExpr, List<Var>> varsMap = new java.util.HashMap<>();
		varsMap.put(stableCardinalityWinner, stableCardinalityWinner.getVarList());
		varsMap.put(productiveProbePattern, productiveProbePattern.getVarList());

		Map<Var, Integer> varFreqMap = new java.util.HashMap<>();
		for (Var var : stableCardinalityWinner.getVarList()) {
			varFreqMap.put(var, 1);
		}
		for (Var var : productiveProbePattern.getVarList()) {
			varFreqMap.put(var, 1);
		}

		QueryJoinOptimizer optimizer = new QueryJoinOptimizer(new FlatJoinStatistics(), new EmptyTripleSource());
		Object joinVisitor = buildJoinVisitor(optimizer);
		Method selectNextTupleExpr = joinVisitor.getClass()
				.getDeclaredMethod("selectNextTupleExpr", List.class, Map.class, Map.class, Map.class);
		selectNextTupleExpr.setAccessible(true);

		TupleExpr selected = (TupleExpr) selectNextTupleExpr
				.invoke(joinVisitor, expressions, cardinalityMap, varsMap, varFreqMap);

		assertThat(selected).isSameAs(stableCardinalityWinner);
	}

	@Test
	public void selectNextTupleExprUsesDescendantTelemetryForCompositeTupleExprs() throws Exception {
		ValueFactory vf = SimpleValueFactory.getInstance();
		Var branch = new Var("branch");
		Var branchName = new Var("branchName");
		Var copy = new Var("copy");

		StatementPattern branchNamePattern = new StatementPattern(branch, new Var("pName", vf.createIRI("ex:pName")),
				branchName);
		branchNamePattern.setSourceRowsScannedActual(45_000);
		branchNamePattern.setSourceRowsMatchedActual(45_000);
		branchNamePattern.setSourceRowsFilteredActual(0);
		Filter branchFilter = new Filter(branchNamePattern,
				new Compare(branchName.clone(), new ValueConstant(vf.createLiteral("Branch 0")), Compare.CompareOp.EQ));

		StatementPattern locatedAtPattern = new StatementPattern(copy, new Var("pLocatedAt", vf.createIRI("ex:pLoc")),
				branch.clone());
		locatedAtPattern.setJoinLeftBindingsConsumedActual(154_406);
		locatedAtPattern.setJoinRightBindingsConsumedActual(154_406);
		locatedAtPattern.setJoinRightIteratorsCreatedActual(154_406);

		StatementPattern copyTypePattern = new StatementPattern(copy.clone(),
				new Var("pType", vf.createIRI("ex:pType")),
				new Var("typeObject"));
		copyTypePattern.setJoinLeftBindingsConsumedActual(154_406);
		copyTypePattern.setJoinRightBindingsConsumedActual(154_406);
		copyTypePattern.setJoinRightIteratorsCreatedActual(154_406);

		Join telemetryHeavyJoin = new Join(locatedAtPattern, copyTypePattern);

		List<TupleExpr> expressions = new ArrayList<>();
		expressions.add(branchFilter);
		expressions.add(telemetryHeavyJoin);

		Map<TupleExpr, Double> cardinalityMap = new java.util.HashMap<>();
		cardinalityMap.put(branchFilter, 100.0);
		cardinalityMap.put(telemetryHeavyJoin, 100.0);

		Map<TupleExpr, List<Var>> varsMap = new java.util.HashMap<>();
		varsMap.put(branchFilter, List.of(branch, branchName));
		varsMap.put(telemetryHeavyJoin, List.of(copy, branch));

		Map<Var, Integer> varFreqMap = new java.util.HashMap<>();
		for (List<Var> vars : varsMap.values()) {
			for (Var var : vars) {
				varFreqMap.merge(var, 1, Integer::sum);
			}
		}

		QueryJoinOptimizer optimizer = new QueryJoinOptimizer(new FlatJoinStatistics(), new EmptyTripleSource());
		Object joinVisitor = buildJoinVisitor(optimizer);
		Method selectNextTupleExpr = joinVisitor.getClass()
				.getDeclaredMethod("selectNextTupleExpr", List.class, Map.class, Map.class, Map.class);
		selectNextTupleExpr.setAccessible(true);

		TupleExpr selected = (TupleExpr) selectNextTupleExpr
				.invoke(joinVisitor, expressions, cardinalityMap, varsMap, varFreqMap);

		assertThat(selected).isSameAs(telemetryHeavyJoin);
	}

	@Test
	public void correlatedNotExistsJoinPrefersOuterBoundVariablePatternFirst() {
		String query = "PREFIX conn: <http://example.com/theme/connected/>\n"
				+ "SELECT * WHERE {\n"
				+ "  VALUES ?threshold { 3 }\n"
				+ "  ?node a conn:Node ; conn:weight ?w .\n"
				+ "  FILTER NOT EXISTS {\n"
				+ "    ?node conn:connectsTo ?n2 .\n"
				+ "    ?n2 conn:weight ?w2 .\n"
				+ "    FILTER(?w2 < ?threshold)\n"
				+ "  }\n"
				+ "}";

		SPARQLParser parser = new SPARQLParser();
		ParsedQuery parsed = parser.parseQuery(query, null);
		QueryJoinOptimizer optimizer = new QueryJoinOptimizer(new EvaluationStatistics(), new EmptyTripleSource());
		QueryRoot root = new QueryRoot(parsed.getTupleExpr());
		optimizer.optimize(root, null, null);

		CorrelatedNotExistsJoinFinder joinFinder = new CorrelatedNotExistsJoinFinder();
		root.visit(joinFinder);
		Join correlatedJoin = joinFinder.getJoin();
		assertNotNull(correlatedJoin, "Expected join between ?node connectsTo ?n2 and ?n2 weight ?w2");

		StatementPattern left = (StatementPattern) correlatedJoin.getLeftArg();
		StatementPattern right = (StatementPattern) correlatedJoin.getRightArg();
		assertEquals("http://example.com/theme/connected/connectsTo",
				left.getPredicateVar().getValue().stringValue(),
				"Expected correlated pattern to be left-most in EXISTS join");
		assertEquals("node", left.getSubjectVar().getName(),
				"Expected correlated pattern to use outer bound ?node");
		assertEquals("http://example.com/theme/connected/weight", right.getPredicateVar().getValue().stringValue());
		assertEquals("n2", right.getSubjectVar().getName());
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

	class CorrelatedNotExistsJoinFinder extends AbstractQueryModelVisitor<RuntimeException> {

		private Join join;
		private boolean insideNotExists;

		@Override
		public void meet(Not not) {
			boolean previous = insideNotExists;
			try {
				insideNotExists = true;
				super.meet(not);
			} finally {
				insideNotExists = previous;
			}
		}

		@Override
		public void meet(Join candidate) {
			if (join == null && insideNotExists
					&& candidate.getLeftArg() instanceof StatementPattern
					&& candidate.getRightArg() instanceof StatementPattern) {
				StatementPattern left = (StatementPattern) candidate.getLeftArg();
				StatementPattern right = (StatementPattern) candidate.getRightArg();
				if (isCorrelatedConnectsTo(left, right) || isCorrelatedConnectsTo(right, left)) {
					join = candidate;
				}
			}
			super.meet(candidate);
		}

		private boolean isCorrelatedConnectsTo(StatementPattern connectsToPattern, StatementPattern weightPattern) {
			return matchesPredicate(connectsToPattern, "http://example.com/theme/connected/connectsTo")
					&& matchesPredicate(weightPattern, "http://example.com/theme/connected/weight")
					&& "node".equals(connectsToPattern.getSubjectVar().getName())
					&& "n2".equals(connectsToPattern.getObjectVar().getName())
					&& "n2".equals(weightPattern.getSubjectVar().getName());
		}

		private boolean matchesPredicate(StatementPattern statementPattern, String iri) {
			Var predicateVar = statementPattern.getPredicateVar();
			return predicateVar != null
					&& predicateVar.getValue() != null
					&& iri.equals(predicateVar.getValue().stringValue());
		}

		Join getJoin() {
			return join;
		}
	}

	private Object buildJoinVisitor(QueryJoinOptimizer optimizer) throws Exception {
		Class<?> joinVisitorClass = Class
				.forName("org.eclipse.rdf4j.query.algebra.evaluation.optimizer.QueryJoinOptimizer$JoinVisitor");
		Constructor<?> constructor = joinVisitorClass.getDeclaredConstructor(QueryJoinOptimizer.class);
		constructor.setAccessible(true);
		return constructor.newInstance(optimizer);
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

	private static final class FlatJoinStatistics extends EvaluationStatistics {

		@Override
		public boolean supportsJoinEstimation() {
			return true;
		}

		@Override
		public double getCardinality(TupleExpr expr) {
			if (expr instanceof Join) {
				return 1_000;
			}
			if (expr instanceof StatementPattern) {
				return 100;
			}
			return super.getCardinality(expr);
		}
	}

	private static final class SlightEstimatorBiasStatistics extends EvaluationStatistics {

		@Override
		public boolean supportsJoinEstimation() {
			return true;
		}

		@Override
		public double getCardinality(TupleExpr expr) {
			if (expr instanceof StatementPattern) {
				return 100;
			}
			if (expr instanceof Join) {
				return getJoinCardinality((Join) expr);
			}
			return super.getCardinality(expr);
		}

		private double getJoinCardinality(Join join) {
			String left = predicate(join.getLeftArg());
			String right = predicate(join.getRightArg());
			if (left == null || right == null) {
				return super.getCardinality(join);
			}

			if (isStable(left) && isStable(right)) {
				return 2;
			}
			if (isSparseProbe(left) || isSparseProbe(right)) {
				return 3;
			}
			return 10;
		}

		private boolean isStable(String predicate) {
			return "ex:pStableA".equals(predicate) || "ex:pStableB".equals(predicate);
		}

		private boolean isSparseProbe(String predicate) {
			return "ex:pSparseProbe".equals(predicate);
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

	private static final class AsymmetricSparseStartPairStatistics extends EvaluationStatistics {

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
			if (predicate == null) {
				return 100;
			}
			switch (predicate) {
			case "ex:pLow1":
				return 1;
			case "ex:pLow2":
				return 2;
			case "ex:pLow3":
				return 3;
			case "ex:pMid4":
				return 50;
			case "ex:pMid5":
				return 60;
			case "ex:pMid6":
				return 70;
			case "ex:pSparseOutlier":
				return 1_000;
			default:
				return 100;
			}
		}

		private double getJoinCardinality(Join join) {
			String left = predicate(join.getLeftArg());
			String right = predicate(join.getRightArg());

			if (left == null || right == null) {
				return super.getCardinality(join);
			}

			if ("ex:pSparseOutlier".equals(left) && "ex:pLow1".equals(right)) {
				return 1;
			}
			if ("ex:pLow1".equals(left) && "ex:pSparseOutlier".equals(right)) {
				return 200;
			}
			if (isLow(left) && isLow(right) && !left.equals(right)) {
				return 2;
			}
			if (isLow(left) || isLow(right)) {
				return 5;
			}
			return 10;
		}

		private boolean isLow(String predicate) {
			return "ex:pLow1".equals(predicate)
					|| "ex:pLow2".equals(predicate)
					|| "ex:pLow3".equals(predicate);
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

	private static final class LowSampleAsymmetricJoinStatistics extends EvaluationStatistics {

		@Override
		public boolean supportsJoinEstimation() {
			return true;
		}

		@Override
		public double getCardinality(TupleExpr expr) {
			if (expr instanceof StatementPattern) {
				return 100;
			}
			if (expr instanceof Join) {
				return getJoinCardinality((Join) expr);
			}
			return super.getCardinality(expr);
		}

		private double getJoinCardinality(Join join) {
			String left = predicate(join.getLeftArg());
			String right = predicate(join.getRightArg());

			if (left == null || right == null) {
				return super.getCardinality(join);
			}

			if ("ex:pStableDirectionalWinner".equals(left) && "ex:pLowSampleDenseFanoutReorder".equals(right)) {
				return 1;
			}
			if ("ex:pLowSampleDenseFanoutReorder".equals(left) && "ex:pStableDirectionalWinner".equals(right)) {
				return 2;
			}
			return 10;
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

}
