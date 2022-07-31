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
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.federated.endpoint.Endpoint;
import org.eclipse.rdf4j.federated.repository.RepositorySettings;
import org.eclipse.rdf4j.federated.server.NativeStoreServer;
import org.eclipse.rdf4j.federated.server.SPARQLEmbeddedServer;
import org.eclipse.rdf4j.federated.server.Server;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for any federation test, this class is self-contained with regard to testing if run in a distinct JVM.
 *
 * @author as
 *
 */
public abstract class SPARQLServerBaseTest extends FedXBaseTest {

	/**
	 * The repository type used for testing
	 */
	public enum REPOSITORY_TYPE {
		SPARQLREPOSITORY,
		REMOTEREPOSITORY,
		NATIVE
	}

	protected static final int MAX_ENDPOINTS = 4;

	public static Logger log;

	/**
	 * the server, e.g. SparqlEmbeddedServer or NativeStoreServer
	 */
	protected static Server server;

	@TempDir
	static Path tempDir;

	private static REPOSITORY_TYPE repositoryType = REPOSITORY_TYPE.SPARQLREPOSITORY;

	@BeforeAll
	public static void initTest() throws Exception {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "true");

		log = LoggerFactory.getLogger(SPARQLServerBaseTest.class);

		if (System.getProperty("repositoryType") != null) {
			repositoryType = REPOSITORY_TYPE.valueOf(System.getProperty("repositoryType"));
		}

		switch (repositoryType) {
		case NATIVE:
			initializeLocalNativeStores();
			break;
		case REMOTEREPOSITORY:
		case SPARQLREPOSITORY:
		default:
			initializeServer();
		}
	}

	@AfterAll
	public static void afterTest() throws Exception {
		if (server != null) {
			server.shutdown();
		}
		System.setProperty("org.eclipse.rdf4j.repository.debug", "false");
	}

	@BeforeEach
	public void beforeEachTest() throws Exception {
		// reset operations counter and fail after
		for (int i = 1; i <= MAX_ENDPOINTS; i++) {
			RepositorySettings repoSettings = repoSettings(i);
			repoSettings.resetOperationsCounter();
			repoSettings.setFailAfter(-1);
		}
	}

	public boolean isSPARQLServer() {
		return server instanceof SPARQLEmbeddedServer;
	}

	/**
	 * Initialization of the embedded web server hosting an openrdf workbench. Used for remote and sparql repository
	 * setting
	 *
	 * @throws Exception
	 */
	private static void initializeServer() throws Exception {

		// set up the server: the maximal number of endpoints must be known
		List<String> repositoryIds = new ArrayList<>(MAX_ENDPOINTS);
		for (int i = 1; i <= MAX_ENDPOINTS; i++) {
			repositoryIds.add("endpoint" + i);
		}
		File dataDir = new File(tempDir.toFile(), "datadir");
		server = new SPARQLEmbeddedServer(dataDir, repositoryIds, repositoryType == REPOSITORY_TYPE.REMOTEREPOSITORY);

		server.initialize(MAX_ENDPOINTS);
	}

	/**
	 * Initialization of the embedded web server hosting an openrdf workbench. Used for remote and sparql repository
	 * setting
	 *
	 * @throws Exception
	 */
	private static void initializeLocalNativeStores() throws Exception {

		File dataDir = new File(tempDir.toFile(), "datadir");
		server = new NativeStoreServer(dataDir);
		server.initialize(MAX_ENDPOINTS);
	}

	/**
	 * Get the repository, initialized repositories are called
	 *
	 * endpoint1 endpoint2 .. endpoint%MAX_ENDPOINTS%
	 *
	 * @param i the index of the repository, starting with 1
	 * @return
	 */
	protected static Repository getRepository(int i) {
		return server.getRepository(i);
	}

	protected List<Endpoint> prepareTest(List<String> sparqlEndpointData) throws Exception {

		// clear federation
		federationContext().getManager().removeAll();

		// prepare the test endpoints (i.e. load data)
		if (sparqlEndpointData.size() > MAX_ENDPOINTS) {
			throw new RuntimeException("MAX_ENDPOINTs to low, " + sparqlEndpointData.size()
					+ " repositories needed. Adjust configuration");
		}

		int i = 1; // endpoint id, start with 1
		for (String s : sparqlEndpointData) {
			loadDataSet(server.getRepository(i++), s);
		}

		// configure federation
		List<Endpoint> endpoints = new ArrayList<>();
		for (i = 1; i <= sparqlEndpointData.size(); i++) {
			Endpoint e = server.loadEndpoint(i);
			endpoints.add(e);
			federationContext().getManager().addEndpoint(e, true);
		}
		return endpoints;
	}

	/**
	 * Load a dataset. Note: the repositories are cleared before loading data
	 *
	 * @param rep
	 * @param datasetFile
	 * @throws RDFParseException
	 * @throws RepositoryException
	 * @throws IOException
	 */
	protected void loadDataSet(Repository rep, String datasetFile)
			throws RDFParseException, RepositoryException, IOException {
		log.debug("loading dataset...");
		InputStream dataset = SPARQLServerBaseTest.class.getResourceAsStream(datasetFile);

		boolean needToShutdown = false;
		if (!rep.isInitialized()) {
			rep.init();
			needToShutdown = true;
		}
		RepositoryConnection con = rep.getConnection();
		try {
			con.clear();
			con.add(dataset, "", Rio.getParserFormatForFileName(datasetFile).get());
		} finally {
			dataset.close();
			con.close();
			if (needToShutdown) {
				rep.shutDown();
			}
		}
		log.debug("dataset loaded.");
	}

	protected void ignoreForNativeStore() {
		// ignore these tests for native store
		Assumptions.assumeTrue(isSPARQLServer(), "Test is ignored for native store federation");
	}

	protected void assumeNativeStore() {
		Assumptions.assumeTrue(server instanceof NativeStoreServer,
				"Test can be executed with native store federation only.");
	}

	protected void assumeSparqlEndpoint() {
		Assumptions.assumeTrue(repositoryType == REPOSITORY_TYPE.SPARQLREPOSITORY,
				"Test can be executed for SPARQL Repository only.");
	}

	/**
	 * Return the {@link RepositorySettings} for configuring the repository
	 *
	 * @param endpoint the endpoint index, starting with 1
	 * @return
	 */
	protected RepositorySettings repoSettings(int endpoint) {
		return server.getRepository(endpoint);
	}

}
