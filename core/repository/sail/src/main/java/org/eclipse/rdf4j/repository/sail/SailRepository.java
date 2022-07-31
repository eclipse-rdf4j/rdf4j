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

import org.apache.http.client.HttpClient;
import org.eclipse.rdf4j.http.client.HttpClientDependent;
import org.eclipse.rdf4j.http.client.HttpClientSessionManager;
import org.eclipse.rdf4j.http.client.SessionManagerDependent;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolver;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolverClient;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryLockedException;
import org.eclipse.rdf4j.repository.RepositoryResolver;
import org.eclipse.rdf4j.repository.RepositoryResolverClient;
import org.eclipse.rdf4j.repository.base.AbstractRepository;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.SailLockedException;
import org.eclipse.rdf4j.sail.StackableSail;

/**
 * An implementation of the {@link Repository} interface that operates on a (stack of) {@link Sail Sail} object(s). The
 * behaviour of the repository is determined by the Sail stack that it operates on; for example, the repository will
 * only support RDF Schema or OWL semantics if the Sail stack includes an inferencer for this.
 * <p>
 * Creating a repository object of this type is very easy. For example, the following code creates and initializes a
 * main-memory store with RDF Schema semantics:
 *
 * <pre>
 * Repository repository = new SailRepository(new ForwardChainingRDFSInferencer(new MemoryStore()));
 * repository.initialize();
 * </pre>
 *
 * Or, alternatively:
 *
 * <pre>
 * Sail sailStack = new MemoryStore();
 * sailStack = new ForwardChainingRDFSInferencer(sailStack);
 *
 * Repository repository = new SailRepository(sailStack);
 * repository.initialize();
 * </pre>
 *
 * @author Arjohn Kampman
 */
public class SailRepository extends AbstractRepository implements FederatedServiceResolverClient,
		RepositoryResolverClient, HttpClientDependent, SessionManagerDependent {

	/*-----------*
	 * Constants *
	 *-----------*/

	private final Sail sail;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new repository object that operates on the supplied Sail.
	 *
	 * @param sail A Sail object.
	 */
	public SailRepository(Sail sail) {
		this.sail = sail;
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public File getDataDir() {
		return sail.getDataDir();
	}

	@Override
	public void setDataDir(File dataDir) {
		sail.setDataDir(dataDir);
	}

	@Override
	public void setFederatedServiceResolver(FederatedServiceResolver resolver) {
		FederatedServiceResolverClient stack = findSailOf(sail, FederatedServiceResolverClient.class);
		if (stack != null) {
			stack.setFederatedServiceResolver(resolver);
		}
	}

	@Override
	public void setRepositoryResolver(RepositoryResolver resolver) {
		RepositoryResolverClient stack = findSailOf(sail, RepositoryResolverClient.class);
		if (stack != null) {
			stack.setRepositoryResolver(resolver);
		}
	}

	@Override
	public HttpClientSessionManager getHttpClientSessionManager() {
		SessionManagerDependent stack = findSailOf(sail, SessionManagerDependent.class);
		if (stack != null) {
			return stack.getHttpClientSessionManager();
		} else {
			return null;
		}
	}

	@Override
	public void setHttpClientSessionManager(HttpClientSessionManager client) {
		SessionManagerDependent stack = findSailOf(sail, SessionManagerDependent.class);
		if (stack != null) {
			stack.setHttpClientSessionManager(client);
		}
	}

	@Override
	public HttpClient getHttpClient() {
		HttpClientDependent stack = findSailOf(sail, HttpClientDependent.class);
		if (stack != null) {
			return stack.getHttpClient();
		} else {
			return null;
		}
	}

	@Override
	public void setHttpClient(HttpClient client) {
		HttpClientDependent stack = findSailOf(sail, HttpClientDependent.class);
		if (stack != null) {
			stack.setHttpClient(client);
		}
	}

	@Override
	protected void initializeInternal() throws RepositoryException {
		try {
			sail.init();
		} catch (SailLockedException e) {
			String l = e.getLockedBy();
			String r = e.getRequestedBy();
			String m = e.getMessage();
			throw new RepositoryLockedException(l, r, m, e);
		} catch (SailException e) {
			throw new RepositoryException(e.getMessage(), e);
		}
	}

	@Override
	protected void shutDownInternal() throws RepositoryException {
		try {
			sail.shutDown();
		} catch (SailException e) {
			throw new RepositoryException("Unable to shutdown Sail", e);
		}
	}

	/**
	 * Gets the Sail object that is on top of the Sail stack that this repository operates on.
	 *
	 * @return A Sail object.
	 */
	public Sail getSail() {
		return sail;
	}

	@Override
	public boolean isWritable() throws RepositoryException {
		try {
			if (!isInitialized()) {
				init();
			}
			return sail.isWritable();
		} catch (SailException e) {
			throw new RepositoryException("Unable to determine writable status of Sail", e);
		}
	}

	@Override
	public ValueFactory getValueFactory() {
		if (!isInitialized()) {
			init();
		}
		return sail.getValueFactory();
	}

	@Override
	public SailRepositoryConnection getConnection() throws RepositoryException {
		if (!isInitialized()) {
			init();
		}
		try {
			return new SailRepositoryConnection(this, sail.getConnection());
		} catch (SailException e) {
			throw new RepositoryException(e);
		}
	}

	@Override
	public String toString() {
		return sail.toString();
	}

	private <T> T findSailOf(Sail sail, Class<T> type) {
		if (type.isInstance(sail)) {
			return type.cast(sail);
		} else if (sail instanceof StackableSail) {
			return findSailOf(((StackableSail) sail).getBaseSail(), type);
		} else {
			return null;
		}
	}
}
