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

import java.io.File;

import org.eclipse.rdf4j.federated.repository.FedXRepository;
import org.eclipse.rdf4j.federated.server.SPARQLEmbeddedServer;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FedXFactoryTest extends SPARQLServerBaseTest {

	// set during test runtime
	private FederationContext federationContext;

	@BeforeEach
	public void before() {
		federationContext = null;
	}

	@Test
	public void testFederationWithResolver() throws Exception {

		assumeSparqlEndpoint();

		// load some data into endpoint1 and endpoint2
		loadDataSet(server.getRepository(1), "/tests/medium/data1.ttl");
		loadDataSet(server.getRepository(2), "/tests/medium/data2.ttl");

		RepositoryResolver repositoryResolver = ((SPARQLEmbeddedServer) server).getRepositoryResolver();

		FedXRepository repo = FedXFactory.newFederation()
				.withRepositoryResolver(repositoryResolver)
				.withResolvableEndpoint("endpoint1")
				.withResolvableEndpoint("endpoint2")
				.create();

		repo.init();
		federationContext = repo.getFederationContext();
		try (RepositoryConnection conn = repo.getConnection()) {
			execute("/tests/medium/query01.rq", "/tests/medium/query01.srx", false, true);
		}

		repo.shutDown();
	}

	@Test
	public void testFederationWithResolver_writable() throws Exception {

		assumeSparqlEndpoint();

		// load some data into endpoint2
		loadDataSet(server.getRepository(2), "/tests/medium/data2.ttl");

		RepositoryResolver repositoryResolver = ((SPARQLEmbeddedServer) server).getRepositoryResolver();

		FedXRepository repo = FedXFactory.newFederation()
				.withRepositoryResolver(repositoryResolver)
				.withResolvableEndpoint("endpoint1", true)
				.withResolvableEndpoint("endpoint2")
				.create();

		repo.init();

		// load data into the endpoint1 via the federation
		loadDataSet(repo, "/tests/medium/data1.ttl");

		federationContext = repo.getFederationContext();
		try (RepositoryConnection conn = repo.getConnection()) {
			execute("/tests/medium/query01.rq", "/tests/medium/query01.srx", false, true);
		}

		repo.shutDown();
	}

	@Test
	public void testFederationWithResolver_DataConfig() throws Exception {

		assumeSparqlEndpoint();

		// load some data into endpoint1 and endpoint2
		loadDataSet(server.getRepository(1), "/tests/medium/data1.ttl");
		loadDataSet(server.getRepository(2), "/tests/medium/data2.ttl");

		RepositoryResolver repositoryResolver = ((SPARQLEmbeddedServer) server).getRepositoryResolver();

		File dataConfig = toFile("/tests/dataconfig/resolvableRepositories.ttl");

		FedXRepository repo = FedXFactory.newFederation()
				.withRepositoryResolver(repositoryResolver)
				.withMembers(dataConfig)
				.create();

		repo.init();
		federationContext = repo.getFederationContext();
		try (RepositoryConnection conn = repo.getConnection()) {
			execute("/tests/medium/query01.rq", "/tests/medium/query01.srx", false, true);
		}

		repo.shutDown();
	}

	protected File toFile(String resource) throws Exception {
		return new File(FedXFactoryTest.class.getResource(resource).toURI());
	}

	@Override
	protected FederationContext federationContext() {
		return this.federationContext;
	}
}
