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
package org.eclipse.rdf4j.federated.optimizer;

import java.util.Arrays;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.federated.SPARQLBaseTest;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class JoinOptimizerTest extends SPARQLBaseTest {

	@Test
	public void test_emptyJoinOptimizer() throws Exception {

		prepareTest(
				Arrays.asList("/tests/basic/data01endpoint1.ttl", "/tests/basic/data01endpoint2.ttl"));

		// first part of the query produces 0 results, but make sure that this is optimized properly
		// second half of the query is required to not have a SingleSourceQuery
		String query = "SELECT ?person WHERE { "
				+ "{ ?person <" + FOAF.NAME
				+ "> ?name . ?person <urn:doesNotExistProp> ?obj . BIND(?name AS ?nameOut) } UNION { ?person <"
				+ FOAF.INTEREST + "> ?o } }";

		String actualQueryPlan = federationContext().getQueryManager().getQueryPlan(query);
		assertQueryPlanEquals(readResourceAsString("/tests/optimizer/queryPlan_Join_1.txt"), actualQueryPlan);

		try (TupleQueryResult tqr = federationContext().getQueryManager().prepareTupleQuery(query).evaluate()) {
			Assertions.assertEquals(2, Iterations.asList(tqr).size()); // two results from endpoint 2
		}
	}

	@Test
	public void test_checkedJoinOptimizer() throws Exception {

		prepareTest(
				Arrays.asList("/tests/basic/data01endpoint1.ttl", "/tests/basic/data01endpoint2.ttl"));

		// both patterns of the query result in a exclusive statements at the respective endpoints
		String query = "PREFIX : <http://example.org/> SELECT ?person WHERE { "
				+ "{ :a <" + FOAF.NAME + "> 'Alan' . :a <" + FOAF.INTEREST
				+ "> 'SPARQL 1.1 Basic Federated Query' } }";

		String actualQueryPlan = federationContext().getQueryManager().getQueryPlan(query);
		assertQueryPlanEquals(readResourceAsString("/tests/optimizer/queryPlan_Join_2.txt"), actualQueryPlan);

		try (TupleQueryResult tqr = federationContext().getQueryManager().prepareTupleQuery(query).evaluate()) {
			Assertions.assertEquals(1, Iterations.asList(tqr).size()); // one result row, but no bindings
		}
	}
}
