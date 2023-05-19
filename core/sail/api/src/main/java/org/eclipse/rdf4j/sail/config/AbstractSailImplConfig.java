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

import static org.eclipse.rdf4j.model.util.Values.bnode;
import static org.eclipse.rdf4j.model.util.Values.literal;
import static org.eclipse.rdf4j.sail.config.SailConfigSchema.CONNECTION_TIME_OUT;
import static org.eclipse.rdf4j.sail.config.SailConfigSchema.ITERATION_CACHE_SYNC_THRESHOLD;
import static org.eclipse.rdf4j.sail.config.SailConfigSchema.SAILTYPE;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.Configurations;
import org.eclipse.rdf4j.model.util.ModelException;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.CONFIG;

/**
 * Base implementation of {@link SailImplConfig}
 *
 * @author Herko ter Horst
 */
public abstract class AbstractSailImplConfig implements SailImplConfig {

	private static final boolean USE_CONFIG = "true"
			.equalsIgnoreCase(System.getProperty("org.eclipse.rdf4j.model.vocabulary.experimental.enableConfig"));

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
		BNode implNode = bnode();

		if (type != null) {
			if (USE_CONFIG) {
				m.add(implNode, CONFIG.Sail.type, literal(type));
			} else {
				m.add(implNode, SAILTYPE, literal(type));
			}
		}

		if (iterationCacheSyncThreshold > 0) {
			if (USE_CONFIG) {
				m.add(implNode, CONFIG.Sail.iterationCacheSyncThreshold, literal(iterationCacheSyncThreshold));
			} else {
				m.add(implNode, ITERATION_CACHE_SYNC_THRESHOLD, literal(iterationCacheSyncThreshold));
			}
		}

		if (connectionTimeOut > 0) {
			if (USE_CONFIG) {
				m.add(implNode, CONFIG.Sail.connectionTimeOut, literal(connectionTimeOut));
			} else {
				m.add(implNode, CONNECTION_TIME_OUT, literal(connectionTimeOut));
			}
		}
		return implNode;
	}

	@Override
	public void parse(Model m, Resource implNode) throws SailConfigException {
		try {
			Configurations.getLiteralValue(m, implNode, CONFIG.Sail.type, SAILTYPE)
					.ifPresent(lit -> setType(lit.getLabel()));
			Configurations
					.getLiteralValue(m, implNode, CONFIG.Sail.iterationCacheSyncThreshold,
							ITERATION_CACHE_SYNC_THRESHOLD)
					.ifPresent(lit -> setIterationCacheSyncThreshold(lit.longValue()));
			Configurations
					.getLiteralValue(m, implNode, CONFIG.Sail.connectionTimeOut, CONNECTION_TIME_OUT)
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
