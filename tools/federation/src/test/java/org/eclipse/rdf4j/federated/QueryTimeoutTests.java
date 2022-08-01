/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated;

import java.util.Arrays;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.query.Query;
import org.eclipse.rdf4j.query.QueryInterruptedException;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class QueryTimeoutTests extends SPARQLBaseTest {

	/*
	 * LONG RUNNING TESTS WITH TIMEOUT - ACTIVATE IF NEEDED
	 */

	@Test
	@Disabled // local test only
	public void testGlobalTimeout() throws Exception {

		assumeSparqlEndpoint();

		prepareTest(Arrays.asList("/tests/medium/data1.ttl", "/tests/medium/data2.ttl", "/tests/medium/data3.ttl",
				"/tests/medium/data4.ttl"));

		fedxRule.enableDebug();
		fedxRule.setConfig(fedxConfig -> fedxConfig.withEnforceMaxQueryTime(10));
		repoSettings(1).setLatencySimulator(latencySimulator(2000));
		repoSettings(2).setLatencySimulator(latencySimulator(2000));
		repoSettings(3).setLatencySimulator(latencySimulator(4000));

		Assertions.assertThrows(QueryInterruptedException.class, () -> {
			execute("/tests/medium/query05.rq", "/tests/medium/query05.srx", false, true);
		});

	}

	@Test
	@Disabled // local test only
	public void testLocalTimeout() throws Exception {

		assumeSparqlEndpoint();

		prepareTest(Arrays.asList("/tests/medium/data1.ttl", "/tests/medium/data2.ttl", "/tests/medium/data3.ttl",
				"/tests/medium/data4.ttl"));

		fedxRule.enableDebug();
		repoSettings(1).setLatencySimulator(latencySimulator(2000));
		repoSettings(2).setLatencySimulator(latencySimulator(2000));
		repoSettings(3).setLatencySimulator(latencySimulator(4000));

		Query query = queryManager().prepareQuery(readQueryString("/tests/medium/query05.rq"));
		query.setMaxExecutionTime(10);
		Assertions.assertThrows(QueryInterruptedException.class, () -> {
			try (TupleQueryResult tq = ((TupleQuery) query).evaluate()) {
				Iterations.asList(tq);
			}
		});
	}

	@Test
	@Disabled // local test only
	public void testLocalTimeout2() throws Exception {

		assumeSparqlEndpoint();

		prepareTest(Arrays.asList("/tests/medium/data1.ttl", "/tests/medium/data2.ttl", "/tests/medium/data3.ttl",
				"/tests/medium/data4.ttl"));

		fedxRule.enableDebug();
		repoSettings(1).setLatencySimulator(latencySimulator(2000));
		repoSettings(2).setLatencySimulator(latencySimulator(2500));

		Query query = queryManager().prepareQuery(readQueryString("/tests/medium/query01.rq"));
		query.setMaxExecutionTime(5);

		Assertions.assertThrows(QueryInterruptedException.class, () -> {
			try (TupleQueryResult tq = ((TupleQuery) query).evaluate()) {
				Iterations.asList(tq);
			}
		});
	}

	@Test
	@Disabled // local test only
	public void testLocalTimeout3() throws Exception {

		assumeSparqlEndpoint();

		prepareTest(Arrays.asList("/tests/medium/data1.ttl", "/tests/medium/data2.ttl", "/tests/medium/data3.ttl",
				"/tests/medium/data4.ttl"));

		fedxRule.enableDebug();

		String queryString = "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n" +
				"PREFIX ns1: <http://namespace1.org/>\n" +
				"PREFIX ns2: <http://namespace2.org/>\n" +
				"\n" +
				"SELECT ?person ?name  WHERE {\n" +
				" { ?person a ns1:Person . } \n" +
				" UNION" +
				" { ?person a ns2:Person . ?person foaf:name ?name . }\n" +
				"}";
		// make sure that latency does not affect source selection
		federationContext().getQueryManager().getQueryPlan(queryString);

		repoSettings(1).setLatencySimulator(latencySimulator(2000));
		repoSettings(2).setLatencySimulator(latencySimulator(5000));

		Query query = queryManager().prepareQuery(queryString);

		try (TupleQueryResult tq = ((TupleQuery) query).evaluate()) {

			// consume results from EP1
			for (int i = 0; i < 5; i++) {
				if (tq.hasNext()) {
					System.out.println(tq.next());
				}
			}
			// consume result from second union (blocks)
			System.out.println(tq.next());

		}

		System.out.println("Done");

	}

	protected Runnable latencySimulator(long latencyMs) {
		return () -> {
			try {
				Thread.sleep(latencyMs);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		};
	}
}
