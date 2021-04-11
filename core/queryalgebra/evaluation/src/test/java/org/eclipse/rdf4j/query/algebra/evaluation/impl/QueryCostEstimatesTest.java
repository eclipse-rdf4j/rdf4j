/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import static org.junit.Assert.assertEquals;

import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.query.algebra.QueryRoot;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.sparql.SPARQLParser;
import org.junit.Test;

/**
 * Tests that cost estimates are printed as part of the plan
 */
public class QueryCostEstimatesTest {

	@Test
	public void testBindingSetAssignmentOptimization() throws RDF4JException {
		String query = "prefix ex: <ex:>" + "select ?s ?p ?o ?x where {" + " ex:s1 ex:pred ?v. "
				+ " ex:s2 ex:pred 'bah'. {" + "  ?s ?p ?o. " + "  optional {"
				+ "   values ?x {ex:a ex:b ex:c ex:d ex:e ex:f ex:g}. " + "  }" + " }" + "}";

		SPARQLParser parser = new SPARQLParser();
		ParsedQuery q = parser.parseQuery(query, null);
		QueryJoinOptimizer opt = new QueryJoinOptimizer();
		QueryRoot optRoot = new QueryRoot(q.getTupleExpr());
		opt.optimize(optRoot, null, null);

		assertEquals("QueryRoot\n" +
				"   Projection\n" +
				"      ProjectionElemList\n" +
				"         ProjectionElem \"s\"\n" +
				"         ProjectionElem \"p\"\n" +
				"         ProjectionElem \"o\"\n" +
				"         ProjectionElem \"x\"\n" +
				"      Join\n" +
				"         StatementPattern (costEstimate=1, resultSizeEstimate=1)\n" +
				"            Var (name=_const_5c6ba46_uri, value=ex:s2, anonymous)\n" +
				"            Var (name=_const_af00e088_uri, value=ex:pred, anonymous)\n" +
				"            Var (name=_const_17c09_lit_e2eec718_0, value=\"bah\", anonymous)\n" +
				"         Join\n" +
				"            StatementPattern (costEstimate=10, resultSizeEstimate=10)\n" +
				"               Var (name=_const_5c6ba45_uri, value=ex:s1, anonymous)\n" +
				"               Var (name=_const_af00e088_uri, value=ex:pred, anonymous)\n" +
				"               Var (name=v)\n" +
				"            LeftJoin (new scope) (costEstimate=1000, resultSizeEstimate=1000)\n" +
				"               StatementPattern (resultSizeEstimate=1000)\n" +
				"                  Var (name=s)\n" +
				"                  Var (name=p)\n" +
				"                  Var (name=o)\n" +
				"               BindingSetAssignment ([[x=ex:a], [x=ex:b], [x=ex:c], [x=ex:d], [x=ex:e], [x=ex:f], [x=ex:g]])\n",
				optRoot.toString());

	}

}
