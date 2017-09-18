/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.federation.config;

import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.config.RepositoryFactory;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.eclipse.rdf4j.repository.config.RepositoryRegistry;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.config.SailConfigException;
import org.eclipse.rdf4j.sail.config.SailFactory;
import org.eclipse.rdf4j.sail.config.SailImplConfig;
import org.eclipse.rdf4j.sail.federation.Federation;

/**
 * Creates a federation based on its configuration.
 * 
 * @see FederationConfig
 * @author James Leigh
 */
public class FederationFactory implements SailFactory {

	/**
	 * The type of repositories that are created by this factory.
	 * 
	 * @see SailFactory#getSailType()
	 */
	public static final String SAIL_TYPE = "openrdf:Federation";

	/**
	 * Returns the Sail's type: <tt>openrdf:Federation</tt>.
	 */
	public String getSailType() {
		return SAIL_TYPE;
	}

	public SailImplConfig getConfig() {
		return new FederationConfig();
	}

	public Sail getSail(SailImplConfig config)
		throws SailConfigException
	{
		if (!SAIL_TYPE.equals(config.getType())) {
			throw new SailConfigException("Invalid Sail type: " + config.getType());
		}
		assert config instanceof FederationConfig;
		FederationConfig cfg = (FederationConfig)config;
		Federation sail = new Federation();
		for (RepositoryImplConfig member : cfg.getMembers()) {
			RepositoryFactory factory = RepositoryRegistry.getInstance().get(member.getType()).orElseThrow(
					() -> new SailConfigException("Unsupported repository type: " + config.getType()));
			try {
				sail.addMember(factory.getRepository(member));
			}
			catch (RepositoryConfigException e) {
				throw new SailConfigException(e);
			}
		}
		sail.setLocalPropertySpace(cfg.getLocalPropertySpace());
		sail.setDistinct(cfg.isDistinct());
		sail.setReadOnly(cfg.isReadOnly());
		return sail;
	}
}
