/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.config;

import static org.eclipse.rdf4j.sail.config.SailConfigSchema.SAILTYPE;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.Graph;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.GraphUtil;
import org.eclipse.rdf4j.model.util.GraphUtilException;
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

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public void validate()
		throws SailConfigException
	{
		if (type == null) {
			throw new SailConfigException("No type specified for sail implementation");
		}
	}

	public Resource export(Model m) {
		ValueFactory vf = SimpleValueFactory.getInstance();
		BNode implNode = vf.createBNode();

		if (type != null) {
			m.add(implNode, SAILTYPE, vf.createLiteral(type));
		}

		if (iterationCacheSyncThreshold > 0) {
			m.add(implNode, SailConfigSchema.ITERATION_CACHE_SYNC_THRESHOLD,
					vf.createLiteral(iterationCacheSyncThreshold));
		}

		return implNode;
	}

	public void parse(Model m, Resource implNode)
		throws SailConfigException
	{
		try {
			Models.objectLiteral(m.filter(implNode, SAILTYPE, null)).ifPresent(lit -> setType(lit.getLabel()));
			Models.objectLiteral(
					m.filter(implNode, SailConfigSchema.ITERATION_CACHE_SYNC_THRESHOLD, null)).ifPresent(
							lit -> setIterationCacheSyncThreshold(lit.longValue()));
		}
		catch (ModelException e) {
			throw new SailConfigException(e.getMessage(), e);
		}
	}

	/**
	 * @return Returns the iterationCacheSize.
	 */
	public long getIterationCacheSyncThreshold() {
		return iterationCacheSyncThreshold;
	}

	/**
	 * @param iterationCacheSyncThreshold
	 *        The iterationCacheSyncThreshold to set.
	 */
	public void setIterationCacheSyncThreshold(long iterationCacheSyncThreshold) {
		this.iterationCacheSyncThreshold = iterationCacheSyncThreshold;
	}
}
