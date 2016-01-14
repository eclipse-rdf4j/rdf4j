/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import static org.junit.Assert.assertEquals;

import org.eclipse.rdf4j.OpenRDFException;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.UnsupportedQueryLanguageException;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.QueryRoot;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryJoinOptimizer;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.junit.Test;


/**
 * Tests to monitor QueryJoinOptimizer behaviour.
 * @author Mark
 */
public class QueryJoinOptimizerTest {
	@Test
	public void testBindingSetAssignmentOptimization() throws OpenRDFException {
		String query = "prefix ex: <ex:>"
				+ "select ?s ?p ?o ?x where {"
				+ " ex:s1 ex:pred ?v. "
				+ " ex:s2 ex:pred 'bah'. {"
				+ "  ?s ?p ?o. "
				+ "  optional {"
				+ "   values ?x {ex:a ex:b ex:c ex:d ex:e ex:f ex:g}. "
				+ "  }"
				+ " }"
				+ "}";
		// optimal order should be existence check of first statement
		// followed by left join evaluation
		String expectedQuery = "prefix ex: <ex:>"
				+ "select ?s ?p ?o ?x where {"
				+ " ex:s2 ex:pred 'bah'. {"
				+ "  ex:s1 ex:pred ?v. {"
				+ "   ?s ?p ?o. "
				+ "   optional {"
				+ "    values ?x {ex:a ex:b ex:c ex:d ex:e ex:f ex:g}. "
				+ "   }"
				+ "  }"
				+ " }"
				+ "}";

		testOptimizer(expectedQuery, query);
	}

	@Test(expected=AssertionError.class)
	public void testContextOptimization()
		throws OpenRDFException
	{
		String query = "prefix ex: <ex:>"
				+ "select ?x ?y ?z ?g ?p ?o where {"
				+ " graph ?g {"
				+ "  ex:s ?sp ?so. "
				+ "  ?ps ex:p ?po. "
				+ "  ?os ?op 'ex:o'. "
				+ " }"
				+ " ?x ?y ?z. "
				+ "}";
		// optimal order should be ?g graph first
		// as it is all statements about a subject in all graphs
		// rather than all subjects in the default graph:
		// card(?g) << card(?x)
		// and assuming named graph has same access cost as default graph
		String expectedQuery = "prefix ex: <ex:>"
				+ "select ?x ?y ?z ?g ?p ?o where {"
				+ " graph ?g {"
				+ "  ex:s ?sp ?so. "
				+ "  ?ps ex:p ?po. "
				+ "  ?os ?op 'ex:o'. "
				+ " }"
				+ " ?x ?y ?z. "
				+ "}";

		testOptimizer(expectedQuery, query);
	}

	private void testOptimizer(String expectedQuery, String actualQuery)
		throws MalformedQueryException, UnsupportedQueryLanguageException
	{
		ParsedQuery pq = QueryParserUtil.parseQuery(QueryLanguage.SPARQL, actualQuery, null);
		QueryJoinOptimizer opt = new QueryJoinOptimizer();
		QueryRoot optRoot = new QueryRoot(pq.getTupleExpr());
		opt.optimize(optRoot, null, null);

		ParsedQuery expectedParsedQuery = QueryParserUtil.parseQuery(QueryLanguage.SPARQL, expectedQuery, null);
		QueryRoot root = new QueryRoot(expectedParsedQuery.getTupleExpr());
		assertQueryModelTrees(root, optRoot);
	}

	private void assertQueryModelTrees(QueryModelNode expected, QueryModelNode actual) {
		assertEquals(expected, actual);
	}
}
