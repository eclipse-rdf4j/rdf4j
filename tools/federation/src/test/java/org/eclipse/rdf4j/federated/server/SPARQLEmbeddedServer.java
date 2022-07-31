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
package org.eclipse.rdf4j.federated.server;

import java.io.File;
import java.util.List;

import org.eclipse.rdf4j.federated.endpoint.Endpoint;
import org.eclipse.rdf4j.federated.endpoint.EndpointFactory;
import org.eclipse.rdf4j.federated.repository.ConfigurableSailRepository;
import org.eclipse.rdf4j.federated.repository.ConfigurableSailRepositoryFactory;
import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResolver;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.http.HTTPRepository;
import org.eclipse.rdf4j.repository.manager.RemoteRepositoryManager;
import org.eclipse.rdf4j.repository.sail.config.SailRepositoryConfig;
import org.eclipse.rdf4j.sail.memory.config.MemoryStoreConfig;

/**
 * An embedded http server for SPARQL query testing. Initializes a memory store repository for each specified
 * reposiotoryId.
 *
 * @author Andreas Schwarte
 */
public class SPARQLEmbeddedServer extends EmbeddedServer implements Server {

	protected final List<String> repositoryIds;
	// flag to indicate whether a remote repository or SPARQL repository endpoint shall be used
	private final boolean useRemoteRepositoryEndpoint;

	/**
	 * The {@link RepositoryResolver} supplied at runtime by {@link FedXRepositoryResolverBean}
	 */
	private RepositoryResolver repositoryResolver;
	/**
	 * The data directory populated at runtime
	 */
	private final File dataDir;

	/**
	 * @param repositoryIds
	 */
	public SPARQLEmbeddedServer(File dataDir, List<String> repositoryIds, boolean useRemoteRepositoryEndpoint) {
		super();
		this.dataDir = dataDir;
		this.repositoryIds = repositoryIds;
		this.useRemoteRepositoryEndpoint = useRemoteRepositoryEndpoint;
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
		return "http://" + HOST + ":" + PORT + CONTEXT_PATH;
	}

	@Override
	public void start()
			throws Exception {
		System.setProperty("org.eclipse.rdf4j.appdata.basedir", dataDir.getAbsolutePath());

		super.start();

		repositoryResolver = FedXRepositoryResolverBean.getRepositoryResolver();

		createTestRepositories();
	}

	@Override
	public void stop()
			throws Exception {
		RemoteRepositoryManager repoManager = RemoteRepositoryManager.getInstance(getServerUrl());
		try {
			repoManager.init();
			for (String repId : repositoryIds) {
				repoManager.removeRepository(repId);
			}
		} finally {
			repoManager.shutDown();
		}

		super.stop();
	}

	/**
	 * @throws RepositoryException
	 */
	private void createTestRepositories()
			throws RepositoryException, RepositoryConfigException {

		RemoteRepositoryManager repoManager = RemoteRepositoryManager.getInstance(getServerUrl());
		try {
			repoManager.init();

			// create a memory store for each provided repository id
			for (String repId : repositoryIds) {
				MemoryStoreConfig memStoreConfig = new MemoryStoreConfig();
				SailRepositoryConfig sailRepConfig = new ConfigurableSailRepositoryFactory.ConfigurableSailRepositoryConfig(
						memStoreConfig);
				RepositoryConfig repConfig = new RepositoryConfig(repId, sailRepConfig);

				repoManager.addRepositoryConfig(repConfig);
			}
		} finally {
			repoManager.shutDown();
		}

	}

	@Override
	public void initialize(int nRepositories) throws Exception {
		try {
			start();
		} catch (Exception e) {
			stop();
			throw e;
		}

		for (int i = 1; i <= nRepositories; i++) {
			HTTPRepository r = new HTTPRepository(getRepositoryUrl("endpoint" + i));
			r.init();
			r.shutDown();
		}
	}

	@Override
	public void shutdown() throws Exception {
		stop();
	}

	@Override
	public Endpoint loadEndpoint(int i) throws Exception {
		return useRemoteRepositoryEndpoint ? EndpointFactory.loadRemoteRepository(getServerUrl(), "endpoint" + i)
				: EndpointFactory.loadSPARQLEndpoint("http://endpoint" + i, getRepositoryUrl("endpoint" + i));
	}

	/**
	 *
	 * @param i the index of the repository, starting with 1
	 * @return the repository
	 */
	@Override
	public ConfigurableSailRepository getRepository(int i) {
		String repositoryId = repositoryIds.get(i - 1);
		return (ConfigurableSailRepository) repositoryResolver.getRepository(repositoryId);
	}

	public File getDataDir() {
		return dataDir;
	}

	public RepositoryResolver getRepositoryResolver() {
		return repositoryResolver;
	}
}
