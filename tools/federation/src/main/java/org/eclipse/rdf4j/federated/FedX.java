/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.federated.endpoint.Endpoint;
import org.eclipse.rdf4j.federated.exception.ExceptionUtil;
import org.eclipse.rdf4j.federated.exception.FedXException;
import org.eclipse.rdf4j.federated.util.FedXUtil;
import org.eclipse.rdf4j.federated.write.ReadOnlyWriteStrategy;
import org.eclipse.rdf4j.federated.write.RepositoryWriteStrategy;
import org.eclipse.rdf4j.federated.write.WriteStrategy;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FedX serves as implementation of the federation layer. It implements Sesame's Sail interface and can thus be used as
 * a normal repository in a Sesame environment. The federation layer enables transparent access to the underlying
 * members as if they were a central repository.
 * <p>
 * 
 * For initialization of the federation and usage see {@link FederationManager}.
 * 
 * @author Andreas Schwarte
 * 
 */
public class FedX implements Sail {

	private static final Logger log = LoggerFactory.getLogger(FedX.class);

	protected final List<Endpoint> members = new ArrayList<>();
	protected boolean open = false;

	protected FedX() {
		this(null);
	}

	protected FedX(List<Endpoint> endpoints) {
		if (endpoints != null)
			for (Endpoint e : endpoints)
				addMember(e);
		open = true;
	}

	/**
	 * Add a member to the federation (internal)
	 * 
	 * @param endpoint
	 */
	protected void addMember(Endpoint endpoint) {
		members.add(endpoint);
	}

	/**
	 * Remove a member from the federation (internal)
	 * 
	 * @param endpoint
	 * @return whether the member was removed
	 */
	public boolean removeMember(Endpoint endpoint) {
		return members.remove(endpoint);
	}

	/**
	 * Compute and return the {@link WriteStrategy} depending on the current federation configuration.
	 * 
	 * The default implementation uses the {@link RepositoryWriteStrategy} with the first discovered writable
	 * {@link Endpoint}. In none is found, the {@link ReadOnlyWriteStrategy} is used.
	 * 
	 * @return the {@link WriteStrategy}
	 */
	public WriteStrategy getWriteStrategy() {
		for (Endpoint e : members) {
			if (e.isWritable()) {
				return new RepositoryWriteStrategy(e.getRepository());
			}
		}
		return ReadOnlyWriteStrategy.INSTANCE;
	}

	@Override
	public SailConnection getConnection() throws SailException {
		return new FedXConnection(this);
	}

	@Override
	public File getDataDir() {
		throw new UnsupportedOperationException("Operation not supported yet.");
	}

	@Override
	public ValueFactory getValueFactory() {
		return FedXUtil.valueFactory();
	}

	@Override
	public void initialize() throws SailException {
		log.debug("Initializing federation....");
		for (Endpoint member : members) {
			try {
				member.initialize();
			} catch (RepositoryException e) {
				log.error("Initialization of endpoint " + member.getId() + " failed: " + e.getMessage());
				throw new SailException(e);
			}
		}
		open = true;
	}

	@Override
	public boolean isWritable() throws SailException {
		// the federation is writable if there is a WriteStrategy defined
		return !(getWriteStrategy() instanceof ReadOnlyWriteStrategy);
	}

	@Override
	public void setDataDir(File dataDir) {
		throw new UnsupportedOperationException("Operation not supported yet.");
	}

	@Override
	public void shutDown() throws SailException {
		try {
			FederationManager.getInstance().shutDown();
		} catch (FedXException e) {
			throw new SailException(e);
		}
	}

	/**
	 * Try to shut down all federation members.
	 * 
	 * @throws FedXException if not all members could be shut down
	 */
	protected void shutDownInternal() throws FedXException {

		List<Exception> errors = new ArrayList<>();
		for (Endpoint member : members) {
			try {
				member.shutDown();
			} catch (Exception e) {
				log.error(ExceptionUtil.getExceptionString("Error shutting down endpoint " + member.getId(), e));
				errors.add(e);
			}
		}

		if (errors.size() > 0)
			throw new SailException("Federation could not be shut down. See logs for details.");

		open = false;
	}

	public List<Endpoint> getMembers() {
		return new ArrayList<>(members);
	}

	public boolean isOpen() {
		return open;
	}

	@Override
	public IsolationLevel getDefaultIsolationLevel() {
		return IsolationLevels.NONE;
	}

	protected static final List<IsolationLevel> supportedIsolationLevels = new ArrayList<IsolationLevel>(
			Arrays.asList(IsolationLevels.NONE));

	@Override
	public List<IsolationLevel> getSupportedIsolationLevels() {
		return supportedIsolationLevels;
	}
}
