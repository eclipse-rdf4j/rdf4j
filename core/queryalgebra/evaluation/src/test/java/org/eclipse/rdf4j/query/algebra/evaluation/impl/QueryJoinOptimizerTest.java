/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.UnsupportedQueryLanguageException;
import org.eclipse.rdf4j.query.algebra.BinaryTupleOperator;
import org.eclipse.rdf4j.query.algebra.Extension;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.QueryRoot;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.UnaryTupleOperator;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizer;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizerTest;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.eclipse.rdf4j.query.parser.sparql.SPARQLParser;
import org.junit.Assert;
import org.junit.Test;

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

	@Test(expected = AssertionError.class)
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

		testOptimizer(expectedQuery, query);
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
	public void testSES2116JoinBind() throws Exception {

		StringBuilder qb = new StringBuilder();
		qb.append("SELECT ?subject ?name ?row {\n" + "  ?subject <http://localhost/table_1> ?uri .\n"
				+ "  BIND(STR(?uri) AS ?name)\n"
				+ "  ?table <http://linked.opendata.cz/ontology/odcs/tabular/hasRow> ?row .\n"
				+ "  ?table <http://linked.opendata.cz/ontology/odcs/tabular/symbolicName> ?name .\n" + "}");

		SPARQLParser parser = new SPARQLParser();
		ParsedQuery q = parser.parseQuery(qb.toString(), null);
		QueryJoinOptimizer opt = new QueryJoinOptimizer();
		QueryRoot optRoot = new QueryRoot(q.getTupleExpr());
		opt.optimize(optRoot, null, null);
		TupleExpr leaf = findLeaf(optRoot);
		Assert.assertTrue("Extension must be evaluated before StatementPattern",
				leaf.getParentNode() instanceof Extension);
	}

	@Test
	public void bindSubselectJoinOrder() throws Exception {
		String query = "SELECT * WHERE {\n" + "    BIND (bnode() as ?ct01) \n" + "    { SELECT ?s WHERE {\n"
				+ "            ?s ?p ?o .\n" + "      }\n" + "      LIMIT 10\n" + "    }\n" + "}";

		SPARQLParser parser = new SPARQLParser();
		ParsedQuery q = parser.parseQuery(query, null);
		QueryJoinOptimizer opt = new QueryJoinOptimizer();
		QueryRoot optRoot = new QueryRoot(q.getTupleExpr());
		opt.optimize(optRoot, null, null);

		JoinFinder joinFinder = new JoinFinder();
		optRoot.visit(joinFinder);
		Join join = joinFinder.getJoin();

		assertThat(join.getLeftArg()).as("BIND clause should be left-most argument of join")
				.isInstanceOf(Extension.class);
	}

	@Override
	public QueryJoinOptimizer getOptimizer() {
		return new QueryJoinOptimizer();
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

	private void testOptimizer(String expectedQuery, String actualQuery)
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

}
