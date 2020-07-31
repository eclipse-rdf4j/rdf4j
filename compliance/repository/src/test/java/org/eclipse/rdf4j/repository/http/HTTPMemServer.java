/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository.http;

import java.io.File;
import java.io.IOException;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.config.RepositoryConfigUtil;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.eclipse.rdf4j.repository.manager.RepositoryProvider;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;

/**
 * @author Herko ter Horst
 */
public class HTTPMemServer {

	private static final String HOST = "localhost";

	private static final int PORT = 18081;

	private static final String TEST_REPO_ID = "Test";

	private static final String TEST_INFERENCE_REPO_ID = "Test-RDFS";

	private static final String RDF4J_CONTEXT = "/rdf4j";

	private static final String SERVER_URL = "http://" + HOST + ":" + PORT + RDF4J_CONTEXT;

	public static final String REPOSITORY_URL = Protocol.getRepositoryLocation(SERVER_URL, TEST_REPO_ID);

	public static String INFERENCE_REPOSITORY_URL = Protocol.getRepositoryLocation(SERVER_URL, TEST_INFERENCE_REPO_ID);

	private final Server jetty;

	public HTTPMemServer() {
		System.clearProperty("DEBUG");

		jetty = new Server(PORT);

		WebAppContext webapp = new WebAppContext();
		webapp.setContextPath(RDF4J_CONTEXT);
		// warPath configured in pom.xml maven-war-plugin configuration
		webapp.setWar("./target/rdf4j-server");
		jetty.setHandler(webapp);
	}

	public void start() throws Exception {
		File dataDir = new File(System.getProperty("user.dir") + "/target/datadir");
		dataDir.mkdirs();
		System.setProperty("org.eclipse.rdf4j.appdata.basedir", dataDir.getAbsolutePath());

		jetty.start();

		// give the webapp some time to get started - necessary when running multiple test suites in quick succession
		Thread.sleep(1500);

		createTestRepositories();
	}

	public void stop() throws Exception {
		RepositoryManager repositoryManager = RepositoryProvider.getRepositoryManager(SERVER_URL);
		repositoryManager.removeRepository(TEST_REPO_ID);
		repositoryManager.removeRepository(TEST_INFERENCE_REPO_ID);

		jetty.stop();
		System.clearProperty("org.mortbay.log.class");
	}

	private void createTestRepositories() throws RepositoryException, RepositoryConfigException, RDFParseException,
			UnsupportedRDFormatException, IOException {
		RepositoryManager repositoryManager = RepositoryProvider.getRepositoryManager(SERVER_URL);

		if (!repositoryManager.hasRepositoryConfig(TEST_REPO_ID)) {
			Model testRepoConfig = Rio.parse(getClass().getResourceAsStream("/fixtures/memory.ttl"), "",
					RDFFormat.TURTLE);
			repositoryManager
					.addRepositoryConfig(RepositoryConfigUtil.getRepositoryConfig(testRepoConfig, TEST_REPO_ID));
		}

		if (!repositoryManager.hasRepositoryConfig(INFERENCE_REPOSITORY_URL)) {
			Model testRdfsRepoConfig = Rio.parse(getClass().getResourceAsStream("/fixtures/memory-rdfs.ttl"), "",
					RDFFormat.TURTLE);
			repositoryManager
					.addRepositoryConfig(
							RepositoryConfigUtil.getRepositoryConfig(testRdfsRepoConfig, TEST_INFERENCE_REPO_ID));
		}
	}
}
