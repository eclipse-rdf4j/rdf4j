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
package org.eclipse.rdf4j.federated.evaluation;

import java.util.List;

import org.eclipse.rdf4j.federated.SPARQLBaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class FederationEvalStrategyTest extends SPARQLBaseTest {

	@Test
	public void testOptimizeSingleSourceQuery() throws Exception {

		assumeSparqlEndpoint();

		// federation with single member
		prepareTest(List.of("/tests/data/data1.ttl"));

		String query = "SELECT * WHERE { ?s ?o ?o }";
		String queryPlan = federationContext().getQueryManager().getQueryPlan(query);

		Assertions.assertTrue(queryPlan.startsWith("SingleSourceQuery @sparql_localhost:18080_repositories_endpoint1"));
	}

	@Test
	public void testOptimize_SingleMember_Service() throws Exception {

		assumeSparqlEndpoint();

		// federation with single member
		prepareTest(List.of("/tests/data/data1.ttl"));

		// query with service, evaluate using FedX
		String query = "SELECT * WHERE { SERVICE <http://dummy> { ?s ?o ?o } }";
		String queryPlan = federationContext().getQueryManager().getQueryPlan(query);

		Assertions.assertTrue(queryPlan.startsWith("QueryRoot"));
	}
}
