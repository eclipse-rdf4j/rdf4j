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
package org.eclipse.rdf4j.sail.config;

import static org.eclipse.rdf4j.sail.config.SailConfigSchema.SAILTYPE;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelException;
import org.eclipse.rdf4j.model.util.Models;

/**
 * Base implementation of {@link SailImplConfig}
 *
 * @author Herko ter Horst
 */
public abstract class AbstractSailImplConfig implements SailImplConfig {

	private String type;

	private long iterationCacheSyncThreshold;
	private long connectionTimeOut;

	/**
	 * Create a new RepositoryConfigImpl.
	 */
	public AbstractSailImplConfig() {
	}

	/**
	 * Create a new RepositoryConfigImpl.
	 */
	public AbstractSailImplConfig(String type) {
		this();
		setType(type);
	}

	@Override
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@Override
	public void validate() throws SailConfigException {
		if (type == null) {
			throw new SailConfigException("No type specified for sail implementation");
		}
	}

	@Override
	public Resource export(Model m) {
		ValueFactory vf = SimpleValueFactory.getInstance();
		BNode implNode = vf.createBNode();

		m.setNamespace("sail", SailConfigSchema.NAMESPACE);
		if (type != null) {
			m.add(implNode, SAILTYPE, vf.createLiteral(type));
		}

		if (iterationCacheSyncThreshold > 0) {
			m.add(implNode, SailConfigSchema.ITERATION_CACHE_SYNC_THRESHOLD,
					vf.createLiteral(iterationCacheSyncThreshold));
		}

		if (connectionTimeOut > 0) {
			m.add(implNode, SailConfigSchema.CONNECTION_TIME_OUT, vf.createLiteral(connectionTimeOut));
		}
		return implNode;
	}

	@Override
	public void parse(Model m, Resource implNode) throws SailConfigException {
		try {
			Models.objectLiteral(m.getStatements(implNode, SAILTYPE, null)).ifPresent(lit -> setType(lit.getLabel()));
			Models.objectLiteral(m.getStatements(implNode, SailConfigSchema.ITERATION_CACHE_SYNC_THRESHOLD, null))
					.ifPresent(lit -> setIterationCacheSyncThreshold(lit.longValue()));
			Models.objectLiteral(m.getStatements(implNode, SailConfigSchema.CONNECTION_TIME_OUT, null))
					.ifPresent(lit -> setConnectionTimeOut(lit.longValue()));
		} catch (ModelException e) {
			throw new SailConfigException(e.getMessage(), e);
		}
	}

	/**
	 * @return Returns the iterationCacheSize.
	 */
	@Override
	public long getIterationCacheSyncThreshold() {
		return iterationCacheSyncThreshold;
	}

	/**
	 * @param iterationCacheSyncThreshold The iterationCacheSyncThreshold to set.
	 */
	public void setIterationCacheSyncThreshold(long iterationCacheSyncThreshold) {
		this.iterationCacheSyncThreshold = iterationCacheSyncThreshold;
	}

	/**
	 * Get the connection timeout (in ms).
	 *
	 * @return connection timeout (in ms)
	 */
	public long getConnectionTimeOut() {
		return connectionTimeOut;
	}

	/**
	 * Set the connection timeout (in ms).
	 *
	 * @param connectionTimeOut timeout (in ms)
	 */
	public void setConnectionTimeOut(long connectionTimeOut) {
		this.connectionTimeOut = connectionTimeOut;
	}
}
