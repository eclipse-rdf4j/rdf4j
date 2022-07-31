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
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.common.platform.PlatformFactory;
import org.eclipse.rdf4j.federated.repository.FedXRepository;
import org.eclipse.rdf4j.federated.server.SPARQLEmbeddedServer;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.base.RepositoryWrapper;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Sets;

public class FedXInRDF4JWorkbenchTest extends SPARQLServerBaseTest {

	@Test
	public void testFederation() throws Exception {

		assumeSparqlEndpoint();

		// load some data into endpoint1 and endpoint2
		loadDataSet(server.getRepository(1), "/tests/medium/data1.ttl");
		loadDataSet(server.getRepository(2), "/tests/medium/data2.ttl");

		final String repositoryId = "my-federation";
		final SPARQLEmbeddedServer rdf4jServer = (SPARQLEmbeddedServer) server;
		final File dataDir = rdf4jServer.getDataDir();
		String repoPath = "server/repositories";
		if (PlatformFactory.getPlatform().dataDirPreserveCase()) {
			repoPath = "Server/repositories";
		}
		final File repositoriesDir = new File(dataDir, repoPath);

		// preparation: add configuration files to the repository
		File fedXDataDir = new File(repositoriesDir, repositoryId);
		fedXDataDir.mkdirs();

		FileUtils.copyFile(toFile("/tests/rdf4jserver/config.ttl"), new File(fedXDataDir, "config.ttl"));

		String fedXSparqlUrl = rdf4jServer.getRepositoryUrl(repositoryId);
		SPARQLRepository repo = new SPARQLRepository(fedXSparqlUrl);
		repo.init();

		try (RepositoryConnection conn = repo.getConnection()) {
			// simple check: make sure that expected data is present
			Assertions.assertEquals(30, conn.size());
		}

		repo.shutDown();

		// temporary workaround: shutdown the federation repository explicitly here to
		// avoid a long running test. This is because the federation keeps an open
		// connection to other endpoints hosted in the same server, and the shutdown
		// sequence is arbitrary.
		Repository fedx = rdf4jServer.getRepositoryResolver().getRepository(repositoryId);
		fedx.shutDown();
	}

	@Test
	public void testFederation_WithDataConfig() throws Exception {

		assumeSparqlEndpoint();

		// load some data into endpoint1 and endpoint2
		loadDataSet(server.getRepository(1), "/tests/medium/data1.ttl");
		loadDataSet(server.getRepository(2), "/tests/medium/data2.ttl");

		final String repositoryId = "my-federation";
		final SPARQLEmbeddedServer rdf4jServer = (SPARQLEmbeddedServer) server;
		final File dataDir = rdf4jServer.getDataDir();
		String repoPath = "server/repositories";
		if (PlatformFactory.getPlatform().dataDirPreserveCase()) {
			repoPath = "Server/repositories";
		}
		final File repositoriesDir = new File(dataDir, repoPath);

		// preparation: add configuration files to the repository
		File fedXDataDir = new File(repositoriesDir, repositoryId);
		fedXDataDir.mkdirs();

		FileUtils.copyFile(toFile("/tests/rdf4jserver/config-withDataConfig.ttl"), new File(fedXDataDir, "config.ttl"));
		FileUtils.copyFile(toFile("/tests/rdf4jserver/dataConfig.ttl"), new File(fedXDataDir, "dataConfig.ttl"));

		String fedXSparqlUrl = rdf4jServer.getRepositoryUrl(repositoryId);
		SPARQLRepository repo = new SPARQLRepository(fedXSparqlUrl);
		repo.init();

		try (RepositoryConnection conn = repo.getConnection()) {
			// simple check: make sure that expected data is present
			Assertions.assertEquals(30, conn.size());
		}

		repo.shutDown();

		// temporary workaround: shutdown the federation repository explicitly here to
		// avoid a long running test. This is because the federation keeps an open
		// connection to other endpoints hosted in the same server, and the shutdown
		// sequence is arbitrary.
		Repository fedx = rdf4jServer.getRepositoryResolver().getRepository(repositoryId);
		fedx.shutDown();
	}

	@Test
	public void testFederationFilter() throws Exception {

		assumeSparqlEndpoint();

		// load some data into endpoint1 and endpoint2
		loadDataSet(server.getRepository(1), "/tests/data/data1.ttl");
		loadDataSet(server.getRepository(2), "/tests/data/data2.ttl");

		final String repositoryId = "my-federation";
		final SPARQLEmbeddedServer rdf4jServer = (SPARQLEmbeddedServer) server;
		final File dataDir = rdf4jServer.getDataDir();
		String repoPath = "server/repositories";
		if (PlatformFactory.getPlatform().dataDirPreserveCase()) {
			repoPath = "Server/repositories";
		}
		final File repositoriesDir = new File(dataDir, repoPath);

		// preparation: add configuration files to the repository
		File fedXDataDir = new File(repositoriesDir, repositoryId);
		fedXDataDir.mkdirs();

		FileUtils.copyFile(toFile("/tests/rdf4jserver/config.ttl"), new File(fedXDataDir, "config.ttl"));

		String fedXSparqlUrl = rdf4jServer.getRepositoryUrl(repositoryId);
		SPARQLRepository repo = new SPARQLRepository(fedXSparqlUrl);
		repo.init();

		String query = "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n" +
				"PREFIX ns1: <http://namespace1.org/> "
				+ "SELECT * WHERE { "
				+ "?person a foaf:Person ."
				+ "?person foaf:name ?name .\n"
				+ "FILTER (?name IN ('Person1', 'Person2')) "
				+ "}";

		try (RepositoryConnection conn = repo.getConnection()) {
			try (TupleQueryResult tqr = conn.prepareTupleQuery(query).evaluate()) {

				List<BindingSet> res = Iterations.asList(tqr);
				assertContainsAll(res, "person",
						Sets.newHashSet(iri("http://namespace1.org/", "Person_1"),
								iri("http://namespace1.org/", "Person_2")));
			}
		}

		repo.shutDown();

		// temporary workaround: shutdown the federation repository explicitly here to
		// avoid a long running test. This is because the federation keeps an open
		// connection to other endpoints hosted in the same server, and the shutdown
		// sequence is arbitrary.
		Repository fedx = rdf4jServer.getRepositoryResolver().getRepository(repositoryId);
		fedx.shutDown();
	}

	protected File toFile(String resource) throws Exception {
		return new File(FedXInRDF4JWorkbenchTest.class.getResource(resource).toURI());
	}

	@Override
	protected FederationContext federationContext() {
		throw new UnsupportedOperationException("Not available in this test.");
	}

	protected FedXRepository getFedXRepository(String repositoryId) {
		RepositoryWrapper wrapper = (RepositoryWrapper) ((SPARQLEmbeddedServer) server).getRepositoryResolver()
				.getRepository(repositoryId);
		return (FedXRepository) wrapper.getDelegate();
	}
}
