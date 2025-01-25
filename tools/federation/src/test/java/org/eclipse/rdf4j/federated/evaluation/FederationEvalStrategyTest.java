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

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.federated.SPARQLBaseTest;
import org.eclipse.rdf4j.federated.cache.SourceSelectionCache;
import org.eclipse.rdf4j.federated.cache.SourceSelectionCache.StatementSourceAssurance;
import org.eclipse.rdf4j.federated.endpoint.Endpoint;
import org.eclipse.rdf4j.federated.structures.SubQuery;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
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

	@Test
	public void testSourceSelectionCache_setBindings() throws Exception {

		var bob = Values.iri("http://example.com/bob");

		List<Endpoint> endpoints = prepareTest(
				Arrays.asList("/tests/basic/data_emptyStore.ttl", "/tests/basic/data_emptyStore.ttl"));

		Repository repo1 = getRepository(1);
		Repository repo2 = getRepository(2);

		String repo1Id = endpoints.get(0).getId();

		try (RepositoryConnection con = repo1.getConnection()) {
			con.add(bob, RDF.TYPE, FOAF.PERSON);
		}

		try (RepositoryConnection con = repo2.getConnection()) {
			con.add(FOAF.PERSON, RDF.TYPE, OWL.CLASS);
		}

		Repository fedxRepo = fedxRule.getRepository();

		fedxRule.enableDebug();

		try (var conn = fedxRepo.getConnection()) {

			TupleQuery tq = conn.prepareTupleQuery("SELECT * WHERE { ?s a ?type }");
			tq.setBinding("s", bob);

			try (var tqr = tq.evaluate()) {
				// just consume the result
				Assertions.assertEquals(Set.of(FOAF.PERSON),
						tqr.stream().map(bs -> bs.getValue("type")).collect(Collectors.toSet()));
			}
		}

		SourceSelectionCache cache = federationContext().getSourceSelectionCache();

		var assurance = cache.getAssurance(new SubQuery(bob, RDF.TYPE, null),
				federationContext().getEndpointManager().getEndpoint(repo1Id));

		// we expect that the source selection cache can assure statements
		Assertions.assertEquals(StatementSourceAssurance.HAS_REMOTE_STATEMENTS, assurance);
	}
}
