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
package org.eclipse.rdf4j.repository.sail;

import java.io.File;

import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResolver;
import org.eclipse.rdf4j.repository.RepositoryResolverClient;
import org.eclipse.rdf4j.repository.base.AbstractRepository;

/**
 * <p>
 * {@link org.eclipse.rdf4j.repository.Repository} implementation that takes a
 * {@link org.eclipse.rdf4j.repository.RepositoryResolver} instance and the id of a managed repository, and delegate all
 * calls through to the given repository.
 * </p>
 * <p>
 * The purpose is to allow {@link org.eclipse.rdf4j.sail.Sail}s to refer to other local repositories using a unique
 * identifier without having to go through an HTTP layer.
 * </p>
 * <p>
 * The implementation is independent of {@link org.eclipse.rdf4j.repository.DelegatingRepository} so that it is freed
 * from having to provide implementation details in its configuration data. Instead, it only has to provide an
 * unambiguous local identifier to the proxy.
 * </p>
 *
 * @author Dale Visser
 */
public class ProxyRepository extends AbstractRepository implements RepositoryResolverClient {

	private File dataDir;

	private volatile Repository proxiedRepository;

	private String proxiedID;

	/** independent life cycle */
	private volatile RepositoryResolver resolver;

	public ProxyRepository() {
		super();
	}

	/**
	 * Creates a repository instance that proxies to a repository of the give ID.
	 *
	 * @param proxiedIdentity id of the proxied repository
	 */
	public ProxyRepository(String proxiedIdentity) {
		super();
		this.setProxiedIdentity(proxiedIdentity);
	}

	/**
	 * Creates a repository instance that proxies to the given repository.
	 *
	 * @param resolver        manager that the proxied repository is associated with
	 * @param proxiedIdentity id of the proxied repository
	 */
	public ProxyRepository(RepositoryResolver resolver, String proxiedIdentity) {
		super();
		this.setRepositoryResolver(resolver);
		this.setProxiedIdentity(proxiedIdentity);
	}

	public final void setProxiedIdentity(String value) {
		if (!value.equals(this.proxiedID)) {
			this.proxiedID = value;
			this.proxiedRepository = null;
		}
	}

	public String getProxiedIdentity() {
		return this.proxiedID;
	}

	@Override
	public final void setRepositoryResolver(RepositoryResolver resolver) {
		if (resolver != this.resolver) {
			this.resolver = resolver;
			this.proxiedRepository = null;
		}
	}

	private Repository getProxiedRepository() {
		Repository result = proxiedRepository;
		if (result == null) {
			synchronized (this) {
				result = proxiedRepository;
				if (result == null) {
					assert null != resolver : "Expected resolver to be set.";
					assert null != proxiedID : "Expected proxiedID to be set.";
					try {
						result = proxiedRepository = resolver.getRepository(proxiedID);
					} catch (RDF4JException ore) {
						throw new IllegalStateException(ore);
					}
				}
			}
		}
		return result;
	}

	@Override
	public void setDataDir(File dataDir) {
		this.dataDir = dataDir;
	}

	@Override
	public File getDataDir() {
		return this.dataDir;
	}

	@Override
	public boolean isWritable() throws RepositoryException {
		return getProxiedRepository().isWritable();
	}

	@Override
	public RepositoryConnection getConnection() throws RepositoryException {
		return getProxiedRepository().getConnection();
	}

	@Override
	public ValueFactory getValueFactory() {
		return getProxiedRepository().getValueFactory();
	}

	@Override
	protected void initializeInternal() throws RepositoryException {
		if (resolver == null) {
			throw new RepositoryException("Expected RepositoryResolver to be set.");
		}
		getProxiedRepository().init();
	}

	@Override
	protected void shutDownInternal() throws RepositoryException {
		getProxiedRepository().shutDown();
	}
}
