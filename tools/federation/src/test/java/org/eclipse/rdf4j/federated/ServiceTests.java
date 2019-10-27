/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated;

import java.util.Arrays;

import org.eclipse.rdf4j.federated.endpoint.Endpoint;
import org.junit.jupiter.api.Test;

public class ServiceTests extends SPARQLBaseTest {

	@Test
	public void test1() throws Exception {

		assumeSparqlEndpoint();

		/* test select query retrieving all persons from endpoint 1 (SERVICE) */
		prepareTest(Arrays.asList("/tests/data/data1.ttl", "/tests/data/data2.ttl", "/tests/data/data3.ttl",
				"/tests/data/data4.ttl"));

		evaluateQueryPlan("/tests/service/query01.rq", "/tests/service/query01.qp");
		execute("/tests/service/query01.rq", "/tests/service/query01.srx", false);
	}

	@Test
	public void test1a_byName() throws Exception {

		/* test select query retrieving all persons from endpoint 1 (SERVICE) by name */
		prepareTest(Arrays.asList("/tests/data/data1.ttl", "/tests/data/data2.ttl", "/tests/data/data3.ttl",
				"/tests/data/data4.ttl"));

		evaluateQueryPlan("/tests/service/query01a.rq", "/tests/service/query01.qp");
		execute("/tests/service/query01a.rq", "/tests/service/query01.srx", false);
	}

	@Test
	public void test2() throws Exception {

		assumeSparqlEndpoint();

		/* test select query retrieving all persons from endpoint 1 (SERVICE) + exclusive statement => group */
		prepareTest(Arrays.asList("/tests/data/data1.ttl", "/tests/data/data2.ttl", "/tests/data/data3.ttl",
				"/tests/data/data4.ttl"));

		evaluateQueryPlan("/tests/service/query02.rq", "/tests/service/query02.qp");
		execute("/tests/service/query02.rq", "/tests/service/query02.srx", false);
	}

	@Test
	public void test2_differentOrder() throws Exception {

		assumeSparqlEndpoint();

		/*
		 * test select query retrieving all persons from endpoint 1 (SERVICE) + exclusive statement => group In contrast
		 * to test2: order is different
		 */
		prepareTest(Arrays.asList("/tests/data/data1.ttl", "/tests/data/data2.ttl", "/tests/data/data3.ttl",
				"/tests/data/data4.ttl"));

		evaluateQueryPlan("/tests/service/query02a.rq", "/tests/service/query02a.qp");
		execute("/tests/service/query02a.rq", "/tests/service/query02.srx", false);
	}

	@Test
	public void test3() throws Exception {

		assumeSparqlEndpoint();

		/*
		 * test select query retrieving all persons from endpoint 1 (SERVICE), endpoint not part of federation =>
		 * evaluate using SESAME
		 */
		prepareTest(Arrays.asList("/tests/data/data1.ttl", "/tests/data/data2.ttl", "/tests/data/data3.ttl",
				"/tests/data/data4.ttl"));
		Endpoint endpoint1 = federationContext().getEndpointManager().getEndpointByName("http://endpoint1");
		fedxRule.removeEndpoint(endpoint1);
		execute("/tests/service/query03.rq", "/tests/service/query03.srx", false);
	}

	@Test
	public void test4() throws Exception {

		// evaluates by sparql endpoint URL, cannot be done with native store
		assumeSparqlEndpoint();

		/* two service which form exclusive groups */
		prepareTest(Arrays.asList("/tests/data/data1.ttl", "/tests/data/data2.ttl", "/tests/data/data3.ttl",
				"/tests/data/data4.ttl"));

		evaluateQueryPlan("/tests/service/query04.rq", "/tests/service/query04.qp");
		execute("/tests/service/query04.rq", "/tests/service/query04.srx", false);
	}

	@Test
	public void test4a() throws Exception {

		/* two service which form exclusive groups (resolving by name) */
		prepareTest(Arrays.asList("/tests/data/data1.ttl", "/tests/data/data2.ttl", "/tests/data/data3.ttl",
				"/tests/data/data4.ttl"));

		evaluateQueryPlan("/tests/service/query04a.rq", "/tests/service/query04.qp");
		execute("/tests/service/query04a.rq", "/tests/service/query04a.srx", false);
	}

	@Test
	public void test5() throws Exception {

		assumeSparqlEndpoint();

		/* two services, one becomes exclusive group, the other is evaluated as service (filter) */
		prepareTest(Arrays.asList("/tests/data/data1.ttl", "/tests/data/data2.ttl", "/tests/data/data3.ttl",
				"/tests/data/data4.ttl"));
		execute("/tests/service/query05.rq", "/tests/service/query05.srx", false);
	}

	@Test
	public void test6() throws Exception {

		/*
		 * two services, one becomes exclusive group, the other is evaluated as service (filter), uses name of
		 * federation member in SERVICE
		 */
		prepareTest(Arrays.asList("/tests/data/data1.ttl", "/tests/data/data2.ttl", "/tests/data/data3.ttl",
				"/tests/data/data4.ttl"));
		execute("/tests/service/query06.rq", "/tests/service/query06.srx", false);
	}

	@Test
	public void test7() throws Exception {

		// evaluates by sparql endpoint URL, cannot be done with native store
		assumeSparqlEndpoint();

		/* two services, both evaluated as SERVICE (FILTER), uses name of federation member in SERVICE */
		prepareTest(Arrays.asList("/tests/data/data1.ttl", "/tests/data/data2.ttl", "/tests/data/data3.ttl",
				"/tests/data/data4.ttl"));
		execute("/tests/service/query07.rq", "/tests/service/query07.srx", false);
	}

	@Test
	public void test8() throws Exception {

		assumeSparqlEndpoint();

		/*
		 * test select query retrieving all persons from endpoint 1 (SERVICE) + exclusive statement => group
		 */
		prepareTest(Arrays.asList("/tests/data/data1.ttl", "/tests/data/data2.ttl", "/tests/data/data3.ttl",
				"/tests/data/data4.ttl"));

		evaluateQueryPlan("/tests/service/query08.rq", "/tests/service/query08.qp");
		execute("/tests/service/query08.rq", "/tests/service/query08.srx", false);
	}
}
