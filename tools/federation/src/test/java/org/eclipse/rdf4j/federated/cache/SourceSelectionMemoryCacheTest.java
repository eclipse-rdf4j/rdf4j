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
package org.eclipse.rdf4j.federated.cache;

import java.util.Arrays;
import java.util.List;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.federated.SPARQLBaseTest;
import org.eclipse.rdf4j.federated.cache.SourceSelectionCache.StatementSourceAssurance;
import org.eclipse.rdf4j.federated.endpoint.Endpoint;
import org.eclipse.rdf4j.federated.endpoint.EndpointFactory;
import org.eclipse.rdf4j.federated.monitoring.MonitoringImpl.MonitoringInformation;
import org.eclipse.rdf4j.federated.monitoring.MonitoringService;
import org.eclipse.rdf4j.federated.structures.SubQuery;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SourceSelectionMemoryCacheTest extends SPARQLBaseTest {

	@Override
	protected void initFedXConfig() {
		fedxRule.withConfiguration(c -> c.withEnableMonitoring(true));
	}

	@Test
	public void test_neverCacheUnbound() throws Exception {

		// just execute for one kind of test environment
		assumeSparqlEndpoint();

		SourceSelectionMemoryCache cache = new SourceSelectionMemoryCache();

		Endpoint ep = EndpointFactory.loadResolvableRepository("dummy");

		SubQuery s1 = new SubQuery(null, null, null);

		Assertions.assertEquals(StatementSourceAssurance.POSSIBLY_HAS_STATEMENTS, cache.getAssurance(s1, ep));

		cache.updateInformation(s1, ep, true);
		Assertions.assertEquals(StatementSourceAssurance.POSSIBLY_HAS_STATEMENTS, cache.getAssurance(s1, ep));

	}

	@Test
	public void test_inferGeneralized() throws Exception {

		// just execute for one kind of test environment
		assumeSparqlEndpoint();

		SourceSelectionMemoryCache cache = new SourceSelectionMemoryCache();

		Endpoint ep = EndpointFactory.loadResolvableRepository("dummy");

		SubQuery s1 = new SubQuery(null, FOAF.NAME, l("Alan"));
		SubQuery s2 = new SubQuery(null, FOAF.NAME, null);

		Assertions.assertEquals(StatementSourceAssurance.POSSIBLY_HAS_STATEMENTS, cache.getAssurance(s1, ep));

		cache.updateInformation(s1, ep, true);

		Assertions.assertEquals(StatementSourceAssurance.HAS_REMOTE_STATEMENTS, cache.getAssurance(s1, ep));
		Assertions.assertEquals(StatementSourceAssurance.HAS_REMOTE_STATEMENTS, cache.getAssurance(s2, ep));

	}

	@Test
	public void test_inferGeneralized2() throws Exception {

		// just execute for one kind of test environment
		assumeSparqlEndpoint();

		SourceSelectionMemoryCache cache = new SourceSelectionMemoryCache();

		Endpoint ep = EndpointFactory.loadResolvableRepository("dummy");

		SubQuery s1 = new SubQuery(null, FOAF.NAME, l("Alan"));
		SubQuery s2 = new SubQuery(null, FOAF.NAME, null);

		Assertions.assertEquals(StatementSourceAssurance.POSSIBLY_HAS_STATEMENTS, cache.getAssurance(s1, ep));

		cache.updateInformation(s1, ep, false);

		Assertions.assertEquals(StatementSourceAssurance.NONE, cache.getAssurance(s1, ep));

		// we cannot infer any information for endpoint for subquery s2 and have to check
		Assertions.assertEquals(StatementSourceAssurance.POSSIBLY_HAS_STATEMENTS, cache.getAssurance(s2, ep));

	}

	@Test
	public void test_inferGeneralized3() throws Exception {

		// just execute for one kind of test environment
		assumeSparqlEndpoint();

		SourceSelectionMemoryCache cache = new SourceSelectionMemoryCache();

		Endpoint ep = EndpointFactory.loadResolvableRepository("dummy");

		SubQuery s1 = new SubQuery(null, FOAF.NAME, l("Alan"));
		SubQuery s2 = new SubQuery(null, FOAF.NAME, null);

		Assertions.assertEquals(StatementSourceAssurance.POSSIBLY_HAS_STATEMENTS, cache.getAssurance(s1, ep));

		cache.updateInformation(s2, ep, false);

		Assertions.assertEquals(StatementSourceAssurance.NONE, cache.getAssurance(s2, ep));

		// we can infer that ep does not have statements for the more specific subquery s3
		Assertions.assertEquals(StatementSourceAssurance.NONE, cache.getAssurance(s1, ep));
	}

	@Test
	public void testCache_Integration() throws Exception {
		// just execute for one kind of test environment
		assumeSparqlEndpoint();

		List<Endpoint> endpoints = prepareTest(
				Arrays.asList("/tests/basic/data01endpoint1.ttl", "/tests/basic/data01endpoint2.ttl"));

		String query = "SELECT * WHERE { ?person <" + FOAF.NAME + "> ?name }";
		try (TupleQueryResult tqr = federationContext().getQueryManager().prepareTupleQuery(query).evaluate()) {
			Assertions.assertEquals(2, Iterations.asList(tqr).size());
		}

		// 1 request for source selection, 1 for fetching data
		Assertions.assertEquals(2, requestsForEndpoint(endpoints.get(0)));
		// 1 request for source selection, no data
		Assertions.assertEquals(1, requestsForEndpoint(endpoints.get(1)));

		monitoring().resetMonitoringInformation();

		try (TupleQueryResult tqr = federationContext().getQueryManager().prepareTupleQuery(query).evaluate()) {
			Assertions.assertEquals(2, Iterations.asList(tqr).size());
		}

		// source selection is cached, only from fetching data
		Assertions.assertEquals(1, requestsForEndpoint(endpoints.get(0)));
		Assertions.assertEquals(0, requestsForEndpoint(endpoints.get(1)));
	}

	@Test
	public void testCache_Integration_InferredCachePatterns() throws Exception {

		// just execute for one kind of test environment
		assumeSparqlEndpoint();

		// assumption of this test: if we know that an endpoint can provide
		// statements {s, foaf:name, "Alan"}, it can also provide a statement
		// for {s, foaf:name, ?name}

		List<Endpoint> endpoints = prepareTest(
				Arrays.asList("/tests/basic/data01endpoint1.ttl", "/tests/basic/data01endpoint2.ttl"));

		String query = "SELECT * WHERE { ?person <" + FOAF.NAME + "> 'Alan' }";
		try (TupleQueryResult tqr = federationContext().getQueryManager().prepareTupleQuery(query).evaluate()) {
			Assertions.assertEquals(1, Iterations.asList(tqr).size());
		}

		// 1 request for source selection, 1 for fetching data
		Assertions.assertEquals(2, requestsForEndpoint(endpoints.get(0)));
		// 1 request for source selection, no data
		Assertions.assertEquals(1, requestsForEndpoint(endpoints.get(1)));

		monitoring().resetMonitoringInformation();

		String query2 = "SELECT * WHERE { ?person <" + FOAF.NAME + "> ?name }";
		try (TupleQueryResult tqr = federationContext().getQueryManager().prepareTupleQuery(query2).evaluate()) {
			Assertions.assertEquals(2, Iterations.asList(tqr).size());
		}

		// source selection is cached for endpoint 1 (can be inferred) only from fetching data
		Assertions.assertEquals(1, requestsForEndpoint(endpoints.get(0)));

		// source selection for endpoint 2 cannot be inferred and is redone
		Assertions.assertEquals(1, requestsForEndpoint(endpoints.get(1)));
	}

	@Test
	public void testCache_Integration_InferredCachePatterns2() throws Exception {

		// just execute for one kind of test environment
		assumeSparqlEndpoint();

		// assumption of this test: if we know that an endpoint cannot provide
		// statements {s, foaf:name, ?o}, it neither can provide a statement
		// for {s, foaf:name, "Alan"}

		List<Endpoint> endpoints = prepareTest(
				Arrays.asList("/tests/basic/data01endpoint1.ttl", "/tests/basic/data01endpoint2.ttl"));

		String query = "SELECT * WHERE { ?person <" + FOAF.NAME + "> ?name }";
		try (TupleQueryResult tqr = federationContext().getQueryManager().prepareTupleQuery(query).evaluate()) {
			Assertions.assertEquals(2, Iterations.asList(tqr).size());
		}

		// 1 request for source selection, 1 for fetching data
		Assertions.assertEquals(2, requestsForEndpoint(endpoints.get(0)));
		// 1 request for source selection, no data
		Assertions.assertEquals(1, requestsForEndpoint(endpoints.get(1)));

		monitoring().resetMonitoringInformation();

		String query2 = "SELECT * WHERE { ?person <" + FOAF.NAME + "> 'Alan' }";
		try (TupleQueryResult tqr = federationContext().getQueryManager().prepareTupleQuery(query2).evaluate()) {
			Assertions.assertEquals(1, Iterations.asList(tqr).size());
		}

		// source selection is redone, we cannot infer information for the more specific pattern + data
		Assertions.assertEquals(2, requestsForEndpoint(endpoints.get(0)));

		// source selection for endpoint 2 can be inferred
		Assertions.assertEquals(0, requestsForEndpoint(endpoints.get(1)));
	}

	private int requestsForEndpoint(Endpoint endpoint) {
		MonitoringInformation m = monitoring().getMonitoringInformation(endpoint);
		return m == null ? 0 : m.getNumberOfRequests();
	}

	private MonitoringService monitoring() {
		return (MonitoringService) federationContext().getMonitoringService();
	}
}
