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
package org.eclipse.rdf4j.federated.repository;

import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.SailException;

/**
 * Specialized {@link SailRepository} that allows configuration of various behaviors, e.g. fail after N operations.
 *
 * @author Andreas Schwarte
 *
 */
public class ConfigurableSailRepository extends SailRepository implements RepositorySettings {
	volatile int failAfter = -1; // fail after x operations, -1 means inactive
	boolean writable;

	/**
	 * A runnable that can be used to simulate latency
	 */
	Runnable latencySimulator = null;

	/**
	 * Counter for operations, only active if {@link #failAfter} is set
	 */
	AtomicInteger operationsCount = new AtomicInteger(0);

	public ConfigurableSailRepository(Sail sail, boolean writable) {
		super(sail);
		this.writable = writable;
	}

	/**
	 * @param nOperations fail after nOperations, -1 to deactivate
	 */
	@Override
	public void setFailAfter(int nOperations) {
		this.failAfter = nOperations;
	}

	@Override
	public void setWritable(boolean flag) {
		this.writable = flag;
	}

	@Override
	public void resetOperationsCounter() {
		this.operationsCount.set(0);
	}

	@Override
	public boolean isWritable() throws RepositoryException {
		return writable && super.isWritable();
	}

	@Override
	public SailRepositoryConnection getConnection()
			throws RepositoryException {
		try {
			return new ConfigurableSailRepositoryConnection(this, getSail().getConnection());
		} catch (SailException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void setLatencySimulator(Runnable runnable) {
		this.latencySimulator = runnable;
	}
}
