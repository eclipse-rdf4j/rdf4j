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
package org.eclipse.rdf4j.repository.manager.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.http.client.HttpClient;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.manager.RepositoryInfo;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;

/**
 * @author Herko ter Horst
 * @author Arjohn Kampman
 */
public class TypeFilteringRepositoryManager extends RepositoryManager {

	private final String type;

	private final RepositoryManager delegate;

	public TypeFilteringRepositoryManager(String type, RepositoryManager delegate) {
		assert type != null : "type must not be null";
		assert delegate != null : "delegate must not be null";

		this.type = type;
		this.delegate = delegate;
	}

	/**
	 * @see org.eclipse.rdf4j.repository.manager.RepositoryManager#getHttpClient()
	 */
	@Override
	public HttpClient getHttpClient() {
		return delegate.getHttpClient();
	}

	/**
	 * @param httpClient
	 * @see org.eclipse.rdf4j.repository.manager.RepositoryManager#setHttpClient(org.apache.http.client.HttpClient)
	 */
	@Override
	public void setHttpClient(HttpClient httpClient) {
		delegate.setHttpClient(httpClient);
	}

	@Override
	public void init() throws RepositoryException {
		delegate.init();
	}

	@Override
	public URL getLocation() throws MalformedURLException {
		return delegate.getLocation();
	}

	@Override
	public String getNewRepositoryID(String baseName) throws RepositoryException, RepositoryConfigException {
		return delegate.getNewRepositoryID(baseName);
	}

	@Override
	public Set<String> getRepositoryIDs() throws RepositoryException {
		Set<String> result = new LinkedHashSet<>();

		for (String id : delegate.getRepositoryIDs()) {
			try {
				if (isCorrectType(id)) {
					result.add(id);
				}
			} catch (RepositoryConfigException e) {
				throw new RepositoryException(e);
			}
		}

		return result;
	}

	@Override
	public boolean hasRepositoryConfig(String repositoryID) throws RepositoryException, RepositoryConfigException {
		boolean result = false;

		if (isCorrectType(repositoryID)) {
			result = delegate.hasRepositoryConfig(repositoryID);
		}

		return result;
	}

	@Override
	public RepositoryConfig getRepositoryConfig(String repositoryID)
			throws RepositoryConfigException, RepositoryException {
		RepositoryConfig result = delegate.getRepositoryConfig(repositoryID);

		if (result != null) {
			if (!isCorrectType(result)) {
				logger.debug(
						"Surpressing retrieval of repository {}: repository type {} did not match expected type {}",
						new Object[] { result.getID(), result.getRepositoryImplConfig().getType(), type });

				result = null;
			}
		}

		return result;
	}

	@Override
	public void addRepositoryConfig(RepositoryConfig config) throws RepositoryException, RepositoryConfigException {
		if (isCorrectType(config)) {
			delegate.addRepositoryConfig(config);
		} else {
			throw new UnsupportedOperationException(
					"Only repositories of type " + type + " can be added to this manager.");
		}
	}

	@Override
	public Repository getRepository(String id) throws RepositoryConfigException, RepositoryException {
		Repository result = null;

		if (isCorrectType(id)) {
			result = delegate.getRepository(id);
		}

		return result;
	}

	@Override
	public Set<String> getInitializedRepositoryIDs() {
		Set<String> result = new LinkedHashSet<>();

		for (String id : delegate.getInitializedRepositoryIDs()) {
			try {
				if (isCorrectType(id)) {
					result.add(id);
				}
			} catch (RepositoryConfigException | RepositoryException e) {
				logger.error("Failed to verify repository type", e);
			}
		}

		return result;
	}

	@Override
	public Collection<Repository> getInitializedRepositories() {
		List<Repository> result = new ArrayList<>();

		for (String id : getInitializedRepositoryIDs()) {
			try {
				Repository repository = getRepository(id);

				if (repository != null) {
					result.add(repository);
				}
			} catch (RepositoryConfigException | RepositoryException e) {
				logger.error("Failed to verify repository type", e);
			}
		}

		return result;
	}

	@Override
	protected Repository createRepository(String id) throws RepositoryConfigException, RepositoryException {
		throw new UnsupportedOperationException(
				"Repositories cannot be created through this wrapper. This method should not have been called, the delegate should take care of it.");
	}

	@Override
	public Collection<RepositoryInfo> getAllRepositoryInfos() throws RepositoryException {
		List<RepositoryInfo> result = new ArrayList<>();

		for (RepositoryInfo repInfo : delegate.getAllRepositoryInfos()) {
			try {
				if (isCorrectType(repInfo.getId())) {
					result.add(repInfo);
				}
			} catch (RepositoryConfigException e) {
				throw new RepositoryException(e.getMessage(), e);
			}
		}

		return result;
	}

	@Override
	public RepositoryInfo getRepositoryInfo(String id) throws RepositoryException {
		try {
			if (isCorrectType(id)) {
				return delegate.getRepositoryInfo(id);
			}

			return null;
		} catch (RepositoryConfigException e) {
			throw new RepositoryException(e.getMessage(), e);
		}
	}

	@Override
	public void refresh() {
		delegate.refresh();
	}

	@Override
	public void shutDown() {
		delegate.shutDown();
	}

	protected boolean isCorrectType(String repositoryID) throws RepositoryConfigException, RepositoryException {
		return isCorrectType(delegate.getRepositoryConfig(repositoryID));
	}

	protected boolean isCorrectType(RepositoryConfig repositoryConfig) {
		boolean result = false;

		if (repositoryConfig != null) {
			result = repositoryConfig.getRepositoryImplConfig().getType().equals(type);
		}

		return result;
	}
}
