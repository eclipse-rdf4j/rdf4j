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
package org.eclipse.rdf4j.repository.sail.config;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.config.RepositoryFactory;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.StackableSail;
import org.eclipse.rdf4j.sail.config.DelegatingSailImplConfig;
import org.eclipse.rdf4j.sail.config.SailConfigException;
import org.eclipse.rdf4j.sail.config.SailFactory;
import org.eclipse.rdf4j.sail.config.SailImplConfig;
import org.eclipse.rdf4j.sail.config.SailRegistry;

/**
 * A {@link RepositoryFactory} that creates {@link SailRepository}s based on RDF configuration data.
 *
 * @author Arjohn Kampman
 */
public class SailRepositoryFactory implements RepositoryFactory {

	/*-----------*
	 * Constants *
	 *-----------*/

	/**
	 * The type of repositories that are created by this factory.
	 *
	 * @see RepositoryFactory#getRepositoryType()
	 */
	public static final String REPOSITORY_TYPE = "openrdf:SailRepository";

	/*---------*
	 * Methods *
	 *---------*/

	/**
	 * Returns the repository's type: <var>openrdf:SailRepository</var>.
	 */
	@Override
	public String getRepositoryType() {
		return REPOSITORY_TYPE;
	}

	@Override
	public RepositoryImplConfig getConfig() {
		return new SailRepositoryConfig();
	}

	@Override
	public Repository getRepository(RepositoryImplConfig config) throws RepositoryConfigException {
		if (config instanceof SailRepositoryConfig) {
			SailRepositoryConfig sailRepConfig = (SailRepositoryConfig) config;

			try {
				Sail sail = createSailStack(sailRepConfig.getSailImplConfig());
				return new SailRepository(sail);
			} catch (SailConfigException e) {
				throw new RepositoryConfigException(e.getMessage(), e);
			}
		}

		throw new RepositoryConfigException("Invalid configuration class: " + config.getClass());
	}

	private Sail createSailStack(SailImplConfig config) throws RepositoryConfigException, SailConfigException {
		Sail sail = createSail(config);

		if (config instanceof DelegatingSailImplConfig) {
			SailImplConfig delegateConfig = ((DelegatingSailImplConfig) config).getDelegate();
			if (delegateConfig != null) {
				addDelegate(delegateConfig, sail);
			}
		}

		return sail;
	}

	private Sail createSail(SailImplConfig config) throws RepositoryConfigException, SailConfigException {
		SailFactory sailFactory = SailRegistry.getInstance()
				.get(config.getType())
				.orElseThrow(() -> new RepositoryConfigException("Unsupported Sail type: " + config.getType()));
		return sailFactory.getSail(config);
	}

	private void addDelegate(SailImplConfig config, Sail sail) throws RepositoryConfigException, SailConfigException {
		Sail delegateSail = createSailStack(config);

		try {
			((StackableSail) sail).setBaseSail(delegateSail);
		} catch (ClassCastException e) {
			throw new RepositoryConfigException(
					"Delegate configured but " + sail.getClass() + " is not a StackableSail");
		}
	}
}
