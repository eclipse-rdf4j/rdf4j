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
package org.eclipse.rdf4j.federated.endpoint;

import org.eclipse.rdf4j.federated.EndpointManager;
import org.eclipse.rdf4j.federated.FederationContext;
import org.eclipse.rdf4j.federated.endpoint.provider.RepositoryInformation;
import org.eclipse.rdf4j.federated.evaluation.TripleSource;
import org.eclipse.rdf4j.federated.evaluation.TripleSourceFactory;
import org.eclipse.rdf4j.federated.exception.FedXRuntimeException;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.base.RepositoryConnectionWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base implementation for an {@link Endpoint}.
 *
 * <p>
 * Provides implementation for the common behavior as well as connection management. Typically a fresh
 * {@link RepositoryConnection} is returned when invoking {@link #getConnection()}, however, it is configurable that a
 * managed (singleton) connection can be used.
 * </p>
 *
 * @author Andreas Schwarte
 * @see EndpointManager
 */
public abstract class EndpointBase implements Endpoint {

	protected static final Logger log = LoggerFactory.getLogger(EndpointBase.class);

	protected final RepositoryInformation repoInfo; // the repository information
	protected final String endpoint; // the endpoint, e.g. for SPARQL the URL
	protected EndpointClassification endpointClassification; // the endpoint classification
	protected boolean writable; // can this endpoint be used for write operation

	private ManagedRepositoryConnection dependentConn = null; // if configured, contains the managed connection
	protected boolean initialized = false; // true, iff the contained repository is initialized
	protected TripleSource tripleSource; // the triple source, initialized when repository is set
	protected EndpointConfiguration endpointConfiguration; // additional endpoint type specific configuration

	public EndpointBase(RepositoryInformation repoInfo, String endpoint,
			EndpointClassification endpointClassification) {
		super();
		this.repoInfo = repoInfo;
		this.endpoint = endpoint;
		this.writable = repoInfo.isWritable();
		this.endpointClassification = endpointClassification;
	}

	@Override
	public String getName() {
		return repoInfo.getName();
	}

	@Override
	public TripleSource getTripleSource() {
		return tripleSource;
	}

	@Override
	public EndpointClassification getEndpointClassification() {
		return endpointClassification;
	}

	public void setEndpointClassification(EndpointClassification endpointClassification) {
		this.endpointClassification = endpointClassification;
	}

	public boolean isLocal() {
		return endpointClassification == EndpointClassification.Local;
	}

	@Override
	public boolean isWritable() {
		return writable;
	}

	public RepositoryInformation getRepoInfo() {
		return repoInfo;
	}

	/**
	 * @param writable the writable to set
	 */
	public void setWritable(boolean writable) {
		this.writable = writable;
	}

	@Override
	public EndpointConfiguration getEndpointConfiguration() {
		return endpointConfiguration;
	}

	/**
	 * @param endpointConfiguration the endpointConfiguration to set
	 */
	public void setEndpointConfiguration(EndpointConfiguration endpointConfiguration) {
		this.endpointConfiguration = endpointConfiguration;
	}

	@Override
	public RepositoryConnection getConnection() {
		if (!initialized) {
			throw new FedXRuntimeException("Repository for endpoint " + getId() + " not initialized");
		}
		if (dependentConn != null) {
			return this.dependentConn;
		}
		return getFreshConnection();
	}

	protected RepositoryConnection getFreshConnection() {
		return getRepository().getConnection();
	}

	@Override
	public String getId() {
		return repoInfo.getId();
	}

	@Override
	public String getEndpoint() {
		return endpoint;
	}

	public EndpointType getType() {
		return repoInfo.getType();
	}

	@Override
	public boolean isInitialized() {
		return initialized;
	}

	@Override
	public long size() throws RepositoryException {
		try (RepositoryConnection conn = getConnection()) {
			return conn.size();
		}
	}

	@Override
	public void init(FederationContext federationContext) throws RepositoryException {
		if (isInitialized()) {
			return;
		}
		Repository repo = getRepository();
		tripleSource = TripleSourceFactory.tripleSourceFor(this, getType(), federationContext);
		if (useSingleConnection()) {
			dependentConn = new ManagedRepositoryConnection(repo, repo.getConnection());
		}
		initialized = true;
	}

	/**
	 * Whether to reuse the same {@link RepositoryConnection} throughout the lifetime of this Endpoint.
	 *
	 * <p>
	 * Note that the {@link RepositoryConnection} is wrapped as {@link ManagedRepositoryConnection}
	 * </p>
	 *
	 * @return indicator whether a single connection should be used
	 */
	protected boolean useSingleConnection() {
		return false;
	}

	@Override
	public void shutDown() throws RepositoryException {
		if (!isInitialized()) {
			return;
		}
		if (dependentConn != null) {
			dependentConn.closeManagedConnection();
			dependentConn = null;
		}
		initialized = false;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
		result = prime * result + ((getType() == null) ? 0 : getType().hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		EndpointBase other = (EndpointBase) obj;
		if (getId() == null) {
			if (other.getId() != null) {
				return false;
			}
		} else if (!getId().equals(other.getId())) {
			return false;
		}
		if (getType() == null) {
			if (other.getType() != null) {
				return false;
			}
		} else if (!getType().equals(other.getType())) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "Endpoint [id=" + getId() + ", name=" + getName() + ", type=" + getType() + "]";
	}

	/**
	 * A wrapper for managed {@link RepositoryConnection}s which makes sure that {@link #close()} is a no-op, i.e. the
	 * actual closing of the managed connection is controlled by the {@link Endpoint}.
	 *
	 * @author Andreas Schwarte
	 *
	 */
	public static class ManagedRepositoryConnection extends RepositoryConnectionWrapper {

		public ManagedRepositoryConnection(Repository repository, RepositoryConnection delegate) {
			super(repository, delegate);
		}

		@Override
		public void close() throws RepositoryException {
			// Do nothing: this repository connection is managed by FedX
		}

		public void closeManagedConnection() throws RepositoryException {
			this.getDelegate().close();
		}
	}
}
