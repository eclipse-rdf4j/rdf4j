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
package org.eclipse.rdf4j.repository.manager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.http.client.HttpClient;
import org.eclipse.rdf4j.common.io.FileUtil;
import org.eclipse.rdf4j.http.client.HttpClientDependent;
import org.eclipse.rdf4j.http.client.SessionManagerDependent;
import org.eclipse.rdf4j.http.client.SharedHttpClientSessionManager;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolver;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolverClient;
import org.eclipse.rdf4j.repository.DelegatingRepository;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResolverClient;
import org.eclipse.rdf4j.repository.config.DelegatingRepositoryImplConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.config.RepositoryConfigUtil;
import org.eclipse.rdf4j.repository.config.RepositoryFactory;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.eclipse.rdf4j.repository.config.RepositoryRegistry;
import org.eclipse.rdf4j.repository.sparql.federation.SPARQLServiceResolver;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;

/**
 * An implementation of the {@link RepositoryManager} interface that operates directly on the repository data files in
 * the local file system.
 *
 * @author Arjohn Kampman
 */
public class LocalRepositoryManager extends RepositoryManager {

	public static final String REPOSITORIES_DIR = "repositories";

	private static final RDFFormat CONFIG_FORMAT = RDFFormat.TURTLE;

	private static final String CFG_FILE = "config." + CONFIG_FORMAT.getDefaultFileExtension();

	private static final WriterConfig CFG_CONFIG = new WriterConfig().set(BasicWriterSettings.BASE_DIRECTIVE, false)
			.set(BasicWriterSettings.PRETTY_PRINT, true)
			.set(BasicWriterSettings.INLINE_BLANK_NODES, true);

	/**
	 * The base dir to resolve any relative paths against.
	 */
	private final File baseDir;

	/** dependent life cycle */
	private volatile SharedHttpClientSessionManager client;

	/** dependent life cycle */
	private volatile SPARQLServiceResolver serviceResolver;

	/**
	 * Creates a new RepositoryManager that operates on the specfified base directory.
	 *
	 * @param baseDir The base directory where data for repositories can be stored, among other things.
	 */
	public LocalRepositoryManager(File baseDir) {
		super();

		this.baseDir = baseDir;
	}

	/**
	 * Gets the base dir against which to resolve relative paths.
	 */
	public File getBaseDir() {
		return baseDir;
	}

	/**
	 * Gets the base dir against which to resolve relative paths.
	 *
	 * @throws MalformedURLException If the path cannot be parsed as a URL
	 */
	@Override
	public URL getLocation() throws MalformedURLException {
		return baseDir.toURI().toURL();
	}

	/**
	 * @return Returns the httpClient.
	 */
	protected SharedHttpClientSessionManager getSesameClient() {
		SharedHttpClientSessionManager result = client;
		if (result == null) {
			synchronized (this) {
				result = client;
				if (result == null) {
					result = client = new SharedHttpClientSessionManager();
				}
			}
		}
		return result;
	}

	@Override
	public HttpClient getHttpClient() {
		SharedHttpClientSessionManager nextClient = client;
		if (nextClient == null) {
			return null;
		} else {
			return nextClient.getHttpClient();
		}
	}

	@Override
	public void setHttpClient(HttpClient httpClient) {
		getSesameClient().setHttpClient(httpClient);
	}

	/**
	 * @return Returns the serviceResolver.
	 */
	protected FederatedServiceResolver getFederatedServiceResolver() {
		SPARQLServiceResolver result = serviceResolver;
		if (result == null) {
			synchronized (this) {
				result = serviceResolver;
				if (result == null) {
					result = serviceResolver = new SPARQLServiceResolver();
					result.setHttpClientSessionManager(getSesameClient());
				}
			}
		}
		return result;
	}

	@Override
	public void shutDown() {
		try {
			super.shutDown();
		} finally {
			try {
				SPARQLServiceResolver toCloseServiceResolver = serviceResolver;
				serviceResolver = null;
				if (toCloseServiceResolver != null) {
					toCloseServiceResolver.shutDown();
				}
			} finally {
				SharedHttpClientSessionManager toCloseClient = client;
				client = null;
				if (toCloseClient != null) {
					toCloseClient.shutDown();
				}
			}
		}
	}

	/**
	 * Resolves the specified path against the manager's base directory.
	 *
	 * @see #getBaseDir
	 */
	public File resolvePath(String path) {
		return new File(getBaseDir(), path);
	}

	public File getRepositoryDir(String repositoryID) {
		File repositoriesDir = resolvePath(REPOSITORIES_DIR);
		return new File(repositoriesDir, repositoryID);
	}

	@Override
	protected Repository createRepository(String id) throws RepositoryConfigException, RepositoryException {
		Repository repository = null;

		RepositoryConfig repConfig = getRepositoryConfig(id);
		if (repConfig != null) {
			repConfig.validate();

			repository = createRepositoryStack(repConfig.getRepositoryImplConfig());
			repository.setDataDir(getRepositoryDir(id));
			repository.init();
		}

		return repository;
	}

	/**
	 * Creates the stack of Repository objects for the repository represented by the specified
	 * {@link org.eclipse.rdf4j.repository.config.RepositoryImplConfig}. Uses a
	 * {@link org.eclipse.rdf4j.repository.config.RepositoryFactory} to create the repository and initialize it.
	 *
	 * @param config The node representing the to-be-created repository in the configuration.
	 * @return The created repository, or <var>null</var> if no such repository exists.
	 * @throws RepositoryConfigException If no repository could be created due to invalid or incomplete configuration
	 *                                   data.
	 */
	private Repository createRepositoryStack(RepositoryImplConfig config) throws RepositoryConfigException {
		RepositoryFactory factory = RepositoryRegistry.getInstance()
				.get(config.getType())
				.orElseThrow(() -> new RepositoryConfigException("Unsupported repository type: " + config.getType()));
		Repository repository = factory.getRepository(config);
		if (repository instanceof RepositoryResolverClient) {
			((RepositoryResolverClient) repository).setRepositoryResolver(this);
		}
		if (repository instanceof FederatedServiceResolverClient) {
			((FederatedServiceResolverClient) repository).setFederatedServiceResolver(getFederatedServiceResolver());
		}
		if (repository instanceof SessionManagerDependent) {
			((SessionManagerDependent) repository).setHttpClientSessionManager(client);
		} else if (repository instanceof HttpClientDependent) {
			((HttpClientDependent) repository).setHttpClient(getHttpClient());
		}
		if (config instanceof DelegatingRepositoryImplConfig) {
			RepositoryImplConfig delegateConfig = ((DelegatingRepositoryImplConfig) config).getDelegate();
			Repository delegate = createRepositoryStack(delegateConfig);
			try {
				((DelegatingRepository) repository).setDelegate(delegate);
			} catch (ClassCastException e) {
				throw new RepositoryConfigException(
						"Delegate specified for repository that is not a DelegatingRepository: " + delegate.getClass(),
						e);
			}
		}
		return repository;
	}

	@Override
	public synchronized RepositoryConfig getRepositoryConfig(String id) {
		File dataDir = getRepositoryDir(id);
		if (new File(dataDir, CFG_FILE).exists()) {
			File configFile = new File(dataDir, CFG_FILE);
			try (InputStream input = new FileInputStream(configFile)) {
				Model model = Rio.parse(input, configFile.toURI().toString(), CONFIG_FORMAT);
				Set<String> repositoryIDs = RepositoryConfigUtil.getRepositoryIDs(model);
				if (repositoryIDs.isEmpty()) {
					throw new RepositoryConfigException("No repository ID in configuration: " + configFile);
				} else if (repositoryIDs.size() != 1) {
					throw new RepositoryConfigException("Multiple repository IDs in configuration: " + configFile);
				}
				String repositoryID = repositoryIDs.iterator().next();
				if (!id.equals(repositoryID)
						&& !getRepositoryDir(repositoryID).getCanonicalFile().equals(dataDir.getCanonicalFile())) {
					throw new RepositoryConfigException("Wrong repository ID in configuration: " + configFile);
				}
				return RepositoryConfigUtil.getRepositoryConfig(model, repositoryID);
			} catch (IOException e) {
				throw new RepositoryConfigException(e);
			}
		}
		return null;
	}

	@Override
	public RepositoryInfo getRepositoryInfo(String id) {
		RepositoryConfig config = getRepositoryConfig(id);
		if (config == null) {
			return null;
		}
		RepositoryInfo repInfo = new RepositoryInfo();
		repInfo.setId(config.getID());
		repInfo.setDescription(config.getTitle());
		try {
			repInfo.setLocation(getRepositoryDir(config.getID()).toURI().toURL());
		} catch (MalformedURLException mue) {
			throw new RepositoryException("Location of repository does not resolve to a valid URL", mue);
		}
		repInfo.setReadable(true);
		repInfo.setWritable(true);
		return repInfo;
	}

	@Override
	public synchronized List<RepositoryInfo> getAllRepositoryInfos() throws RepositoryException {
		File repositoriesDir = resolvePath(REPOSITORIES_DIR);
		String[] dirs = repositoriesDir.list((File repositories, String name) -> {
			File dataDir = new File(repositories, name);
			return dataDir.isDirectory() && new File(dataDir, CFG_FILE).exists();
		});
		if (dirs == null) {
			return Collections.emptyList();
		}
		List<RepositoryInfo> result = new ArrayList<>();
		for (String name : dirs) {
			RepositoryInfo repInfo = getRepositoryInfo(name);
			result.add(repInfo);
		}
		return result;
	}

	@Override
	public synchronized void addRepositoryConfig(RepositoryConfig config)
			throws RepositoryException, RepositoryConfigException {
		Objects.requireNonNull(config).validate();
		File dataDir = getRepositoryDir(config.getID());
		if (!dataDir.exists()) {
			dataDir.mkdirs();
		}
		if (!dataDir.isDirectory()) {
			throw new RepositoryConfigException("Could not create directory: " + dataDir);
		}
		File configFile = new File(dataDir, CFG_FILE);
		Model model = getModelFactory().createEmptyModel();
		String ns = configFile.toURI().toString() + "#";
		config.export(model, SimpleValueFactory.getInstance().createIRI(ns, config.getID()));
		File part = new File(configFile.getParentFile(), configFile.getName() + ".part");
		try (OutputStream output = new FileOutputStream(part)) {
			Rio.write(model, output, configFile.toURI().toString(), CONFIG_FORMAT, CFG_CONFIG);
		} catch (IOException | RDFHandlerException | UnsupportedRDFormatException | URISyntaxException e) {
			throw new RepositoryConfigException(e);
		}
		try {
			Files.move(part.toPath(), configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			throw new RepositoryConfigException(e);
		}
	}

	@Override
	public synchronized boolean removeRepository(String repositoryID)
			throws RepositoryException, RepositoryConfigException {
		boolean removed = super.removeRepository(repositoryID);
		File dataDir = getRepositoryDir(repositoryID);
		if (dataDir.isDirectory()) {
			logger.debug("Cleaning up data dir {} for repository {}", dataDir.getAbsolutePath(), repositoryID);
			try {
				FileUtil.deleteDir(dataDir);
			} catch (IOException e) {
				throw new RepositoryConfigException(e);
			}
			return true;
		}
		return removed;
	}
}
