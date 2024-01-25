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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.query.algebra.evaluation.optimizer.QueryJoinOptimizer;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.sparql.SPARQLParser;
import org.junit.jupiter.api.Test;

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
		QueryJoinOptimizer opt = new QueryJoinOptimizer(new EvaluationStatistics(), new EmptyTripleSource());
		opt.optimize(q.getTupleExpr(), null, null);

		String actual = q.getTupleExpr().toString();

		assertThat(actual).contains(System.lineSeparator());

		assertThat(actual).isEqualToNormalizingNewlines("QueryRoot\n" +
				"   Projection\n" +
				"      ProjectionElemList\n" +
				"         ProjectionElem \"s\"\n" +
				"         ProjectionElem \"p\"\n" +
				"         ProjectionElem \"o\"\n" +
				"         ProjectionElem \"x\"\n" +
				"      Join\n" +
				"         StatementPattern (costEstimate=6.00, resultSizeEstimate=1.00)\n" +
				"            Var (name=_const_5c6ba46_uri, value=ex:s2, anonymous)\n" +
				"            Var (name=_const_af00e088_uri, value=ex:pred, anonymous)\n" +
				"            Var (name=_const_17c09_lit_e2eec718, value=\"bah\", anonymous)\n" +
				"         Join\n" +
				"            StatementPattern (costEstimate=90, resultSizeEstimate=10)\n" +
				"               Var (name=_const_5c6ba45_uri, value=ex:s1, anonymous)\n" +
				"               Var (name=_const_af00e088_uri, value=ex:pred, anonymous)\n" +
				"               Var (name=v)\n" +
				"            LeftJoin (new scope) (costEstimate=90.5K, resultSizeEstimate=1000)\n" +
				"               StatementPattern (resultSizeEstimate=1000)\n" +
				"                  Var (name=s)\n" +
				"                  Var (name=p)\n" +
				"                  Var (name=o)\n" +
				"               BindingSetAssignment ([[x=ex:a], [x=ex:b], [x=ex:c], [x=ex:d], [x=ex:e], [x=ex:f], [x=ex:g]])\n"
		);

	}

}
