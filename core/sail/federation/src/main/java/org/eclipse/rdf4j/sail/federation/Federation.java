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
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.http.client.HttpClient;
import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.http.client.HttpClientDependent;
import org.eclipse.rdf4j.http.client.SesameClient;
import org.eclipse.rdf4j.http.client.SesameClientDependent;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolver;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolverClient;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolverImpl;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.SimpleEvaluationStrategy;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.config.RepositoryResolver;
import org.eclipse.rdf4j.repository.sail.config.RepositoryResolverClient;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.federation.evaluation.FederationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Union multiple (possibly remote) Repositories into a single RDF store.
 * 
 * @author James Leigh
 * @author Arjohn Kampman
 */
public class Federation implements Sail, Executor, FederatedServiceResolverClient, RepositoryResolverClient,
		HttpClientDependent, SesameClientDependent
{

	private static final Logger LOGGER = LoggerFactory.getLogger(Federation.class);

	private final List<Repository> members = new ArrayList<Repository>();

	private final ExecutorService executor = Executors.newCachedThreadPool();

	private PrefixHashSet localPropertySpace; // NOPMD

	private boolean distinct;

	private boolean readOnly;

	private File dataDir;

	/** independent life cycle */
	private FederatedServiceResolver serviceResolver;

	/** dependent life cycle */
	private FederatedServiceResolverImpl dependentServiceResolver;

	public File getDataDir() {
		return dataDir;
	}

	public void setDataDir(File dataDir) {
		this.dataDir = dataDir;
	}

	public ValueFactory getValueFactory() {
		return SimpleValueFactory.getInstance();
	}

	public boolean isWritable()
		throws SailException
	{
		return !isReadOnly();
	}

	public void addMember(Repository member) {
		members.add(member);
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
		}
		else {
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
				dependentServiceResolver = new FederatedServiceResolverImpl();
			}
			return serviceResolver = dependentServiceResolver;
		}
		return serviceResolver;
	}

	/**
	 * Overrides the {@link FederatedServiceResolver} used by this instance, but
	 * the given resolver is not shutDown when this instance is.
	 * 
	 * @param reslover
	 *        The SERVICE resolver to set.
	 */
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
	public SesameClient getSesameClient() {
		for (Repository member : members) {
			if (member instanceof SesameClientDependent) {
				SesameClient client = ((SesameClientDependent) member).getSesameClient();
				if (client != null) {
					return client;
				}
			}
		}
		return null;
	}

	@Override
	public void setSesameClient(SesameClient client) {
		for (Repository member : members) {
			if (member instanceof SesameClientDependent) {
				((SesameClientDependent) member).setSesameClient(client);
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

	@Override
	public void initialize()
		throws SailException
	{
		for (Repository member : members) {
			try {
				member.initialize();
			}
			catch (RepositoryException e) {
				throw new SailException(e);
			}
		}
	}

	public void shutDown()
		throws SailException
	{
		for (Repository member : members) {
			try {
				member.shutDown();
			}
			catch (RepositoryException e) {
				throw new SailException(e);
			}
		}
		executor.shutdown();
		if (dependentServiceResolver != null) {
			dependentServiceResolver.shutDown();
		}
	}

	/**
	 * Required by {@link java.util.concurrent.Executor Executor} interface.
	 */
	public void execute(Runnable command) {
		executor.execute(command);
	}

	public SailConnection getConnection()
		throws SailException
	{
		List<RepositoryConnection> connections = new ArrayList<RepositoryConnection>(members.size());
		try {
			for (Repository member : members) {
				connections.add(member.getConnection());
			}
			return readOnly ? new ReadOnlyConnection(this, connections) : new WritableConnection(this,
					connections);
		}
		catch (RepositoryException e) {
			closeAll(connections);
			throw new SailException(e);
		}
		catch (RuntimeException e) {
			closeAll(connections);
			throw e;
		}
	}

	protected EvaluationStrategy createEvaluationStrategy(TripleSource tripleSource, Dataset dataset, FederatedServiceResolver resolver) {
		return new FederationStrategy(this, tripleSource, dataset, getFederatedServiceResolver());
	}

	private void closeAll(Iterable<RepositoryConnection> connections) {
		for (RepositoryConnection con : connections) {
			try {
				con.close();
			}
			catch (RepositoryException e) {
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
