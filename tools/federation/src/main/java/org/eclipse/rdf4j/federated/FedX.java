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
package org.eclipse.rdf4j.federated;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.federated.endpoint.Endpoint;
import org.eclipse.rdf4j.federated.endpoint.ResolvableEndpoint;
import org.eclipse.rdf4j.federated.evaluation.FederationEvaluationStrategyFactory;
import org.eclipse.rdf4j.federated.exception.ExceptionUtil;
import org.eclipse.rdf4j.federated.exception.FedXException;
import org.eclipse.rdf4j.federated.exception.FedXRuntimeException;
import org.eclipse.rdf4j.federated.util.FedXUtil;
import org.eclipse.rdf4j.federated.write.DefaultWriteStrategyFactory;
import org.eclipse.rdf4j.federated.write.ReadOnlyWriteStrategy;
import org.eclipse.rdf4j.federated.write.RepositoryWriteStrategy;
import org.eclipse.rdf4j.federated.write.WriteStrategy;
import org.eclipse.rdf4j.federated.write.WriteStrategyFactory;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResolver;
import org.eclipse.rdf4j.repository.RepositoryResolverClient;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.helpers.AbstractSail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FedX serves as implementation of the federation layer. It implements RDF4J's Sail interface and can thus be used as a
 * normal repository in a RDF4J environment. The federation layer enables transparent access to the underlying members
 * as if they were a central repository.
 * <p>
 *
 * For initialization of the federation and usage see {@link FederationManager}.
 *
 * @author Andreas Schwarte
 *
 */
public class FedX extends AbstractSail implements RepositoryResolverClient {

	private static final Logger log = LoggerFactory.getLogger(FedX.class);

	private final List<Endpoint> members = new ArrayList<>();

	private FederationContext federationContext;

	private RepositoryResolver repositoryResolver;

	private FederationEvaluationStrategyFactory strategyFactory;

	private WriteStrategyFactory writeStrategyFactory;

	private File dataDir;

	public FedX(List<Endpoint> endpoints) {
		if (endpoints != null) {
			members.addAll(endpoints);
		}
		setDefaultIsolationLevel(IsolationLevels.NONE);
	}

	public void setFederationContext(FederationContext federationContext) {
		this.federationContext = federationContext;
	}

	/**
	 * Note: consumers must obtain the instance through
	 * {@link FederationManager#getFederationEvaluationStrategyFactory()}
	 *
	 * @return the {@link FederationEvaluationStrategyFactory}
	 */
	/* package */ FederationEvaluationStrategyFactory getFederationEvaluationStrategyFactory() {
		if (strategyFactory == null) {
			strategyFactory = new FederationEvaluationStrategyFactory();
		}
		return strategyFactory;
	}

	public void setFederationEvaluationStrategy(FederationEvaluationStrategyFactory strategyFactory) {
		this.strategyFactory = strategyFactory;
	}

	/**
	 *
	 * @param writeStrategyFactory the {@link WriteStrategyFactory}
	 */
	public void setWriteStrategyFactory(WriteStrategyFactory writeStrategyFactory) {
		this.writeStrategyFactory = writeStrategyFactory;
	}

	/* package */ WriteStrategyFactory getWriteStrategyFactory() {
		if (writeStrategyFactory == null) {
			writeStrategyFactory = new DefaultWriteStrategyFactory();
		}
		return writeStrategyFactory;
	}

	/**
	 * Add a member to the federation (internal).
	 * <p>
	 * If the federation is already initialized, the given endpoint is explicitly initialized as well.
	 * </p>
	 *
	 * @param endpoint
	 */
	protected void addMember(Endpoint endpoint) {
		if (isInitialized()) {
			initializeMember(endpoint);
		}
		members.add(endpoint);
	}

	/**
	 * Remove a member from the federation (internal)
	 *
	 * @param endpoint
	 * @return whether the member was removed
	 */
	public boolean removeMember(Endpoint endpoint) {
		endpoint.shutDown();
		return members.remove(endpoint);
	}

	/**
	 * Compute and return the {@link WriteStrategy} depending on the current federation configuration.
	 * <p>
	 * The default implementation uses the {@link RepositoryWriteStrategy} with the first discovered writable
	 * {@link Endpoint}. In none is found, the {@link ReadOnlyWriteStrategy} is used.
	 * </p>
	 *
	 * @return the {@link WriteStrategy}
	 * @throws FedXRuntimeException if the {@link WriteStrategy} could not be created
	 * @see FedXFactory#withWriteStrategyFactory(WriteStrategyFactory)
	 */
	/* package */ WriteStrategy getWriteStrategy() {
		try {
			return getWriteStrategyFactory()
					.create(members, federationContext);
		} catch (Exception e) {
			throw new FedXRuntimeException("Failed to instantiate write strategy: " + e.getMessage(), e);
		}
	}

	@Override
	protected SailConnection getConnectionInternal() throws SailException {
		return new FedXConnection(this, federationContext);
	}

	@Override
	public File getDataDir() {
		return dataDir;
	}

	@Override
	public ValueFactory getValueFactory() {
		return FedXUtil.valueFactory();
	}

	@Override
	protected void initializeInternal() throws SailException {
		log.debug("Initializing federation....");
		for (Endpoint member : members) {
			initializeMember(member);
		}
	}

	protected void initializeMember(Endpoint member) throws SailException {
		if (member.isInitialized()) {
			log.warn("Endpoint " + member.getId() + " was already initialized.");
			return;
		}
		if (member instanceof ResolvableEndpoint) {
			if (this.repositoryResolver != null) {
				((ResolvableEndpoint) member).setRepositoryResolver(this.repositoryResolver);
			}
		}
		try {
			member.init(federationContext);
		} catch (RepositoryException e) {
			log.error("Initialization of endpoint " + member.getId() + " failed: " + e.getMessage());
			throw new SailException(e);
		}
	}

	@Override
	public boolean isWritable() throws SailException {
		// the federation is writable if there is a WriteStrategy defined
		return !(getWriteStrategy() instanceof ReadOnlyWriteStrategy);
	}

	@Override
	public void setDataDir(File dataDir) {
		this.dataDir = dataDir;
	}

	/**
	 * Try to shut down all federation members.
	 *
	 * @throws FedXException if not all members could be shut down
	 */
	@Override
	protected void shutDownInternal() throws SailException {

		List<Exception> errors = new ArrayList<>();
		for (Endpoint member : members) {
			try {
				member.shutDown();
			} catch (Exception e) {
				log.error(ExceptionUtil.getExceptionString("Error shutting down endpoint " + member.getId(), e));
				errors.add(e);
			}
		}

		if (errors.size() > 0) {
			throw new SailException("Federation could not be shut down. See logs for details.");
		}
	}

	/**
	 *
	 * @return an unmodifiable view of the current members
	 */
	public List<Endpoint> getMembers() {
		return Collections.unmodifiableList(members);
	}

	@Override
	public void setRepositoryResolver(RepositoryResolver resolver) {
		this.repositoryResolver = resolver;
	}
}
