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
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.federated.endpoint.Endpoint;
import org.eclipse.rdf4j.federated.repository.FedXRepository;
import org.eclipse.rdf4j.http.client.HttpClientSessionManager;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.algebra.Service;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedService;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolver;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sparql.federation.RepositoryFederatedService;
import org.eclipse.rdf4j.repository.sparql.federation.SPARQLFederatedService;
import org.eclipse.rdf4j.repository.sparql.federation.SPARQLServiceResolver;
import org.eclipse.rdf4j.repository.util.Repositories;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Sets;

public class ServiceTests extends SPARQLBaseTest {

	@Test
	public void test1() throws Exception {

		assumeSparqlEndpoint();

		/* test select query retrieving all persons from endpoint 1 (SERVICE) */
		prepareTest(Arrays.asList("/tests/data/data1.ttl", "/tests/data/data2.ttl", "/tests/data/data3.ttl",
				"/tests/data/data4.ttl"));

		evaluateQueryPlan("/tests/service/query01.rq", "/tests/service/query01.qp");
		execute("/tests/service/query01.rq", "/tests/service/query01.srx", false, true);
	}

	@Test
	public void test1a_byName() throws Exception {

		/* test select query retrieving all persons from endpoint 1 (SERVICE) by name */
		prepareTest(Arrays.asList("/tests/data/data1.ttl", "/tests/data/data2.ttl", "/tests/data/data3.ttl",
				"/tests/data/data4.ttl"));

		evaluateQueryPlan("/tests/service/query01a.rq", "/tests/service/query01.qp");
		execute("/tests/service/query01a.rq", "/tests/service/query01.srx", false, true);
	}

	@Test
	public void test2() throws Exception {

		assumeSparqlEndpoint();

		/* test select query retrieving all persons from endpoint 1 (SERVICE) + exclusive statement => group */
		prepareTest(Arrays.asList("/tests/data/data1.ttl", "/tests/data/data2.ttl", "/tests/data/data3.ttl",
				"/tests/data/data4.ttl"));

		evaluateQueryPlan("/tests/service/query02.rq", "/tests/service/query02.qp");
		execute("/tests/service/query02.rq", "/tests/service/query02.srx", false, true);
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
		execute("/tests/service/query02a.rq", "/tests/service/query02.srx", false, true);
	}

	@Test
	public void test3() throws Exception {

		assumeSparqlEndpoint();

		/*
		 * test select query retrieving all persons from endpoint 1 (SERVICE), endpoint not part of federation =>
		 * evaluate using RDF4J
		 */
		prepareTest(Arrays.asList("/tests/data/data1.ttl", "/tests/data/data2.ttl", "/tests/data/data3.ttl",
				"/tests/data/data4.ttl"));
		Endpoint endpoint1 = federationContext().getEndpointManager().getEndpointByName("http://endpoint1");
		fedxRule.removeEndpoint(endpoint1);
		execute("/tests/service/query03.rq", "/tests/service/query03.srx", false, true);
	}

	@Test
	public void test4() throws Exception {

		// evaluates by sparql endpoint URL, cannot be done with native store
		assumeSparqlEndpoint();

		/* two service which form exclusive groups */
		prepareTest(Arrays.asList("/tests/data/data1.ttl", "/tests/data/data2.ttl", "/tests/data/data3.ttl",
				"/tests/data/data4.ttl"));

		evaluateQueryPlan("/tests/service/query04.rq", "/tests/service/query04.qp");
		execute("/tests/service/query04.rq", "/tests/service/query04.srx", false, true);
	}

	@Test
	public void test4a() throws Exception {

		/* two service which form exclusive groups (resolving by name) */
		prepareTest(Arrays.asList("/tests/data/data1.ttl", "/tests/data/data2.ttl", "/tests/data/data3.ttl",
				"/tests/data/data4.ttl"));

		evaluateQueryPlan("/tests/service/query04a.rq", "/tests/service/query04.qp");
		execute("/tests/service/query04a.rq", "/tests/service/query04a.srx", false, true);
	}

	@Test
	public void test5() throws Exception {

		assumeSparqlEndpoint();

		/* two services, one becomes exclusive group, the other is evaluated as service (filter) */
		prepareTest(Arrays.asList("/tests/data/data1.ttl", "/tests/data/data2.ttl", "/tests/data/data3.ttl",
				"/tests/data/data4.ttl"));
		execute("/tests/service/query05.rq", "/tests/service/query05.srx", false, true);
	}

	@Test
	public void test6() throws Exception {

		/*
		 * two services, one becomes exclusive group, the other is evaluated as service (filter), uses name of
		 * federation member in SERVICE
		 */
		prepareTest(Arrays.asList("/tests/data/data1.ttl", "/tests/data/data2.ttl", "/tests/data/data3.ttl",
				"/tests/data/data4.ttl"));
		execute("/tests/service/query06.rq", "/tests/service/query06.srx", false, true);
	}

	@Test
	public void test7() throws Exception {

		// evaluates by sparql endpoint URL, cannot be done with native store
		assumeSparqlEndpoint();

		/* two services, both evaluated as SERVICE (FILTER), uses name of federation member in SERVICE */
		prepareTest(Arrays.asList("/tests/data/data1.ttl", "/tests/data/data2.ttl", "/tests/data/data3.ttl",
				"/tests/data/data4.ttl"));
		execute("/tests/service/query07.rq", "/tests/service/query07.srx", false, true);
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
		execute("/tests/service/query08.rq", "/tests/service/query08.srx", false, true);
	}

	@Test
	public void test9() throws Exception {

		assumeSparqlEndpoint();

		FederatedServiceResolver serviceResolver = new SPARQLServiceResolver() {
			@Override
			protected FederatedService createService(String serviceUrl) throws QueryEvaluationException {
				return new TestSparqlFederatedService(serviceUrl, getHttpClientSessionManager());
			}
		};

		// workaround for test: shutdown and re-initialize in order to set a custom federated service
		FedXRepository repo = fedxRule.getRepository();
		repo.shutDown();
		repo.setFederatedServiceResolver(serviceResolver);
		repo.init();

		/*
		 * test select query retrieving all persons from endpoint 1 (SERVICE), endpoint not part of federation =>
		 * evaluate using externally provided service resolver endpoint1 is reachable as
		 * http://localhost:18080/repositories/endpoint1 via HTTP
		 */
		prepareTest(Arrays.asList("/tests/data/data1.ttl", "/tests/data/data2.ttl", "/tests/data/data3.ttl",
				"/tests/data/data4.ttl"));
		Endpoint endpoint1 = federationContext().getEndpointManager().getEndpointByName("http://endpoint1");
		fedxRule.removeEndpoint(endpoint1);
		execute("/tests/service/query03.rq", "/tests/service/query03.srx", false, false);

		Assertions.assertEquals(1,
				((TestSparqlFederatedService) serviceResolver
						.getService("http://localhost:18080/repositories/endpoint1")).serviceRequestCount.get());
	}

	@Test
	public void test10_serviceBoundJoin() throws Exception {

		assumeSparqlEndpoint();

		FederatedServiceResolver serviceResolver = new SPARQLServiceResolver() {
			@Override
			protected FederatedService createService(String serviceUrl) throws QueryEvaluationException {
				return new TestSparqlFederatedService(serviceUrl, getHttpClientSessionManager());
			}
		};

		// workaround for test: shutdown and re-initialize in order to set a custom federated service
		FedXRepository repo = fedxRule.getRepository();
		repo.shutDown();
		repo.setFederatedServiceResolver(serviceResolver);
		repo.init();

		/*
		 * test select query retrieving all persons from endpoint 1 (SERVICE), endpoint not part of federation =>
		 * evaluate using externally provided service resolver endpoint1 is reachable as
		 * http://localhost:18080/repositories/endpoint1 via HTTP
		 */
		prepareTest(Arrays.asList("/tests/data/data1.ttl", "/tests/data/data2.ttl", "/tests/data/data3.ttl",
				"/tests/data/data4.ttl"));
		Endpoint endpoint1 = federationContext().getEndpointManager().getEndpointByName("http://endpoint1");
		fedxRule.removeEndpoint(endpoint1);

		StringBuilder query = new StringBuilder();
		query.append("SELECT * WHERE { VALUES ?input { ");
		for (int i = 0; i < 50; i++) {
			query.append(" \"input").append(i).append("\" ");
		}
		query.append(" }");
		query.append(
				" SERVICE <http://localhost:18080/repositories/endpoint1> { BIND (CONCAT(?input, '_processed') AS ?output) } ");
		query.append(" }");

		try (TupleQueryResult tqr = queryManager().prepareTupleQuery(query.toString()).evaluate()) {
			List<BindingSet> res = Iterations.asList(tqr);
			Assertions.assertEquals(50, res.size());
			Set<Value> expected = Sets.newHashSet();
			for (int i = 0; i < 50; i++) {
				expected.add(SimpleValueFactory.getInstance().createLiteral("input" + i + "_processed"));
			}
			Assertions.assertEquals(expected, res.stream().map(b -> b.getValue("output")).collect(Collectors.toSet()));
		}

		// first binding is evaluated using regular service, then we have groups of 4 groups of three bindings and 3
		// groups with 15
		TestSparqlFederatedService tfs = ((TestSparqlFederatedService) serviceResolver
				.getService("http://localhost:18080/repositories/endpoint1"));
		Assertions.assertEquals(1, tfs.serviceRequestCount.get());
		Assertions.assertEquals(7, tfs.boundJoinRequestCount.get());
	}

	@Test
	public void test10_serviceSimpleEvaluation() throws Exception {

		assumeSparqlEndpoint();

		fedxRule.setConfig(c -> c.withEnableServiceAsBoundJoin(false));

		FederatedServiceResolver serviceResolver = new SPARQLServiceResolver() {
			@Override
			protected FederatedService createService(String serviceUrl) throws QueryEvaluationException {
				return new TestSparqlFederatedService(serviceUrl, getHttpClientSessionManager());
			}
		};

		// workaround for test: shutdown and re-initialize in order to set a custom federated service
		FedXRepository repo = fedxRule.getRepository();
		repo.shutDown();
		repo.setFederatedServiceResolver(serviceResolver);
		repo.init();

		/*
		 * test select query retrieving all persons from endpoint 1 (SERVICE), endpoint not part of federation =>
		 * evaluate using externally provided service resolver endpoint1 is reachable as
		 * http://localhost:18080/repositories/endpoint1 via HTTP
		 */
		prepareTest(Arrays.asList("/tests/data/data1.ttl", "/tests/data/data2.ttl", "/tests/data/data3.ttl",
				"/tests/data/data4.ttl"));
		Endpoint endpoint1 = federationContext().getEndpointManager().getEndpointByName("http://endpoint1");
		fedxRule.removeEndpoint(endpoint1);

		StringBuilder query = new StringBuilder();
		query.append("SELECT * WHERE { VALUES ?input { ");
		for (int i = 0; i < 50; i++) {
			query.append(" \"input").append(i).append("\" ");
		}
		query.append(" }");
		query.append(
				" SERVICE <http://localhost:18080/repositories/endpoint1> { BIND (CONCAT(?input, '_processed') AS ?output) } ");
		query.append(" }");

		try (TupleQueryResult tqr = queryManager().prepareTupleQuery(query.toString()).evaluate()) {
			List<BindingSet> res = Iterations.asList(tqr);
			Assertions.assertEquals(50, res.size());
			Set<Value> expected = Sets.newHashSet();
			for (int i = 0; i < 50; i++) {
				expected.add(SimpleValueFactory.getInstance().createLiteral("input" + i + "_processed"));
			}
			Assertions.assertEquals(expected, res.stream().map(b -> b.getValue("output")).collect(Collectors.toSet()));
		}

		// all input bindings are evaluated as simple join
		TestSparqlFederatedService tfs = ((TestSparqlFederatedService) serviceResolver
				.getService("http://localhost:18080/repositories/endpoint1"));
		Assertions.assertEquals(50, tfs.serviceRequestCount.get());
		Assertions.assertEquals(0, tfs.boundJoinRequestCount.get());
	}

	@Test
	public void test10_serviceSilent() throws Exception {

		assumeSparqlEndpoint();

		Repository localStore = new SailRepository(new MemoryStore());

		SPARQLServiceResolver serviceResolver = new SPARQLServiceResolver() {
			@Override
			protected FederatedService createService(String serviceUrl) throws QueryEvaluationException {
				if (serviceUrl.equals("urn:memStore")) {
					return new RepositoryFederatedService(localStore, true);
				}
				return new TestSparqlFederatedService(serviceUrl, getHttpClientSessionManager());
			}
		};

		// workaround for test: shutdown and re-initialize in order to set a custom service resolver
		FedXRepository repo = fedxRule.getRepository();
		repo.shutDown();
		repo.setFederatedServiceResolver(serviceResolver);
		repo.init();

		prepareTest(Arrays.asList("/tests/data/data1.ttl", "/tests/data/data2.ttl", "/tests/data/data3.ttl",
				"/tests/data/data4.ttl"));
		List<BindingSet> bs = Repositories.tupleQueryNoTransaction(fedxRule.repository,
				"SELECT * WHERE { VALUES ?input { 'input1'  } . SERVICE SILENT <urn:memStore> { BIND (CONCAT(?input, '_processed') AS ?output) } }",
				iter -> QueryResults.asList(iter));
		assertContainsAll(bs, "output", Sets.newHashSet(l("input1_processed")));

		serviceResolver.shutDown();
	}

	@Test
	@Disabled("test is flaky - see https://github.com/eclipse/rdf4j/issues/3160")
	public void test11_errorHandling() throws Exception {

		assumeSparqlEndpoint();

		/*
		 * test select query where SERVICE is not part of federation and produces error
		 */
		prepareTest(Arrays.asList("/tests/data/data1.ttl", "/tests/data/data2.ttl", "/tests/data/data3.ttl",
				"/tests/data/data4.ttl"));
		Endpoint endpoint1 = federationContext().getEndpointManager().getEndpointByName("http://endpoint1");
		fedxRule.removeEndpoint(endpoint1);

		// run a simple SERVICE query
		repoSettings(1).resetOperationsCounter();
		repoSettings(1).setFailAfter(0);
		String query_a = readQueryString("/tests/service/query11_error_a.rq");

		Assertions.assertThrows(QueryEvaluationException.class, () -> {
			Repositories.tupleQueryNoTransaction(fedxRule.repository, query_a,
					iter -> QueryResults.asList(iter));
		});

		// run query where service does not produce errors
		String query_b = readQueryString("/tests/service/query11_error_b.rq");
		repoSettings(1).setFailAfter(-1);
		List<BindingSet> bs = Repositories.tupleQueryNoTransaction(fedxRule.repository, query_b,
				iter -> QueryResults.asList(iter));
		Assertions
				.assertEquals(Sets.newHashSet("Person2", "Person5"),
						bs.stream()
								.map(b -> b.getValue("name").stringValue())
								.collect(Collectors.toSet()));

		// re-run, but now simulate errors
		repoSettings(1).resetOperationsCounter();
		repoSettings(1).setFailAfter(1);
		Assertions.assertThrows(QueryEvaluationException.class, () -> {
			Repositories.tupleQueryNoTransaction(fedxRule.repository, query_b,
					iter -> QueryResults.asList(iter));
		});

	}

	static class TestSparqlFederatedService extends SPARQLFederatedService {

		AtomicInteger serviceRequestCount = new AtomicInteger(0);
		AtomicInteger boundJoinRequestCount = new AtomicInteger(0);

		public TestSparqlFederatedService(String serviceUrl, HttpClientSessionManager client) {
			super(serviceUrl, client);
		}

		@Override
		public CloseableIteration<BindingSet, QueryEvaluationException> select(Service service,
				Set<String> projectionVars, BindingSet bindings, String baseUri) throws QueryEvaluationException {
			serviceRequestCount.incrementAndGet();
			return super.select(service, projectionVars, bindings, baseUri);
		}

		@Override
		public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(Service service,
				CloseableIteration<BindingSet, QueryEvaluationException> bindings, String baseUri)
				throws QueryEvaluationException {
			boundJoinRequestCount.incrementAndGet();
			return super.evaluate(service, bindings, baseUri);
		}

	}
}
