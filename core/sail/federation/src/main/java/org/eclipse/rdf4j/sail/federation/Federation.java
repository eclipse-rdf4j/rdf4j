/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.federation;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.HttpClient;
import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.http.client.HttpClientDependent;
import org.eclipse.rdf4j.http.client.HttpClientSessionManager;
import org.eclipse.rdf4j.http.client.SessionManagerDependent;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolver;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolverClient;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResolver;
import org.eclipse.rdf4j.repository.RepositoryResolverClient;
import org.eclipse.rdf4j.repository.filters.RepositoryBloomFilter;
import org.eclipse.rdf4j.repository.sparql.federation.SPARQLServiceResolver;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.federation.evaluation.FederationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * Union multiple (possibly remote) Repositories into a single RDF store.
 *
 * @author James Leigh
 * @author Arjohn Kampman
 */
public class Federation implements Sail, Executor, FederatedServiceResolverClient, RepositoryResolverClient,
		HttpClientDependent, SessionManagerDependent {

	private static final Logger LOGGER = LoggerFactory.getLogger(Federation.class);

	private final List<Repository> members = new ArrayList<>();

	private final Map<Repository, RepositoryBloomFilter> bloomFilters = new HashMap<>();

	private final ExecutorService executor = Executors
			.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("rdf4j-federation-%d").build());

	private PrefixHashSet localPropertySpace; // NOPMD

	private boolean distinct;

	private boolean readOnly;

	private File dataDir;

	/** independent life cycle */
	private volatile FederatedServiceResolver serviceResolver;

	/** dependent life cycle */
	private volatile SPARQLServiceResolver dependentServiceResolver;

	@Override
	public File getDataDir() {
		return dataDir;
	}

	@Override
	public void setDataDir(File dataDir) {
		this.dataDir = dataDir;
	}

	@Override
	public ValueFactory getValueFactory() {
		return SimpleValueFactory.getInstance();
	}

	@Override
	public boolean isWritable() throws SailException {
		return !isReadOnly();
	}

	public void addMember(Repository member) {
		members.add(member);
	}

	/**
	 * Returns the members of this federation.
	 *
	 * @return unmodifiable list of federation members.
	 */
	protected List<Repository> getMembers() {
		// unmodifiable to ensure no back-door changes
		return Collections.unmodifiableList(members);
	}

	/**
	 * Sets an optional {@link RepositoryBloomFilter} to use with the given {@link Repository}.
	 *
	 * @param filter the filter to use or null to not use a filter.
	 */
	public void setBloomFilter(Repository member, RepositoryBloomFilter filter) {
		bloomFilters.put(member, filter);
	}

	/**
	 * Returns the configured {@link RepositoryBloomFilter}s (if any).
	 *
	 * @return unmodifiable map of repositories to bloom filters.
	 */
	protected Map<Repository, RepositoryBloomFilter> getBloomFilters() {
		// unmodifiable to ensure no back-door changes
		return Collections.unmodifiableMap(bloomFilters);
	}

	/**
	 * @return PrefixHashSet or null
	 */
	public PrefixHashSet getLocalPropertySpace() {
		return localPropertySpace;
	}

	public void setLocalPropertySpace(Collection<String> localPropertySpace) { // NOPMD
		if (localPropertySpace.isEmpty()) {
			this.localPropertySpace = null; // NOPMD
		} else {
			this.localPropertySpace = new PrefixHashSet(localPropertySpace);
		}
	}

	public boolean isDistinct() {
		return distinct;
	}

	public void setDistinct(boolean distinct) {
		this.distinct = distinct;
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	/**
	 * @return Returns the SERVICE resolver.
	 */
	public synchronized FederatedServiceResolver getFederatedServiceResolver() {
		if (serviceResolver == null) {
			if (dependentServiceResolver == null) {
				dependentServiceResolver = new SPARQLServiceResolver();
			}
			return serviceResolver = dependentServiceResolver;
		}
		return serviceResolver;
	}

	/**
	 * Overrides the {@link FederatedServiceResolver} used by this instance, but the given resolver is not shutDown when
	 * this instance is.
	 *
	 * @param resolver The SERVICE resolver to set.
	 */
	@Override
	public synchronized void setFederatedServiceResolver(FederatedServiceResolver resolver) {
		this.serviceResolver = resolver;
		for (Repository member : members) {
			if (member instanceof FederatedServiceResolverClient) {
				((FederatedServiceResolverClient) member).setFederatedServiceResolver(resolver);
			}
		}
	}

	@Override
	public void setRepositoryResolver(RepositoryResolver resolver) {
		for (Repository member : members) {
			if (member instanceof RepositoryResolverClient) {
				((RepositoryResolverClient) member).setRepositoryResolver(resolver);
			}
		}
	}

	@Override
	public HttpClientSessionManager getHttpClientSessionManager() {
		for (Repository member : members) {
			if (member instanceof SessionManagerDependent) {
				HttpClientSessionManager client = ((SessionManagerDependent) member).getHttpClientSessionManager();
				if (client != null) {
					return client;
				}
			}
		}
		return null;
	}

	@Override
	public void setHttpClientSessionManager(HttpClientSessionManager client) {
		for (Repository member : members) {
			if (member instanceof SessionManagerDependent) {
				((SessionManagerDependent) member).setHttpClientSessionManager(client);
			}
		}
	}

	@Override
	public HttpClient getHttpClient() {
		for (Repository member : members) {
			if (member instanceof HttpClientDependent) {
				HttpClient client = ((HttpClientDependent) member).getHttpClient();
				if (client != null) {
					return client;
				}
			}
		}
		return null;
	}

	@Override
	public void setHttpClient(HttpClient client) {
		for (Repository member : members) {
			if (member instanceof HttpClientDependent) {
				((HttpClientDependent) member).setHttpClient(client);
			}
		}
	}

	@Deprecated
	@Override
	public void initialize() throws SailException {
		init();
	}

	@Override
	public void init() throws SailException {
		for (Repository member : members) {
			try {
				member.initialize();
			} catch (RepositoryException e) {
				throw new SailException(e);
			}
		}
	}

	@Override
	public void shutDown() throws SailException {
		List<SailException> toThrowExceptions = new ArrayList<>();
		try {
			for (Repository member : members) {
				try {
					member.shutDown();
				} catch (SailException e) {
					toThrowExceptions.add(e);
				} catch (RDF4JException e) {
					toThrowExceptions.add(new SailException(e));
				}
			}
		} finally {
			try {
				SPARQLServiceResolver toCloseServiceResolver = dependentServiceResolver;
				dependentServiceResolver = null;
				if (toCloseServiceResolver != null) {
					toCloseServiceResolver.shutDown();
				}
			} finally {
				try {
					executor.shutdown();
					executor.awaitTermination(10, TimeUnit.SECONDS);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				} finally {
					if (!executor.isShutdown()) {
						executor.shutdownNow();
					}
				}
			}
		}
		if (!toThrowExceptions.isEmpty()) {
			throw toThrowExceptions.get(0);
		}
	}

	/**
	 * Required by {@link java.util.concurrent.Executor Executor} interface.
	 */
	@Override
	public void execute(Runnable command) {
		executor.execute(command);
	}

	@Override
	public SailConnection getConnection() throws SailException {
		List<RepositoryConnection> connections = new ArrayList<>(members.size());
		boolean allGood = false;
		try {
			for (Repository member : members) {
				connections.add(member.getConnection());
			}
			SailConnection result = readOnly ? new ReadOnlyConnection(this, connections)
					: new WritableConnection(this, connections);
			allGood = true;
			return result;
		} catch (RepositoryException e) {
			throw new SailException(e);
		} finally {
			if (!allGood) {
				closeAll(connections);
			}
		}

	}

	protected EvaluationStrategy createEvaluationStrategy(TripleSource tripleSource, Dataset dataset,
			FederatedServiceResolver resolver) {
		return new FederationStrategy(this, tripleSource, dataset, getFederatedServiceResolver());
	}

	private void closeAll(Iterable<RepositoryConnection> connections) {
		for (RepositoryConnection con : connections) {
			try {
				con.close();
			} catch (RepositoryException e) {
				LOGGER.error(e.getMessage(), e);
			}
		}
	}

	@Override
	public List<IsolationLevel> getSupportedIsolationLevels() {
		return Arrays.asList(new IsolationLevel[] { IsolationLevels.NONE });
	}

	@Override
	public IsolationLevel getDefaultIsolationLevel() {
		return IsolationLevels.NONE;
	}
}
