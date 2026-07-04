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

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.config.RepositoryFactory;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.eclipse.rdf4j.repository.sail.config.SailRepositoryConfig;
import org.eclipse.rdf4j.repository.sail.config.SailRepositoryFactory;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.StackableSail;
import org.eclipse.rdf4j.sail.config.DelegatingSailImplConfig;
import org.eclipse.rdf4j.sail.config.SailConfigException;
import org.eclipse.rdf4j.sail.config.SailFactory;
import org.eclipse.rdf4j.sail.config.SailImplConfig;
import org.eclipse.rdf4j.sail.config.SailRegistry;

/**
 * See {@link SailRepositoryFactory}
 *
 * @author Andreas Schwarte
 *
 */
public class ConfigurableSailRepositoryFactory implements RepositoryFactory {

	public static final String REPOSITORY_TYPE = "openrdf:ConfigurableSailRepository";

	@Override
	public String getRepositoryType() {
		return REPOSITORY_TYPE;
	}

	@Override
	public RepositoryImplConfig getConfig() {
		return new ConfigurableSailRepositoryConfig();
	}

	@Override
	public Repository getRepository(RepositoryImplConfig config) throws RepositoryConfigException {
		if (config instanceof ConfigurableSailRepositoryConfig) {
			ConfigurableSailRepositoryConfig sailRepConfig = (ConfigurableSailRepositoryConfig) config;

			try {
				Sail sail = createSailStack(sailRepConfig.getSailImplConfig());
				return new ConfigurableSailRepository(sail, true);
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

	public static class ConfigurableSailRepositoryConfig extends SailRepositoryConfig {

		public ConfigurableSailRepositoryConfig() {
			super();
			setType(ConfigurableSailRepositoryFactory.REPOSITORY_TYPE);
		}

		public ConfigurableSailRepositoryConfig(SailImplConfig sailImplConfig) {
			super(sailImplConfig);
			setType(ConfigurableSailRepositoryFactory.REPOSITORY_TYPE);
		}

	}

	public static class FailingRepositoryException extends RepositoryException {
		private static final long serialVersionUID = 1L;

		public FailingRepositoryException(String message) {
			super(message);
		}
	}

}
