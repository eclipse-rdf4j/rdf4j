package com.fluidops.fedx;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fluidops.fedx.server.SPARQLEmbeddedServer;

public class FedXInRDF4JWorkbenchTest extends SPARQLServerBaseTest {


	@Test
	public void testFederation() throws Exception {

		assumeSparqlEndpoint();

		// load some data into endpoint1 and endpoint2
		loadDataSet(server.getRepository(1), "/tests/medium/data1.ttl");
		loadDataSet(server.getRepository(2), "/tests/medium/data2.ttl");

		final String repositoryId = "myFederation";
		final SPARQLEmbeddedServer rdf4jServer = (SPARQLEmbeddedServer) server;
		final File dataDir = rdf4jServer.getDataDir();
		final File repositoriesDir = new File(dataDir, "Server/repositories/");

		// preparation: add configuration files to the repository
		File fedXDataDir = new File(repositoriesDir, repositoryId);
		fedXDataDir.mkdirs();

		FileUtils.copyFile(toFile("/tests/rdf4jserver/config.ttl"), new File(fedXDataDir, "config.ttl"));
		FileUtils.copyFile(toFile("/tests/rdf4jserver/dataConfig.ttl"), new File(fedXDataDir, "dataConfig.ttl"));
		FileUtils.copyFile(toFile("/tests/rdf4jserver/fedxConfig.prop"), new File(fedXDataDir, "fedxConfig.prop"));

		String fedXSparqlUrl = rdf4jServer.getRepositoryUrl(repositoryId);
		SPARQLRepository repo = new SPARQLRepository(fedXSparqlUrl);
		repo.initialize();

		try (RepositoryConnection conn = repo.getConnection()) {
			// simple check: make sure that expected data is present
			Assertions.assertEquals(30, conn.size());
		}

		repo.shutDown();

		// check that cache is persisted in the expected location
		FederationManager.getInstance().getCache().persist();
		Assertions.assertTrue(new File(fedXDataDir, "cache.db").isFile());

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
}
