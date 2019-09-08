/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package com.fluidops.fedx;

import java.io.File;

import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResolver;
import org.junit.jupiter.api.Test;

import com.fluidops.fedx.repository.FedXRepository;
import com.fluidops.fedx.server.SPARQLEmbeddedServer;

public class FedXFactoryTest extends SPARQLServerBaseTest {


	@Test
	public void testFederationWithResolver() throws Exception {
		
		assumeSparqlEndpoint();
		
		// load some data into endpoint1 and endpoint2
		loadDataSet(server.getRepository(1), "/tests/medium/data1.ttl");
		loadDataSet(server.getRepository(2), "/tests/medium/data2.ttl");
		
		RepositoryResolver repositoryResolver = ((SPARQLEmbeddedServer) server).getRepositoryResolver();
		
		FedXRepository repo = FedXFactory.newFederation().withRepositoryResolver(repositoryResolver)
					.withResolvableEndpoint("endpoint1")
					.withResolvableEndpoint("endpoint2")
				.create();

		try (RepositoryConnection conn = repo.getConnection()) {
			execute(conn, "/tests/medium/query01.rq", "/tests/medium/query01.srx", false);
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
		
		FedXRepository repo = FedXFactory.newFederation().withRepositoryResolver(repositoryResolver)
					.withMembers(dataConfig)
				.create();
		
		try (RepositoryConnection conn = repo.getConnection()) {
			execute(conn, "/tests/medium/query01.rq", "/tests/medium/query01.srx", false);
		}

		repo.shutDown();
	}

	protected File toFile(String resource) throws Exception {
		return new File(FedXFactoryTest.class.getResource(resource).toURI());
	}
}
