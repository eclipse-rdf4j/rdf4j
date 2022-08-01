/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.parser.sparql;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.manager.RemoteRepositoryManager;
import org.eclipse.rdf4j.repository.sail.config.SailRepositoryConfig;
import org.eclipse.rdf4j.sail.memory.config.MemoryStoreConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An embedded http server for SPARQL query testing. Initializes a memory store repository for each specified
 * repositoryId.
 *
 * @author Andreas Schwarte
 */
public class SPARQLEmbeddedServer {

	private static final Logger logger = LoggerFactory.getLogger(SPARQLEmbeddedServer.class);

	private static final String HOST = "localhost";

	private static final int PORT = 18080; // this port is hardcoded in some (service) query fixtures

	private static final String SERVER_CONTEXT = "/rdf4j-server";

	private final List<String> repositoryIds;

	private final RemoteRepositoryManager repositoryManager;

	private final Server jetty;

	/**
	 * @param repositoryIds
	 * @throws IOException
	 */
	public SPARQLEmbeddedServer(List<String> repositoryIds) throws IOException {
		this.repositoryIds = repositoryIds;
		System.clearProperty("DEBUG");

		PropertiesReader reader = new PropertiesReader("maven-config.properties");
		String webappDir = reader.getProperty("testserver.webapp.dir");
		logger.debug("build path: {}", webappDir);
		jetty = new Server(PORT);

		WebAppContext webapp = new WebAppContext();
		webapp.setContextPath(SERVER_CONTEXT);
		// warPath configured in pom.xml maven-war-plugin configuration
		webapp.setWar(webappDir);
		jetty.setHandler(webapp);

		repositoryManager = new RemoteRepositoryManager(getServerUrl());
	}

	/**
	 * @return the url to the repository with given id
	 */
	public String getRepositoryUrl(String repoId) {
		return Protocol.getRepositoryLocation(getServerUrl(), repoId);
	}

	/**
	 * @return the server url
	 */
	public String getServerUrl() {
		return "http://" + HOST + ":" + PORT + SERVER_CONTEXT;
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
			repositoryManager.getAllRepositoryInfos().forEach(ri -> repositoryManager.removeRepository(ri.getId()));
			repositoryManager.shutDown();
		} finally {
			jetty.stop();
			System.clearProperty("org.mortbay.log.class");
		}
	}

	private void createTestRepositories() throws RepositoryException, RepositoryConfigException {
		// create a memory store for each provided repository id
		for (String repId : repositoryIds) {
			MemoryStoreConfig memStoreConfig = new MemoryStoreConfig();
			memStoreConfig.setPersist(false);
			SailRepositoryConfig sailRepConfig = new SailRepositoryConfig(memStoreConfig);
			RepositoryConfig repConfig = new RepositoryConfig(repId, sailRepConfig);
			repositoryManager.addRepositoryConfig(repConfig);
		}

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
