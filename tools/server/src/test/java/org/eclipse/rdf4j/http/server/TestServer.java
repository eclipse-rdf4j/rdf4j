/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.http.server;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.manager.RemoteRepositoryManager;
import org.eclipse.rdf4j.repository.sail.config.SailRepositoryConfig;
import org.eclipse.rdf4j.sail.inferencer.fc.config.SchemaCachingRDFSInferencerConfig;
import org.eclipse.rdf4j.sail.memory.config.MemoryStoreConfig;
import org.eclipse.rdf4j.sail.shacl.config.ShaclSailConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Herko ter Horst
 */
public class TestServer {

	private static final Logger logger = LoggerFactory.getLogger(TestServer.class);

	private static final String HOST = "localhost";

	private static final int PORT = 18080;

	public static final String TEST_REPO_ID = "Test";

	public static final String TEST_INFERENCE_REPO_ID = "Test-RDFS";
	public static final String TEST_SHACL_REPO_ID = "Test-SHACL";

	private static final String RDF4J_CONTEXT = "/rdf4j";

	public static final String SERVER_URL = "http://" + HOST + ":" + PORT + RDF4J_CONTEXT;
	public static String REPOSITORY_URL = Protocol.getRepositoryLocation(SERVER_URL, TEST_REPO_ID);

	private final RemoteRepositoryManager manager;

	private final Server jetty;

	public TestServer() throws IOException {
		System.clearProperty("DEBUG");
		PropertiesReader reader = new PropertiesReader("maven-config.properties");
		String webappDir = reader.getProperty("testserver.webapp.dir");
		logger.debug("build path: {}", webappDir);

		jetty = new Server();

		ServerConnector conn = new ServerConnector(jetty);
		conn.setHost(HOST);
		conn.setPort(PORT);
		jetty.addConnector(conn);

		WebAppContext webapp = new WebAppContext();
		webapp.addSystemClass("org.slf4j.");
		webapp.addSystemClass("ch.qos.logback.");
		webapp.setContextPath(RDF4J_CONTEXT);
		// warPath configured in pom.xml maven-war-plugin configuration
		webapp.setWar("./target/rdf4j-server");
		jetty.setHandler(webapp);

		manager = RemoteRepositoryManager.getInstance(SERVER_URL);
	}

	public void start() throws Exception {
		File dataDir = new File(System.getProperty("user.dir") + "/target/datadir");
		dataDir.mkdirs();
		System.setProperty("org.eclipse.rdf4j.appdata.basedir", dataDir.getAbsolutePath());

		jetty.start();
		createTestRepositories();
	}

	public void stop() throws Exception {
		try {
			manager.getAllRepositoryInfos().forEach(ri -> manager.removeRepository(ri.getId()));
			manager.shutDown();
		} finally {
			jetty.stop();
			System.clearProperty("org.mortbay.log.class");
		}
	}

	private void createTestRepositories() throws RepositoryException, RepositoryConfigException {
		// create a (non-inferencing) memory store
		MemoryStoreConfig memStoreConfig = new MemoryStoreConfig();
		SailRepositoryConfig sailRepConfig = new SailRepositoryConfig(memStoreConfig);
		RepositoryConfig repConfig = new RepositoryConfig(TEST_REPO_ID, sailRepConfig);
		manager.addRepositoryConfig(repConfig);

		// create an inferencing memory store
		SchemaCachingRDFSInferencerConfig inferMemStoreConfig = new SchemaCachingRDFSInferencerConfig(
				new MemoryStoreConfig());
		sailRepConfig = new SailRepositoryConfig(inferMemStoreConfig);
		repConfig = new RepositoryConfig(TEST_INFERENCE_REPO_ID, sailRepConfig);
		manager.addRepositoryConfig(repConfig);

		// create memory store with shacl support
		ShaclSailConfig shaclConfig = new ShaclSailConfig(new MemoryStoreConfig());
		sailRepConfig = new SailRepositoryConfig(shaclConfig);
		repConfig = new RepositoryConfig(TEST_SHACL_REPO_ID, sailRepConfig);
		manager.addRepositoryConfig(repConfig);
	}

	static class PropertiesReader {
		private final Properties properties;

		public PropertiesReader(String propertyFileName) throws IOException {
			InputStream is = getClass().getClassLoader()
					.getResourceAsStream(propertyFileName);
			this.properties = new Properties();
			this.properties.load(is);
		}

		public String getProperty(String propertyName) {
			return this.properties.getProperty(propertyName);
		}
	}

}
