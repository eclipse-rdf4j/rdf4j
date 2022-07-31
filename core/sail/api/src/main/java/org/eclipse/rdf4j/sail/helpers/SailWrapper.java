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
package org.eclipse.rdf4j.sail.helpers;

import java.io.File;
import java.util.List;

import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolver;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolverClient;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.StackableSail;

/**
 * An implementation of the StackableSail interface that wraps another Sail object and forwards any relevant calls to
 * the wrapped Sail.
 *
 * @author Arjohn Kampman
 */
public class SailWrapper implements StackableSail, FederatedServiceResolverClient {

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * The base Sail for this SailWrapper.
	 */
	private Sail baseSail;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new SailWrapper. The base Sail for the created SailWrapper can be set later using {@link #setBaseSail}.
	 */
	public SailWrapper() {
	}

	/**
	 * Creates a new SailWrapper that wraps the supplied Sail.
	 */
	public SailWrapper(Sail baseSail) {
		setBaseSail(baseSail);
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public void setBaseSail(Sail baseSail) {
		this.baseSail = baseSail;
	}

	@Override
	public Sail getBaseSail() {
		return baseSail;
	}

	protected void verifyBaseSailSet() {
		if (baseSail == null) {
			throw new IllegalStateException("No base Sail has been set");
		}
	}

	@Override
	public void setFederatedServiceResolver(FederatedServiceResolver resolver) {
		if (baseSail instanceof FederatedServiceResolverClient) {
			((FederatedServiceResolverClient) baseSail).setFederatedServiceResolver(resolver);
		}
	}

	@Override
	public File getDataDir() {
		return baseSail.getDataDir();
	}

	@Override
	public void setDataDir(File dataDir) {
		baseSail.setDataDir(dataDir);
	}

	@Override
	public void init() throws SailException {
		verifyBaseSailSet();
		baseSail.init();
	}

	@Override
	public void shutDown() throws SailException {
		verifyBaseSailSet();
		baseSail.shutDown();
	}

	@Override
	public boolean isWritable() throws SailException {
		verifyBaseSailSet();
		return baseSail.isWritable();
	}

	@Override
	public SailConnection getConnection() throws SailException {
		verifyBaseSailSet();
		return baseSail.getConnection();
	}

	@Override
	public ValueFactory getValueFactory() {
		verifyBaseSailSet();
		return baseSail.getValueFactory();
	}

	@Override
	public List<IsolationLevel> getSupportedIsolationLevels() {
		verifyBaseSailSet();
		return baseSail.getSupportedIsolationLevels();
	}

	@Override
	public IsolationLevel getDefaultIsolationLevel() {
		verifyBaseSailSet();
		return baseSail.getDefaultIsolationLevel();
	}

}
