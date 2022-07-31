/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import static org.junit.Assert.assertEquals;

import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.query.algebra.evaluation.optimizer.QueryJoinOptimizer;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.sparql.SPARQLParser;
import org.junit.Test;

/**
 * Tests that cost estimates are printed as part of the plan
 */
public class QueryCostEstimatesTest {

	private final String LINE_SEP = System.lineSeparator();

	@Test
	public void testBindingSetAssignmentOptimization() throws RDF4JException {
		String query = "prefix ex: <ex:>" + "select ?s ?p ?o ?x where {" + " ex:s1 ex:pred ?v. "
				+ " ex:s2 ex:pred 'bah'. {" + "  ?s ?p ?o. " + "  optional {"
				+ "   values ?x {ex:a ex:b ex:c ex:d ex:e ex:f ex:g}. " + "  }" + " }" + "}";

		SPARQLParser parser = new SPARQLParser();
		ParsedQuery q = parser.parseQuery(query, null);
		QueryJoinOptimizer opt = new QueryJoinOptimizer(new EvaluationStatistics());
		opt.optimize(q.getTupleExpr(), null, null);

		assertEquals("QueryRoot" + LINE_SEP +
				"   Projection" + LINE_SEP +
				"      ProjectionElemList" + LINE_SEP +
				"         ProjectionElem \"s\"" + LINE_SEP +
				"         ProjectionElem \"p\"" + LINE_SEP +
				"         ProjectionElem \"o\"" + LINE_SEP +
				"         ProjectionElem \"x\"" + LINE_SEP +
				"      Join" + LINE_SEP +
				"         StatementPattern (costEstimate=1, resultSizeEstimate=1)" + LINE_SEP +
				"            Var (name=_const_5c6ba46_uri, value=ex:s2, anonymous)" + LINE_SEP +
				"            Var (name=_const_af00e088_uri, value=ex:pred, anonymous)" + LINE_SEP +
				"            Var (name=_const_17c09_lit_e2eec718, value=\"bah\", anonymous)" + LINE_SEP +
				"         Join" + LINE_SEP +
				"            StatementPattern (costEstimate=10, resultSizeEstimate=10)" + LINE_SEP +
				"               Var (name=_const_5c6ba45_uri, value=ex:s1, anonymous)" + LINE_SEP +
				"               Var (name=_const_af00e088_uri, value=ex:pred, anonymous)" + LINE_SEP +
				"               Var (name=v)" + LINE_SEP +
				"            LeftJoin (new scope) (costEstimate=1000, resultSizeEstimate=1000)" + LINE_SEP +
				"               StatementPattern (resultSizeEstimate=1000)" + LINE_SEP +
				"                  Var (name=s)" + LINE_SEP +
				"                  Var (name=p)" + LINE_SEP +
				"                  Var (name=o)" + LINE_SEP +
				"               BindingSetAssignment ([[x=ex:a], [x=ex:b], [x=ex:c], [x=ex:d], [x=ex:e], [x=ex:f], [x=ex:g]])"
				+ LINE_SEP,
				q.getTupleExpr().toString());

	}

}
